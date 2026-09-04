package com.varsel.expensetracker.ui.model

data class BalanceSummaryUiModel(

    val totalBalance: Double,

    /**
     * Current calendar month's actual income.
     *
     * Reimbursements are excluded because they are
     * recovery of previous LENT expenses, not income.
     */
    val totalIncome: Double,

    /**
     * Current calendar month's effective expense.
     *
     * LENT expenses are expenses, while REIMBURSEMENT
     * transactions reduce the effective expense.
     */
    val totalExpense: Double,

    /**
     * Current month's income minus current month's
     * effective expense.
     */
    val savings: Double,

    /**
     * Previous calendar month's income.
     */
    val previousMonthIncome: Double = 0.0,

    /**
     * Previous calendar month's effective expense.
     */
    val previousMonthExpense: Double = 0.0,

    /**
     * Percentage change in income compared with
     * the previous calendar month.
     *
     * Null means there is no meaningful percentage,
     * for example previous month was zero.
     */
    val incomeChangePercent: Double? = null,

    /**
     * Percentage change in expense compared with
     * the previous calendar month.
     *
     * Null means there is no meaningful percentage,
     * for example previous month was zero.
     */
    val expenseChangePercent: Double? = null,

    val accounts: List<AccountBalanceUiModel> = emptyList()
)

data class AccountBalanceUiModel(

    val bankName: String,

    val bankShortName: String = "",

    val accountDisplayName: String,

    val balance: Double
)
