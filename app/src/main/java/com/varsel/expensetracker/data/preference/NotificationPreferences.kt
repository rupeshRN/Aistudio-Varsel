package com.varsel.expensetracker.data.preference

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val NOTIFICATION_SETTINGS_DATASTORE = "notification_settings_prefs"

val Context.notificationDataStore by preferencesDataStore(
    name = NOTIFICATION_SETTINGS_DATASTORE
)

data class NotificationConfig(
    val statementReminderEnabled: Boolean = true,
    val emiReminderEnabled: Boolean = true,
    val emiReminderDaysBefore: Int = 2,
    val lastStatementReminderMonth: String = "",
    val notifiedEmiKeys: Set<String> = emptySet()
)

object NotificationPreferenceKeys {
    val STATEMENT_REMINDER_ENABLED = booleanPreferencesKey("statement_reminder_enabled")
    val EMI_REMINDER_ENABLED = booleanPreferencesKey("emi_reminder_enabled")
    val EMI_REMINDER_DAYS_BEFORE = intPreferencesKey("emi_reminder_days_before")
    val LAST_STATEMENT_REMINDER_MONTH = stringPreferencesKey("last_statement_reminder_month")
    val NOTIFIED_EMI_KEYS = stringSetPreferencesKey("notified_emi_keys")
}

@Singleton
class NotificationPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val notificationConfig: Flow<NotificationConfig> = context.notificationDataStore.data.map { prefs ->
        val statementEnabled = prefs[NotificationPreferenceKeys.STATEMENT_REMINDER_ENABLED] ?: true
        val emiEnabled = prefs[NotificationPreferenceKeys.EMI_REMINDER_ENABLED] ?: true
        val daysBefore = prefs[NotificationPreferenceKeys.EMI_REMINDER_DAYS_BEFORE] ?: 2
        val lastMonth = prefs[NotificationPreferenceKeys.LAST_STATEMENT_REMINDER_MONTH] ?: ""
        val emiKeys = prefs[NotificationPreferenceKeys.NOTIFIED_EMI_KEYS] ?: emptySet()

        NotificationConfig(
            statementReminderEnabled = statementEnabled,
            emiReminderEnabled = emiEnabled,
            emiReminderDaysBefore = daysBefore,
            lastStatementReminderMonth = lastMonth,
            notifiedEmiKeys = emiKeys
        )
    }

    suspend fun setStatementReminderEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { prefs ->
            prefs[NotificationPreferenceKeys.STATEMENT_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setEmiReminderEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { prefs ->
            prefs[NotificationPreferenceKeys.EMI_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setEmiReminderDaysBefore(days: Int) {
        context.notificationDataStore.edit { prefs ->
            prefs[NotificationPreferenceKeys.EMI_REMINDER_DAYS_BEFORE] = days
        }
    }

    suspend fun recordStatementReminderNotified(yearMonth: String) {
        context.notificationDataStore.edit { prefs ->
            prefs[NotificationPreferenceKeys.LAST_STATEMENT_REMINDER_MONTH] = yearMonth
        }
    }

    suspend fun recordEmiReminderNotified(emiKey: String) {
        context.notificationDataStore.edit { prefs ->
            val current = prefs[NotificationPreferenceKeys.NOTIFIED_EMI_KEYS] ?: emptySet()
            // Keep set size bounded to last 50 entries
            val updated = (current + emiKey).toList().takeLast(50).toSet()
            prefs[NotificationPreferenceKeys.NOTIFIED_EMI_KEYS] = updated
        }
    }

    suspend fun clearNotifiedHistory() {
        context.notificationDataStore.edit { prefs ->
            prefs[NotificationPreferenceKeys.LAST_STATEMENT_REMINDER_MONTH] = ""
            prefs[NotificationPreferenceKeys.NOTIFIED_EMI_KEYS] = emptySet()
        }
    }
}
