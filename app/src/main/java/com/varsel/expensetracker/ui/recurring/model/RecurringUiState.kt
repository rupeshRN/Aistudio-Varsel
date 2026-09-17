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

enum class RecurringPeriod(val label: String) {
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    YTD("YTD")
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

    // Separated commitment fields for calendar-accurate calculation
    val expectedBillsMonthly: Double = 0.0,
    val expectedSubscriptionsMonthly: Double = 0.0,
    val expectedVariableMonthly: Double = 0.0,
    val expectedIncomeMonthly: Double = 0.0,
    val estimatedMonthlyCommitment: Double = 0.0, // bills + subscriptions + variable
    val occurrencesInMonthCount: Int = 0,

    val totalActiveYearlyExpense: Double = 0.0,
    val totalActiveYearlyIncome: Double = 0.0,
    val totalActiveYearlySubscriptions: Double = 0.0,

    val totalYtdExpense: Double = 0.0,
    val totalYtdIncome: Double = 0.0,
    val totalYtdSubscriptions: Double = 0.0,
    val elapsedMonthsCount: Int = 1,
    val currentYear: Int = 2026,
    val currentMonthName: String = "Sep",

    val activeSubscriptionsCount: Int = 0,
    val activeRecurringCount: Int = 0,
    val dueItemsCount: Int = 0,
    val overdueItemsCount: Int = 0
)

data class RecurringUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedTab: RecurringFilterTab = RecurringFilterTab.ALL,
    val selectedPeriod: RecurringPeriod = RecurringPeriod.MONTHLY,
    val allItems: List<RecurringItemUiModel> = emptyList(),
    val filteredItems: List<RecurringItemUiModel> = emptyList(),
    val upcomingItems: List<RecurringItemUiModel> = emptyList(),
    val summary: RecurringSummaryModel = RecurringSummaryModel(),
    val insights: SubscriptionInsights = SubscriptionInsights(),
    val reviewItems: List<SubscriptionReviewItem> = emptyList(),
    val availableAccounts: List<AccountOption> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val editingItem: RecurringItem? = null,
    val isAddEditSheetOpen: Boolean = false,
    val isInsightsSheetOpen: Boolean = false,
    val isReviewSheetOpen: Boolean = false,
    val variableAmountItem: RecurringItem? = null,
    val variableItemPastPayments: List<Double> = emptyList(),
    val catchUpItem: RecurringItem? = null,
    val catchUpMissedDates: List<Long> = emptyList(),
    val userMessage: String? = null
)
