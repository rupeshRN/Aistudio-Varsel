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
            val ref = t.referenceNumber.orEmpty().uppercase()
            val desc = t.description.uppercase()
            val fp = t.transactionFingerprint.orEmpty().uppercase()

            when {
                // Indian Bank (IFSC IDIB or Indian Bank text)
                ref.contains("IDIB") || desc.contains("INDIAN BANK") || desc.contains("IND BL") || fp.contains("IDIB") -> return "Indian Bank"
                ref.contains("SBIN") || desc.contains("STATE BANK OF INDIA") || desc.contains("SBI MAIN") -> return "SBI"
                ref.contains("HDFC") || desc.contains("HDFC BANK") -> return "HDFC Bank"
                ref.contains("ICIC") || desc.contains("ICICI BANK") -> return "ICICI Bank"
                ref.contains("UTIB") || desc.contains("AXIS BANK") -> return "Axis Bank"
                ref.contains("KKBK") || desc.contains("KOTAK MAHINDRA") -> return "Kotak Bank"
                ref.contains("CNRB") || desc.contains("CANARA BANK") -> return "Canara Bank"
                ref.contains("BARB") || desc.contains("BANK OF BARODA") -> return "Bank of Baroda"
                ref.contains("PUNB") || desc.contains("PUNJAB NATIONAL") -> return "PNB"
                ref.contains("IDFB") || desc.contains("IDFC FIRST") -> return "IDFC FIRST"
                ref.contains("FDRL") || desc.contains("FEDERAL BANK") -> return "Federal Bank"
                ref.contains("INDB") || desc.contains("INDUSIND BANK") -> return "IndusInd Bank"
                ref.contains("UBIN") || desc.contains("UNION BANK") -> return "Union Bank"
                ref.contains("IOBA") || desc.contains("INDIAN OVERSEAS") -> return "Indian Overseas Bank"
                ref.contains("CBIN") || desc.contains("CENTRAL BANK") -> return "Central Bank"
                ref.contains("BKID") || desc.contains("BANK OF INDIA") -> return "Bank of India"
                ref.contains("YESB") || desc.contains("YES BANK") -> return "Yes Bank"
                ref.contains("RATN") || desc.contains("RBL BANK") -> return "RBL Bank"
                ref.contains("SCBL") || desc.contains("STANDARD CHARTERED") -> return "Standard Chartered"
                ref.contains("CITI") || desc.contains("CITIBANK") -> return "Citi Bank"
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
