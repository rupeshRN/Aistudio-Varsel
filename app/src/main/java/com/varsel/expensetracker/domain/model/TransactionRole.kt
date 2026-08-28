package com.varsel.expensetracker.domain.model

enum class TransactionRole {

    /**
     * Regular transaction.
     */
    NORMAL,

    /**
     * Expense paid on behalf of another person.
     */
    LENT,

    /**
     * Money received to recover a previous LENT expense.
     *
     * This remains an INCOME transaction at the
     * bank-account level, but is excluded from
     * actual-income reporting.
     */
    REIMBURSEMENT,

    /**
     * Money moved out of one of the user's own accounts.
     *
     * This is NOT an expense.
     */
    TRANSFER_OUT,

    /**
     * Money moved into one of the user's own accounts.
     *
     * This is NOT income.
     */
    TRANSFER_IN
}
