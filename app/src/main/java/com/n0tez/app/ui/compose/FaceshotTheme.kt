package com.n0tez.app.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkPalette = darkColorScheme(
    primary = Color(0xFF8AE7FF),
    onPrimary = Color(0xFF031720),
    primaryContainer = Color(0xFF123447),
    onPrimaryContainer = Color(0xFFD6F7FF),
    secondary = Color(0xFFB3B7FF),
    onSecondary = Color(0xFF1A214B),
    secondaryContainer = Color(0xFF272F65),
    onSecondaryContainer = Color(0xFFE3E6FF),
    tertiary = Color(0xFF7FF4D5),
    onTertiary = Color(0xFF032118),
    tertiaryContainer = Color(0xFF0E3E33),
    onTertiaryContainer = Color(0xFFD8FFF3),
    background = Color(0xFF030711),
    onBackground = Color(0xFFF5F7FC),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFF5F7FC),
    surfaceVariant = Color(0xFF151F32),
    onSurfaceVariant = Color(0xFF9DABC2),
    outline = Color(0xFF3A4F69),
    outlineVariant = Color(0xFF253449),
    inverseSurface = Color(0xFFE9EDF5),
    inverseOnSurface = Color(0xFF101723),
    error = Color(0xFFFF8BA7),
    onError = Color(0xFF350913),
    errorContainer = Color(0xFF6B1730),
    onErrorContainer = Color(0xFFFFD9E3),
    scrim = Color(0xE6000106),
)

private val LightPalette = lightColorScheme(
    primary = Color(0xFF005A75),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC1F0FF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF4653B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E4FF),
    onSecondaryContainer = Color(0xFF171D4C),
    tertiary = Color(0xFF006B58),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF8EF8DB),
    onTertiaryContainer = Color(0xFF002019),
    background = Color(0xFFF2F6FC),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE7EDF7),
    onSurfaceVariant = Color(0xFF495B70),
    outline = Color(0xFF71859C),
    outlineVariant = Color(0xFFC5CEDA),
    inverseSurface = Color(0xFF1B2433),
    inverseOnSurface = Color(0xFFF5F7FB),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.45.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(26.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

@Composable
fun FaceshotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkPalette else LightPalette,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
