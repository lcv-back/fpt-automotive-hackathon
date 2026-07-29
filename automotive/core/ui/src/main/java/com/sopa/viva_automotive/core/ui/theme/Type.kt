package com.sopa.viva_automotive.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val VivaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 56.sp, // ~1.0 for large headers
        letterSpacing = (-0.03).em,
        fontFeatureSettings = "tnum",
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 48.sp, // ~1.1
        letterSpacing = (-0.03).em,
        fontFeatureSettings = "tnum",
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSansDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 40.sp, // ~1.1
        letterSpacing = (-0.03).em,
        fontFeatureSettings = "tnum",
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp, // ~1.1
        letterSpacing = (-0.03).em,
        fontFeatureSettings = "tnum",
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp, // ~1.3
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSansDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp, // ~1.3
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp, // ~1.3
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp, // ~1.3
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp, // ~1.3, even+even
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansText,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 27.sp, // 17 × 1.6
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansText,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp, // 16 × 1.6 → 26 (even)
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansText,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp, // 14 × 1.6 → 22 (even)
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansText,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp, // ideal button size
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansText,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
        labelSmall = TextStyle(
        fontFamily = GoogleSansText,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp, // button safe floor
        lineHeight = 20.sp,
        letterSpacing = 0.05.em,
    ),
)
