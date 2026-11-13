package com.varsel.expensetracker.data.repository

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
import com.varsel.expensetracker.data.preference.DataQualityPreferences
import com.varsel.expensetracker.domain.engine.DataQualityAuditEngine
import com.varsel.expensetracker.domain.model.quality.DataQualityReport
import com.varsel.expensetracker.domain.model.quality.QualityIssue
import com.varsel.expensetracker.domain.model.quality.QualitySeverity
import com.varsel.expensetracker.domain.model.quality.QualitySummaryUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataQualityRepository @Inject constructor(
    private val auditEngine: DataQualityAuditEngine,
    private val transactionDao: TransactionDao,
    private val statementSnapshotDao: StatementSnapshotDao,
    private val recurringItemDao: RecurringItemDao,
    private val loanAccountDao: LoanAccountDao,
    private val loanPaymentDao: LoanPaymentDao,
    private val budgetDao: BudgetDao,
    private val financialEventAllocationDao: FinancialEventAllocationDao,
    private val preferences: DataQualityPreferences
) {

    private data class LedgerSnapshotInputs(
        val transactions: List<TransactionEntity>,
        val snapshots: List<StatementSnapshotEntity>,
        val recurringItems: List<RecurringItemEntity>,
        val loanAccounts: List<LoanAccountEntity>
    )

    private data class AuxiliaryAuditInputs(
        val loanPayments: List<LoanPaymentEntity>,
        val budgets: List<BudgetEntity>,
        val allocations: List<FinancialEventAllocationEntity>,
        val dismissedKeys: Set<String>
    )

    val dismissedIssueKeys: Flow<Set<String>> = preferences.dismissedIssueKeys

    val dataQualityReport: Flow<DataQualityReport> = combine(
        combine(
            transactionDao.getAllTransactions(),
            statementSnapshotDao.observeAllSnapshots(),
            recurringItemDao.getAllRecurringItems(),
            loanAccountDao.getAllLoanAccounts()
        ) { txs, snaps, recurring, loans ->
            LedgerSnapshotInputs(
                transactions = txs,
                snapshots = snaps,
                recurringItems = recurring,
                loanAccounts = loans
            )
        },
        combine(
            loanPaymentDao.getAllPayments(),
            budgetDao.getAllBudgets(),
            financialEventAllocationDao.getAllAllocations(),
            preferences.dismissedIssueKeys
        ) { loanPayments, budgets, allocs, dismissed ->
            AuxiliaryAuditInputs(
                loanPayments = loanPayments,
                budgets = budgets,
                allocations = allocs,
                dismissedKeys = dismissed
            )
        }
    ) { core, aux ->
        auditEngine.auditWithData(
            transactions = core.transactions,
            snapshots = core.snapshots,
            recurringItems = core.recurringItems,
            loanAccounts = core.loanAccounts,
            loanPayments = aux.loanPayments,
            budgets = aux.budgets,
            allocations = aux.allocations,
            dismissedKeys = aux.dismissedKeys
        )
    }

    val qualitySummary: Flow<QualitySummaryUiModel> = dataQualityReport.map { report ->
        val headline = if (report.totalIssues == 0) {
            "100% Healthy • All ${report.totalChecksCount} ledger checks passing"
        } else if (report.errorCount > 0) {
            "${report.totalIssues} item${if (report.totalIssues > 1) "s" else ""} need review (${report.errorCount} critical) • Tap to inspect"
        } else {
            "${report.totalIssues} item${if (report.totalIssues > 1) "s" else ""} need review • Tap to inspect"
        }

        val severity = if (report.errorCount > 0) {
            QualitySeverity.ERROR
        } else if (report.warningCount > 0) {
            QualitySeverity.WARNING
        } else {
            QualitySeverity.INFO
        }

        QualitySummaryUiModel(
            healthScore = report.healthScore,
            issueCount = report.totalIssues,
            hasIssues = report.totalIssues > 0,
            headline = headline,
            severity = severity
        )
    }

    suspend fun dismissIssue(issueKey: String) {
        preferences.dismissIssue(issueKey)
    }

    suspend fun restoreDismissedIssue(issueKey: String) {
        preferences.restoreDismissedIssue(issueKey)
    }

    suspend fun clearAllDismissed() {
        preferences.clearAllDismissed()
    }

    suspend fun deleteTransaction(transactionId: Long): Boolean {
        val entity = transactionDao.getTransactionById(transactionId) ?: return false
        transactionDao.deleteTransaction(entity)
        return true
    }

    suspend fun categorizeTransaction(transactionId: Long, categoryName: String): Boolean {
        val entity = transactionDao.getTransactionById(transactionId) ?: return false
        val updated = entity.copy(category = categoryName)
        transactionDao.updateTransaction(updated)
        return true
    }

    suspend fun adjustTransactionDate(transactionId: Long, newTimestamp: Long): Boolean {
        val entity = transactionDao.getTransactionById(transactionId) ?: return false
        val updated = entity.copy(dateTimestamp = newTimestamp)
        transactionDao.updateTransaction(updated)
        return true
    }

    suspend fun cleanOrphanIssue(issue: QualityIssue): Boolean {
        val id = issue.entityId ?: return false
        return when {
            issue.id.startsWith("orphan_alloc_") -> {
                financialEventAllocationDao.deleteAllocationById(id)
                true
            }
            issue.id.startsWith("orphan_loanpay_") -> {
                val payment = loanPaymentDao.getAllPaymentsList().firstOrNull { it.id == id } ?: return false
                loanPaymentDao.updatePayment(payment.copy(linkedTransactionId = null))
                true
            }
            issue.id.startsWith("orphan_rec_") -> {
                val tx = transactionDao.getTransactionById(id) ?: return false
                transactionDao.updateTransaction(tx.copy(recurringItemId = null))
                true
            }
            else -> false
        }
    }
}
