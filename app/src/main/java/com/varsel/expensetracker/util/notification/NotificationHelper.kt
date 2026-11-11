package com.varsel.expensetracker.util.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.varsel.expensetracker.MainActivity
import com.varsel.expensetracker.R
import com.varsel.expensetracker.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_STATEMENT_REMINDER = "channel_statement_reminder"
    const val CHANNEL_EMI_REMINDER = "channel_emi_reminder"

    const val EXTRA_NAVIGATE_TO = "navigate_to"
    const val DESTINATION_IMPORT = "import_statement"
    const val DESTINATION_LOANS = "loans"

    const val NOTIFICATION_ID_STATEMENT = 2001
    const val NOTIFICATION_ID_EMI_BASE = 3000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            // Channel 1: Statement Reminders
            val statementChannel = NotificationChannel(
                CHANNEL_STATEMENT_REMINDER,
                "Statement Upload Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Monthly reminders to upload your bank e-statements"
                enableVibration(true)
            }

            // Channel 2: Loan EMI Alerts
            val emiChannel = NotificationChannel(
                CHANNEL_EMI_REMINDER,
                "Loan EMI Due Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming loan EMI payment due dates"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(statementChannel)
            notificationManager.createNotificationChannel(emiChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showStatementUploadNotification(
        context: Context,
        pendingBanks: List<String> = emptyList(),
        isTest: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, DESTINATION_IMPORT)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_STATEMENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isTest && pendingBanks.isNotEmpty() -> {
                if (pendingBanks.size == 1) {
                    "[Test] Pending Statement: ${pendingBanks.first()}"
                } else {
                    "[Test] Pending Statements: ${pendingBanks.joinToString(", ")}"
                }
            }
            isTest -> "[Test] Monthly Statement Upload Reminder"
            pendingBanks.isNotEmpty() -> {
                if (pendingBanks.size == 1) {
                    "Pending Statement: ${pendingBanks.first()}"
                } else {
                    "Pending Statements: ${pendingBanks.joinToString(", ")}"
                }
            }
            else -> "New Month: Upload Your Bank Statement"
        }

        val message = when {
            isTest && pendingBanks.isNotEmpty() -> {
                "Varsel detected pending statements for: ${pendingBanks.joinToString(", ")}. Import them to keep balances synchronized."
            }
            isTest -> {
                "Varsel will notify you at the start of each month to import your latest bank statement."
            }
            pendingBanks.isNotEmpty() -> {
                "You still have pending statement(s) to upload for: ${pendingBanks.joinToString(", ")}. Upload now to keep account balances and transactions up to date."
            }
            else -> {
                "A new month has begun! Upload your latest bank statement to sync account balances and auto-categorize your spending."
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_STATEMENT_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_STATEMENT, notification)
    }

    fun showEmiDueNotification(
        context: Context,
        loanId: Long,
        loanName: String,
        emiAmount: Double,
        dueDateTimestamp: Long,
        daysRemaining: Int,
        isTest: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, DESTINATION_LOANS)
        }

        val notificationId = (NOTIFICATION_ID_EMI_BASE + (loanId % 1000)).toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dueDateFormatted = dateFormatter.format(Date(dueDateTimestamp))
        val emiFormatted = CurrencyFormatter.format(emiAmount, includeDecimals = false)

        val title = when {
            isTest -> "[Test] Upcoming EMI Alert: $loanName"
            daysRemaining <= 0 -> "EMI Due Today: $loanName"
            daysRemaining == 1 -> "EMI Due Tomorrow: $loanName"
            else -> "EMI Due in $daysRemaining Days: $loanName"
        }

        val message = when {
            isTest -> "Reminder: EMI of $emiFormatted for $loanName is scheduled for $dueDateFormatted. Maintain adequate bank balance."
            daysRemaining <= 0 -> "Your monthly EMI payment of $emiFormatted for $loanName is due today ($dueDateFormatted). Ensure sufficient account balance."
            else -> "Your monthly EMI of $emiFormatted for $loanName is due on $dueDateFormatted. Keep your linked account funded to avoid late charges."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_EMI_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
