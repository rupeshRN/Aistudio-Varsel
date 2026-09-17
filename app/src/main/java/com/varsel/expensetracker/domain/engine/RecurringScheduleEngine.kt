package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.domain.model.recurring.RecurringFrequency
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringScheduleEngine @Inject constructor() {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    /**
     * Calculates the next occurrence date timestamp starting from an existing occurrence timestamp.
     * Safely handles month-end days (e.g. Jan 31 -> Feb 28/29) and leap years.
     */
    fun calculateNextOccurrence(
        currentOccurrenceTimestamp: Long,
        frequency: RecurringFrequency,
        startDateTimestamp: Long = currentOccurrenceTimestamp
    ): Long {
        val currentLocalDate = Instant.ofEpochMilli(currentOccurrenceTimestamp)
            .atZone(zoneId)
            .toLocalDate()
        
        val startLocalDate = Instant.ofEpochMilli(startDateTimestamp)
            .atZone(zoneId)
            .toLocalDate()

        val targetDayOfMonth = startLocalDate.dayOfMonth

        val nextLocalDate = when (frequency) {
            RecurringFrequency.DAILY -> currentLocalDate.plusDays(1)
            RecurringFrequency.WEEKLY -> currentLocalDate.plusWeeks(1)
            RecurringFrequency.MONTHLY -> {
                // Move one month forward, and try to restore original start day of month if possible
                val nextMonth = currentLocalDate.plusMonths(1)
                val maxDayInNextMonth = nextMonth.lengthOfMonth()
                val adjustedDay = targetDayOfMonth.coerceAtMost(maxDayInNextMonth)
                nextMonth.withDayOfMonth(adjustedDay)
            }
            RecurringFrequency.YEARLY -> {
                val nextYear = currentLocalDate.plusYears(1)
                val maxDayInMonth = nextYear.lengthOfMonth()
                val adjustedDay = targetDayOfMonth.coerceAtMost(maxDayInMonth)
                nextYear.withDayOfMonth(adjustedDay)
            }
        }

        return nextLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    /**
     * Advances occurrence until it is in the future relative to [referenceTimestamp] (defaults to now).
     * Useful when activating or catching up without duplicate iterations.
     */
    fun getNextFutureOccurrence(
        item: RecurringItem,
        referenceTimestamp: Long = System.currentTimeMillis()
    ): Long {
        var nextTimestamp = item.nextOccurrenceTimestamp
        val refDate = Instant.ofEpochMilli(referenceTimestamp).atZone(zoneId).toLocalDate()
        var nextDate = Instant.ofEpochMilli(nextTimestamp).atZone(zoneId).toLocalDate()

        var iterations = 0
        val maxIterations = 500 // Prevent infinite loops

        while (nextDate.isBefore(refDate) && iterations < maxIterations) {
            nextTimestamp = calculateNextOccurrence(nextTimestamp, item.frequency, item.startDateTimestamp)
            nextDate = Instant.ofEpochMilli(nextTimestamp).atZone(zoneId).toLocalDate()
            iterations++
        }

        return nextTimestamp
    }

    /**
     * Checks if a recurring item is due today or past due.
     */
    fun isDue(item: RecurringItem, currentTimestamp: Long = System.currentTimeMillis()): Boolean {
        if (!item.isActive) return false
        val today = Instant.ofEpochMilli(currentTimestamp).atZone(zoneId).toLocalDate()
        val nextDate = Instant.ofEpochMilli(item.nextOccurrenceTimestamp).atZone(zoneId).toLocalDate()
        return !nextDate.isAfter(today)
    }

    /**
     * Returns the number of days until next occurrence.
     * Negative means past due.
     */
    fun getDaysUntilNext(item: RecurringItem, currentTimestamp: Long = System.currentTimeMillis()): Long {
        val today = Instant.ofEpochMilli(currentTimestamp).atZone(zoneId).toLocalDate()
        val nextDate = Instant.ofEpochMilli(item.nextOccurrenceTimestamp).atZone(zoneId).toLocalDate()
        return ChronoUnit.DAYS.between(today, nextDate)
    }

    /**
     * Formats frequency for friendly display.
     */
    fun formatFrequencySchedule(frequency: RecurringFrequency, timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
        return when (frequency) {
            RecurringFrequency.DAILY -> "Every day"
            RecurringFrequency.WEEKLY -> "Every ${date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}"
            RecurringFrequency.MONTHLY -> "Monthly on ${getOrdinal(date.dayOfMonth)}"
            RecurringFrequency.YEARLY -> "Yearly on ${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}"
        }
    }

    private fun getOrdinal(day: Int): String {
        return when {
            day in 11..13 -> "${day}th"
            day % 10 == 1 -> "${day}st"
            day % 10 == 2 -> "${day}nd"
            day % 10 == 3 -> "${day}rd"
            else -> "${day}th"
        }
    }
}
