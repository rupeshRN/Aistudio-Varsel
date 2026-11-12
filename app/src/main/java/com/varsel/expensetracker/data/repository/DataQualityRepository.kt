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

    val dismissedIssueKeys: Flow<Set<String>> = preferences.dismissedIssueKeys

    val dataQualityReport: Flow<DataQualityReport> = combine(
        transactionDao.getAllTransactions(),
        statementSnapshotDao.observeAllSnapshots(),
        recurringItemDao.getAllRecurringItems(),
        loanAccountDao.getAllLoanAccounts(),
        loanPaymentDao.getAllPayments(),
        budgetDao.getAllBudgets(),
        financialEventAllocationDao.getAllAllocations(),
        preferences.dismissedIssueKeys
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val txs = args[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val snaps = args[1] as List<StatementSnapshotEntity>
        @Suppress("UNCHECKED_CAST")
        val recurring = args[2] as List<RecurringItemEntity>
        @Suppress("UNCHECKED_CAST")
        val loans = args[3] as List<LoanAccountEntity>
        @Suppress("UNCHECKED_CAST")
        val loanPayments = args[4] as List<LoanPaymentEntity>
        @Suppress("UNCHECKED_CAST")
        val budgets = args[5] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val allocs = args[6] as List<FinancialEventAllocationEntity>
        @Suppress("UNCHECKED_CAST")
        val dismissed = args[7] as Set<String>

        auditEngine.auditWithData(
            transactions = txs,
            snapshots = snaps,
            recurringItems = recurring,
            loanAccounts = loans,
            loanPayments = loanPayments,
            budgets = budgets,
            allocations = allocs,
            dismissedKeys = dismissed
        )
    }

    val qualitySummary: Flow<QualitySummaryUiModel> = dataQualityReport.map { report ->
        val headline = if (report.totalIssues == 0) {
            "100% Healthy • All 15 ledger checks passing"
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

    suspend fun deleteOrphanAllocation(allocationId: Long): Boolean {
        financialEventAllocationDao.deleteAllocationById(allocationId)
        return true
    }
}
