package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CorporatePrimaryDark,
    secondary = CorporateSecondaryDark,
    tertiary = CorporateTertiaryDark,
    background = CorporateBackgroundDark,
    surface = CorporateSurfaceDark,
    onPrimary = CorporateOnPrimaryDark,
    onSurface = Color(0xFFE2E2E6),
    onBackground = Color(0xFFE2E2E6)
)

private val LightColorScheme = lightColorScheme(
    primary = CorporatePrimaryLight,
    secondary = CorporateSecondaryLight,
    tertiary = CorporateTertiaryLight,
    background = CorporateBackgroundLight,
    surface = CorporateSurfaceLight,
    onPrimary = CorporateOnPrimaryLight,
    onSurface = Color(0xFF191C1E),
    onBackground = Color(0xFF191C1E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force Light Theme for perfect consistency on both user's phone and emulator
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce the strict corporate business styling
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use transparent status bar and enable edge-to-edge light appearance (dark icons)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            // Force dark icons for status bar and navigation bar (light mode theme)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
