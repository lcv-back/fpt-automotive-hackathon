package com.sopa.viva_automotive.core.common.units

import kotlin.math.roundToInt

object TemperatureUnits {

        const val MIN_CELSIUS = 16f
    const val MAX_CELSIUS = 30f

    fun celsiusToFahrenheit(celsius: Float): Float = celsius * 9f / 5f + 32f

    fun fahrenheitToCelsius(fahrenheit: Float): Float = (fahrenheit - 32f) * 5f / 9f

        fun format(celsius: Float, useFahrenheit: Boolean): String {
        return if (useFahrenheit) {
            String.format(java.util.Locale.US, "%.1f\u00B0F", celsiusToFahrenheit(celsius))
        } else {
            String.format(java.util.Locale.US, "%.1f\u00B0C", celsius)
        }
    }

        fun stepDeltaCelsius(useFahrenheit: Boolean): Float =
        if (useFahrenheit) fahrenheitToCelsius(1f) - fahrenheitToCelsius(0f) else 0.5f

        fun snapCelsius(celsius: Float): Float = (celsius * 2f).roundToInt() / 2f
}
