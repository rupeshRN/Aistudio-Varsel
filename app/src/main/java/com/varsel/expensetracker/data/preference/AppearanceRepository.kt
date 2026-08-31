package com.varsel.expensetracker.data.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppearanceConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accentScheme: AccentScheme = AccentScheme.EMERALD,
    val amoledDark: Boolean = false
)

@Singleton
class AppearanceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val appearanceConfig: Flow<AppearanceConfig> = context.appearanceDataStore.data.map { prefs ->
        val modeStr = prefs[AppearancePreferenceKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(modeStr) }.getOrDefault(ThemeMode.SYSTEM)

        val dynamicColor = prefs[AppearancePreferenceKeys.DYNAMIC_COLOR] ?: true

        val schemeStr = prefs[AppearancePreferenceKeys.ACCENT_SCHEME] ?: AccentScheme.EMERALD.name
        val accentScheme = runCatching { AccentScheme.valueOf(schemeStr) }.getOrDefault(AccentScheme.EMERALD)

        val amoledDark = prefs[AppearancePreferenceKeys.AMOLED_DARK] ?: false

        AppearanceConfig(
            themeMode = themeMode,
            dynamicColor = dynamicColor,
            accentScheme = accentScheme,
            amoledDark = amoledDark
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appearanceDataStore.edit { prefs ->
            prefs[AppearancePreferenceKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.appearanceDataStore.edit { prefs ->
            prefs[AppearancePreferenceKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setAccentScheme(scheme: AccentScheme) {
        context.appearanceDataStore.edit { prefs ->
            prefs[AppearancePreferenceKeys.ACCENT_SCHEME] = scheme.name
        }
    }

    suspend fun setAmoledDark(enabled: Boolean) {
        context.appearanceDataStore.edit { prefs ->
            prefs[AppearancePreferenceKeys.AMOLED_DARK] = enabled
        }
    }
}
