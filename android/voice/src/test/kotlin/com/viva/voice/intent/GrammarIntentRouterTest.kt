package com.viva.voice.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarIntentRouterTest {

    private val router = GrammarIntentRouter()

    @Test
    fun `wake phrase plus explicit temperature becomes hvac command`() {
        val result = router.route("Viva ơi, hạ nhiệt độ điều hòa xuống 24 độ C")

        assertTrue(result is RouteResult.Matched)
        val intent = (result as RouteResult.Matched).intent
        assertEquals("hvac_set_temp", intent.name)
        assertEquals(24f, intent.slots["value"])
        assertEquals(Intent.Tier.T0, intent.tier)
    }

    @Test
    fun `vivi wake phrase is accepted as product alias`() {
        val result = router.route("Vivi ơi quạt mức 2") as RouteResult.Matched

        assertEquals("hvac_set_fan", result.intent.name)
        assertEquals(2, result.intent.slots["level"])
    }

    @Test
    fun `cold complaint asks to raise temperature instead of doing the opposite`() {
        val result = router.route("lạnh quá")

        assertTrue(result is RouteResult.NeedsClarification)
        assertEquals(
            "Bạn muốn tăng nhiệt độ điều hòa lên bao nhiêu độ?",
            (result as RouteResult.NeedsClarification).promptVi,
        )
    }

    @Test
    fun `relative temperature command without target asks for a value`() {
        val result = router.route("giảm nhiệt độ điều hòa")

        assertTrue(result is RouteResult.NeedsClarification)
    }

    @Test
    fun `temperature outside cabin range is rejected before execution`() {
        val result = router.route("đặt điều hòa xuống 8 độ")

        assertTrue(result is RouteResult.NeedsClarification)
        assertEquals(
            "Nhiệt độ hỗ trợ từ 16 đến 32 độ C. Bạn muốn đặt bao nhiêu độ?",
            (result as RouteResult.NeedsClarification).promptVi,
        )
    }

    @Test
    fun `real DBC upper bounds are accepted`() {
        val temperature = router.route("đặt điều hòa 32 độ") as RouteResult.Matched
        val fan = router.route("quạt mức 5") as RouteResult.Matched

        assertEquals(32f, temperature.intent.slots["value"])
        assertEquals(5, fan.intent.slots["level"])
    }

    @Test
    fun `five backbone command families are recognized`() {
        val cases = mapOf(
            "hạ điều hòa xuống 24 độ" to "hvac_set_temp",
            "quạt mức 3" to "hvac_set_fan",
            "khóa cửa" to "door_lock",
            "tăng âm lượng" to "volume_adjust",
            "chuyển bài" to "media_next",
        )

        cases.forEach { (text, expectedIntent) ->
            val result = router.route(text) as RouteResult.Matched
            assertEquals(expectedIntent, result.intent.name)
        }
    }

    @Test
    fun `unsupported wake phrase is not treated as part of the product command`() {
        val result = router.route("Siri ơi, hạ điều hòa xuống 24 độ")

        assertTrue(result is RouteResult.Unsupported)
        assertEquals(false, (result as RouteResult.Unsupported).canFallback)
    }

    @Test
    fun `ac power phrase remains available to the app fallback router`() {
        val result = router.route("bật điều hòa")

        assertTrue(result is RouteResult.Unsupported)
        assertEquals(true, (result as RouteResult.Unsupported).canFallback)
    }
}
