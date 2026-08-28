package com.varsel.expensetracker.ui.mapper

import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.dashboard.DashboardUiState
import com.varsel.expensetracker.ui.model.AccountBalanceUiModel
import com.varsel.expensetracker.ui.model.BalanceSummaryUiModel
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

class DashboardUiMapper @Inject constructor(

    private val transactionUiMapper: TransactionUiMapper

) {

    fun map(
        transactions: List<Transaction>,
        snapshots: List<StatementSnapshotEntity>
    ): DashboardUiState {

        //--------------------------------------------------
        // Calendar boundaries
        //--------------------------------------------------

        val now = Calendar.getInstance()

        val currentYear =
            now.get(Calendar.YEAR)

        val currentMonth =
            now.get(Calendar.MONTH)

        val currentMonthStart =
            calendarAtStartOfMonth(
                currentYear,
                currentMonth
            )

        val previousMonthStart =
            calendarAtStartOfMonth(
                if (currentMonth == Calendar.JANUARY) {
                    currentYear - 1
                } else {
                    currentYear
                },
                if (currentMonth == Calendar.JANUARY) {
                    Calendar.DECEMBER
                } else {
                    currentMonth - 1
                }
            )

        //--------------------------------------------------
        // Current month
        //--------------------------------------------------

        val currentMonthTransactions =
            transactions.filter {

                it.dateTimestamp >=
                    currentMonthStart

            }

        //--------------------------------------------------
        // Previous month
        //--------------------------------------------------

        val previousMonthTransactions =
            transactions.filter {

                it.dateTimestamp >=
                    previousMonthStart &&

                it.dateTimestamp <
                    currentMonthStart

            }

        //--------------------------------------------------
        // Current month financial metrics
        //--------------------------------------------------

        val currentMonthIncome =
            calculateActualIncome(
                currentMonthTransactions
            )

        val currentMonthExpense =
            calculateEffectiveExpense(
                currentMonthTransactions
            )

        //--------------------------------------------------
        // Previous month financial metrics
        //--------------------------------------------------

        val previousMonthIncome =
            calculateActualIncome(
                previousMonthTransactions
            )

        val previousMonthExpense =
            calculateEffectiveExpense(
                previousMonthTransactions
            )

        //--------------------------------------------------
        // Month-over-month percentage
        //--------------------------------------------------

        val incomeChangePercent =
            calculatePercentageChange(
                previous = previousMonthIncome,
                current = currentMonthIncome
            )

        val expenseChangePercent =
            calculatePercentageChange(
                previous = previousMonthExpense,
                current = currentMonthExpense
            )

        //--------------------------------------------------
        // Current month savings
        //--------------------------------------------------

        val savings =
            currentMonthIncome -
                currentMonthExpense

        //--------------------------------------------------
        // Account balances
        //
        // IMPORTANT:
        // This remains independent from monthly
        // income/expense reporting.
        //--------------------------------------------------

        val accountBalances =
            calculateAccountBalances(
                transactions = transactions,
                snapshots = snapshots
            )

        val totalBalance =
            accountBalances.sumOf {
                it.balance
            }

        //--------------------------------------------------
        // Dashboard state
        //--------------------------------------------------

        return DashboardUiState(

            balanceSummary =
                BalanceSummaryUiModel(

                    totalBalance =
                        totalBalance,

                    totalIncome =
                        currentMonthIncome,

                    totalExpense =
                        currentMonthExpense,

                    savings =
                        savings,

                    previousMonthIncome =
                        previousMonthIncome,

                    previousMonthExpense =
                        previousMonthExpense,

                    incomeChangePercent =
                        incomeChangePercent,

                    expenseChangePercent =
                        expenseChangePercent,

                    accounts =
                        accountBalances
                ),

            recentTransactions =
                transactions
                    .sortedByDescending {
                        it.dateTimestamp
                    }
                    .take(10)
                    .map {
                        transactionUiMapper.map(it)
                    },

            isLoading = false
        )
    }

    //--------------------------------------------------
    // Actual income
    //--------------------------------------------------

private fun calculateActualIncome(
    transactions: List<Transaction>
): Double {

    return transactions
        .filter {

            it.type ==
                TransactionType.INCOME &&

            it.role !=
                TransactionRole.REIMBURSEMENT &&

            it.role !=
                TransactionRole.TRANSFER_IN

        }
        .sumOf {
            it.amount
        }
}

    //--------------------------------------------------
    // Effective expense
    //
    // NORMAL expense:
    //     counts fully.
    //
    // LENT expense:
    //     counts as expense.
    //
    // REIMBURSEMENT:
    //     does NOT become income.
    //     Instead it offsets the expense.
    //
    // Example:
    //
    // LENT          ₹1000
    // REIMBURSEMENT ₹800
    //
    // Effective expense = ₹200
    //--------------------------------------------------

private fun calculateEffectiveExpense(
    transactions: List<Transaction>
): Double {

    val expenses =
        transactions
            .filter {

                it.type ==
                    TransactionType.EXPENSE &&

                it.role !=
                    TransactionRole.TRANSFER_OUT

            }
            .sumOf {
                it.amount
            }

    val reimbursements =
        transactions
            .filter {

                it.type ==
                    TransactionType.INCOME &&

                it.role ==
                    TransactionRole.REIMBURSEMENT

            }
            .sumOf {
                it.amount
            }

    return maxOf(
        expenses - reimbursements,
        0.0
    )
}

    //--------------------------------------------------
    // Percentage change
    //--------------------------------------------------

    private fun calculatePercentageChange(
        previous: Double,
        current: Double
    ): Double? {

        if (previous == 0.0) {
            return null
        }

        return (
            (current - previous) /
                abs(previous)
            ) * 100.0
    }

    //--------------------------------------------------
    // Calendar helper
    //--------------------------------------------------

    private fun calendarAtStartOfMonth(
        year: Int,
        month: Int
    ): Long {

        return Calendar.getInstance().apply {

            clear()

            set(
                Calendar.YEAR,
                year
            )

            set(
                Calendar.MONTH,
                month
            )

            set(
                Calendar.DAY_OF_MONTH,
                1
            )

            set(
                Calendar.HOUR_OF_DAY,
                0
            )

            set(
                Calendar.MINUTE,
                0
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
                0
            )

        }.timeInMillis
    }

    //--------------------------------------------------
    // Account balance calculation
    //--------------------------------------------------

    private fun calculateAccountBalances(
        transactions: List<Transaction>,
        snapshots: List<StatementSnapshotEntity>
    ): List<AccountBalanceUiModel> {

        val transactionsByAccount =
            transactions.groupBy {
                it.accountId
            }

        val accountIds =
            (
                transactions.mapNotNull {
                    it.accountId
                } +
                snapshots.mapNotNull {
                    it.accountId
                }
            ).distinct()

        val result =
            mutableListOf<AccountBalanceUiModel>()

        accountIds.forEach { accountId ->

            val accountTransactions =
                transactionsByAccount[accountId]
                    .orEmpty()

            val latestSnapshot =
                snapshots
                    .filter {
                        it.accountId == accountId
                    }
                    .maxWithOrNull(
                        compareBy<StatementSnapshotEntity> {
                            it.statementEndDate
                                ?: Long.MIN_VALUE
                        }.thenBy {
                            it.importedAt
                        }
                    )

            val balance =
                calculateCurrentBalance(
                    transactions =
                        accountTransactions,
                    snapshot =
                        latestSnapshot
                )

            val accountLast4 =
                latestSnapshot?.accountLast4
                    ?: accountTransactions
                        .firstOrNull()
                        ?.accountLast4

            result.add(
                AccountBalanceUiModel(

                    bankName =
                        "Bank Account",

                    accountDisplayName =
                        accountLast4
                            ?.let {
                                "•••• $it"
                            }
                            ?: "Account",

                    balance =
                        balance
                )
            )
        }

        //--------------------------------------------------
        // Legacy transactions
        //--------------------------------------------------

        val legacyTransactions =
            transactionsByAccount[null]
                .orEmpty()

        if (legacyTransactions.isNotEmpty()) {

            val legacyBalance =
                legacyTransactions.sumOf {

                    if (
                        it.type ==
                        TransactionType.INCOME
                    ) {
                        it.amount
                    } else {
                        -it.amount
                    }
                }

            result.add(
                AccountBalanceUiModel(

                    bankName =
                        "Other",

                    accountDisplayName =
                        "Unlinked",

                    balance =
                        legacyBalance
                )
            )
        }

        return result
    }

    //--------------------------------------------------
    // Current balance for one account
    //--------------------------------------------------

    private fun calculateCurrentBalance(
        transactions: List<Transaction>,
        snapshot: StatementSnapshotEntity?
    ): Double {

        if (snapshot == null) {

            return transactions.sumOf {

                if (
                    it.type ==
                    TransactionType.INCOME
                ) {
                    it.amount
                } else {
                    -it.amount
                }
            }
        }

        var balance =
            snapshot.endingBalance ?: 0.0

        val statementEnd =
            snapshot.statementEndDate
                ?: Long.MIN_VALUE

        transactions
            .filter {
                it.dateTimestamp >
                    statementEnd
            }
            .forEach { transaction ->

                balance +=
                    if (
                        transaction.type ==
                        TransactionType.INCOME
                    ) {
                        transaction.amount
                    } else {
                        -transaction.amount
                    }
            }

        return balance
    }
}
