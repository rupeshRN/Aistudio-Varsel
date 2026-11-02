package com.varsel.expensetracker.ui.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.preference.NotificationConfig
import com.varsel.expensetracker.data.preference.NotificationPreferencesRepository
import com.varsel.expensetracker.util.notification.NotificationReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
    private val reminderManager: NotificationReminderManager
) : ViewModel() {

    val notificationConfig: StateFlow<NotificationConfig> =
        notificationPreferencesRepository.notificationConfig.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationConfig()
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

    fun sendTestStatementNotification() {
        viewModelScope.launch {
            reminderManager.triggerTestStatementNotification()
        }
    }

    fun sendTestEmiNotification() {
        viewModelScope.launch {
            reminderManager.triggerTestEmiNotification()
        }
    }
}
