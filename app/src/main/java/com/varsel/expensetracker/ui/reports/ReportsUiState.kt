package com.varsel.expensetracker.ui.reports

import java.time.YearMonth
import java.time.LocalDate

/**
 * Complete UI state for the Reports feature.
 *
 * Rendering belongs in ReportsScreen and its smaller UI components.
 * Business calculations belong in ReportsViewModel.
 */
data class ReportsUiState(

    val isLoading: Boolean = true,

    val errorMessage: String? = null,

    /**
 * Current user-selected reporting period.
 */
val periodFilter: PeriodFilter = PeriodFilter.THIS_MONTH,

/**
 * Custom range start date.
 *
 * Used only when periodFilter == CUSTOM.
 */
val customStartDate: LocalDate = LocalDate.now()
    .withDayOfMonth(1),

/**
 * Custom range end date.
 *
 * Used only when periodFilter == CUSTOM.
 */
val customEndDate: LocalDate = LocalDate.now(),

    /**
     * Currently selected reporting period.
     */
    val period: ReportPeriod = ReportPeriod.MONTH,

    /**
     * Currently selected month (or anchor month for 3M/6M).
     */
    val selectedMonth: YearMonth = YearMonth.now(),

    /**
     * Start month when period == CUSTOM.
     */
    val customStartMonth: YearMonth = YearMonth.now().minusMonths(2),

    /**
     * End month when period == CUSTOM.
     */
    val customEndMonth: YearMonth = YearMonth.now(),

    /**
     * Currently selected account IDs.
     *
     * Empty means "All Accounts".
     *
     * A Set is used intentionally because the filter can later
     * support selecting multiple accounts.
     */
    val selectedAccountIds: Set<String> = emptySet(),

    /**
     * Accounts available to the Reports filter.
     */
    val accounts: List<ReportsAccount> = emptyList(),

    /**
     * Currently selected top-level money-flow view.
     */
    val selectedFlow: ReportsFlow = ReportsFlow.EXPENSES,

    /**
     * Selected expense category.
     *
     * null means Overall.
     */
    val selectedExpenseCategory: String? = null,

    /**
     * Selected income category.
     *
     * null means Overall.
     */
    val selectedIncomeCategory: String? = null,

    /**
     * Monthly cash-flow summary.
     */
    val cashFlow: ReportsCashFlow = ReportsCashFlow(),

    /**
     * Expense categories for the selected period/filter.
     */
    val expenseCategories: List<ReportsExpenseCategory> = emptyList(),

    /**
     * Income categories for the selected period/filter.
     */
    val incomeCategories: List<ReportsIncomeCategory> = emptyList(),

    /**
     * Financial Events for the selected period/filter.
     */
    val financialEvents: List<ReportsFinancialEvent> = emptyList(),

    /**
     * Detailed category drill-down state.
     */
    val drillDownState: CategoryDrillDownState = CategoryDrillDownState(),

    /**
     * Active tab on the Reports screen (Overview vs Compare/Trends).
     */
    val currentTab: ReportsTab = ReportsTab.OVERVIEW,

    /**
     * Time window for month-over-month category comparisons.
     */
    val comparisonWindow: ComparisonWindow = ComparisonWindow.THREE_MONTHS,

    /**
     * Selected flow for the comparison view (Expenses vs Income).
     */
    val comparisonFlow: ReportsFlow = ReportsFlow.EXPENSES,

    /**
     * List of category comparison items with multi-month data and sparklines.
     */
    val comparisonItems: List<CategoryComparisonItem> = emptyList(),

    /**
     * Overall summary of trends across all categories in the comparison window.
     */
    val comparisonSummary: ComparisonOverviewSummary? = null
) {

    /**
     * True when the report represents all accounts.
     */
    val isAllAccountsSelected: Boolean
        get() = selectedAccountIds.isEmpty()

    /**
     * Selected expense category model.
     */
    val selectedExpenseCategoryModel: ReportsExpenseCategory?
        get() = selectedExpenseCategory?.let { category ->
            expenseCategories.firstOrNull {
                it.category == category
            }
        }

    /**
     * Selected income category model.
     */
    val selectedIncomeCategoryModel: ReportsIncomeCategory?
        get() = selectedIncomeCategory?.let { category ->
            incomeCategories.firstOrNull {
                it.category == category
            }
        }

    /**
     * User-facing account filter label.
     */
    val accountFilterLabel: String
        get() {
            if (selectedAccountIds.isEmpty()) {
                return "All Accounts"
            }

            if (selectedAccountIds.size == 1) {
                val account = accounts.firstOrNull {
                    it.accountId == selectedAccountIds.first()
                }

                return account?.displayName
                    ?: "1 Account"
            }

            return "${selectedAccountIds.size} Accounts"
        }

    /**
     * Formatted string for the current reporting period.
     * When in COMPARE tab, dynamically reflects the selected 3M / 6M comparison window.
     */
    val formattedPeriodLabel: String
        get() {
            val monthYearFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.ENGLISH)
            val fullMonthYearFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH)
            val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH)

            if (currentTab == ReportsTab.COMPARE) {
                val monthsCount = comparisonWindow.monthsCount
                val startMonth = selectedMonth.minusMonths((monthsCount - 1).toLong())
                return "${startMonth.format(monthYearFormatter)} – ${selectedMonth.format(monthYearFormatter)}"
            }

            return when (periodFilter) {
                PeriodFilter.THIS_MONTH -> {
                    selectedMonth.format(fullMonthYearFormatter)
                }
                PeriodFilter.LAST_3_MONTHS -> {
                    val startMonth = selectedMonth.minusMonths(2)
                    "${startMonth.format(monthYearFormatter)} – ${selectedMonth.format(monthYearFormatter)}"
                }
                PeriodFilter.LAST_6_MONTHS -> {
                    val startMonth = selectedMonth.minusMonths(5)
                    "${startMonth.format(monthYearFormatter)} – ${selectedMonth.format(monthYearFormatter)}"
                }
                PeriodFilter.YEAR_TO_DATE -> {
                    val startMonth = selectedMonth.withMonth(1)
                    if (selectedMonth.monthValue == 1) {
                        startMonth.format(fullMonthYearFormatter)
                    } else {
                        "${startMonth.format(monthYearFormatter)} – ${selectedMonth.format(monthYearFormatter)}"
                    }
                }
                PeriodFilter.CUSTOM -> {
                    "${customStartDate.format(dateFormatter)} – ${customEndDate.format(dateFormatter)}"
                }
            }
        }

    val isPreviousPeriodEnabled: Boolean
        get() {
            if (currentTab == ReportsTab.COMPARE) return true
            return periodFilter != PeriodFilter.CUSTOM
        }

    val isNextPeriodEnabled: Boolean
        get() {
            if (currentTab == ReportsTab.COMPARE) return true
            if (periodFilter == PeriodFilter.CUSTOM) return false
            val currentMonth = YearMonth.now()
            if (periodFilter == PeriodFilter.YEAR_TO_DATE) {
                return selectedMonth < currentMonth
            }
            return true
        }

    /**
     * Actual inclusive date range represented by the report.
     */
    val dateRange: ReportDateRange
    get() {

        return when (periodFilter) {

            PeriodFilter.THIS_MONTH -> {

                ReportDateRange(
                    startDate =
                        selectedMonth
                            .atDay(1),

                    endDate =
                        selectedMonth
                            .atEndOfMonth()
                )
            }

            PeriodFilter.LAST_3_MONTHS -> {

                val startMonth =
                    selectedMonth
                        .minusMonths(2)

                ReportDateRange(
                    startDate =
                        startMonth.atDay(1),

                    endDate =
                        selectedMonth.atEndOfMonth()
                )
            }

            PeriodFilter.LAST_6_MONTHS -> {

                val startMonth =
                    selectedMonth
                        .minusMonths(5)

                ReportDateRange(
                    startDate =
                        startMonth.atDay(1),

                    endDate =
                        selectedMonth.atEndOfMonth()
                )
            }

            PeriodFilter.YEAR_TO_DATE -> {

                ReportDateRange(
                    startDate =
                        selectedMonth
                            .withMonth(1)
                            .atDay(1),

                    endDate =
                        selectedMonth
                            .atEndOfMonth()
                )
            }

            PeriodFilter.CUSTOM -> {

                ReportDateRange(
                    startDate =
                        customStartDate,

                    endDate =
                        customEndDate
                )
            }
        }
    }
}

/**
 * Reporting period.
 *
 * MONTH is the only active period at this stage.
 *
 * WEEK / QUARTER / YEAR / CUSTOM are included now so that
 * future report filtering can be added without changing
 * the architecture.
 */
enum class ReportPeriod {
    WEEK,
    MONTH,
    QUARTER,
    YEAR,
    CUSTOM
}

/**
 * Account displayed by the Reports filter.
 *
 * The full account number is never stored/displayed here.
 */
data class ReportsAccount(

    /**
     * Stable internal account identifier.
     */
    val accountId: String,

    /**
     * Last four digits for safe display.
     */
    val accountLast4: String?
) {

    val displayName: String
        get() = accountLast4
            ?.takeIf { it.isNotBlank() }
            ?.let { "Account ••••$it" }
            ?: "Account"
}

/**
 * Top-level money-flow section.
 */
enum class ReportsFlow {
    EXPENSES,
    INCOME
}

/**
 * Monthly cash-flow totals.
 */
data class ReportsCashFlow(

    /**
     * Actual income after excluding reimbursements
     * and account transfers.
     */
    val actualIncome: Double = 0.0,

    /**
     * Effective expense after excluding transfers
     * and subtracting reimbursements.
     */
    val effectiveExpense: Double = 0.0,

    /**
     * actualIncome - effectiveExpense.
     */
    val netCashFlow: Double = 0.0
)

/**
 * Expense category summary.
 */
data class ReportsExpenseCategory(

    val category: String,

    val totalAmount: Double = 0.0,

    val normalAmount: Double = 0.0,

    val financialEventAmount: Double = 0.0,

    val reimbursedAmount: Double = 0.0,

    val effectiveFinancialEventAmount: Double = 0.0
)

/**
 * Income category summary.
 */
data class ReportsIncomeCategory(

    val category: String,

    val totalAmount: Double = 0.0
)

/**
 * Financial Event summary used by Reports.
 */
data class ReportsFinancialEvent(

    val transactionLinkId: String,

    val groupName: String,

    val category: String,

    val expenseAmount: Double = 0.0,

    val reimbursedAmount: Double = 0.0,

    val effectiveCost: Double = 0.0,

    val totalEventExpense: Double = 0.0,

    val totalEventReimbursement: Double = 0.0,

    val isFinalMonth: Boolean = true,

    val coveredMonths: List<YearMonth> = emptyList()
)
