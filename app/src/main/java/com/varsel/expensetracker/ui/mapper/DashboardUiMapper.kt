package com.varsel.expensetracker.ui.mapper

import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.dashboard.DashboardUiState
import com.varsel.expensetracker.ui.model.AccountBalanceUiModel
import com.varsel.expensetracker.ui.model.BalanceSummaryUiModel
import com.varsel.expensetracker.ui.model.FinancialInsight
import com.varsel.expensetracker.ui.model.InsightType
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

        val insights = generateInsights(
            currentMonthTransactions = currentMonthTransactions,
            currentMonthIncome = currentMonthIncome,
            currentMonthExpense = currentMonthExpense,
            previousMonthExpense = previousMonthExpense,
            expenseChangePercent = expenseChangePercent
        )

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

            insights = insights,

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

            val bankName = detectBankName(accountTransactions)

            result.add(
                AccountBalanceUiModel(

                    bankName =
                        bankName,

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

            val legacyBankName = detectBankName(legacyTransactions)

            result.add(
                AccountBalanceUiModel(

                    bankName =
                        if (legacyBankName != "Bank Account") legacyBankName else "Other",

                    accountDisplayName =
                        "Manual",

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

    private fun detectBankName(transactions: List<Transaction>): String {
        for (t in transactions) {
            val combined = "${t.description} ${t.referenceNumber.orEmpty()} ${t.transactionFingerprint.orEmpty()}".uppercase()
            when {
                combined.contains("INDIAN BANK") || combined.contains("IDIB") || combined.contains("IND BL") -> return "Indian Bank"
                combined.contains("HDFC") -> return "HDFC Bank"
                combined.contains("SBI") || combined.contains("STATE BANK") || combined.contains("SBIN") -> return "SBI"
                combined.contains("ICICI") -> return "ICICI Bank"
                combined.contains("AXIS") || combined.contains("UTIB") -> return "Axis Bank"
                combined.contains("KOTAK") || combined.contains("KKBK") -> return "Kotak Bank"
                combined.contains("CANARA") || combined.contains("CNRB") -> return "Canara Bank"
                combined.contains("BARODA") || combined.contains("BOB") || combined.contains("BARB") -> return "Bank of Baroda"
                combined.contains("PNB") || combined.contains("PUNJAB") || combined.contains("PUNB") -> return "PNB"
                combined.contains("IDFC") -> return "IDFC FIRST"
                combined.contains("FEDERAL") || combined.contains("FDRL") -> return "Federal Bank"
                combined.contains("INDUSIND") || combined.contains("INDB") -> return "IndusInd Bank"
                combined.contains("UNION") || combined.contains("UBIN") -> return "Union Bank"
                combined.contains("IOB") || combined.contains("IOBA") || combined.contains("OVERSEAS") -> return "Indian Overseas Bank"
                combined.contains("CENTRAL") || combined.contains("CBIN") -> return "Central Bank"
                combined.contains("BOI") || combined.contains("BKID") || combined.contains("BANK OF INDIA") -> return "Bank of India"
                combined.contains("PAYTM") || combined.contains("PYTM") -> return "Paytm Payments"
                combined.contains("AIRTEL") -> return "Airtel Payments"
                combined.contains("YES BANK") || combined.contains("YESB") -> return "Yes Bank"
                combined.contains("RBL") || combined.contains("RATN") -> return "RBL Bank"
                combined.contains("STANDARD CHARTERED") || combined.contains("SCBL") -> return "Standard Chartered"
                combined.contains("CITI") -> return "Citi Bank"
            }
        }
        return "Indian Bank"
    }

    private fun generateInsights(
        currentMonthTransactions: List<Transaction>,
        currentMonthIncome: Double,
        currentMonthExpense: Double,
        previousMonthExpense: Double,
        expenseChangePercent: Double?
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()

        // 1. Top Spending Category
        val expenseTransactions = currentMonthTransactions.filter {
            it.type == TransactionType.EXPENSE && it.role != TransactionRole.TRANSFER_OUT
        }
        if (expenseTransactions.isNotEmpty() && currentMonthExpense > 0) {
            val topCategory = expenseTransactions
                .groupBy { it.category.ifBlank { "Uncategorized" } }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .maxByOrNull { it.value }

            if (topCategory != null && topCategory.value > 0) {
                val percentage = ((topCategory.value / currentMonthExpense) * 100).toInt()
                val emoji = com.varsel.expensetracker.category.CategoryMetadata.emojiForCategory(topCategory.key, isIncome = false)
                val formattedAmount = "₹%,.0f".format(topCategory.value)
                insights.add(
                    FinancialInsight(
                        emoji = emoji,
                        title = "${topCategory.key} is top expense",
                        description = "Accounts for $percentage% ($formattedAmount) of your spending this month.",
                        type = InsightType.NEUTRAL
                    )
                )
            }
        }

        // 2. Month-over-Month Velocity
        if (expenseChangePercent != null && previousMonthExpense > 0) {
            val diff = abs(currentMonthExpense - previousMonthExpense)
            val formattedDiff = "₹%,.0f".format(diff)
            if (expenseChangePercent < 0) {
                val pctVal = abs(expenseChangePercent.toInt())
                insights.add(
                    FinancialInsight(
                        emoji = "📉",
                        title = "Spending is down",
                        description = "You spent $formattedDiff less than this time last month (↓ $pctVal%).",
                        type = InsightType.POSITIVE
                    )
                )
            } else if (expenseChangePercent > 10) {
                val pctVal = expenseChangePercent.toInt()
                insights.add(
                    FinancialInsight(
                        emoji = "📈",
                        title = "Spending has increased",
                        description = "You're spending $pctVal% ($formattedDiff) more compared to last month.",
                        type = InsightType.ATTENTION
                    )
                )
            }
        }

        // 3. Savings Rate / Net Cash Flow
        if (currentMonthIncome > 0) {
            val netSavings = currentMonthIncome - currentMonthExpense
            val savingsRate = ((netSavings / currentMonthIncome) * 100).toInt()
            val formattedSavings = "₹%,.0f".format(netSavings)
            if (netSavings >= 0) {
                insights.add(
                    FinancialInsight(
                        emoji = "💰",
                        title = "Net Savings: $savingsRate%",
                        description = "$formattedSavings net surplus saved from this month's income.",
                        type = InsightType.POSITIVE
                    )
                )
            } else {
                val formattedDeficit = "₹%,.0f".format(abs(netSavings))
                insights.add(
                    FinancialInsight(
                        emoji = "⚠️",
                        title = "Deficit this month",
                        description = "Expenses exceeded total income by $formattedDeficit this month.",
                        type = InsightType.ATTENTION
                    )
                )
            }
        }

        if (insights.isEmpty()) {
            insights.add(
                FinancialInsight(
                    emoji = "💡",
                    title = "Automated Insights",
                    description = "Import your monthly bank statements to view instant spending analytics and savings rates.",
                    type = InsightType.NEUTRAL
                )
            )
        }

        return insights
    }
}
