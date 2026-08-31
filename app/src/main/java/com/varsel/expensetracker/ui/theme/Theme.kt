package com.varsel.expensetracker.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import com.varsel.expensetracker.data.preference.AccentScheme
import com.varsel.expensetracker.data.preference.ThemeMode

// Emerald Color Schemes
private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldPrimaryContainerLight,
    onPrimaryContainer = EmeraldOnPrimaryContainerLight,
    secondary = EmeraldSecondaryLight,
    secondaryContainer = EmeraldSecondaryContainerLight
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = EmeraldSecondaryDark,
    secondaryContainer = EmeraldSecondaryContainerDark
)

// Purple Color Schemes
private val PurpleLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val PurpleDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// Ocean Color Schemes
private val OceanLightColorScheme = lightColorScheme(
    primary = OceanPrimaryLight,
    onPrimary = OceanOnPrimaryLight,
    primaryContainer = OceanPrimaryContainerLight,
    onPrimaryContainer = OceanOnPrimaryContainerLight,
    secondary = OceanSecondaryLight,
    secondaryContainer = OceanSecondaryContainerLight
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = OceanPrimaryDark,
    onPrimary = OceanOnPrimaryDark,
    primaryContainer = OceanPrimaryContainerDark,
    onPrimaryContainer = OceanOnPrimaryContainerDark,
    secondary = OceanSecondaryDark,
    secondaryContainer = OceanSecondaryContainerDark
)

// Amber Color Schemes
private val AmberLightColorScheme = lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = AmberOnPrimaryLight,
    primaryContainer = AmberPrimaryContainerLight,
    onPrimaryContainer = AmberOnPrimaryContainerLight,
    secondary = AmberSecondaryLight,
    secondaryContainer = AmberSecondaryContainerLight
)

private val AmberDarkColorScheme = darkColorScheme(
    primary = AmberPrimaryDark,
    onPrimary = AmberOnPrimaryDark,
    primaryContainer = AmberPrimaryContainerDark,
    onPrimaryContainer = AmberOnPrimaryContainerDark,
    secondary = AmberSecondaryDark,
    secondaryContainer = AmberSecondaryContainerDark
)

// Rose Color Schemes
private val RoseLightColorScheme = lightColorScheme(
    primary = RosePrimaryLight,
    onPrimary = RoseOnPrimaryLight,
    primaryContainer = RosePrimaryContainerLight,
    onPrimaryContainer = RoseOnPrimaryContainerLight,
    secondary = RoseSecondaryLight,
    secondaryContainer = RoseSecondaryContainerLight
)

private val RoseDarkColorScheme = darkColorScheme(
    primary = RosePrimaryDark,
    onPrimary = RoseOnPrimaryDark,
    primaryContainer = RosePrimaryContainerDark,
    onPrimaryContainer = RoseOnPrimaryContainerDark,
    secondary = RoseSecondaryDark,
    secondaryContainer = RoseSecondaryContainerDark
)

/**
 * Main Material You Theme wrapper for Varsel Expense Tracker.
 * 
 * Supports dynamic wallpaper colors on Android 12+, customizable static palettes,
 * System/Light/Dark modes, and AMOLED true dark background.
 */
@Composable
fun VarselExpenseTrackerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    accentScheme: AccentScheme = AccentScheme.EMERALD,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> when (accentScheme) {
            AccentScheme.EMERALD -> EmeraldDarkColorScheme
            AccentScheme.PURPLE -> PurpleDarkColorScheme
            AccentScheme.OCEAN -> OceanDarkColorScheme
            AccentScheme.AMBER -> AmberDarkColorScheme
            AccentScheme.ROSE -> RoseDarkColorScheme
        }
        else -> when (accentScheme) {
            AccentScheme.EMERALD -> EmeraldLightColorScheme
            AccentScheme.PURPLE -> PurpleLightColorScheme
            AccentScheme.OCEAN -> OceanLightColorScheme
            AccentScheme.AMBER -> AmberLightColorScheme
            AccentScheme.ROSE -> RoseLightColorScheme
        }
    }

    if (isDark && amoledDark) {
        colorScheme = colorScheme.copy(
            background = AmoledBackground,
            surface = AmoledSurface
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
