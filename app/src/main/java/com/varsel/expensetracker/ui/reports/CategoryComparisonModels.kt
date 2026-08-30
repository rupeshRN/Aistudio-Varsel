package com.varsel.expensetracker.ui.reports

import java.time.YearMonth

/**
 * Top-level view mode on the Reports screen.
 */
enum class ReportsTab {
    OVERVIEW,
    COMPARE
}

/**
 * Time window for month-over-month category comparisons.
 */
enum class ComparisonWindow(
    val monthsCount: Int,
    val label: String
) {
    THREE_MONTHS(3, "3 Months"),
    SIX_MONTHS(6, "6 Months")
}

/**
 * Total spent or earned in a specific month for a category.
 */
data class CategoryMonthTotal(
    val month: YearMonth,
    val amount: Double,
    val formattedAmount: String
)

/**
 * Category comparison model containing multi-month history,
 * sparkline data points, and delta metrics.
 */
data class CategoryComparisonItem(
    val category: String,
    val flow: ReportsFlow = ReportsFlow.EXPENSES,
    val monthlyTotals: List<CategoryMonthTotal> = emptyList(),
    val baselineMonth: YearMonth,
    val targetMonth: YearMonth,
    val baselineAmount: Double = 0.0,
    val targetAmount: Double = 0.0,
    val changeAmount: Double = 0.0,
    val percentageChange: Double = 0.0,
    val isNew: Boolean = false,
    val isEliminated: Boolean = false,
    val peakAmount: Double = 0.0,
    val lowestAmount: Double = 0.0
) {
    /**
     * Normalized values between 0.0 and 1.0 for rendering the sparkline.
     * If all amounts are equal or 0, returns mid-line (0.5).
     */
    val sparklinePoints: List<Float>
        get() {
            if (monthlyTotals.isEmpty()) return emptyList()
            val amounts = monthlyTotals.map { it.amount }
            val max = amounts.maxOrNull() ?: 0.0
            val min = amounts.minOrNull() ?: 0.0
            val range = max - min

            if (range <= 0.0001) {
                return amounts.map { if (it > 0.0) 0.5f else 0.0f }
            }

            return amounts.map { ((it - min) / range).toFloat() }
        }
}

/**
 * High-level summary of total spending or income across the comparison window.
 */
data class ComparisonOverviewSummary(
    val flow: ReportsFlow = ReportsFlow.EXPENSES,
    val months: List<YearMonth> = emptyList(),
    val totalMonthlyTotals: List<CategoryMonthTotal> = emptyList(),
    val totalBaselineAmount: Double = 0.0,
    val totalTargetAmount: Double = 0.0,
    val totalChangeAmount: Double = 0.0,
    val totalPercentageChange: Double = 0.0,
    val topIncreasedCategory: String? = null,
    val topIncreasedAmount: Double = 0.0,
    val topDecreasedCategory: String? = null,
    val topDecreasedAmount: Double = 0.0,
    val averageMonthlyAmount: Double = 0.0
) {
    val sparklinePoints: List<Float>
        get() {
            if (totalMonthlyTotals.isEmpty()) return emptyList()
            val amounts = totalMonthlyTotals.map { it.amount }
            val max = amounts.maxOrNull() ?: 0.0
            val min = amounts.minOrNull() ?: 0.0
            val range = max - min

            if (range <= 0.0001) {
                return amounts.map { if (it > 0.0) 0.5f else 0.0f }
            }

            return amounts.map { ((it - min) / range).toFloat() }
        }
}
