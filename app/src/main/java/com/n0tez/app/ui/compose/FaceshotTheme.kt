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
    primary = Color(0xFF79E2FF),
    onPrimary = Color(0xFF04131C),
    primaryContainer = Color(0xFF11364B),
    onPrimaryContainer = Color(0xFFD8F6FF),
    secondary = Color(0xFFC6B8FF),
    onSecondary = Color(0xFF211A44),
    secondaryContainer = Color(0xFF30295E),
    onSecondaryContainer = Color(0xFFEBE4FF),
    tertiary = Color(0xFF73F1D1),
    onTertiary = Color(0xFF052019),
    tertiaryContainer = Color(0xFF103D34),
    onTertiaryContainer = Color(0xFFD8FFF3),
    background = Color(0xFF020611),
    onBackground = Color(0xFFF7F9FD),
    surface = Color(0xFF0A1220),
    onSurface = Color(0xFFF5F7FC),
    surfaceVariant = Color(0xFF141F31),
    onSurfaceVariant = Color(0xFFA5B3C9),
    outline = Color(0xFF41556E),
    outlineVariant = Color(0xFF253347),
    inverseSurface = Color(0xFFE9EDF5),
    inverseOnSurface = Color(0xFF101723),
    error = Color(0xFFFF8DA7),
    onError = Color(0xFF350913),
    errorContainer = Color(0xFF6B1730),
    onErrorContainer = Color(0xFFFFD9E3),
    scrim = Color(0xE6000106),
)

private val LightPalette = lightColorScheme(
    primary = Color(0xFF005C78),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBF3FF),
    onPrimaryContainer = Color(0xFF001E29),
    secondary = Color(0xFF5243B8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E1FF),
    onSecondaryContainer = Color(0xFF1E1749),
    tertiary = Color(0xFF006C59),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF98F8DD),
    onTertiaryContainer = Color(0xFF002019),
    background = Color(0xFFF3F7FC),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121A27),
    surfaceVariant = Color(0xFFE9EEF7),
    onSurfaceVariant = Color(0xFF4B5C71),
    outline = Color(0xFF73859B),
    outlineVariant = Color(0xFFC9D1DC),
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
        fontSize = 46.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.0).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.65).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
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
        fontSize = 21.sp,
        lineHeight = 28.sp,
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
        lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
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
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(22.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(34.dp),
    extraLarge = RoundedCornerShape(42.dp),
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
