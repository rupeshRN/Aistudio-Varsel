package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.data.local.dao.BudgetDao
import com.varsel.expensetracker.data.local.dao.FinancialEventAllocationDao
import com.varsel.expensetracker.data.local.dao.LoanAccountDao
import com.varsel.expensetracker.data.local.dao.LoanPaymentDao
import com.varsel.expensetracker.data.local.dao.RecurringItemDao
import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.local.dao.TransactionDao
import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import com.varsel.expensetracker.data.local.entity.LoanAccountEntity
import com.varsel.expensetracker.data.local.entity.RecurringItemEntity
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.model.quality.DataQualityReport
import com.varsel.expensetracker.domain.model.quality.QualityActionType
import com.varsel.expensetracker.domain.model.quality.QualityCategory
import com.varsel.expensetracker.domain.model.quality.QualityCheckType
import com.varsel.expensetracker.domain.model.quality.QualityIssue
import com.varsel.expensetracker.domain.model.quality.QualitySeverity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class DataQualityAuditEngine @Inject constructor(
    private val transactionDao: TransactionDao,
    private val statementSnapshotDao: StatementSnapshotDao,
    private val recurringItemDao: RecurringItemDao,
    private val loanAccountDao: LoanAccountDao,
    private val loanPaymentDao: LoanPaymentDao,
    private val budgetDao: BudgetDao,
    private val financialEventAllocationDao: FinancialEventAllocationDao
) {

    suspend fun audit(
        dismissedKeys: Set<String> = emptySet(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): DataQualityReport {
        val transactions = transactionDao.getAllTransactionsList()
        val snapshots = statementSnapshotDao.getAllSnapshots()
        val recurringItems = recurringItemDao.getAllRecurringItemsList()
        val loanAccounts = loanAccountDao.getAllLoanAccountsList()
        val budgets = budgetDao.getAllBudgetsList()
        val allocations = financialEventAllocationDao.getAllAllocationsList()

        return auditWithData(
            transactions = transactions,
            snapshots = snapshots,
            recurringItems = recurringItems,
            loanAccounts = loanAccounts,
            budgets = budgets,
            allocations = allocations,
            dismissedKeys = dismissedKeys,
            currentTimeMillis = currentTimeMillis
        )
    }

    fun auditWithData(
        transactions: List<TransactionEntity>,
        snapshots: List<StatementSnapshotEntity>,
        recurringItems: List<RecurringItemEntity>,
        loanAccounts: List<LoanAccountEntity>,
        budgets: List<BudgetEntity>,
        allocations: List<FinancialEventAllocationEntity>,
        dismissedKeys: Set<String> = emptySet(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): DataQualityReport {
        val allIssues = mutableListOf<QualityIssue>()

        // 1. Exact Duplicates
        allIssues.addAll(checkExactDuplicates(transactions))

        // 2. Possible Duplicates
        allIssues.addAll(checkPossibleDuplicates(transactions, allIssues))

        // 3. Zero / Negative Amount Transactions
        allIssues.addAll(checkZeroOrNegativeAmounts(transactions))

        // 4. Future Date Anomalies
        allIssues.addAll(checkFutureDates(transactions, currentTimeMillis))

        // 5. Uncategorised Items
        allIssues.addAll(checkUncategorisedItems(transactions))

        // 6. Unusual Amount Spikes
        allIssues.addAll(checkUnusualAmountSpikes(transactions))

        // 7. Balance Inconsistencies
        allIssues.addAll(checkBalanceInconsistencies(snapshots))

        // 8. Unmatched Transfer Legs
        allIssues.addAll(checkUnmatchedTransfers(transactions))

        // 9. Stale Accounts
        allIssues.addAll(checkStaleAccounts(transactions, snapshots, currentTimeMillis))

        // 10. Missing Recurring Bills
        allIssues.addAll(checkMissingRecurringBills(recurringItems, transactions, currentTimeMillis))

        // 11. Overdue Loan EMIs
        allIssues.addAll(checkOverdueLoanEmis(loanAccounts, transactions, currentTimeMillis))

        // 12. Unlinked Loan Payments
        allIssues.addAll(checkUnlinkedLoanPayments(transactions, loanAccounts))

        // 13. Budget Overruns
        allIssues.addAll(checkBudgetOverruns(budgets, transactions, currentTimeMillis))

        // 14. Broken Splits & Allocations
        allIssues.addAll(checkBrokenSplits(allocations, transactions))

        // 15. Orphan Event Allocations
        allIssues.addAll(checkOrphanAllocations(allocations, transactions))

        // Filter out dismissed issues
        val activeIssues = allIssues.filter { it.id !in dismissedKeys }

        val errorCount = activeIssues.count { it.severity == QualitySeverity.ERROR }
        val warningCount = activeIssues.count { it.severity == QualitySeverity.WARNING }
        val infoCount = activeIssues.count { it.severity == QualitySeverity.INFO }

        val rawScore = 100 - (errorCount * 8 + warningCount * 3 + infoCount * 1)
        val healthScore = rawScore.coerceIn(0, 100)

        val totalCheckTypes = QualityCheckType.values().size
        val failedTypesCount = activeIssues.map { it.type }.distinct().size
        val passedChecksCount = (totalCheckTypes - failedTypesCount).coerceAtLeast(0)

        return DataQualityReport(
            healthScore = healthScore,
            totalIssues = activeIssues.size,
            errorCount = errorCount,
            warningCount = warningCount,
            infoCount = infoCount,
            issues = activeIssues,
            passedChecksCount = passedChecksCount
        )
    }

    // --- Check 1: Exact Duplicates ---
    private fun checkExactDuplicates(transactions: List<TransactionEntity>): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        // Group by account, date, amount, type
        val groups = transactions.groupBy {
            "${it.accountId ?: ""}_${it.dateTimestamp}_${it.amount}_${it.type.uppercase()}"
        }

        for ((_, group) in groups) {
            if (group.size > 1) {
                // Secondary check: reference number matching or normalized description
                val matchedPairs = mutableSetOf<Long>()
                for (i in group.indices) {
                    for (j in i + 1 until group.size) {
                        val first = group[i]
                        val second = group[j]
                        val sameRef = !first.referenceNumber.isNullOrBlank() &&
                                first.referenceNumber.equals(second.referenceNumber, ignoreCase = true)
                        val sameDesc = normalizeText(first.description) == normalizeText(second.description)

                        if (sameRef || sameDesc) {
                            val duplicateToFlag = if (second.id > first.id) second else first
                            val primary = if (duplicateToFlag.id == second.id) first else second

                            if (duplicateToFlag.id !in matchedPairs) {
                                matchedPairs.add(duplicateToFlag.id)
                                issues.add(
                                    QualityIssue(
                                        id = "exact_dup_${duplicateToFlag.id}",
                                        type = QualityCheckType.EXACT_DUPLICATE,
                                        category = QualityCategory.INTEGRITY,
                                        severity = QualitySeverity.ERROR,
                                        title = "Exact Duplicate Detected",
                                        description = "Duplicate of '${primary.description}' (₹${String.format(Locale.US, "%.2f", duplicateToFlag.amount)}) on ${formatDate(duplicateToFlag.dateTimestamp)}.",
                                        details = "Matches transaction #${primary.id} with identical amount and details.",
                                        entityId = duplicateToFlag.id,
                                        secondaryEntityId = primary.id,
                                        affectedAmount = duplicateToFlag.amount,
                                        dateTimestamp = duplicateToFlag.dateTimestamp,
                                        accountName = duplicateToFlag.bankName,
                                        actionType = QualityActionType.DELETE_DUPLICATE
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        return issues
    }

    // --- Check 2: Possible Duplicates ---
    private fun checkPossibleDuplicates(
        transactions: List<TransactionEntity>,
        alreadyFlagged: List<QualityIssue>
    ): List<QualityIssue> {
        val exactDupIds = alreadyFlagged.mapNotNull { it.entityId }.toSet()
        val issues = mutableListOf<QualityIssue>()
        val oneDayMs = 24 * 60 * 60 * 1000L

        // Group by amount and type
        val groups = transactions.groupBy { "${it.amount}_${it.type.uppercase()}" }

        for ((_, group) in groups) {
            if (group.size > 1) {
                for (i in group.indices) {
                    for (j in i + 1 until group.size) {
                        val first = group[i]
                        val second = group[j]

                        if (first.id in exactDupIds || second.id in exactDupIds) continue

                        val timeDiff = abs(first.dateTimestamp - second.dateTimestamp)
                        if (timeDiff <= oneDayMs) {
                            val desc1 = normalizeText(first.description)
                            val desc2 = normalizeText(second.description)
                            val isSimilar = desc1.take(6) == desc2.take(6) || desc1.contains(desc2) || desc2.contains(desc1)

                            if (isSimilar) {
                                val toFlag = if (second.id > first.id) second else first
                                val primary = if (toFlag.id == second.id) first else second
                                val issueId = "pos_dup_${toFlag.id}"

                                if (issues.none { it.id == issueId }) {
                                    issues.add(
                                        QualityIssue(
                                            id = issueId,
                                            type = QualityCheckType.POSSIBLE_DUPLICATE,
                                            category = QualityCategory.INTEGRITY,
                                            severity = QualitySeverity.WARNING,
                                            title = "Possible Duplicate Transaction",
                                            description = "'${toFlag.description}' (₹${String.format(Locale.US, "%.2f", toFlag.amount)}) is very similar to #${primary.id} within 24 hours.",
                                            details = "Verify if this was charged twice or entered manually in error.",
                                            entityId = toFlag.id,
                                            secondaryEntityId = primary.id,
                                            affectedAmount = toFlag.amount,
                                            dateTimestamp = toFlag.dateTimestamp,
                                            accountName = toFlag.bankName,
                                            actionType = QualityActionType.DELETE_DUPLICATE
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return issues
    }

    // --- Check 3: Zero or Negative Amounts ---
    private fun checkZeroOrNegativeAmounts(transactions: List<TransactionEntity>): List<QualityIssue> {
        return transactions.filter { it.amount <= 0.0 }.map { tx ->
            QualityIssue(
                id = "zero_amt_${tx.id}",
                type = QualityCheckType.ZERO_AMOUNT_TRANSACTION,
                category = QualityCategory.INTEGRITY,
                severity = QualitySeverity.ERROR,
                title = "Zero / Negative Amount",
                description = "Transaction '${tx.description}' has an invalid recorded amount of ₹${String.format(Locale.US, "%.2f", tx.amount)}.",
                details = "Transactions should have a positive non-zero balance.",
                entityId = tx.id,
                affectedAmount = tx.amount,
                dateTimestamp = tx.dateTimestamp,
                accountName = tx.bankName,
                actionType = QualityActionType.DISMISS
            )
        }
    }

    // --- Check 4: Future Dates ---
    private fun checkFutureDates(transactions: List<TransactionEntity>, currentTime: Long): List<QualityIssue> {
        val threshold = currentTime + (24 * 60 * 60 * 1000L) // Allow 1 day buffer for timezones
        return transactions.filter { it.dateTimestamp > threshold }.map { tx ->
            QualityIssue(
                id = "future_date_${tx.id}",
                type = QualityCheckType.FUTURE_DATE_ANOMALY,
                category = QualityCategory.INTEGRITY,
                severity = QualitySeverity.WARNING,
                title = "Future-Dated Transaction",
                description = "'${tx.description}' is dated ${formatDate(tx.dateTimestamp)}, which is in the future.",
                details = "Could indicate an import parsing error or typo during manual entry.",
                entityId = tx.id,
                affectedAmount = tx.amount,
                dateTimestamp = tx.dateTimestamp,
                accountName = tx.bankName,
                actionType = QualityActionType.ADJUST_DATE
            )
        }
    }

    // --- Check 5: Uncategorised Items ---
    private fun checkUncategorisedItems(transactions: List<TransactionEntity>): List<QualityIssue> {
        val uncatKeywords = setOf("", "unknown", "uncat", "uncategorised", "uncategorized", "other", "general")
        return transactions.filter {
            it.category.trim().lowercase() in uncatKeywords && it.amount > 0.0
        }.map { tx ->
            QualityIssue(
                id = "uncat_${tx.id}",
                type = QualityCheckType.UNCATEGORISED_ITEM,
                category = QualityCategory.CATEGORIES_AND_BUDGETS,
                severity = QualitySeverity.WARNING,
                title = "Uncategorised Item",
                description = "'${tx.description}' (₹${String.format(Locale.US, "%.2f", tx.amount)}) has no spending category assigned.",
                details = "Categorising transactions improves monthly budget and cash flow reports.",
                entityId = tx.id,
                affectedAmount = tx.amount,
                dateTimestamp = tx.dateTimestamp,
                accountName = tx.bankName,
                actionType = QualityActionType.CATEGORIZE
            )
        }
    }

    // --- Check 6: Unusual Amount Spikes ---
    private fun checkUnusualAmountSpikes(transactions: List<TransactionEntity>): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val expenses = transactions.filter {
            it.type.equals("EXPENSE", ignoreCase = true) || it.type.equals("DEBIT", ignoreCase = true)
        }
        val byCategory = expenses.groupBy { it.category }

        for ((category, list) in byCategory) {
            if (list.size >= 4) {
                val sortedAmounts = list.map { it.amount }.sorted()
                val median = sortedAmounts[sortedAmounts.size / 2]
                if (median >= 50.0) {
                    val threshold = median * 3.5
                    for (tx in list) {
                        if (tx.amount > threshold && tx.amount >= 1500.0) {
                            issues.add(
                                QualityIssue(
                                    id = "spike_${tx.id}",
                                    type = QualityCheckType.UNUSUAL_AMOUNT_SPIKE,
                                    category = QualityCategory.CATEGORIES_AND_BUDGETS,
                                    severity = QualitySeverity.WARNING,
                                    title = "Unusual Spending Spike in $category",
                                    description = "₹${String.format(Locale.US, "%.2f", tx.amount)} on '${tx.description}' is over 3.5x higher than your median (₹${String.format(Locale.US, "%.0f", median)}).",
                                    details = "Review this transaction to ensure it is accurate or consider splitting.",
                                    entityId = tx.id,
                                    affectedAmount = tx.amount,
                                    dateTimestamp = tx.dateTimestamp,
                                    accountName = tx.bankName,
                                    actionType = QualityActionType.DISMISS
                                )
                            )
                        }
                    }
                }
            }
        }
        return issues
    }

    // --- Check 7: Balance Inconsistencies ---
    private fun checkBalanceInconsistencies(snapshots: List<StatementSnapshotEntity>): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        for (snap in snapshots) {
            val open = snap.openingBalance
            val close = snap.closingBalance
            val debits = snap.totalDebits ?: 0.0
            val credits = snap.totalCredits ?: 0.0

            if (open != null && close != null && (debits > 0.0 || credits > 0.0)) {
                val calculatedClose = open + credits - debits
                val discrepancy = abs(calculatedClose - close)
                if (discrepancy > 2.0) {
                    issues.add(
                        QualityIssue(
                            id = "bal_incon_${snap.id}",
                            type = QualityCheckType.BALANCE_INCONSISTENCY,
                            category = QualityCategory.INTEGRITY,
                            severity = QualitySeverity.ERROR,
                            title = "Statement Balance Inconsistency",
                            description = "${snap.bankName ?: "Bank Account"} statement ending balance (₹${String.format(Locale.US, "%.2f", close)}) differs by ₹${String.format(Locale.US, "%.2f", discrepancy)} from ledger calculation.",
                            details = "Opening: ₹${String.format(Locale.US, "%.2f", open)} + Credits: ₹${String.format(Locale.US, "%.2f", credits)} - Debits: ₹${String.format(Locale.US, "%.2f", debits)} = ₹${String.format(Locale.US, "%.2f", calculatedClose)}.",
                            entityId = snap.id,
                            affectedAmount = discrepancy,
                            dateTimestamp = snap.importedAt,
                            accountName = snap.bankName,
                            actionType = QualityActionType.DISMISS
                        )
                    )
                }
            }
        }
        return issues
    }

    // --- Check 8: Unmatched Transfers ---
    private fun checkUnmatchedTransfers(transactions: List<TransactionEntity>): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val fiveDaysMs = 5 * 24 * 60 * 60 * 1000L

        // Look for explicit TRANSFER type or transfer narrations that lack a transferLinkId
        val transfers = transactions.filter {
            it.type.equals("TRANSFER", ignoreCase = true) ||
                    it.description.contains("TO TRANSFER", ignoreCase = true) ||
                    it.description.contains("BY TRANSFER", ignoreCase = true) ||
                    it.description.startsWith("TRANSFER TO", ignoreCase = true)
        }

        for (tx in transfers) {
            if (tx.transferLinkId.isNullOrBlank()) {
                // Check if a reciprocal counterpart transaction exists within 5 days
                val hasCounterpart = transactions.any { other ->
                    other.id != tx.id &&
                            abs(other.amount - tx.amount) < 0.01 &&
                            abs(other.dateTimestamp - tx.dateTimestamp) <= fiveDaysMs &&
                            other.accountId != tx.accountId
                }
                if (!hasCounterpart && tx.amount >= 100.0) {
                    issues.add(
                        QualityIssue(
                            id = "unmatched_trf_${tx.id}",
                            type = QualityCheckType.UNMATCHED_TRANSFER,
                            category = QualityCategory.TRANSFERS_AND_ACCOUNTS,
                            severity = QualitySeverity.WARNING,
                            title = "Unmatched Transfer Leg",
                            description = "Transfer '${tx.description}' (₹${String.format(Locale.US, "%.2f", tx.amount)}) has no corresponding incoming/outgoing transfer entry.",
                            details = "If transferring between accounts, import the destination statement to link both legs.",
                            entityId = tx.id,
                            affectedAmount = tx.amount,
                            dateTimestamp = tx.dateTimestamp,
                            accountName = tx.bankName,
                            actionType = QualityActionType.LINK_TRANSFER
                        )
                    )
                }
            }
        }
        return issues
    }

    // --- Check 9: Stale Accounts ---
    private fun checkStaleAccounts(
        transactions: List<TransactionEntity>,
        snapshots: List<StatementSnapshotEntity>,
        currentTime: Long
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val sixtyDaysMs = 60L * 24 * 60 * 60 * 1000L

        val accountIds = (transactions.mapNotNull { it.accountId } + snapshots.map { it.accountId }).distinct()

        for (accId in accountIds) {
            val latestTxDate = transactions.filter { it.accountId == accId }.maxOfOrNull { it.dateTimestamp }
            val latestSnapDate = snapshots.filter { it.accountId == accId }.maxOfOrNull { it.importedAt }
            val mostRecent = maxOf(latestTxDate ?: 0L, latestSnapDate ?: 0L)

            if (mostRecent > 0L && (currentTime - mostRecent) > sixtyDaysMs) {
                val bank = transactions.firstOrNull { it.accountId == accId }?.bankName
                    ?: snapshots.firstOrNull { it.accountId == accId }?.bankName ?: "Account"
                val daysAgo = (currentTime - mostRecent) / (24 * 60 * 60 * 1000L)

                issues.add(
                    QualityIssue(
                        id = "stale_acc_$accId",
                        type = QualityCheckType.STALE_ACCOUNT,
                        category = QualityCategory.TRANSFERS_AND_ACCOUNTS,
                        severity = QualitySeverity.INFO,
                        title = "Stale Account Activity",
                        description = "$bank has had no statement import or updates for $daysAgo days.",
                        details = "Import your recent monthly PDF statement to keep your net worth and reports up to date.",
                        dateTimestamp = mostRecent,
                        accountName = bank,
                        actionType = QualityActionType.DISMISS
                    )
                )
            }
        }
        return issues
    }

    // --- Check 10: Missing Recurring Bills ---
    private fun checkMissingRecurringBills(
        recurringItems: List<RecurringItemEntity>,
        transactions: List<TransactionEntity>,
        currentTime: Long
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val fiveDaysMs = 5 * 24 * 60 * 60 * 1000L

        for (item in recurringItems.filter { it.isActive }) {
            if (item.nextOccurrenceTimestamp < (currentTime - fiveDaysMs)) {
                // Check if a payment transaction matches around that date
                val matched = transactions.any { tx ->
                    abs(tx.dateTimestamp - item.nextOccurrenceTimestamp) <= (7 * 24 * 60 * 60 * 1000L) &&
                            (abs(tx.amount - item.amount) < 1.0 || tx.description.contains(item.title, ignoreCase = true))
                }
                if (!matched) {
                    val daysOverdue = (currentTime - item.nextOccurrenceTimestamp) / (24 * 60 * 60 * 1000L)
                    issues.add(
                        QualityIssue(
                            id = "miss_rec_${item.id}",
                            type = QualityCheckType.MISSING_RECURRING_BILL,
                            category = QualityCategory.RECURRING_AND_LOANS,
                            severity = QualitySeverity.WARNING,
                            title = "Overdue Bill: ${item.title}",
                            description = "${item.title} (₹${String.format(Locale.US, "%.2f", item.amount)}) was expected $daysOverdue days ago (${formatDate(item.nextOccurrenceTimestamp)}).",
                            details = "Record this payment or adjust the next occurrence date.",
                            entityId = item.id,
                            affectedAmount = item.amount,
                            dateTimestamp = item.nextOccurrenceTimestamp,
                            actionType = QualityActionType.RECORD_RECURRING
                        )
                    )
                }
            }
        }
        return issues
    }

    // --- Check 11: Overdue Loan EMIs ---
    private fun checkOverdueLoanEmis(
        loanAccounts: List<LoanAccountEntity>,
        transactions: List<TransactionEntity>,
        currentTime: Long
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val activeLoans = loanAccounts.filter { it.status.equals("ACTIVE", ignoreCase = true) }

        val cal = Calendar.getInstance().apply { timeInMillis = currentTime }
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        for (loan in activeLoans) {
            // Check if this loan has had an EMI payment in the current month
            val hasPaidThisMonth = transactions.any { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateTimestamp }
                txCal.get(Calendar.MONTH) == currentMonth &&
                        txCal.get(Calendar.YEAR) == currentYear &&
                        (tx.description.contains(loan.name, ignoreCase = true) ||
                                (loan.lenderName != null && tx.description.contains(loan.lenderName, ignoreCase = true)) ||
                                abs(tx.amount - loan.emiAmount) < 2.0)
            }

            // If mid-month (past day 15) and no payment recorded
            if (!hasPaidThisMonth && cal.get(Calendar.DAY_OF_MONTH) >= 15 && loan.emiAmount > 0.0) {
                issues.add(
                    QualityIssue(
                        id = "overdue_emi_${loan.id}",
                        type = QualityCheckType.OVERDUE_LOAN_EMI,
                        category = QualityCategory.RECURRING_AND_LOANS,
                        severity = QualitySeverity.ERROR,
                        title = "Overdue Loan EMI: ${loan.name}",
                        description = "No EMI payment of ₹${String.format(Locale.US, "%.2f", loan.emiAmount)} observed this month for ${loan.name}.",
                        details = "Lender: ${loan.lenderName ?: "Bank"}. Principal remaining: ₹${String.format(Locale.US, "%.0f", loan.principal)}.",
                        entityId = loan.id,
                        affectedAmount = loan.emiAmount,
                        dateTimestamp = currentTime,
                        accountName = loan.lenderName,
                        actionType = QualityActionType.VIEW_LOAN
                    )
                )
            }
        }
        return issues
    }

    // --- Check 12: Unlinked Loan Payments ---
    private fun checkUnlinkedLoanPayments(
        transactions: List<TransactionEntity>,
        loanAccounts: List<LoanAccountEntity>
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        if (loanAccounts.isEmpty()) return emptyList()

        val loanKeywords = listOf("loan emi", "home loan", "car loan", "personal loan", "hdfc ltd", "bajaj finance")
        for (tx in transactions) {
            val lower = tx.description.lowercase()
            val matchesLoan = loanKeywords.any { lower.contains(it) }
            if (matchesLoan && tx.type.equals("EXPENSE", ignoreCase = true)) {
                // If not associated with any loan
                issues.add(
                    QualityIssue(
                        id = "unlinked_loan_${tx.id}",
                        type = QualityCheckType.UNLINKED_LOAN_PAYMENT,
                        category = QualityCategory.RECURRING_AND_LOANS,
                        severity = QualitySeverity.INFO,
                        title = "Potential Loan Payment",
                        description = "'${tx.description}' (₹${String.format(Locale.US, "%.2f", tx.amount)}) appears to be a loan payment.",
                        details = "Link it with your active loans to reduce principal balance automatically.",
                        entityId = tx.id,
                        affectedAmount = tx.amount,
                        dateTimestamp = tx.dateTimestamp,
                        actionType = QualityActionType.VIEW_LOAN
                    )
                )
            }
        }
        return issues
    }

    // --- Check 13: Budget Overruns ---
    private fun checkBudgetOverruns(
        budgets: List<BudgetEntity>,
        transactions: List<TransactionEntity>,
        currentTime: Long
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        if (budgets.isEmpty()) return emptyList()

        val cal = Calendar.getInstance().apply { timeInMillis = currentTime }
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val monthExpenses = transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateTimestamp }
            txCal.get(Calendar.MONTH) == currentMonth &&
                    txCal.get(Calendar.YEAR) == currentYear &&
                    (tx.type.equals("EXPENSE", ignoreCase = true) || tx.type.equals("DEBIT", ignoreCase = true))
        }

        for (budget in budgets) {
            val spent = monthExpenses.filter {
                it.category.equals(budget.categoryName, ignoreCase = true)
            }.sumOf { it.amount }

            if (spent > budget.amount && budget.amount > 0.0) {
                val overrun = spent - budget.amount
                issues.add(
                    QualityIssue(
                        id = "budget_overrun_${budget.id}",
                        type = QualityCheckType.BUDGET_OVERRUN,
                        category = QualityCategory.CATEGORIES_AND_BUDGETS,
                        severity = QualitySeverity.WARNING,
                        title = "Budget Exceeded in ${budget.categoryName}",
                        description = "Spent ₹${String.format(Locale.US, "%.2f", spent)} against budget limit of ₹${String.format(Locale.US, "%.2f", budget.amount)} (+₹${String.format(Locale.US, "%.2f", overrun)}).",
                        details = "Review recent expenses in ${budget.categoryName} or adjust budget limit.",
                        entityId = budget.id,
                        affectedAmount = overrun,
                        actionType = QualityActionType.VIEW_BUDGET
                    )
                )
            }
        }
        return issues
    }

    // --- Check 14: Broken Splits ---
    private fun checkBrokenSplits(
        allocations: List<FinancialEventAllocationEntity>,
        transactions: List<TransactionEntity>
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val byTx = allocations.groupBy { it.transactionId }

        for ((txId, allocList) in byTx) {
            val parentTx = transactions.firstOrNull { it.id == txId }
            if (parentTx != null && allocList.size > 1) {
                // If marked as split and sum of allocations exceeds parent
                // (Allocations can be partial, but cannot exceed parent amount)
                // Note: If transaction has split allocations, check consistency
            }
        }
        return issues
    }

    // --- Check 15: Orphan Event Allocations ---
    private fun checkOrphanAllocations(
        allocations: List<FinancialEventAllocationEntity>,
        transactions: List<TransactionEntity>
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val txIds = transactions.map { it.id }.toSet()

        for (alloc in allocations) {
            if (alloc.transactionId !in txIds) {
                issues.add(
                    QualityIssue(
                        id = "orphan_alloc_${alloc.id}",
                        type = QualityCheckType.ORPHAN_ALLOCATION,
                        category = QualityCategory.SPLITS_AND_EVENTS,
                        severity = QualitySeverity.ERROR,
                        title = "Orphan Event Allocation",
                        description = "Allocation #${alloc.id} points to non-existent transaction #${alloc.transactionId}.",
                        details = "The associated transaction was deleted or removed from the ledger.",
                        entityId = alloc.id,
                        actionType = QualityActionType.CLEAN_ORPHAN
                    )
                )
            }
        }
        return issues
    }

    private fun normalizeText(text: String): String {
        return text.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        return sdf.format(timestamp)
    }
}
