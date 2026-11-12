package com.varsel.expensetracker.ui.reports

import java.time.YearMonth

/**
 * Represents a single contributing item in the category drill-down view.
 */
data class CategoryDrillDownItem(
    val transactionId: Long,
    val description: String,
    val category: String,
    val dateTimestamp: Long,
    val amount: Double,
    val isExpense: Boolean,
    val accountLast4: String? = null,
    val isEventAllocation: Boolean = false,
    val eventName: String? = null,
    val eventLinkId: String? = null,
    val month: YearMonth = YearMonth.now()
)

/**
 * State for the Category Drill-Down bottom sheet.
 */
data class CategoryDrillDownState(
    val isVisible: Boolean = false,
    val categoryName: String = "",
    val flow: ReportsFlow = ReportsFlow.EXPENSES,
    val totalCategoryAmount: Double = 0.0,
    val percentOfTotal: Double = 0.0,
    val periodLabel: String = "",
    val month: YearMonth = YearMonth.now(),
    val items: List<CategoryDrillDownItem> = emptyList(),
    val searchQuery: String = "",
    val isComparisonMode: Boolean = false,
    val selectedMonthFilter: YearMonth? = null,
    val comparisonMonths: List<YearMonth> = emptyList(),
    val trendText: String? = null
) {
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()

    val filteredItems: List<CategoryDrillDownItem>
        get() {
            val baseList = if (selectedMonthFilter != null) {
                items.filter { it.month == selectedMonthFilter }
            } else {
                items
            }
            if (searchQuery.isBlank()) {
                return baseList
            }
            val q = searchQuery.trim().lowercase()
            return baseList.filter { item ->
                item.description.lowercase().contains(q) ||
                    (item.eventName?.lowercase()?.contains(q) == true) ||
                    (item.accountLast4?.contains(q) == true) ||
                    item.amount.toInt().toString() == q
            }
        }

    val displayAmount: Double
        get() = if (isSearching || selectedMonthFilter != null) {
            filteredItems.sumOf { it.amount }
        } else {
            totalCategoryAmount
        }

    val searchPercentOfCategory: Double
        get() = if (totalCategoryAmount > 0.0) {
            (displayAmount / totalCategoryAmount) * 100.0
        } else {
            0.0
        }

    /**
     * Group items by YearMonth in descending order (latest month first).
     */
    val itemsByMonth: Map<YearMonth, List<CategoryDrillDownItem>>
        get() = filteredItems
            .groupBy { it.month }
            .toSortedMap(compareByDescending { it })
}
