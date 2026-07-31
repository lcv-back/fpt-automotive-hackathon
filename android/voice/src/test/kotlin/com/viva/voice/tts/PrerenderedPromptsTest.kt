package com.viva.voice.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrerenderedPromptsTest {

    @Test
    fun `catalog contains at least thirty unique fallback clips`() {
        assertTrue(PrerenderedPrompts.all.size >= 30)
        assertEquals(
            PrerenderedPrompts.all.size,
            PrerenderedPrompts.all.map { it.rawName }.distinct().size,
        )
    }

    @Test
    fun `temperature response describes a target instead of achieved cabin temperature`() {
        assertEquals(
            "tts_temp_24",
            PrerenderedPrompts.rawNameFor("Đã đặt nhiệt độ mục tiêu 24°C"),
        )
    }

    @Test
    fun `fan fallback follows the zero through five contract`() {
        assertEquals("tts_fan_0", PrerenderedPrompts.rawNameFor("Đã đặt quạt mức 0."))
        assertEquals("tts_fan_5", PrerenderedPrompts.rawNameFor("Đã đặt quạt mức 5."))
        assertNull(PrerenderedPrompts.rawNameFor("Đã đặt quạt mức 6."))
    }

    @Test
    fun `unknown dynamic text does not play an unrelated clip`() {
        assertNull(PrerenderedPrompts.rawNameFor("Hôm nay trời đẹp."))
    }
}
