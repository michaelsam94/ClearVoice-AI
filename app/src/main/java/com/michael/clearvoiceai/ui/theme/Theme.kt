package com.michael.clearvoiceai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = MutedBlue,
    tertiary = EmeraldGreen,
    background = SlateDarkBackground,
    surface = SlateDarkSurface,
    surfaceVariant = SlateDarkSurfaceVariant,
    onBackground = LightGrayOnSurface,
    onSurface = LightGrayOnSurface,
    outline = DarkBorderOutline
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlueDark,
    secondary = MutedBlue,
    tertiary = EmeraldGreen,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = DarkGrayOnSurface,
    onSurface = DarkGrayOnSurface,
    outline = LightBorderOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to enforce our high-quality custom studio dark layout by default, or true
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
