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

        val hasCurrentMonthData = transactions.any { it.dateTimestamp >= currentMonthStart }
        val anchorYear: Int
        val anchorMonth: Int

        if (hasCurrentMonthData || transactions.isEmpty()) {
            anchorYear = currentYear
            anchorMonth = currentMonth
        } else {
            val latestTime = transactions.maxOf { it.dateTimestamp }
            val cal = Calendar.getInstance().apply { timeInMillis = latestTime }
            anchorYear = cal.get(Calendar.YEAR)
            anchorMonth = cal.get(Calendar.MONTH)
        }

        val activeMonthStart = calendarAtStartOfMonth(anchorYear, anchorMonth)
        val nextMonthStart = calendarAtStartOfMonth(
            if (anchorMonth == Calendar.DECEMBER) anchorYear + 1 else anchorYear,
            if (anchorMonth == Calendar.DECEMBER) Calendar.JANUARY else anchorMonth + 1
        )

        val prevYear = if (anchorMonth == Calendar.JANUARY) anchorYear - 1 else anchorYear
        val prevMonth = if (anchorMonth == Calendar.JANUARY) Calendar.DECEMBER else anchorMonth - 1
        val prevMonthStart = calendarAtStartOfMonth(prevYear, prevMonth)

        //--------------------------------------------------
        // Current / Active month transactions
        //--------------------------------------------------

        val currentMonthTransactions =
            transactions.filter {
                it.dateTimestamp >= activeMonthStart &&
                    (if (anchorYear == currentYear && anchorMonth == currentMonth) true else it.dateTimestamp < nextMonthStart)
            }

        //--------------------------------------------------
        // Previous month transactions
        //--------------------------------------------------

        val previousMonthTransactions =
            transactions.filter {
                it.dateTimestamp >= prevMonthStart &&
                    it.dateTimestamp < activeMonthStart
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

            val bankName = latestSnapshot?.bankName?.takeIf { it.isNotBlank() && it != "Bank Statement" }
                ?: detectBankName(accountTransactions)
            val bankShortName = com.varsel.expensetracker.util.BankInfoHelper.getBankShortName(bankName)

            result.add(
                AccountBalanceUiModel(
                    bankName =
                        bankName,

                    bankShortName =
                        bankShortName,

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
            val legacyShortName = if (legacyBankName != "Bank Account") com.varsel.expensetracker.util.BankInfoHelper.getBankShortName(legacyBankName) else "Manual"

            result.add(
                AccountBalanceUiModel(
                    bankName =
                        if (legacyBankName != "Bank Account") legacyBankName else "Other",

                    bankShortName =
                        legacyShortName,

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

        if (snapshot == null || snapshot.endingBalance == null) {
            return transactions.sumOf {
                if (it.type == TransactionType.INCOME) {
                    it.amount
                } else {
                    -it.amount
                }
            }
        }

        var balance: Double = snapshot.endingBalance ?: 0.0

        val statementEnd = snapshot.statementEndDate ?: Long.MIN_VALUE

        transactions
            .filter {
                it.dateTimestamp > statementEnd
            }
            .forEach { transaction ->
                balance += if (transaction.type == TransactionType.INCOME) {
                    transaction.amount
                } else {
                    -transaction.amount
                }
            }

        return balance
    }

    private fun detectBankName(transactions: List<Transaction>): String {
        if (transactions.isEmpty()) return "Bank Account"

        val explicitBank = transactions.mapNotNull { it.bankName }.firstOrNull { it.isNotBlank() && it != "Bank Account" && it != "Bank Statement" }
        if (explicitBank != null) return explicitBank

        val bankScores = mutableMapOf<String, Int>()

        for (t in transactions) {
            val ref = t.referenceNumber.orEmpty().uppercase()
            val desc = t.description.uppercase()
            val fp = t.transactionFingerprint.orEmpty().uppercase()

            fun vote(name: String, weight: Int = 1) {
                bankScores[name] = (bankScores[name] ?: 0) + weight
            }

            // Indian Bank indicators
            if (ref.contains("IDIB") || desc.contains("INDIAN BANK") || desc.contains("IND BL") || fp.contains("IDIB") || fp.contains("INDIAN_BANK") || fp.contains("INDIANBANK") || fp.contains("IB_")) {
                vote("Indian Bank", 3)
            }
            // ICICI Bank indicators
            if (desc.contains("ICICI BANK") || desc.contains("ICICI.BANK") || desc.contains("ICICIBANK") || desc.contains("ICICI DIRECT") || ref.contains("ICIC0") || fp.contains("ICICI") || fp.contains("ICIC")) {
                vote("ICICI Bank", 3)
            } else if (desc.contains("ICICI") || ref.contains("ICIC")) {
                vote("ICICI Bank", 1)
            }
            // SBI
            if (ref.contains("SBIN") || desc.contains("STATE BANK OF INDIA") || desc.contains("SBI MAIN") || fp.contains("SBI")) vote("SBI", 3)
            // HDFC
            if (ref.contains("HDFC") || desc.contains("HDFC BANK") || fp.contains("HDFC")) vote("HDFC Bank", 3)
            // Axis
            if (ref.contains("UTIB") || desc.contains("AXIS BANK") || fp.contains("AXIS")) vote("Axis Bank", 3)
            // Kotak
            if (ref.contains("KKBK") || desc.contains("KOTAK MAHINDRA") || fp.contains("KOTAK")) vote("Kotak Bank", 3)
            // Canara
            if (ref.contains("CNRB") || desc.contains("CANARA BANK") || fp.contains("CANARA")) vote("Canara Bank", 3)
            // Bank of Baroda
            if (ref.contains("BARB") || desc.contains("BANK OF BARODA") || fp.contains("BARODA")) vote("Bank of Baroda", 3)
            // PNB
            if (ref.contains("PUNB") || desc.contains("PUNJAB NATIONAL") || fp.contains("PNB")) vote("PNB", 3)
            // IDFC
            if (ref.contains("IDFB") || desc.contains("IDFC FIRST") || fp.contains("IDFC")) vote("IDFC FIRST", 3)
            // Federal Bank
            if (ref.contains("FDRL") || desc.contains("FEDERAL BANK") || fp.contains("FEDERAL")) vote("Federal Bank", 3)
            // IndusInd
            if (ref.contains("INDB") || desc.contains("INDUSIND BANK") || fp.contains("INDUSIND")) vote("IndusInd Bank", 3)
            // Union Bank
            if (ref.contains("UBIN") || desc.contains("UNION BANK") || fp.contains("UNION")) vote("Union Bank", 3)
            // IOB
            if (ref.contains("IOBA") || desc.contains("INDIAN OVERSEAS") || fp.contains("IOB")) vote("Indian Overseas Bank", 3)
            // Central Bank
            if (ref.contains("CBIN") || desc.contains("CENTRAL BANK") || fp.contains("CBI")) vote("Central Bank", 3)
            // Bank of India
            if (ref.contains("BKID") || desc.contains("BANK OF INDIA") || fp.contains("BOI")) vote("Bank of India", 3)
            // Yes Bank
            if (ref.contains("YESB") || desc.contains("YES BANK") || fp.contains("YES")) vote("Yes Bank", 3)
            // RBL Bank
            if (ref.contains("RATN") || desc.contains("RBL BANK") || fp.contains("RBL")) vote("RBL Bank", 3)
            // Standard Chartered
            if (ref.contains("SCBL") || desc.contains("STANDARD CHARTERED") || fp.contains("SCBL") || fp.contains("SC_")) vote("Standard Chartered", 3)
            // Citi Bank
            if (ref.contains("CITI") || desc.contains("CITIBANK") || fp.contains("CITI")) vote("Citi Bank", 3)
        }

        val topBank = bankScores.maxByOrNull { it.value }
        if (topBank != null && topBank.value > 0) {
            return topBank.key
        }

        return "Bank Account"
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
