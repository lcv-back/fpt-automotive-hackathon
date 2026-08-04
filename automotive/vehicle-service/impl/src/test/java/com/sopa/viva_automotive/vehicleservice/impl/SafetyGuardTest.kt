package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.SafetyRules
import com.sopa.viva_automotive.vehicleservice.api.VehicleAreas
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleSafetyState
import com.sopa.viva_automotive.vehicleservice.api.VehicleWriteRequest
import com.sopa.viva_automotive.vehicleservice.api.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mỗi luật một test case — `03-contracts.md` §4 yêu cầu đúng như vậy
 * (*"mỗi luật phải có test case"*).
 */
class SafetyGuardTest {

    private val guard = DefaultSafetyGuard()

    private fun unlockDoor(confidence: Float? = null) = VehicleWriteRequest(
        propertyId = VehicleProperties.DOOR_LOCK,
        areaId = VehicleAreas.DOOR_ROW_1_LEFT,
        value = false,
        confidence = confidence,
    )

    private fun lockDoor() = VehicleWriteRequest(
        propertyId = VehicleProperties.DOOR_LOCK,
        areaId = VehicleAreas.DOOR_ROW_1_LEFT,
        value = true,
    )

    private fun setTemp(celsius: Double) = VehicleWriteRequest(
        propertyId = VehicleProperties.HVAC_TEMPERATURE_SET,
        areaId = VehicleAreas.SEAT_ZONE_DRIVER,
        value = celsius,
    )

    private fun moving(kmh: Float) = VehicleSafetyState(speedKmh = kmh)
    private val stopped = VehicleSafetyState(speedKmh = 0f)

    @Test
    fun `G1_SPEED_LOCK chan mo cua khi xe dang chay`() {
        val verdict = guard.evaluate(unlockDoor(), moving(60f))

        assertTrue("$verdict", verdict is Verdict.Deny)
        assertEquals(SafetyRules.SPEED_LOCK, (verdict as Verdict.Deny).rule)
        assertTrue(verdict.reasonVi, verdict.reasonVi.contains("Xe đang chạy"))
    }

    @Test
    fun `khoa cua khi dang chay thi khong bi chan`() {
        // Khóa cửa lúc xe chạy là hành động an toàn. Một guard chặn cả chiều
        // này là guard sai, không phải guard chặt.
        assertEquals(Verdict.Allow, guard.evaluate(lockDoor(), moving(60f)))
    }

    @Test
    fun `duoi nguong toc do thi hoi xac nhan chu khong tu mo`() {
        val verdict = guard.evaluate(unlockDoor(), stopped)

        assertTrue("$verdict", verdict is Verdict.Confirm)
        assertEquals(SafetyRules.CONFIRM_DOOR, (verdict as Verdict.Confirm).rule)
    }

    @Test
    fun `G1_GEAR_LOCK chi kich hoat khi doc duoc so`() {
        val notParked = VehicleSafetyState(speedKmh = 0f, gear = "D")
        val denied = guard.evaluate(unlockDoor(), notParked)
        assertEquals(SafetyRules.GEAR_LOCK, (denied as Verdict.Deny).rule)

        // Khong doc duoc so -> luat im lang, roi xuong buoc hoi xac nhan.
        // Thieu du lieu thi khong doan, xem ghi chu dau DefaultSafetyGuard.
        val unknownGear = guard.evaluate(unlockDoor(), VehicleSafetyState(speedKmh = 0f, gear = null))
        assertTrue("$unknownGear", unknownGear is Verdict.Confirm)
    }

    @Test
    fun `G3_LOW_CONFIDENCE hoi lai khi nghe khong ro`() {
        val verdict = guard.evaluate(unlockDoor(confidence = 0.4f), stopped)

        assertTrue("$verdict", verdict is Verdict.Confirm)
        assertEquals(SafetyRules.LOW_CONFIDENCE, (verdict as Verdict.Confirm).rule)
    }

    @Test
    fun `lenh nguy hiem VA nghe khong ro thi bi TU CHOI, khong phai hoi lai`() {
        // Thu tu luat co y nghia: hoi lai mot lenh dang nao cung bi tu choi chi
        // lam tai xe mat them mot luot noi trong luc dang lai.
        val verdict = guard.evaluate(unlockDoor(confidence = 0.2f), moving(60f))

        assertEquals(SafetyRules.SPEED_LOCK, (verdict as Verdict.Deny).rule)
    }

    @Test
    fun `cham tay tren HMI khong bi luat do tin cay dung toi`() {
        // confidence = null nghia la nguoi dung bam nut, khong co gi de nghi ngo
        // ve chuyen "may nghe nham".
        assertTrue(guard.evaluate(unlockDoor(confidence = null), stopped) is Verdict.Confirm)
    }

    @Test
    fun `G3_VALUE_RANGE chan nhiet do ngoai dai 16-32`() {
        assertEquals(SafetyRules.VALUE_RANGE, (guard.evaluate(setTemp(40.0), stopped) as Verdict.Deny).rule)
        assertEquals(SafetyRules.VALUE_RANGE, (guard.evaluate(setTemp(5.0), stopped) as Verdict.Deny).rule)
        assertEquals(SafetyRules.VALUE_RANGE, (guard.evaluate(setTemp(Double.NaN), stopped) as Verdict.Deny).rule)
    }

    @Test
    fun `nhiet do trong dai thi cho qua, ke ca khi xe dang chay`() {
        assertEquals(Verdict.Allow, guard.evaluate(setTemp(22.0), moving(80f)))
        assertEquals(Verdict.Allow, guard.evaluate(setTemp(16.0), stopped))
        assertEquals(Verdict.Allow, guard.evaluate(setTemp(32.0), stopped))
    }

    @Test
    fun `property khong nam trong pham vi luat thi di thang`() {
        val volume = VehicleWriteRequest(
            propertyId = VehicleProperties.CABIN_LIGHTS_SWITCH,
            areaId = VehicleAreas.GLOBAL,
            value = 1,
        )
        assertEquals(Verdict.Allow, guard.evaluate(volume, moving(60f)))
    }
}
