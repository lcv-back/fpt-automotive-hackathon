package com.sopa.viva_automotive.core.common.units

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureUnitsTest {

    @Test
    fun `celsius to fahrenheit`() {
        assertEquals(32f, TemperatureUnits.celsiusToFahrenheit(0f), 0.01f)
        assertEquals(68f, TemperatureUnits.celsiusToFahrenheit(20f), 0.01f)
        assertEquals(62.6f, TemperatureUnits.celsiusToFahrenheit(17f), 0.01f)
    }

    @Test
    fun `fahrenheit to celsius`() {
        assertEquals(0f, TemperatureUnits.fahrenheitToCelsius(32f), 0.01f)
        assertEquals(20f, TemperatureUnits.fahrenheitToCelsius(68f), 0.01f)
    }

    @Test
    fun `round trip preserves value`() {
        val c = 22.5f
        val back = TemperatureUnits.fahrenheitToCelsius(TemperatureUnits.celsiusToFahrenheit(c))
        assertEquals(c, back, 0.01f)
    }

    @Test
    fun `format includes unit suffix`() {
        assertEquals("17.0°C", TemperatureUnits.format(17f, useFahrenheit = false))
        assertEquals("62.6°F", TemperatureUnits.format(17f, useFahrenheit = true))
    }

    @Test
    fun `fahrenheit step is about one degree F in celsius`() {
        val delta = TemperatureUnits.stepDeltaCelsius(useFahrenheit = true)
        assertEquals(5f / 9f, delta, 0.001f)
    }
}
