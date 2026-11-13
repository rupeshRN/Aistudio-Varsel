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
import com.varsel.expensetracker.data.local.entity.LoanPaymentEntity
import com.varsel.expensetracker.data.local.entity.RecurringItemEntity
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import com.varsel.expensetracker.domain.model.TransactionRole
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
        val loanPayments = loanPaymentDao.getAllPaymentsList()
        val budgets = budgetDao.getAllBudgetsList()
        val allocations = financialEventAllocationDao.getAllAllocationsList()

        return auditWithData(
            transactions = transactions,
            snapshots = snapshots,
            recurringItems = recurringItems,
            loanAccounts = loanAccounts,
            loanPayments = loanPayments,
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
        loanPayments: List<LoanPaymentEntity> = emptyList(),
        budgets: List<BudgetEntity>,
        allocations: List<FinancialEventAllocationEntity>,
        dismissedKeys: Set<String> = emptySet(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): DataQualityReport {
        val allIssues = mutableListOf<QualityIssue>()

        // 1. Exact Duplicates (cross-source or manual + import overlap)
        allIssues.addAll(checkExactDuplicates(transactions))

        // 2. Possible Duplicates
        allIssues.addAll(checkPossibleDuplicates(transactions, allIssues))

        // 3. Zero / Negative Amount Transactions
        allIssues.addAll(checkZeroOrNegativeAmounts(transactions))

        // 4. Future Date Anomalies (excluding scheduled recurring entries)
        allIssues.addAll(checkFutureDates(transactions, currentTimeMillis))

        // 5. Statement Balance Inconsistencies
        allIssues.addAll(checkBalanceInconsistencies(snapshots))

        // 6. Transaction Role & Type Mismatches
        allIssues.addAll(checkRoleTypeMismatches(transactions))

        // 7. Uncategorised Items
        allIssues.addAll(checkUncategorisedItems(transactions))

        // 8. Unusual Spending Spikes (merchant median or category median)
        allIssues.addAll(checkUnusualAmountSpikes(transactions))

        // 9. Budget Overruns
        allIssues.addAll(checkBudgetOverruns(budgets, transactions, currentTimeMillis))

        // 10. Unmatched or Broken Transfer Legs
        allIssues.addAll(checkUnmatchedTransfers(transactions))

        // 11. Stale Accounts
        allIssues.addAll(checkStaleAccounts(transactions, snapshots, currentTimeMillis))

        // 12. Missing Recurring Bills
        allIssues.addAll(checkMissingRecurringBills(recurringItems, transactions, currentTimeMillis))

        // 13. Overdue Loan EMIs
        allIssues.addAll(checkOverdueLoanEmis(loanAccounts, loanPayments, transactions, currentTimeMillis))

        // 14. Unlinked Loan Payments
        allIssues.addAll(checkUnlinkedLoanPayments(transactions, loanAccounts, loanPayments))

        // 15. Old Unsettled Receivables (LENT > 30 days)
        allIssues.addAll(checkOldReceivables(transactions, allocations, currentTimeMillis))

        // 16. Over-Allocation on Split Event Transactions
        allIssues.addAll(checkOverAllocations(allocations, transactions))

        // 17. Over-Settlement in Financial Events (Reimbursements > Lent)
        allIssues.addAll(checkOverSettlement(transactions, allocations))

        // 18. Orphan Mappings (Allocations / LoanPayments / Recurring links pointing to deleted entries)
        allIssues.addAll(checkOrphanMappings(allocations, loanPayments, recurringItems, transactions))

        val dismissedCount = allIssues.count { it.id in dismissedKeys }
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
            passedChecksCount = passedChecksCount,
            totalChecksCount = totalCheckTypes,
            dismissedCount = dismissedCount
        )
    }

    // --- Check 1: Exact Duplicates ---
    private fun checkExactDuplicates(transactions: List<TransactionEntity>): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val groups = transactions.groupBy {
            "${it.accountId ?: ""}_${dayKey(it.dateTimestamp)}_${it.amount}_${it.type.uppercase()}"
        }

        for ((_, group) in groups) {
            if (group.size > 1) {
                val matchedIds = mutableSetOf<Long>()
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

                            if (duplicateToFlag.id !in matchedIds) {
                                matchedIds.add(duplicateToFlag.id)
                                issues.add(
                                    QualityIssue(
                                        id = "exact_dup_${duplicateToFlag.id}",
                                        type = QualityCheckType.EXACT_DUPLICATE,
                                        category = QualityCategory.INTEGRITY,
                                        severity = QualitySeverity.ERROR,
                                        title = "Exact Duplicate Detected",
                                        description = "Duplicate of '${primary.description}' (${formatCurrency(duplicateToFlag.amount)}) on ${formatDate(duplicateToFlag.dateTimestamp)}.",
                                        details = "Matches transaction #${primary.id} with identical account, date, amount, and reference/description.",
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

        val groups = transactions.groupBy { "${it.accountId ?: ""}_${it.amount}_${it.type.uppercase()}" }

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
                            val isSimilar = desc1.isNotEmpty() && desc2.isNotEmpty() && (
                                (desc1.length >= 5 && desc2.length >= 5 && desc1.take(6) == desc2.take(6)) ||
                                    desc1.contains(desc2) ||
                                    desc2.contains(desc1)
                                )

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
                                            description = "'${toFlag.description}' (${formatCurrency(toFlag.amount)}) looks similar to #${primary.id} within 24 hours.",
                                            details = "May occur when a manual entry overlaps with an imported bank statement.",
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
                description = "Transaction '${tx.description}' has an invalid recorded amount of ${formatCurrency(tx.amount)}.",
                details = "Open the transaction to correct its amount or remove it.",
                entityId = tx.id,
                affectedAmount = tx.amount,
                dateTimestamp = tx.dateTimestamp,
                accountName = tx.bankName,
                actionType = QualityActionType.VIEW_TRANSACTION
            )
        }
    }

    // --- Check 4: Future Dates ---
    private fun checkFutureDates(transactions: List<TransactionEntity>, currentTime: Long): List<QualityIssue> {
        val threshold = currentTime + (24 * 60 * 60 * 1000L) // Allow 1 day buffer for timezones
        return transactions.filter {
            it.dateTimestamp > threshold && it.recurringItemId == null
        }.map { tx ->
            QualityIssue(
                id = "future_date_${tx.id}",
                type = QualityCheckType.FUTURE_DATE_ANOMALY,
                category = QualityCategory.INTEGRITY,
                severity = QualitySeverity.WARNING,
                title = "Future-Dated Transaction",
                description = "'${tx.description}' is dated ${formatDate(tx.dateTimestamp)}, which is in the future.",
                details = "Could indicate a statement date parsing issue or typo during manual entry.",
                entityId = tx.id,
                affectedAmount = tx.amount,
                dateTimestamp = tx.dateTimestamp,
                accountName = tx.bankName,
                actionType = QualityActionType.ADJUST_DATE
            )
        }
    }

    // --- Check 5: Statement Balance Inconsistencies ---
    private fun checkBalanceInconsistencies(snapshots: List<StatementSnapshotEntity>): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        for (snap in snapshots) {
            val open = snap.openingBalance
            val end = snap.endingBalance
            val debits = snap.totalDebits ?: 0.0
            val credits = snap.totalCredits ?: 0.0

            if (open != null && end != null && (debits > 0.0 || credits > 0.0)) {
                val calculatedClose = open + credits - debits
                val discrepancy = abs(calculatedClose - end)
                if (discrepancy > 2.0) {
                    issues.add(
                        QualityIssue(
                            id = "bal_incon_${snap.id}",
                            type = QualityCheckType.BALANCE_INCONSISTENCY,
                            category = QualityCategory.INTEGRITY,
                            severity = QualitySeverity.ERROR,
                            title = "Statement Balance Inconsistency",
                            description = "${snap.bankName ?: "Bank Account"} ending balance (${formatCurrency(end)}) differs by ${formatCurrency(discrepancy)} from ledger math.",
                            details = "Open ${formatCurrency(open)} + Credits ${formatCurrency(credits)} - Debits ${formatCurrency(debits)} = ${formatCurrency(calculatedClose)}.",
                            entityId = snap.id,
                            affectedAmount = discrepancy,
                            dateTimestamp = snap.importedAt,
                            accountName = snap.bankName,
                            actionType = QualityActionType.IMPORT_STATEMENT
                        )
                    )
                }
            }
        }
        return issues
    }

    // --- Check 6: Transaction Role & Type Mismatches ---
    private fun checkRoleTypeMismatches(transactions: List<TransactionEntity>): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        for (tx in transactions) {
            val isExpense = tx.type.equals("EXPENSE", ignoreCase = true) || tx.type.equals("DEBIT", ignoreCase = true)
            val isIncome = tx.type.equals("INCOME", ignoreCase = true) || tx.type.equals("CREDIT", ignoreCase = true)

            val mismatchReason = when {
                tx.role == TransactionRole.LENT.name && !isExpense ->
                    "Marked as LENT (paid on behalf of someone) but recorded as ${tx.type} instead of EXPENSE."
                tx.role == TransactionRole.REIMBURSEMENT.name && !isIncome ->
                    "Marked as REIMBURSEMENT but recorded as ${tx.type} instead of INCOME."
                tx.role == TransactionRole.TRANSFER_OUT.name && isIncome ->
                    "Marked as TRANSFER_OUT but recorded as ${tx.type}."
                tx.role == TransactionRole.TRANSFER_IN.name && isExpense ->
                    "Marked as TRANSFER_IN but recorded as ${tx.type}."
                else -> null
            }

            if (mismatchReason != null) {
                issues.add(
                    QualityIssue(
                        id = "role_mismatch_${tx.id}",
                        type = QualityCheckType.ROLE_TYPE_MISMATCH,
                        category = QualityCategory.INTEGRITY,
                        severity = QualitySeverity.ERROR,
                        title = "Role & Type Mismatch",
                        description = "'${tx.description}' (${formatCurrency(tx.amount)}) has a conflicting role configuration.",
                        details = mismatchReason,
                        entityId = tx.id,
                        affectedAmount = tx.amount,
                        dateTimestamp = tx.dateTimestamp,
                        accountName = tx.bankName,
                        actionType = QualityActionType.VIEW_TRANSACTION
                    )
                )
            }
        }
        return issues
    }

    // --- Check 7: Uncategorised Items ---
    private fun checkUncategorisedItems(transactions: List<TransactionEntity>): List<QualityIssue> {
        val uncatKeywords = setOf("", "unknown", "uncat", "uncategorised", "uncategorized")
        return transactions.filter {
            it.category.trim().lowercase() in uncatKeywords &&
                it.amount > 0.0 &&
                it.role != TransactionRole.TRANSFER_IN.name &&
                it.role != TransactionRole.TRANSFER_OUT.name
        }.map { tx ->
            QualityIssue(
                id = "uncat_${tx.id}",
                type = QualityCheckType.UNCATEGORISED_ITEM,
                category = QualityCategory.CATEGORIES_AND_BUDGETS,
                severity = QualitySeverity.WARNING,
                title = "Uncategorised Item",
                description = "'${tx.description}' (${formatCurrency(tx.amount)}) has no spending category assigned.",
                details = "Categorising transactions improves monthly budget and cash flow reports.",
                entityId = tx.id,
                affectedAmount = tx.amount,
                dateTimestamp = tx.dateTimestamp,
                accountName = tx.bankName,
                actionType = QualityActionType.CATEGORIZE
            )
        }
    }

    // --- Check 8: Unusual Spending Spikes ---
    private fun checkUnusualAmountSpikes(transactions: List<TransactionEntity>): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val flaggedIds = mutableSetOf<Long>()
        val expenses = transactions.filter {
            (it.type.equals("EXPENSE", ignoreCase = true) || it.type.equals("DEBIT", ignoreCase = true)) &&
                it.role == TransactionRole.NORMAL.name
        }

        // First: Merchant/Description median (>= 3x merchant's median when >= 3 transactions exist)
        val byMerchant = expenses.groupBy { normalizeText(it.description) }.filterKeys { it.length >= 4 }
        for ((_, list) in byMerchant) {
            if (list.size >= 3) {
                val sorted = list.map { it.amount }.sorted()
                val median = sorted[sorted.size / 2]
                if (median >= 50.0) {
                    val threshold = median * 3.0
                    for (tx in list) {
                        if (tx.amount > threshold && tx.amount >= 500.0) {
                            flaggedIds.add(tx.id)
                            issues.add(
                                QualityIssue(
                                    id = "spike_${tx.id}",
                                    type = QualityCheckType.UNUSUAL_AMOUNT_SPIKE,
                                    category = QualityCategory.CATEGORIES_AND_BUDGETS,
                                    severity = QualitySeverity.WARNING,
                                    title = "Unusual Merchant Amount",
                                    description = "${formatCurrency(tx.amount)} at '${tx.description}' is over 3x your typical spend (${formatCurrency(median)}).",
                                    details = "Tap to inspect transaction details or split shared costs.",
                                    entityId = tx.id,
                                    affectedAmount = tx.amount,
                                    dateTimestamp = tx.dateTimestamp,
                                    accountName = tx.bankName,
                                    actionType = QualityActionType.VIEW_TRANSACTION
                                )
                            )
                        }
                    }
                }
            }
        }

        // Second: Category median fallback (>= 4 items, > 3.5x category median)
        val byCategory = expenses.groupBy { it.category }
        for ((category, list) in byCategory) {
            if (list.size >= 4) {
                val sortedAmounts = list.map { it.amount }.sorted()
                val median = sortedAmounts[sortedAmounts.size / 2]
                if (median >= 50.0) {
                    val threshold = median * 3.5
                    for (tx in list) {
                        if (tx.id !in flaggedIds && tx.amount > threshold && tx.amount >= 1500.0) {
                            issues.add(
                                QualityIssue(
                                    id = "spike_${tx.id}",
                                    type = QualityCheckType.UNUSUAL_AMOUNT_SPIKE,
                                    category = QualityCategory.CATEGORIES_AND_BUDGETS,
                                    severity = QualitySeverity.WARNING,
                                    title = "Unusual Spending Spike in $category",
                                    description = "${formatCurrency(tx.amount)} on '${tx.description}' is over 3.5x your $category median (${formatCurrency(median)}).",
                                    details = "Review this transaction to verify accuracy or convert into a Financial Event if shared.",
                                    entityId = tx.id,
                                    affectedAmount = tx.amount,
                                    dateTimestamp = tx.dateTimestamp,
                                    accountName = tx.bankName,
                                    actionType = QualityActionType.VIEW_TRANSACTION
                                )
                            )
                        }
                    }
                }
            }
        }
        return issues
    }

    // --- Check 9: Budget Overruns ---
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
                (tx.type.equals("EXPENSE", ignoreCase = true) || tx.type.equals("DEBIT", ignoreCase = true)) &&
                tx.role != TransactionRole.TRANSFER_OUT.name
        }

        for (budget in budgets.filter { it.budgetType.equals("EXPENSE", ignoreCase = true) }) {
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
                        title = "Budget Exceeded: ${budget.name}",
                        description = "Spent ${formatCurrency(spent)} against ${formatCurrency(budget.amount)} limit (+${formatCurrency(overrun)}).",
                        details = "Category: ${budget.categoryName}. Open budget to review contributing expenses.",
                        entityId = budget.id,
                        affectedAmount = overrun,
                        actionType = QualityActionType.VIEW_BUDGET
                    )
                )
            }
        }
        return issues
    }

    // --- Check 10: Unmatched or Broken Transfer Legs ---
    private fun checkUnmatchedTransfers(transactions: List<TransactionEntity>): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val threeDaysMs = 3 * 24 * 60 * 60 * 1000L

        // 10a. Check broken transferLinkIds (partner transaction was deleted)
        val linkedGroups = transactions.filter { !it.transferLinkId.isNullOrBlank() }
            .groupBy { it.transferLinkId!! }
        for ((_, group) in linkedGroups) {
            if (group.size == 1) {
                val lonely = group.first()
                issues.add(
                    QualityIssue(
                        id = "unmatched_trf_${lonely.id}",
                        type = QualityCheckType.UNMATCHED_TRANSFER,
                        category = QualityCategory.TRANSFERS_AND_ACCOUNTS,
                        severity = QualitySeverity.WARNING,
                        title = "Broken Transfer Pair",
                        description = "Linked transfer '${lonely.description}' (${formatCurrency(lonely.amount)}) is missing its counterpart leg.",
                        details = "The linked counterpart transaction was likely deleted. Tap to review and relink or unlink.",
                        entityId = lonely.id,
                        affectedAmount = lonely.amount,
                        dateTimestamp = lonely.dateTimestamp,
                        accountName = lonely.bankName,
                        actionType = QualityActionType.LINK_TRANSFER
                    )
                )
            }
        }

        // 10b. Check unlinked transfer-like transactions
        val unlinkedTransfers = transactions.filter { tx ->
            tx.transferLinkId.isNullOrBlank() && (
                tx.role == TransactionRole.TRANSFER_OUT.name ||
                    tx.role == TransactionRole.TRANSFER_IN.name ||
                    tx.type.equals("TRANSFER", ignoreCase = true) ||
                    tx.category.equals("Transfer", ignoreCase = true) ||
                    tx.description.contains("SELF TRANSFER", ignoreCase = true) ||
                    tx.description.contains("OWN ACCOUNT", ignoreCase = true)
                )
        }

        for (tx in unlinkedTransfers) {
            val hasUnlinkedCandidateNearby = transactions.any { other ->
                other.id != tx.id &&
                    abs(other.amount - tx.amount) < 0.01 &&
                    abs(other.dateTimestamp - tx.dateTimestamp) <= threeDaysMs &&
                    other.accountId != tx.accountId &&
                    other.type != tx.type
            }
            if (tx.amount >= 100.0 && (hasUnlinkedCandidateNearby || tx.role == TransactionRole.TRANSFER_OUT.name || tx.role == TransactionRole.TRANSFER_IN.name || tx.type.equals("TRANSFER", ignoreCase = true))) {
                issues.add(
                    QualityIssue(
                        id = "unmatched_trf_${tx.id}",
                        type = QualityCheckType.UNMATCHED_TRANSFER,
                        category = QualityCategory.TRANSFERS_AND_ACCOUNTS,
                        severity = QualitySeverity.WARNING,
                        title = "Unmatched Transfer Leg",
                        description = "Transfer '${tx.description}' (${formatCurrency(tx.amount)}) on ${formatDate(tx.dateTimestamp)} is not linked to its other account leg.",
                        details = if (hasUnlinkedCandidateNearby) {
                            "A matching counterpart amount exists within 3 days — tap to link both legs."
                        } else {
                            "Import the destination/source bank statement or tap to review."
                        },
                        entityId = tx.id,
                        affectedAmount = tx.amount,
                        dateTimestamp = tx.dateTimestamp,
                        accountName = tx.bankName,
                        actionType = QualityActionType.LINK_TRANSFER
                    )
                )
            }
        }
        return issues
    }

    // --- Check 11: Stale Accounts ---
    private fun checkStaleAccounts(
        transactions: List<TransactionEntity>,
        snapshots: List<StatementSnapshotEntity>,
        currentTime: Long
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val fortyFiveDaysMs = 45L * 24 * 60 * 60 * 1000L

        val accountIds = (transactions.mapNotNull { it.accountId } + snapshots.mapNotNull { it.accountId }).distinct()

        for (accId in accountIds) {
            val latestTxDate = transactions.filter { it.accountId == accId }.maxOfOrNull { it.dateTimestamp }
            val latestSnapDate = snapshots.filter { it.accountId == accId }.maxOfOrNull { it.importedAt }
            val mostRecent = maxOf(latestTxDate ?: 0L, latestSnapDate ?: 0L)

            if (mostRecent > 0L && (currentTime - mostRecent) > fortyFiveDaysMs) {
                val bank = transactions.firstOrNull { it.accountId == accId }?.bankName
                    ?: snapshots.firstOrNull { it.accountId == accId }?.bankName ?: "Bank Account"
                val last4 = transactions.firstOrNull { it.accountId == accId }?.accountLast4
                    ?: snapshots.firstOrNull { it.accountId == accId }?.accountLast4
                val displayName = if (last4 != null) "$bank ••$last4" else bank
                val daysAgo = (currentTime - mostRecent) / (24 * 60 * 60 * 1000L)

                issues.add(
                    QualityIssue(
                        id = "stale_acc_$accId",
                        type = QualityCheckType.STALE_ACCOUNT,
                        category = QualityCategory.TRANSFERS_AND_ACCOUNTS,
                        severity = QualitySeverity.INFO,
                        title = "Stale Account Activity",
                        description = "$displayName has had no statement import or updates for $daysAgo days.",
                        details = "Import a recent statement PDF to keep account balance and cash flow current.",
                        dateTimestamp = mostRecent,
                        accountName = displayName,
                        actionType = QualityActionType.IMPORT_STATEMENT
                    )
                )
            }
        }
        return issues
    }

    // --- Check 12: Missing Recurring Bills ---
    private fun checkMissingRecurringBills(
        recurringItems: List<RecurringItemEntity>,
        transactions: List<TransactionEntity>,
        currentTime: Long
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val fiveDaysMs = 5 * 24 * 60 * 60 * 1000L

        for (item in recurringItems.filter { it.isActive }) {
            if (item.nextOccurrenceTimestamp < (currentTime - fiveDaysMs)) {
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
                            description = "${item.title} (${formatCurrency(item.amount)}) was expected $daysOverdue days ago (${formatDate(item.nextOccurrenceTimestamp)}).",
                            details = "Open Recurring & Subscriptions to record this payment or update the schedule.",
                            entityId = item.id,
                            affectedAmount = item.amount,
                            dateTimestamp = item.nextOccurrenceTimestamp,
                            accountName = item.bankName,
                            actionType = QualityActionType.RECORD_RECURRING
                        )
                    )
                }
            }
        }
        return issues
    }

    // --- Check 13: Overdue Loan EMIs ---
    private fun checkOverdueLoanEmis(
        loanAccounts: List<LoanAccountEntity>,
        loanPayments: List<LoanPaymentEntity>,
        transactions: List<TransactionEntity>,
        currentTime: Long
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val activeLoans = loanAccounts.filter { it.status.equals("ACTIVE", ignoreCase = true) }

        val cal = Calendar.getInstance().apply { timeInMillis = currentTime }
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        for (loan in activeLoans) {
            val hasLoanPaymentThisMonth = loanPayments.any { pay ->
                val pCal = Calendar.getInstance().apply { timeInMillis = pay.paymentDateTimestamp }
                pay.loanId == loan.id &&
                    pCal.get(Calendar.MONTH) == currentMonth &&
                    pCal.get(Calendar.YEAR) == currentYear
            }

            val hasMatchingTxThisMonth = transactions.any { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateTimestamp }
                txCal.get(Calendar.MONTH) == currentMonth &&
                    txCal.get(Calendar.YEAR) == currentYear &&
                    (tx.description.contains(loan.name, ignoreCase = true) ||
                        (loan.lenderName != null && tx.description.contains(loan.lenderName, ignoreCase = true)) ||
                        abs(tx.amount - loan.emiAmount) < 2.0)
            }

            if (!hasLoanPaymentThisMonth && !hasMatchingTxThisMonth && cal.get(Calendar.DAY_OF_MONTH) >= 15 && loan.emiAmount > 0.0) {
                issues.add(
                    QualityIssue(
                        id = "overdue_emi_${loan.id}",
                        type = QualityCheckType.OVERDUE_LOAN_EMI,
                        category = QualityCategory.RECURRING_AND_LOANS,
                        severity = QualitySeverity.ERROR,
                        title = "Overdue Loan EMI: ${loan.name}",
                        description = "No EMI payment of ${formatCurrency(loan.emiAmount)} recorded this month for ${loan.name}.",
                        details = "Lender: ${loan.lenderName ?: "Bank"}. Open loan detail to log EMI or link a statement debit.",
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

    // --- Check 14: Unlinked Loan Payments ---
    private fun checkUnlinkedLoanPayments(
        transactions: List<TransactionEntity>,
        loanAccounts: List<LoanAccountEntity>,
        loanPayments: List<LoanPaymentEntity>
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        if (loanAccounts.isEmpty()) return emptyList()

        val alreadyLinkedTxIds = loanPayments.mapNotNull { it.linkedTransactionId }.toSet()
        val loanKeywords = listOf("loan emi", "home loan", "car loan", "personal loan", "hdfc ltd", "bajaj finance", "emi payment")

        for (tx in transactions) {
            if (tx.id in alreadyLinkedTxIds) continue
            val lower = tx.description.lowercase()
            val matchesLoan = loanKeywords.any { lower.contains(it) }
            if (matchesLoan && (tx.type.equals("EXPENSE", ignoreCase = true) || tx.type.equals("DEBIT", ignoreCase = true))) {
                issues.add(
                    QualityIssue(
                        id = "unlinked_loan_${tx.id}",
                        type = QualityCheckType.UNLINKED_LOAN_PAYMENT,
                        category = QualityCategory.RECURRING_AND_LOANS,
                        severity = QualitySeverity.INFO,
                        title = "Potential Unlinked Loan EMI",
                        description = "'${tx.description}' (${formatCurrency(tx.amount)}) looks like a loan repayment.",
                        details = "Link this debit to an active loan so your principal balance stays accurate.",
                        entityId = tx.id,
                        affectedAmount = tx.amount,
                        dateTimestamp = tx.dateTimestamp,
                        accountName = tx.bankName,
                        actionType = QualityActionType.VIEW_LOAN
                    )
                )
            }
        }
        return issues
    }

    // --- Check 15: Old Unsettled Receivables (LENT > 30 days) ---
    private fun checkOldReceivables(
        transactions: List<TransactionEntity>,
        allocations: List<FinancialEventAllocationEntity>,
        currentTime: Long
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L

        val lentTransactions = transactions.filter { it.role == TransactionRole.LENT.name }
        val reimbursementsByLink = transactions
            .filter { it.role == TransactionRole.REIMBURSEMENT.name && !it.transactionLinkId.isNullOrBlank() }
            .groupBy { it.transactionLinkId!! }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val allocByLink = allocations.groupBy { it.transactionLinkId }

        for (lent in lentTransactions) {
            if ((currentTime - lent.dateTimestamp) <= thirtyDaysMs) continue

            val daysOutstanding = (currentTime - lent.dateTimestamp) / (24 * 60 * 60 * 1000L)
            val linkId = lent.transactionLinkId

            val isSettled = if (!linkId.isNullOrBlank()) {
                val totalLentInGroup = transactions
                    .filter { it.transactionLinkId == linkId && it.role == TransactionRole.LENT.name }
                    .sumOf { it.amount }
                val totalReimbInGroup = reimbursementsByLink[linkId] ?: 0.0
                val allocReimb = allocByLink[linkId]
                    ?.mapNotNull { a -> transactions.firstOrNull { it.id == a.transactionId }?.let { tx -> a to tx } }
                    ?.filter { (_, tx) -> tx.role == TransactionRole.REIMBURSEMENT.name }
                    ?.sumOf { (a, _) -> a.allocatedAmount } ?: 0.0
                (totalReimbInGroup + allocReimb) >= totalLentInGroup - 1.0
            } else {
                false
            }

            if (!isSettled) {
                issues.add(
                    QualityIssue(
                        id = "old_recv_${lent.id}",
                        type = QualityCheckType.OLD_RECEIVABLE,
                        category = QualityCategory.SPLITS_AND_EVENTS,
                        severity = QualitySeverity.WARNING,
                        title = "Receivable Outstanding > 30 Days",
                        description = "'${lent.description}' (${formatCurrency(lent.amount)}) marked as LENT has been open for $daysOutstanding days.",
                        details = "Link a reimbursement or review if you've already been repaid.",
                        entityId = lent.id,
                        affectedAmount = lent.amount,
                        dateTimestamp = lent.dateTimestamp,
                        accountName = lent.bankName,
                        actionType = QualityActionType.VIEW_TRANSACTION
                    )
                )
            }
        }
        return issues
    }

    // --- Check 16: Over-Allocation on Financial Event Splits ---
    private fun checkOverAllocations(
        allocations: List<FinancialEventAllocationEntity>,
        transactions: List<TransactionEntity>
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val byTx = allocations.groupBy { it.transactionId }

        for ((txId, allocList) in byTx) {
            val parentTx = transactions.firstOrNull { it.id == txId } ?: continue
            val allocatedSum = allocList.sumOf { it.allocatedAmount }
            if (allocatedSum > parentTx.amount + 0.01) {
                val excess = allocatedSum - parentTx.amount
                issues.add(
                    QualityIssue(
                        id = "over_alloc_$txId",
                        type = QualityCheckType.OVER_ALLOCATION,
                        category = QualityCategory.SPLITS_AND_EVENTS,
                        severity = QualitySeverity.ERROR,
                        title = "Event Allocation Exceeds Amount",
                        description = "'${parentTx.description}' (${formatCurrency(parentTx.amount)}) has ${formatCurrency(allocatedSum)} allocated across events (+${formatCurrency(excess)}).",
                        details = "Open the transaction to adjust its Financial Event allocations.",
                        entityId = txId,
                        affectedAmount = excess,
                        dateTimestamp = parentTx.dateTimestamp,
                        actionType = QualityActionType.VIEW_TRANSACTION
                    )
                )
            }
        }
        return issues
    }

    // --- Check 17: Over-Settlement in Financial Events ---
    private fun checkOverSettlement(
        transactions: List<TransactionEntity>,
        allocations: List<FinancialEventAllocationEntity>
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val txMap = transactions.associateBy { it.id }

        val eventIds = (transactions.mapNotNull { it.transactionLinkId } + allocations.map { it.transactionLinkId }).distinct()

        for (eventId in eventIds) {
            // Direct links without allocations + explicit allocations
            val directTxs = transactions.filter { it.transactionLinkId == eventId }
            val eventAllocs = allocations.filter { it.transactionLinkId == eventId }

            var lentTotal = 0.0
            var reimbTotal = 0.0

            val allocatedTxIds = eventAllocs.map { it.transactionId }.toSet()
            for (alloc in eventAllocs) {
                val tx = txMap[alloc.transactionId] ?: continue
                if (tx.role == TransactionRole.LENT.name) lentTotal += alloc.allocatedAmount
                else if (tx.role == TransactionRole.REIMBURSEMENT.name) reimbTotal += alloc.allocatedAmount
            }
            for (tx in directTxs) {
                if (tx.id in allocatedTxIds) continue
                if (tx.role == TransactionRole.LENT.name) lentTotal += tx.amount
                else if (tx.role == TransactionRole.REIMBURSEMENT.name) reimbTotal += tx.amount
            }

            if (lentTotal > 0.0 && reimbTotal > lentTotal + 1.0) {
                val excess = reimbTotal - lentTotal
                val sampleTx = directTxs.firstOrNull() ?: eventAllocs.firstNotNullOfOrNull { txMap[it.transactionId] }
                issues.add(
                    QualityIssue(
                        id = "over_settle_$eventId",
                        type = QualityCheckType.OVER_SETTLEMENT,
                        category = QualityCategory.SPLITS_AND_EVENTS,
                        severity = QualitySeverity.WARNING,
                        title = "Over-Settled Financial Event",
                        description = "Reimbursements (${formatCurrency(reimbTotal)}) exceed LENT total (${formatCurrency(lentTotal)}) by ${formatCurrency(excess)}.",
                        details = "Verify if an extra reimbursement or shared expense needs to be re-assigned.",
                        entityId = sampleTx?.id,
                        affectedAmount = excess,
                        actionType = QualityActionType.VIEW_TRANSACTION
                    )
                )
            }
        }
        return issues
    }

    // --- Check 18: Orphan Mappings ---
    private fun checkOrphanMappings(
        allocations: List<FinancialEventAllocationEntity>,
        loanPayments: List<LoanPaymentEntity>,
        recurringItems: List<RecurringItemEntity>,
        transactions: List<TransactionEntity>
    ): List<QualityIssue> {
        val issues = mutableListOf<QualityIssue>()
        val txIds = transactions.map { it.id }.toSet()
        val recurringIds = recurringItems.map { it.id }.toSet()

        for (alloc in allocations) {
            if (alloc.transactionId !in txIds) {
                issues.add(
                    QualityIssue(
                        id = "orphan_alloc_${alloc.id}",
                        type = QualityCheckType.ORPHAN_MAPPING,
                        category = QualityCategory.SPLITS_AND_EVENTS,
                        severity = QualitySeverity.ERROR,
                        title = "Orphan Event Allocation",
                        description = "Event allocation #${alloc.id} (${formatCurrency(alloc.allocatedAmount)}) points to deleted transaction #${alloc.transactionId}.",
                        details = "Tap Clean Orphan to safely remove this dangling allocation record.",
                        entityId = alloc.id,
                        affectedAmount = alloc.allocatedAmount,
                        actionType = QualityActionType.CLEAN_ORPHAN
                    )
                )
            }
        }

        for (payment in loanPayments) {
            val linkedTxId = payment.linkedTransactionId
            if (linkedTxId != null && linkedTxId !in txIds) {
                issues.add(
                    QualityIssue(
                        id = "orphan_loanpay_${payment.id}",
                        type = QualityCheckType.ORPHAN_MAPPING,
                        category = QualityCategory.RECURRING_AND_LOANS,
                        severity = QualitySeverity.ERROR,
                        title = "Orphan Loan Payment Link",
                        description = "Loan payment #${payment.id} (${formatCurrency(payment.amount)}) references deleted transaction #$linkedTxId.",
                        details = "Tap Clean Orphan to unlink the deleted transaction reference while keeping payment history.",
                        entityId = payment.id,
                        secondaryEntityId = linkedTxId,
                        affectedAmount = payment.amount,
                        actionType = QualityActionType.CLEAN_ORPHAN
                    )
                )
            }
        }

        for (tx in transactions) {
            val recId = tx.recurringItemId
            if (recId != null && recId !in recurringIds) {
                issues.add(
                    QualityIssue(
                        id = "orphan_rec_${tx.id}",
                        type = QualityCheckType.ORPHAN_MAPPING,
                        category = QualityCategory.RECURRING_AND_LOANS,
                        severity = QualitySeverity.INFO,
                        title = "Deleted Recurring Schedule Link",
                        description = "'${tx.description}' still points to deleted recurring rule #$recId.",
                        details = "Tap to clear the stale recurring reference on this transaction.",
                        entityId = tx.id,
                        affectedAmount = tx.amount,
                        dateTimestamp = tx.dateTimestamp,
                        actionType = QualityActionType.CLEAN_ORPHAN
                    )
                )
            }
        }

        return issues
    }

    private fun dayKey(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(timestamp)
    }

    private fun normalizeText(text: String): String {
        return text.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        return sdf.format(timestamp)
    }

    private fun formatCurrency(amount: Double): String {
        return "₹${String.format(Locale.US, "%.2f", amount)}"
    }
}
