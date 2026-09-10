package com.varsel.expensetracker.ui.heatmap

import com.varsel.expensetracker.ui.model.TransactionUiModel
import java.time.LocalDate
import java.time.YearMonth

enum class HeatmapMetric(val label: String) {
    BOTH("Both (All)"),
    EXPENSE("Expenses"),
    INCOME("Income"),
    NET("Net Cash Flow"),
    ACTIVITY("Activity")
}

enum class HeatmapViewMode(val label: String) {
    MONTH("Month View"),
    YEAR("Year View")
}

data class DayHeatmapEntry(
    val date: LocalDate,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val totalExpense: Double,
    val totalIncome: Double,
    val netFlow: Double,
    val transactionCount: Int,
    val intensityLevel: Int, // 0 to 4
    val expenseIntensity: Int = 0,
    val incomeIntensity: Int = 0,
    val transactions: List<TransactionUiModel> = emptyList()
)

data class MonthOverview(
    val yearMonth: YearMonth,
    val totalExpense: Double,
    val totalIncome: Double,
    val totalTransactions: Int,
    val zeroSpendDays: Int,
    val days: List<DayHeatmapEntry>
)

data class CalendarHeatmapUiState(
    val isLoading: Boolean = true,
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedYear: Int = YearMonth.now().year,
    val viewMode: HeatmapViewMode = HeatmapViewMode.MONTH,
    val selectedMetric: HeatmapMetric = HeatmapMetric.BOTH,
    val selectedCategory: String? = null,
    val selectedAccount: String? = null,
    val availableCategories: List<String> = emptyList(),
    val availableAccounts: List<String> = emptyList(),
    val availableYears: List<Int> = listOf(YearMonth.now().year),

    // Month metrics
    val monthTotalExpense: Double = 0.0,
    val monthTotalIncome: Double = 0.0,
    val monthNetSavings: Double = 0.0,
    val monthDailyAverage: Double = 0.0,
    val monthZeroSpendDaysCount: Int = 0,
    val monthActiveDaysCount: Int = 0,
    val monthPeakDay: LocalDate? = null,
    val monthPeakAmount: Double = 0.0,
    val longestZeroSpendStreak: Int = 0,
    val currentZeroSpendStreak: Int = 0,

    // Year metrics
    val yearTotalExpense: Double = 0.0,
    val yearTotalIncome: Double = 0.0,
    val yearZeroSpendDaysCount: Int = 0,
    val yearActiveDaysCount: Int = 0,

    // Grid data
    val monthDays: List<DayHeatmapEntry> = emptyList(),
    val yearMonths: List<MonthOverview> = emptyList(),

    // Legend reference
    val legendMaxThreshold: Double = 0.0,

    // Selected day drill-down
    val selectedDayData: DayHeatmapEntry? = null
)
