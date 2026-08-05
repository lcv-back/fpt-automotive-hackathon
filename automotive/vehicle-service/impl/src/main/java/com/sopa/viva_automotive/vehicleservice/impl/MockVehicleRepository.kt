package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.core.common.coroutines.ApplicationScope
import com.sopa.viva_automotive.vehicleservice.api.CarPropertyResult
import com.sopa.viva_automotive.vehicleservice.api.FanDirection
import com.sopa.viva_automotive.vehicleservice.api.LightSwitch
import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleWriteContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class MockVehicleRepository @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) : VehicleRepository {

    constructor(scope: CoroutineScope, simulate: Boolean) : this(scope) {
        this.simulate = simulate
    }

    private var simulate: Boolean = true
    private val simulationStarted = AtomicBoolean(false)

    private data class Key(val propertyId: Int, val areaId: Int)

    private val state = MutableStateFlow(defaultState())

    /**
     * Properties the tester has set by hand through `VSTATE`; the simulator must
     * leave them alone from then on.
     *
     * Vì sao cần: [simulateTick] quét tốc độ theo hình sin 0→90 km/h mỗi 90 giây.
     * Không có cơ chế ghim thì `adb shell am broadcast … unit_type speed
     * state_value 60` chỉ sống đúng **một tick**, và kịch bản *"mở cửa lúc xe
     * đang chạy"* lúc bị `G1_SPEED_LOCK` chặn, lúc lại được cho qua vì rơi đúng
     * đáy hình sin. Bảng ablation A1 dựng trên nền đó không tái lập được — mà
     * tái lập được chính là thứ ô *Demo live và độ ổn định* chấm.
     */
    private val pinned = java.util.concurrent.ConcurrentHashMap.newKeySet<Key>()

    override fun observeProperty(propertyId: Int): Flow<CarPropertyResult> = channelFlow {
        startSimulationIfNeeded()
        val lastSent = mutableMapOf<Int, Any>()
        state.collect { snapshot ->
            snapshot.forEach { (key, result) ->
                if (key.propertyId == propertyId && lastSent[key.areaId] != result.value) {
                    lastSent[key.areaId] = result.value
                    send(result)
                }
            }
        }
    }

    override suspend fun getProperty(propertyId: Int, areaId: Int): Result<CarPropertyResult> =
        state.value[Key(propertyId, areaId)]
            ?.let { Result.success(it) }
            ?: Result.failure(
                IllegalArgumentException("No mock value for property=$propertyId area=$areaId"),
            )

    override suspend fun setProperty(
        propertyId: Int,
        areaId: Int,
        value: Any,
        context: VehicleWriteContext,
    ): Result<Unit> {
        val key = Key(propertyId, areaId)
        if (key !in state.value) {
            return Result.failure(
                IllegalArgumentException("No mock value for property=$propertyId area=$areaId"),
            )
        }
        write(propertyId, areaId, value)
        return Result.success(Unit)
    }

    /**
     * Đặt giá trị thay cho "phía xe" và **ghim** nó: đây là người kiểm thử nói
     * *"xe đang chạy 60 km/h"*, không phải một mẫu đo thoáng qua.
     *
     * Ghim theo từng property, nên bơm tốc độ không làm đứng phần mô phỏng
     * nhiệt độ. Ghim sống tới khi process chết — khởi động lại app là về mặc
     * định. Không có lệnh gỡ ghim vì kịch bản dùng nó (A1 · E09) cần giá trị
     * đứng yên, không cần quay lại chế độ dao động.
     */
    fun injectVehicleEvent(propertyId: Int, areaId: Int, value: Any) {
        pinned += Key(propertyId, areaId)
        write(propertyId, areaId, value)
    }

    /**
     * Ghi từ vòng mô phỏng — bỏ qua property đã bị ghim bằng tay.
     *
     * Tách khỏi [write] có chủ đích: lệnh thật từ app ([setProperty]) vẫn phải
     * ghi được lên property đã ghim. Ghim chỉ chặn **vòng mô phỏng**, không
     * chặn xe làm việc của nó — nếu không, ghim tốc độ xong thì khoá cửa cũng
     * hỏng theo.
     */
    private fun simulatedWrite(propertyId: Int, areaId: Int, value: Any) {
        if (Key(propertyId, areaId) in pinned) return
        write(propertyId, areaId, value)
    }

    private fun write(propertyId: Int, areaId: Int, value: Any) {
        state.update { current ->
            current + (
                Key(propertyId, areaId) to CarPropertyResult(
                    propertyId = propertyId,
                    areaId = areaId,
                    value = value,
                    timestampNanos = System.nanoTime(),
                )
                )
        }
    }

    private fun startSimulationIfNeeded() {
        if (!simulate || !simulationStarted.compareAndSet(false, true)) return
        scope.launch {
            var tick = 0
            while (true) {
                delay(1_000)
                tick++
                simulateTick(tick)
            }
        }
    }

    private fun simulateTick(tick: Int) {
        val speed = (12.5f * (1 + sin(tick * 2 * PI / 90))).toFloat()
        simulatedWrite(VehicleProperties.PERF_VEHICLE_SPEED, VehicleAreas.GLOBAL, speed)

        val current = floatAt(VehicleProperties.HVAC_TEMPERATURE_CURRENT, VehicleAreas.SEAT_ZONE_DRIVER)
        val target = floatAt(VehicleProperties.HVAC_TEMPERATURE_SET, VehicleAreas.SEAT_ZONE_DRIVER)
        if (abs(current - target) > 0.05f) {
            val next = current + (target - current).coerceIn(-0.2f, 0.2f)
            simulatedWrite(VehicleProperties.HVAC_TEMPERATURE_CURRENT, VehicleAreas.SEAT_ZONE_DRIVER, next)
        }

        if (tick % 30 == 0) {
            simulatedWrite(
                VehicleProperties.FUEL_LEVEL,
                VehicleAreas.GLOBAL,
                (floatAt(VehicleProperties.FUEL_LEVEL, VehicleAreas.GLOBAL) - 0.1f).coerceAtLeast(0f),
            )
            simulatedWrite(
                VehicleProperties.EV_BATTERY_LEVEL,
                VehicleAreas.GLOBAL,
                (floatAt(VehicleProperties.EV_BATTERY_LEVEL, VehicleAreas.GLOBAL) - 0.1f).coerceAtLeast(0f),
            )
        }
    }

    private fun floatAt(propertyId: Int, areaId: Int): Float =
        state.value[Key(propertyId, areaId)]?.floatValue() ?: 0f

    private companion object {
        fun defaultState(): Map<Key, CarPropertyResult> = buildMap {
            fun put(propertyId: Int, areaId: Int, value: Any) {
                put(
                    Key(propertyId, areaId),
                    CarPropertyResult(propertyId, areaId, value, timestampNanos = System.nanoTime()),
                )
            }
            put(VehicleProperties.PERF_VEHICLE_SPEED, VehicleAreas.GLOBAL, 0f)
            put(VehicleProperties.FUEL_LEVEL, VehicleAreas.GLOBAL, 68f)
            put(VehicleProperties.EV_BATTERY_LEVEL, VehicleAreas.GLOBAL, 81f)
            put(VehicleProperties.IGNITION_STATE, VehicleAreas.GLOBAL, 4) // ON
            put(VehicleProperties.DOOR_LOCK, VehicleAreas.DOOR_ROW_1_LEFT, true)
            put(VehicleProperties.DOOR_POS, VehicleAreas.DOOR_ROW_1_LEFT, 0) // closed
            put(VehicleProperties.CABIN_LIGHTS_SWITCH, VehicleAreas.GLOBAL, LightSwitch.OFF)
            put(VehicleProperties.HVAC_POWER_ON, VehicleAreas.GLOBAL, true)
            put(VehicleProperties.HVAC_AC_ON, VehicleAreas.GLOBAL, true)
            put(VehicleProperties.HVAC_AUTO_ON, VehicleAreas.GLOBAL, false)
            put(VehicleProperties.HVAC_FAN_SPEED, VehicleAreas.GLOBAL, 3)
            put(VehicleProperties.HVAC_FAN_DIRECTION, VehicleAreas.GLOBAL, FanDirection.FACE)
            put(VehicleProperties.HVAC_TEMPERATURE_SET, VehicleAreas.SEAT_ZONE_DRIVER, 22f)
            put(VehicleProperties.HVAC_TEMPERATURE_SET, VehicleAreas.SEAT_ZONE_PASSENGER, 22f)
            put(VehicleProperties.HVAC_TEMPERATURE_CURRENT, VehicleAreas.SEAT_ZONE_DRIVER, 26f)
        }
    }
}
