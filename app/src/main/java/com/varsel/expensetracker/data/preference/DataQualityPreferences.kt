package com.varsel.expensetracker.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataQualityDataStore: DataStore<Preferences> by preferencesDataStore(name = "data_quality_preferences")

@Singleton
class DataQualityPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dismissedKeysKey = stringSetPreferencesKey("dismissed_issue_keys")

    val dismissedIssueKeys: Flow<Set<String>> = context.dataQualityDataStore.data.map { prefs ->
        prefs[dismissedKeysKey] ?: emptySet()
    }

    suspend fun dismissIssue(issueKey: String) {
        context.dataQualityDataStore.edit { prefs ->
            val current = prefs[dismissedKeysKey] ?: emptySet()
            prefs[dismissedKeysKey] = current + issueKey
        }
    }

    suspend fun restoreDismissedIssue(issueKey: String) {
        context.dataQualityDataStore.edit { prefs ->
            val current = prefs[dismissedKeysKey] ?: emptySet()
            prefs[dismissedKeysKey] = current - issueKey
        }
    }

    suspend fun clearAllDismissed() {
        context.dataQualityDataStore.edit { prefs ->
            prefs[dismissedKeysKey] = emptySet()
        }
    }
}
