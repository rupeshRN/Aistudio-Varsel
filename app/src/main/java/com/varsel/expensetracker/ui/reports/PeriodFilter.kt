package com.varsel.expensetracker.ui.reports

import java.time.LocalDate

/**
 * User-selectable reporting period.
 *
 * THIS_MONTH
 *      Current calendar month.
 *
 * LAST_3_MONTHS
 *      Current month + previous 2 calendar months.
 *
 * LAST_6_MONTHS
 *      Current month + previous 5 calendar months.
 *
 * YEAR_TO_DATE
 *      January 1 through today/current month.
 *
 * CUSTOM
 *      Explicit start/end dates selected by the user.
 */
enum class PeriodFilter {
    THIS_MONTH,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    YEAR_TO_DATE,
    CUSTOM
}

/**
 * Inclusive reporting date range.
 */
data class ReportDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    init {
        require(!endDate.isBefore(startDate)) {
            "Report end date cannot be before start date."
        }
    }

    fun contains(date: LocalDate): Boolean {
        return !date.isBefore(startDate) &&
            !date.isAfter(endDate)
    }
}
