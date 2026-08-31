package com.varsel.expensetracker.data.preference

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val APPEARANCE_SETTINGS = "appearance_settings"

val Context.appearanceDataStore by preferencesDataStore(
    name = APPEARANCE_SETTINGS
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AccentScheme(val label: String, val primaryHex: Long) {
    PURPLE("Classic Purple", 0xFF6750A4),
    EMERALD("Emerald Green", 0xFF0D9488),
    OCEAN("Ocean Blue", 0xFF0284C7),
    AMBER("Sunset Amber", 0xFFD97706),
    ROSE("Berry Rose", 0xFFBE123C)
}

object AppearancePreferenceKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val ACCENT_SCHEME = stringPreferencesKey("accent_scheme")
    val AMOLED_DARK = booleanPreferencesKey("amoled_dark")
}
