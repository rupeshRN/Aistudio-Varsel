package com.varsel.expensetracker.ui.reports

import java.time.YearMonth

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
     * Currently selected reporting period.
     *
     * The first implementation supports MONTH.
     * The model is intentionally separated so WEEK, QUARTER,
     * YEAR and CUSTOM can be added later without redesigning
     * the account filter.
     */
    val period: ReportPeriod = ReportPeriod.MONTH,

    /**
     * Currently selected month.
     *
     * Used while period == MONTH.
     */
    val selectedMonth: YearMonth = YearMonth.now(),

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
    val financialEvents: List<ReportsFinancialEvent> = emptyList()
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
    /**
     * All calendar months in which this Financial Event
     * has relevant linked transactions.
     *
     * This is intentionally separate from the selected
     * report period.
     *
     * Example:
     *
     * selected report = July 2026
     *
     * event transactions:
     * - June 2026
     * - July 2026
     *
     * coveredMonths = [2026-06, 2026-07]
     *
     * The report still calculates July's effective cost only,
     * but the UI can tell the user that the event spans
     * multiple months.
     */
    val coveredMonths: List<YearMonth> = emptyList()
)
