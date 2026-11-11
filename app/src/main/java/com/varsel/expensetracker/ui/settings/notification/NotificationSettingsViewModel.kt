package com.varsel.expensetracker.ui.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.preference.NotificationConfig
import com.varsel.expensetracker.data.preference.NotificationPreferencesRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.util.notification.NotificationReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TrackedBankStatus(
    val accountKey: String,
    val bankName: String,
    val accountLast4: String?,
    val isUploadedThisMonth: Boolean
) {
    val displayName: String
        get() = if (!accountLast4.isNullOrBlank()) {
            "$bankName (••$accountLast4)"
        } else {
            bankName
        }
}

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
    private val reminderManager: NotificationReminderManager,
    private val statementSnapshotRepository: StatementSnapshotRepository
) : ViewModel() {

    val notificationConfig: StateFlow<NotificationConfig> =
        notificationPreferencesRepository.notificationConfig.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationConfig()
        )

    val trackedBankStatuses: StateFlow<List<TrackedBankStatus>> =
        statementSnapshotRepository.observeAllSnapshots()
            .map { snapshots ->
                val startOfMonthTimestamp = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                snapshots
                    .filter { !it.bankName.isNullOrBlank() || !it.accountLast4.isNullOrBlank() }
                    .groupBy { it.accountId ?: "${it.bankName}_${it.accountLast4}" }
                    .map { (key, list) ->
                        val latest = list.maxByOrNull { it.importedAt } ?: list.first()
                        val bank = latest.bankName?.takeIf { it.isNotBlank() && it != "Bank Statement" } ?: "Bank"
                        val isUploaded = list.any { it.importedAt >= startOfMonthTimestamp }
                        TrackedBankStatus(
                            accountKey = key,
                            bankName = bank,
                            accountLast4 = latest.accountLast4,
                            isUploadedThisMonth = isUploaded
                        )
                    }
                    .sortedBy { it.displayName }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun toggleStatementReminder(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferencesRepository.setStatementReminderEnabled(enabled)
            reminderManager.scheduleDailyAlarm()
        }
    }

    fun toggleEmiReminder(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferencesRepository.setEmiReminderEnabled(enabled)
            reminderManager.scheduleDailyAlarm()
        }
    }

    fun setEmiReminderDaysBefore(days: Int) {
        viewModelScope.launch {
            notificationPreferencesRepository.setEmiReminderDaysBefore(days)
        }
    }

    fun sendTestStatementNotification(forcePendingSample: Boolean = false) {
        viewModelScope.launch {
            reminderManager.triggerTestStatementNotification(forcePendingSample)
        }
    }

    fun sendTestEmiNotification() {
        viewModelScope.launch {
            reminderManager.triggerTestEmiNotification()
        }
    }
}
