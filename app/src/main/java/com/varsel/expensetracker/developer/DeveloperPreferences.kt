package com.varsel.expensetracker.developer

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val DEVELOPER_SETTINGS = "developer_settings"

val Context.developerDataStore by preferencesDataStore(
    name = DEVELOPER_SETTINGS
)

object DeveloperPreferences {

    val PARSER_DIAGNOSTICS =
        booleanPreferencesKey("parser_diagnostics")

}
