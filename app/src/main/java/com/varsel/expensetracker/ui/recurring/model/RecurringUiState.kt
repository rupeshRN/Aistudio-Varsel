package com.varsel.expensetracker.ui.recurring.model

import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.domain.model.recurring.RecurringFrequency
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.domain.model.recurring.RecurringType
import com.varsel.expensetracker.ui.transaction.model.AccountOption

enum class RecurringFilterTab(val title: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    SUBSCRIPTIONS("Subscriptions"),
    EXPENSES("Expenses"),
    INCOME("Income")
}

data class RecurringItemUiModel(
    val item: RecurringItem,
    val formattedAmount: String,
    val scheduleText: String,
    val nextOccurrenceFormatted: String,
    val daysUntilNext: Long,
    val isDue: Boolean,
    val categoryIcon: String = "ic_help",
    val categoryColorHex: String = "#9E9E9E",
    val accountDisplayName: String
)

data class RecurringSummaryModel(
    val totalActiveMonthlyExpense: Double = 0.0,
    val totalActiveMonthlyIncome: Double = 0.0,
    val totalActiveMonthlySubscriptions: Double = 0.0,
    val activeSubscriptionsCount: Int = 0,
    val activeRecurringCount: Int = 0,
    val dueItemsCount: Int = 0
)

data class RecurringUiState(
    val isLoading: Boolean = true,
    val selectedTab: RecurringFilterTab = RecurringFilterTab.ALL,
    val allItems: List<RecurringItemUiModel> = emptyList(),
    val filteredItems: List<RecurringItemUiModel> = emptyList(),
    val upcomingItems: List<RecurringItemUiModel> = emptyList(),
    val summary: RecurringSummaryModel = RecurringSummaryModel(),
    val availableAccounts: List<AccountOption> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val editingItem: RecurringItem? = null,
    val isAddEditSheetOpen: Boolean = false,
    val userMessage: String? = null
)
