package com.thumbnailstudio.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val Violet = Color(0xFF6D5BFF)
val VioletSoft = Color(0xFF9C8CFF)
val Cyan = Color(0xFF22D3EE)
val Rose = Color(0xFFFF4D6D)
val Amber = Color(0xFFFFC857)
val Ink = Color(0xFF070A14)
val InkSurface = Color(0xFF111725)
val Paper = Color(0xFFF6F7FB)

private val DarkScheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A2350),
    onPrimaryContainer = Color(0xFFE5E0FF),
    secondary = Cyan,
    onSecondary = Color(0xFF04202A),
    tertiary = Rose,
    onTertiary = Color.White,
    background = Ink,
    onBackground = Color(0xFFEDEFF7),
    surface = InkSurface,
    onSurface = Color(0xFFEDEFF7),
    surfaceVariant = Color(0xFF1B2233),
    onSurfaceVariant = Color(0xFFAEB6C9),
    outline = Color(0xFF39415A),
    error = Color(0xFFFF6B6B)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5A46F0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF1B1246),
    secondary = Color(0xFF0E7490),
    onSecondary = Color.White,
    tertiary = Color(0xFFD9254B),
    onTertiary = Color.White,
    background = Paper,
    onBackground = Color(0xFF141824),
    surface = Color.White,
    onSurface = Color(0xFF141824),
    surfaceVariant = Color(0xFFEDEFF6),
    onSurfaceVariant = Color(0xFF4A5165),
    outline = Color(0xFFC8CCDA),
    error = Color(0xFFC0272D)
)

private val StudioTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

@Composable
fun ThumbnailStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = StudioTypography,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
        ),
        content = content
    )
}

@Composable
fun isDark(): Boolean {
    LocalContext.current
    return isSystemInDarkTheme()
}
