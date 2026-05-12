package com.n0tez.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Midnight = Color(0xFF040914)
private val DeepSpace = Color(0xFF0A1525)
private val Surface = Color(0xFF0E1B2E)
private val SurfaceElevated = Color(0xFF13263D)
private val SurfaceHighest = Color(0xFF18324E)
private val Outline = Color(0xFF2E4D71)
private val Aqua = Color(0xFF89F7FF)
private val AquaStrong = Color(0xFF3EDEFF)
private val Silver = Color(0xFFF2F6FD)
private val Muted = Color(0xFF9EB4CF)
private val Violet = Color(0xFFA394FF)
private val Magenta = Color(0xFFFF79C6)
private val Emerald = Color(0xFF7CF0D3)
private val Error = Color(0xFFFF8FA1)
private val SoftBackground = Color(0xFFF5F8FF)
private val SoftSurface = Color(0xFFEAF1FB)
private val SoftOutline = Color(0xFFC5D3E8)
private val SoftText = Color(0xFF162235)

private val N0tezDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Aqua,
    onPrimary = Midnight,
    primaryContainer = SurfaceHighest,
    onPrimaryContainer = Silver,
    secondary = Violet,
    onSecondary = Midnight,
    secondaryContainer = Color(0xFF2C2754),
    onSecondaryContainer = Color(0xFFE2DCFF),
    tertiary = Emerald,
    onTertiary = Midnight,
    tertiaryContainer = Color(0xFF0F423A),
    onTertiaryContainer = Color(0xFFD0FFF3),
    error = Error,
    onError = Midnight,
    errorContainer = Color(0xFF4D1C2B),
    onErrorContainer = Color(0xFFFFD9DD),
    background = Midnight,
    onBackground = Silver,
    surface = Surface,
    onSurface = Silver,
    surfaceVariant = DeepSpace,
    onSurfaceVariant = Muted,
    outline = Outline,
    outlineVariant = Color(0xFF1B304A),
    surfaceTint = AquaStrong,
    inversePrimary = AquaStrong,
    inverseSurface = Color(0xFFEAF2FD),
    inverseOnSurface = Midnight,
    scrim = Color(0xE6020610)
)

private val N0tezLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF006D86),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2F1FF),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = Color(0xFF5D53B4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5DEFF),
    onSecondaryContainer = Color(0xFF181041),
    tertiary = Color(0xFF006B59),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF8AF8DC),
    onTertiaryContainer = Color(0xFF002019),
    error = Color(0xFFB32543),
    onError = Color.White,
    errorContainer = Color(0xFFFFD9E0),
    onErrorContainer = Color(0xFF3F0013),
    background = SoftBackground,
    onBackground = SoftText,
    surface = Color.White,
    onSurface = SoftText,
    surfaceVariant = SoftSurface,
    onSurfaceVariant = Color(0xFF45576F),
    outline = Color(0xFF6D7F97),
    outlineVariant = SoftOutline,
    surfaceTint = Color(0xFF006D86),
    inversePrimary = AquaStrong,
    inverseSurface = Color(0xFF1C2737),
    inverseOnSurface = Color(0xFFF4F8FF),
    scrim = Color(0xB3000000)
)

@Composable
fun N0tezTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme || isSystemInDarkTheme()) {
        N0tezDarkColorScheme
    } else {
        N0tezLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = N0tezTypography,
        content = content
    )
}
