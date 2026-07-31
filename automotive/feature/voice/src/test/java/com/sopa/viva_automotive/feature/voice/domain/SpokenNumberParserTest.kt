package com.sopa.viva_automotive.feature.voice.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpokenNumberParserTest {

    @Test
    fun `parses digits`() {
        assertEquals(22.0, SpokenNumberParser.parse("set temperature to 22 degrees")!!, 0.0)
        assertEquals(19.5, SpokenNumberParser.parse("set it to 19.5")!!, 0.0)
    }

    @Test
    fun `parses simple number words`() {
        assertEquals(5.0, SpokenNumberParser.parse("set fan speed to five")!!, 0.0)
        assertEquals(16.0, SpokenNumberParser.parse("sixteen degrees please")!!, 0.0)
    }

    @Test
    fun `parses compound number words`() {
        assertEquals(22.0, SpokenNumberParser.parse("temperature to twenty two")!!, 0.0)
        assertEquals(21.0, SpokenNumberParser.parse("twenty-one degrees")!!, 0.0)
        assertEquals(30.0, SpokenNumberParser.parse("set it to thirty")!!, 0.0)
    }

    @Test
    fun `parses half degrees`() {
        assertEquals(22.5, SpokenNumberParser.parse("twenty two point five degrees")!!, 0.0)
        assertEquals(21.5, SpokenNumberParser.parse("twenty one and a half degrees")!!, 0.0)
    }

    @Test
    fun `parses vietnamese number words`() {
        assertEquals(22.0, SpokenNumberParser.parse("đặt nhiệt độ hai mươi hai")!!, 0.0)
        assertEquals(16.0, SpokenNumberParser.parse("mười sáu độ")!!, 0.0)
        assertEquals(5.0, SpokenNumberParser.parse("quạt mức năm")!!, 0.0)
        assertEquals(20.0, SpokenNumberParser.parse("hai mươi")!!, 0.0)
    }

    @Test
    fun `returns null when no number present`() {
        assertNull(SpokenNumberParser.parse("turn on the air conditioning"))
        assertNull(SpokenNumberParser.parse(""))
    }
}
