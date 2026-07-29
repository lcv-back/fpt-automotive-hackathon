package com.sopa.viva_automotive.vehicleservice.impl

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.sopa.viva_automotive.core.common.coroutines.IoDispatcher
import com.sopa.viva_automotive.vehicleservice.api.CarPropertyResult
import com.sopa.viva_automotive.vehicleservice.api.PropertyStatus
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class RealVehicleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VehicleRepository {

    private val propertyManager = MutableStateFlow<CarPropertyManager?>(null)

    init {
        Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER) { car, ready ->
            propertyManager.value = if (ready) {
                car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            } else {
                null
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeProperty(propertyId: Int): Flow<CarPropertyResult> =
        propertyManager.filterNotNull().flatMapLatest { manager ->
            callbackFlow {
                val permission = requiredPermission(propertyId)
                if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Cannot observe property $propertyId: missing $permission")
                    trySend(errorResult(propertyId))
                    awaitClose()
                    return@callbackFlow
                }

                val callback = object : CarPropertyManager.CarPropertyEventCallback {
                    override fun onChangeEvent(value: CarPropertyValue<*>) {
                        trySend(value.toResult())
                    }

                    override fun onErrorEvent(propId: Int, areaId: Int) {
                        trySend(errorResult(propId, areaId))
                    }
                }
                val registered = runCatching {
                    manager.registerCallback(
                        callback,
                        propertyId,
                        CarPropertyManager.SENSOR_RATE_ONCHANGE,
                    )
                }
                registered.exceptionOrNull()?.let { error ->
                    Log.w(TAG, "registerCallback failed for $propertyId", error)
                    trySend(errorResult(propertyId))
                }
                awaitClose {
                    if (registered.isSuccess) {
                        runCatching { manager.unregisterCallback(callback, propertyId) }
                    }
                }
            }
        }

    override suspend fun getProperty(propertyId: Int, areaId: Int): Result<CarPropertyResult> =
        withContext(ioDispatcher) {
            runCatching {
                val manager = awaitManager()
                val resolved = resolveAreaIds(manager, propertyId, areaId).first()
                manager.getProperty<Any>(propertyId, resolved).toResult()
            }.withPermissionHint(propertyId)
        }

    override suspend fun setProperty(propertyId: Int, areaId: Int, value: Any): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val manager = awaitManager()
                resolveAreaIds(manager, propertyId, areaId).forEach { resolved ->
                    when (value) {
                        is Boolean -> manager.setBooleanProperty(propertyId, resolved, value)
                        is Int -> manager.setIntProperty(propertyId, resolved, value)
                        is Float -> manager.setFloatProperty(propertyId, resolved, value)
                        is Double -> manager.setFloatProperty(propertyId, resolved, value.toFloat())
                        else -> error("Unsupported property value type: ${value.javaClass}")
                    }
                }
            }.withPermissionHint(propertyId)
        }

        private fun resolveAreaIds(
        manager: CarPropertyManager,
        propertyId: Int,
        requestedAreaId: Int,
    ): List<Int> {
        val declared = declaredAreaIds(manager, propertyId)
        val resolved = AreaIdResolver.resolve(declared, requestedAreaId)
        if (declared != null && resolved == listOf(requestedAreaId) && requestedAreaId !in declared) {
            Log.w(
                TAG,
                "No declared area overlaps 0x${requestedAreaId.toString(16)} " +
                    "for property $propertyId (declared: ${declared.joinToString()})",
            )
        }
        return resolved
    }

    private fun declaredAreaIds(manager: CarPropertyManager, propertyId: Int): IntArray? =
        areaIdCache.getOrPut(propertyId) {
            runCatching { manager.getCarPropertyConfig(propertyId)?.areaIds }
                .getOrNull()
                ?: EMPTY_AREA_IDS
        }.takeIf { it !== EMPTY_AREA_IDS && it.isNotEmpty() }

    private val areaIdCache = ConcurrentHashMap<Int, IntArray>()

        private suspend fun awaitManager(): CarPropertyManager =
        withTimeoutOrNull(CAR_CONNECT_TIMEOUT_MS) { propertyManager.filterNotNull().first() }
            ?: throw IllegalStateException(
                "Car service not connected after ${CAR_CONNECT_TIMEOUT_MS}ms",
            )

        private fun <T> Result<T>.withPermissionHint(propertyId: Int): Result<T> {
        val error = exceptionOrNull()
        if (error is SecurityException) {
            val permission = requiredPermission(propertyId)
            return Result.failure(
                SecurityException(
                    "Missing $permission - privileged car permissions must be " +
                        "OEM-allowlisted and the app installed as privileged/platform-signed",
                    error,
                ),
            )
        }
        return this
    }

    private fun requiredPermission(propertyId: Int): String = when (propertyId) {
        VehicleProperties.HVAC_FAN_SPEED,
        VehicleProperties.HVAC_FAN_DIRECTION,
        VehicleProperties.HVAC_TEMPERATURE_CURRENT,
        VehicleProperties.HVAC_TEMPERATURE_SET,
        VehicleProperties.HVAC_AC_ON,
        VehicleProperties.HVAC_AUTO_ON,
        VehicleProperties.HVAC_POWER_ON,
        -> Car.PERMISSION_CONTROL_CAR_CLIMATE

        VehicleProperties.DOOR_LOCK,
        VehicleProperties.DOOR_POS,
        -> Car.PERMISSION_CONTROL_CAR_DOORS

        VehicleProperties.CABIN_LIGHTS_STATE -> Car.PERMISSION_READ_INTERIOR_LIGHTS
        VehicleProperties.CABIN_LIGHTS_SWITCH -> Car.PERMISSION_CONTROL_INTERIOR_LIGHTS
        VehicleProperties.PERF_VEHICLE_SPEED -> Car.PERMISSION_SPEED
        VehicleProperties.FUEL_LEVEL, VehicleProperties.EV_BATTERY_LEVEL -> Car.PERMISSION_ENERGY
        VehicleProperties.IGNITION_STATE -> Car.PERMISSION_POWERTRAIN
        else -> Car.PERMISSION_CAR_INFO
    }

    private fun errorResult(propertyId: Int, areaId: Int = 0) = CarPropertyResult(
        propertyId = propertyId,
        areaId = areaId,
        value = 0,
        status = PropertyStatus.ERROR,
    )

    private fun CarPropertyValue<*>.toResult() = CarPropertyResult(
        propertyId = propertyId,
        areaId = areaId,
        value = value as Any,
        timestampNanos = timestamp,
        status = when (status) {
            CarPropertyValue.STATUS_AVAILABLE -> PropertyStatus.AVAILABLE
            CarPropertyValue.STATUS_UNAVAILABLE -> PropertyStatus.UNAVAILABLE
            else -> PropertyStatus.ERROR
        },
    )

    private companion object {
        const val TAG = "RealVehicleRepo"
        const val CAR_CONNECT_TIMEOUT_MS = 5_000L

                val EMPTY_AREA_IDS = IntArray(0)
    }
}
