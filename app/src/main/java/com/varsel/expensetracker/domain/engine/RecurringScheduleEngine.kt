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
            RecurringFrequency.QUARTERLY -> {
                val nextQuarter = currentLocalDate.plusMonths(3)
                val maxDay = nextQuarter.lengthOfMonth()
                val adjustedDay = targetDayOfMonth.coerceAtMost(maxDay)
                nextQuarter.withDayOfMonth(adjustedDay)
            }
            RecurringFrequency.SEMI_ANNUALLY -> {
                val nextHalf = currentLocalDate.plusMonths(6)
                val maxDay = nextHalf.lengthOfMonth()
                val adjustedDay = targetDayOfMonth.coerceAtMost(maxDay)
                nextHalf.withDayOfMonth(adjustedDay)
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
            RecurringFrequency.QUARTERLY -> "Every 3 months on ${getOrdinal(date.dayOfMonth)}"
            RecurringFrequency.SEMI_ANNUALLY -> "Every 6 months on ${getOrdinal(date.dayOfMonth)}"
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

    /**
     * Calculates the exact number of occurrences of an item in a specific calendar month and year,
     * taking into account startDateTimestamp, endDateTimestamp, and the item's schedule.
     */
    fun countOccurrencesInMonth(item: RecurringItem, year: Int, month: Int): Int {
        if (!item.isActive) return 0
        val monthStart = LocalDate.of(year, month, 1)
        val monthEnd = monthStart.plusMonths(1).minusDays(1)
        val monthStartMillis = monthStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val monthEndMillis = monthEnd.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()

        if (item.startDateTimestamp > monthEndMillis) return 0
        if (item.endDateTimestamp != null && item.endDateTimestamp < monthStartMillis) return 0

        // Step through occurrences from start date
        var count = 0
        var current = item.startDateTimestamp
        val maxIterations = 600
        var iteration = 0

        while (current <= monthEndMillis && iteration < maxIterations) {
            iteration++
            if (item.endDateTimestamp != null && current > item.endDateTimestamp) {
                break
            }
            if (current >= monthStartMillis && current <= monthEndMillis) {
                count++
            }
            val next = calculateNextOccurrence(current, item.frequency, item.startDateTimestamp)
            if (next <= current) break // Safety check against non-advancing loops
            current = next
        }

        return count
    }

    /**
     * Finds all missed occurrences up to referenceTimestamp (defaults to today's date).
     * Returns a list of timestamps representing each past occurrence that was not yet processed.
     */
    fun getMissedOccurrences(
        item: RecurringItem,
        referenceTimestamp: Long = System.currentTimeMillis()
    ): List<Long> {
        if (!item.isActive) return emptyList()
        val missed = mutableListOf<Long>()
        val today = Instant.ofEpochMilli(referenceTimestamp).atZone(zoneId).toLocalDate()
        var current = item.nextOccurrenceTimestamp
        var currentDate = Instant.ofEpochMilli(current).atZone(zoneId).toLocalDate()

        var iterations = 0
        val maxIterations = 120 // Protect against runaway loops

        // While scheduled date is strictly before today
        while (currentDate.isBefore(today) && iterations < maxIterations) {
            iterations++
            if (item.endDateTimestamp != null && current > item.endDateTimestamp) break
            missed.add(current)
            val next = calculateNextOccurrence(current, item.frequency, item.startDateTimestamp)
            if (next <= current) break
            current = next
            currentDate = Instant.ofEpochMilli(current).atZone(zoneId).toLocalDate()
        }

        return missed
    }
}
