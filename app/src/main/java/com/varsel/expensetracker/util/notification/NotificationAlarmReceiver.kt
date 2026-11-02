package com.varsel.expensetracker.util.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderManager: NotificationReminderManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                reminderManager.scheduleDailyAlarm()
                reminderManager.checkAndTriggerReminders()
            }
            NotificationReminderManager.ACTION_CHECK_NOTIFICATIONS -> {
                reminderManager.checkAndTriggerReminders()
            }
        }
    }
}
