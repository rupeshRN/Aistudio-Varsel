package com.varsel.expensetracker.util.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.preference.NotificationPreferencesRepository
import com.varsel.expensetracker.domain.model.loan.LoanStatus
import com.varsel.expensetracker.domain.repository.LoanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
    private val loanRepository: LoanRepository,
    private val statementSnapshotDao: StatementSnapshotDao
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun checkAndTriggerReminders() {
        scope.launch {
            try {
                val config = notificationPreferencesRepository.notificationConfig.first()

                // Check 1: Monthly Statement Upload Reminder
                if (config.statementReminderEnabled) {
                    checkStatementReminder(config.lastStatementReminderMonth)
                }

                // Check 2: Loan EMI Due Date Reminder
                if (config.emiReminderEnabled) {
                    checkEmiReminders(
                        reminderDaysBefore = config.emiReminderDaysBefore,
                        notifiedKeys = config.notifiedEmiKeys
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun checkStatementReminder(lastNotifiedMonth: String) {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val currentYearMonth = String.format(
            "%04d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )

        // Notify during the start of the month (Days 1 to 7) if not already notified this month
        if (dayOfMonth in 1..7 && lastNotifiedMonth != currentYearMonth) {
            val startOfMonthTimestamp = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val snapshots = statementSnapshotDao.getAllSnapshots()
            val alreadyImportedThisMonth = snapshots.any { it.importedAt >= startOfMonthTimestamp }

            if (!alreadyImportedThisMonth) {
                NotificationHelper.showStatementUploadNotification(context, isTest = false)
            }
            // Mark notified for this month so we don't trigger repeatedly
            notificationPreferencesRepository.recordStatementReminderNotified(currentYearMonth)
        }
    }

    private suspend fun checkEmiReminders(
        reminderDaysBefore: Int,
        notifiedKeys: Set<String>
    ) {
        val loanSummaries = loanRepository.getAllLoansSummary().first()
        val activeLoans = loanSummaries.filter { it.loan.status == LoanStatus.ACTIVE }

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (summary in activeLoans) {
            val dueTimestamp = summary.nextEmiDueDateTimestamp ?: continue
            val dueCalendar = Calendar.getInstance().apply {
                timeInMillis = dueTimestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMillis = dueCalendar.timeInMillis - todayCalendar.timeInMillis
            val daysRemaining = (diffMillis / (24 * 60 * 60 * 1000L)).toInt()

            if (daysRemaining in 0..reminderDaysBefore) {
                val cycleKey = "loan_${summary.loan.id}_$dueTimestamp"
                if (!notifiedKeys.contains(cycleKey)) {
                    NotificationHelper.showEmiDueNotification(
                        context = context,
                        loanId = summary.loan.id,
                        loanName = summary.loan.name,
                        emiAmount = summary.nextEmiAmount,
                        dueDateTimestamp = dueTimestamp,
                        daysRemaining = daysRemaining,
                        isTest = false
                    )
                    notificationPreferencesRepository.recordEmiReminderNotified(cycleKey)
                }
            }
        }
    }

    fun scheduleDailyAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = ACTION_CHECK_NOTIFICATIONS
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule at 9:00 AM every morning
        val alarmCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If 9 AM has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            alarmCalendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    suspend fun triggerTestStatementNotification() {
        NotificationHelper.showStatementUploadNotification(context, isTest = true)
    }

    suspend fun triggerTestEmiNotification() {
        val loanSummaries = loanRepository.getAllLoansSummary().first()
        val firstActive = loanSummaries.firstOrNull { it.loan.status == LoanStatus.ACTIVE }

        if (firstActive != null && firstActive.nextEmiDueDateTimestamp != null) {
            NotificationHelper.showEmiDueNotification(
                context = context,
                loanId = firstActive.loan.id,
                loanName = firstActive.loan.name,
                emiAmount = firstActive.nextEmiAmount,
                dueDateTimestamp = firstActive.nextEmiDueDateTimestamp!!,
                daysRemaining = 2,
                isTest = true
            )
        } else {
            // Provide representative sample loan notification for testing
            val sampleDueTimestamp = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000L)
            NotificationHelper.showEmiDueNotification(
                context = context,
                loanId = 999L,
                loanName = "Home Loan (HDFC)",
                emiAmount = 24500.0,
                dueDateTimestamp = sampleDueTimestamp,
                daysRemaining = 2,
                isTest = true
            )
        }
    }

    companion object {
        const val ACTION_CHECK_NOTIFICATIONS = "com.varsel.expensetracker.ACTION_CHECK_NOTIFICATIONS"
        const val ALARM_REQUEST_CODE = 4001
    }
}
