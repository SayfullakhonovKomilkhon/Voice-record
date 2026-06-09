package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// High-Fidelity Mockup Palette - Vibrant Royal Blue & Light Periwinkle Canvas
val CorporatePrimaryLight = Color(0xFF2B5CFF)     // Vibrant Royal Blue from Mockup
val CorporateSecondaryLight = Color(0xFF4C75FF)   // Sky Blue Accent Gradient
val CorporateTertiaryLight = Color(0xFFEFF2FC)    // Soft Periwinkle Slate Container
val CorporateBackgroundLight = Color(0xFFF4F6FD)  // Pastel Light Blue-Gray Backdrop
val CorporateSurfaceLight = Color(0xFFFFFFFF)     // High-Fidelity White Cards
val CorporateOnPrimaryLight = Color(0xFFFFFFFF)
val CleanBorderLight = Color(0xFFE2EAFD)          // Custom faint blue border
val SlateBlueContainer = Color(0xFFE6EDFF)        // Softest highlight blue

val CorporatePrimaryDark = Color(0xFF4D77FF)      // Bright Neon Blue for Dark Theme
val CorporateSecondaryDark = Color(0xFF2563EB)    // Royal Blue for Dark Theme Accent
val CorporateTertiaryDark = Color(0xFF1E293B)     // Cool Slate
val CorporateBackgroundDark = Color(0xFF0C101D)   // Premium Deep Midnight Blue (from dark mockups)
val CorporateSurfaceDark = Color(0xFF171E2F)      // Dark card container blue
val CorporateOnPrimaryDark = Color(0xFF0F1524)


data class CustomThemeColors(
    val primary: Color,
    val secondary: Color,
    val bannerStart: Color,
    val bannerEnd: Color,
    val accentCircle: Color,
    val buttonGradientStart: Color,
    val buttonGradientEnd: Color,
    val textHighlight: Color,
    val pulseAlphaColor: Color
)

object ThemeColorsProvider {
    val Teal = CustomThemeColors(
        primary = Color(0xFF00AAA6),
        secondary = Color(0xFF05C3DE),
        bannerStart = Color(0xFF00AAA6),
        bannerEnd = Color(0xFF00C4B4),
        accentCircle = Color(0xFFABD8D4),
        buttonGradientStart = Color(0xFF00AAA6),
        buttonGradientEnd = Color(0xFF05C3DE),
        textHighlight = Color(0xFF00AAA6),
        pulseAlphaColor = Color(0xFF00AAA6)
    )

    val Lilac = CustomThemeColors(
        primary = Color(0xFF8B5CF6), // Royal lilac/purple
        secondary = Color(0xFFD8B4FE), // Lavender secondary
        bannerStart = Color(0xFF6D28D9), // Vibrant premium orchid start
        bannerEnd = Color(0xFF8B5CF6), // Soft violet end
        accentCircle = Color(0xFFDDD6FE), // Soft light periwinkle circular accent
        buttonGradientStart = Color(0xFF7C3AED),
        buttonGradientEnd = Color(0xFFA78BFA),
        textHighlight = Color(0xFF8B5CF6), // Dynamic lilac highlight
        pulseAlphaColor = Color(0xFF8B5CF6)
    )

    val Pink = CustomThemeColors(
        primary = Color(0xFFEC4899), // Dream rose pink
        secondary = Color(0xFFFBCFE8), // Pastel blossom pink
        bannerStart = Color(0xFFBE185D), // Rich barberry crimson ruby
        bannerEnd = Color(0xFFEC4899), // Deep rich rose pink
        accentCircle = Color(0xFFFCE7F3), // Ultra-soft powder pink circular details
        buttonGradientStart = Color(0xFFDB2777),
        buttonGradientEnd = Color(0xFFEC4899),
        textHighlight = Color(0xFFEC4899), // Dynamic pink highlight
        pulseAlphaColor = Color(0xFFEC4899)
    )

    fun getColors(themeName: String): CustomThemeColors {
        return when (themeName.lowercase()) {
            "lilac" -> Lilac
            "pink" -> Pink
            else -> Teal
        }
    }
}


