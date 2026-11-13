package com.varsel.expensetracker.data.repository

import com.varsel.expensetracker.data.local.dao.BudgetDao
import com.varsel.expensetracker.data.local.dao.FinancialEventAllocationDao
import com.varsel.expensetracker.data.local.dao.LoanAccountDao
import com.varsel.expensetracker.data.local.dao.LoanPaymentDao
import com.varsel.expensetracker.data.local.dao.RecurringItemDao
import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.local.dao.TransactionDao
import com.varsel.expensetracker.data.preference.DataQualityPreferences
import com.varsel.expensetracker.domain.engine.DataQualityAuditEngine
import com.varsel.expensetracker.domain.model.quality.DataQualityReport
import com.varsel.expensetracker.domain.model.quality.QualitySeverity
import com.varsel.expensetracker.domain.model.quality.QualitySummaryUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
        budgetDao.getAllBudgets(),
        financialEventAllocationDao.getAllAllocations(),
        preferences.dismissedIssueKeys
    ) { txs, snaps, recurring, loans, budgets, allocs, dismissed ->
        auditEngine.auditWithData(
            transactions = txs,
            snapshots = snaps,
            recurringItems = recurring,
            loanAccounts = loans,
            budgets = budgets,
            allocations = allocs,
            dismissedKeys = dismissed
        )
    }

    val qualitySummary: Flow<QualitySummaryUiModel> = dataQualityReport.combine(preferences.dismissedIssueKeys) { report, _ ->
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
