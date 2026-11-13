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
import com.varsel.expensetracker.domain.model.quality.QualityCheckType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class DataQualityAuditEngineTest {

    private lateinit var engine: DataQualityAuditEngine
    private val dayMs = 24L * 60L * 60L * 1000L
    private val now = 1_700_000_000_000L // fixed Nov 2023 timestamp

    @Before
    fun setup() {
        engine = DataQualityAuditEngine(
            transactionDao = object : TransactionDao {
                override fun getAllTransactions(): Flow<List<TransactionEntity>> = emptyFlow()
                override suspend fun getAllTransactionsList(): List<TransactionEntity> = emptyList()
                override suspend fun insertTransactions(transactions: List<TransactionEntity>) {}
                override suspend fun insertTransaction(transaction: TransactionEntity) {}
                override suspend fun updateTransaction(transaction: TransactionEntity) {}
                override suspend fun deleteTransaction(transaction: TransactionEntity) {}
                override suspend fun getTransactionById(id: Long): TransactionEntity? = null
                override suspend fun findExistingFingerprints(fingerprints: List<String>): List<String> = emptyList()
                override suspend fun linkTransactions(transactionIds: List<Long>, transactionLinkId: String) {}
                override suspend fun linkTransferTransactions(transferOutTransactionId: Long, transferInTransactionId: Long, transferLinkId: String) {}
                override suspend fun unlinkTransfer(transactionId: Long) {}
                override suspend fun unlinkTransaction(transactionId: Long) {}
                override suspend fun getLinkedTransfer(transferLinkId: String, currentTransactionId: Long): TransactionEntity? = null
                override suspend fun getLinkedTransactions(transactionLinkId: String): List<TransactionEntity> = emptyList()
                override suspend fun getUnlinkedReimbursements(currentTransactionId: Long): List<TransactionEntity> = emptyList()
                override suspend fun updateTransactions(transactions: List<TransactionEntity>) {}
                override suspend fun getTransactionsSince(excludeId: Long, sinceTimestamp: Long): List<TransactionEntity> = emptyList()
                override suspend fun findUnlinkedTransferCandidates(type: String, amount: Double, minDate: Long, maxDate: Long): List<TransactionEntity> = emptyList()
                override suspend fun findUnlinkedTransferCandidatesByReference(type: String, referenceNumber: String): List<TransactionEntity> = emptyList()
                override suspend fun getTransactionsByIds(ids: List<Long>): List<TransactionEntity> = emptyList()
                override suspend fun getRecentUnlinkedTransactions(limit: Int): List<TransactionEntity> = emptyList()
                override suspend fun getRecentUnlinkedTransactionsSince(minDateTimestamp: Long, limit: Int): List<TransactionEntity> = emptyList()
                override suspend fun getAllUnlinkedTransactions(): List<TransactionEntity> = emptyList()
                override suspend fun getTransactionsByRecurringItemId(recurringItemId: Long): List<TransactionEntity> = emptyList()
                override suspend fun countTransactionsByReferenceNumber(referenceNumber: String): Int = 0
            },
            statementSnapshotDao = object : StatementSnapshotDao {
                override suspend fun insertSnapshot(snapshot: StatementSnapshotEntity) {}
                override suspend fun insertSnapshots(snapshots: List<StatementSnapshotEntity>) {}
                override suspend fun getLatestSnapshot(accountId: String): StatementSnapshotEntity? = null
                override suspend fun getAllSnapshots(): List<StatementSnapshotEntity> = emptyList()
                override fun observeAllSnapshots(): Flow<List<StatementSnapshotEntity>> = emptyFlow()
                override suspend fun deleteSnapshotById(id: Long) {}
                override suspend fun deleteTransactionsForAccountPeriod(accountId: String, startDate: Long, endDate: Long) {}
            },
            recurringItemDao = object : RecurringItemDao {
                override fun getAllRecurringItems(): Flow<List<RecurringItemEntity>> = emptyFlow()
                override suspend fun getAllRecurringItemsList(): List<RecurringItemEntity> = emptyList()
                override fun getActiveRecurringItems(): Flow<List<RecurringItemEntity>> = emptyFlow()
                override suspend fun getRecurringItemById(id: Long): RecurringItemEntity? = null
                override fun getRecurringItemByIdFlow(id: Long): Flow<RecurringItemEntity?> = emptyFlow()
                override fun getRecurringItemsByType(type: String): Flow<List<RecurringItemEntity>> = emptyFlow()
                override suspend fun getDueRecurringItems(timestamp: Long): List<RecurringItemEntity> = emptyList()
                override suspend fun insertRecurringItem(item: RecurringItemEntity): Long = 0L
                override suspend fun insertRecurringItems(items: List<RecurringItemEntity>): List<Long> = emptyList()
                override suspend fun updateRecurringItem(item: RecurringItemEntity) {}
                override suspend fun deleteRecurringItem(item: RecurringItemEntity) {}
                override suspend fun deleteRecurringItemById(id: Long) {}
                override suspend fun setRecurringItemActive(id: Long, isActive: Boolean, updatedAt: Long) {}
                override suspend fun updateOccurrence(id: Long, nextTimestamp: Long, lastGenTimestamp: Long, updatedAt: Long) {}
            },
            loanAccountDao = object : LoanAccountDao {
                override fun getAllLoanAccounts(): Flow<List<LoanAccountEntity>> = emptyFlow()
                override suspend fun getAllLoanAccountsList(): List<LoanAccountEntity> = emptyList()
                override fun getLoanAccountById(id: Long): Flow<LoanAccountEntity?> = emptyFlow()
                override suspend fun getLoanAccountByIdSync(id: Long): LoanAccountEntity? = null
                override suspend fun insertLoanAccount(loan: LoanAccountEntity): Long = 0L
                override suspend fun insertLoanAccounts(loans: List<LoanAccountEntity>) {}
                override suspend fun updateLoanAccount(loan: LoanAccountEntity) {}
                override suspend fun deleteLoanAccount(loan: LoanAccountEntity) {}
                override suspend fun deleteLoanAccountById(id: Long) {}
            },
            loanPaymentDao = object : LoanPaymentDao {
                override fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>> = emptyFlow()
                override suspend fun getPaymentsForLoanAscSync(loanId: Long): List<LoanPaymentEntity> = emptyList()
                override fun getAllPayments(): Flow<List<LoanPaymentEntity>> = emptyFlow()
                override suspend fun getAllPaymentsList(): List<LoanPaymentEntity> = emptyList()
                override suspend fun getPaymentByTransactionId(transactionId: Long): LoanPaymentEntity? = null
                override suspend fun insertPayment(payment: LoanPaymentEntity): Long = 0L
                override suspend fun insertPayments(payments: List<LoanPaymentEntity>) {}
                override suspend fun updatePayment(payment: LoanPaymentEntity) {}
                override suspend fun deletePayment(payment: LoanPaymentEntity) {}
                override suspend fun deletePaymentsForLoan(loanId: Long) {}
                override suspend fun deletePaymentByTransactionId(transactionId: Long) {}
                override suspend fun deletePaymentById(id: Long) {}
            },
            budgetDao = object : BudgetDao {
                override fun getAllBudgets(): Flow<List<BudgetEntity>> = emptyFlow()
                override suspend fun getAllBudgetsList(): List<BudgetEntity> = emptyList()
                override fun getBudgetById(id: Long): Flow<BudgetEntity?> = emptyFlow()
                override suspend fun getBudgetByIdSnapshot(id: Long): BudgetEntity? = null
                override suspend fun insertBudget(budget: BudgetEntity): Long = 0L
                override suspend fun insertBudgets(budgets: List<BudgetEntity>) {}
                override suspend fun updateBudget(budget: BudgetEntity) {}
                override suspend fun deleteBudget(budget: BudgetEntity) {}
                override suspend fun deleteBudgetById(id: Long) {}
            },
            financialEventAllocationDao = object : FinancialEventAllocationDao {
                override fun getAllAllocations(): Flow<List<FinancialEventAllocationEntity>> = emptyFlow()
                override suspend fun getAllAllocationsList(): List<FinancialEventAllocationEntity> = emptyList()
                override suspend fun getAllocationsForTransaction(transactionId: Long): List<FinancialEventAllocationEntity> = emptyList()
                override suspend fun getAllocationsForFinancialEvent(transactionLinkId: String): List<FinancialEventAllocationEntity> = emptyList()
                override suspend fun insertAllocation(allocation: FinancialEventAllocationEntity): Long = 0L
                override suspend fun insertAllocations(allocations: List<FinancialEventAllocationEntity>) {}
                override suspend fun deleteAllocation(allocation: FinancialEventAllocationEntity) {}
                override suspend fun deleteAllocationById(allocationId: Long) {}
                override suspend fun deleteAllocationsForTransaction(transactionId: Long) {}
                override suspend fun deleteAllocationsForFinancialEvent(transactionLinkId: String) {}
                override suspend fun getAllocatedAmountForTransaction(transactionId: Long): Double = 0.0
                override suspend fun getTransactionIdsForFinancialEvent(transactionLinkId: String): List<Long> = emptyList()
                override fun observeAllocationsForTransaction(transactionId: Long): Flow<List<FinancialEventAllocationEntity>> = emptyFlow()
                override fun observeAllocationsForFinancialEvent(transactionLinkId: String): Flow<List<FinancialEventAllocationEntity>> = emptyFlow()
                override suspend fun updateAllocationAmount(transactionId: Long, transactionLinkId: String, newAmount: Double) {}
                override suspend fun deleteAllocationForTransactionAndEvent(transactionId: Long, transactionLinkId: String) {}
            }
        )
    }

    @Test
    fun `healthy ledger returns 100 percent score and zero issues`() {
        val txs = listOf(
            TransactionEntity(
                id = 1L,
                amount = 500.0,
                type = "EXPENSE",
                description = "Swiggy Order",
                category = "Dining & Food",
                dateTimestamp = now - 2 * dayMs,
                accountId = "acc_1",
                bankName = "HDFC Bank"
            ),
            TransactionEntity(
                id = 2L,
                amount = 65000.0,
                type = "INCOME",
                description = "Monthly Salary",
                category = "Salary",
                dateTimestamp = now - 5 * dayMs,
                accountId = "acc_1",
                bankName = "HDFC Bank"
            )
        )
        val snap = StatementSnapshotEntity(
            id = 1L,
            accountId = "acc_1",
            openingBalance = 10000.0,
            totalCredits = 65000.0,
            totalDebits = 500.0,
            endingBalance = 74500.0,
            importedAt = now - dayMs,
            bankName = "HDFC Bank"
        )

        val report = engine.auditWithData(
            transactions = txs,
            snapshots = listOf(snap),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        assertTrue(report.isHealthy)
        assertEquals(100, report.healthScore)
        assertEquals(0, report.totalIssues)
        assertEquals(QualityCheckType.values().size, report.passedChecksCount)
    }

    @Test
    fun `detects exact and possible duplicates`() {
        val txs = listOf(
            TransactionEntity(
                id = 1L, amount = 450.0, type = "EXPENSE", description = "Amazon Shopping",
                category = "Shopping", dateTimestamp = now - 2 * dayMs, referenceNumber = "REF123", accountId = "acc_1"
            ),
            TransactionEntity(
                id = 2L, amount = 450.0, type = "EXPENSE", description = "Amazon Shopping",
                category = "Shopping", dateTimestamp = now - 2 * dayMs, referenceNumber = "REF123", accountId = "acc_1"
            ),
            TransactionEntity(
                id = 3L, amount = 250.0, type = "EXPENSE", description = "Uber Trip",
                category = "Fuel & Transport", dateTimestamp = now - 5 * dayMs, accountId = "acc_2"
            ),
            TransactionEntity(
                id = 4L, amount = 250.0, type = "EXPENSE", description = "Uber Trip Airport",
                category = "Fuel & Transport", dateTimestamp = now - 5 * dayMs + 3600_000L, accountId = "acc_2"
            )
        )

        val report = engine.auditWithData(
            transactions = txs,
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        assertTrue(report.issues.any { it.type == QualityCheckType.EXACT_DUPLICATE && it.entityId == 2L })
        assertTrue(report.issues.any { it.type == QualityCheckType.POSSIBLE_DUPLICATE && it.entityId == 4L })
    }

    @Test
    fun `detects balance inconsistency and uncategorised and future date`() {
        val txs = listOf(
            TransactionEntity(
                id = 10L, amount = 320.0, type = "EXPENSE", description = "Unknown Merchant",
                category = "Uncategorized", dateTimestamp = now - dayMs, accountId = "acc_1"
            ),
            TransactionEntity(
                id = 11L, amount = 1200.0, type = "EXPENSE", description = "Future Invoice",
                category = "Utilities", dateTimestamp = now + 10 * dayMs, accountId = "acc_1"
            ),
            TransactionEntity(
                id = 12L, amount = 900.0, type = "EXPENSE", description = "Scheduled Subscription",
                category = "Utilities", dateTimestamp = now + 10 * dayMs, recurringItemId = 5L, accountId = "acc_1"
            )
        )
        val snap = StatementSnapshotEntity(
            id = 1L, accountId = "acc_1", openingBalance = 10000.0,
            totalCredits = 2000.0, totalDebits = 1000.0, endingBalance = 10500.0, // should be 11000.0
            importedAt = now, bankName = "SBI"
        )

        val report = engine.auditWithData(
            transactions = txs,
            snapshots = listOf(snap),
            recurringItems = listOf(
                RecurringItemEntity(
                    id = 5L, title = "Scheduled Subscription", amount = 900.0,
                    type = "EXPENSE", frequency = "MONTHLY",
                    startDateTimestamp = now - 30 * dayMs, nextOccurrenceTimestamp = now + 10 * dayMs
                )
            ),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        assertTrue(report.issues.any { it.type == QualityCheckType.UNCATEGORISED_ITEM && it.entityId == 10L })
        assertTrue(report.issues.any { it.type == QualityCheckType.FUTURE_DATE_ANOMALY && it.entityId == 11L })
        assertFalse(report.issues.any { it.type == QualityCheckType.FUTURE_DATE_ANOMALY && it.entityId == 12L })
        assertTrue(report.issues.any { it.type == QualityCheckType.BALANCE_INCONSISTENCY && it.entityId == 1L })
    }

    @Test
    fun `detects merchant spike, unmatched transfer, stale account, and budget overrun`() {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val recentThisMonth = cal.timeInMillis - dayMs

        val txs = listOf(
            TransactionEntity(id = 1L, amount = 200.0, type = "EXPENSE", description = "Swiggy", category = "Dining & Food", dateTimestamp = recentThisMonth, accountId = "acc_1"),
            TransactionEntity(id = 2L, amount = 220.0, type = "EXPENSE", description = "Swiggy", category = "Dining & Food", dateTimestamp = recentThisMonth, accountId = "acc_1"),
            TransactionEntity(id = 3L, amount = 210.0, type = "EXPENSE", description = "Swiggy", category = "Dining & Food", dateTimestamp = recentThisMonth, accountId = "acc_1"),
            TransactionEntity(id = 4L, amount = 1800.0, type = "EXPENSE", description = "Swiggy", category = "Dining & Food", dateTimestamp = recentThisMonth, accountId = "acc_1"),
            TransactionEntity(
                id = 5L, amount = 5000.0, type = "EXPENSE", role = TransactionRole.TRANSFER_OUT.name,
                description = "Self Transfer to Axis", category = "Transfer", dateTimestamp = recentThisMonth, accountId = "acc_1"
            ),
            TransactionEntity(
                id = 6L, amount = 300.0, type = "EXPENSE", description = "Old Wallet Tx",
                category = "Shopping", dateTimestamp = now - 65 * dayMs, accountId = "acc_stale", bankName = "Axis Bank"
            )
        )
        val budgets = listOf(
            BudgetEntity(id = 1L, name = "Food Cap", categoryName = "Dining & Food", amount = 2000.0, budgetType = "EXPENSE")
        )

        val report = engine.auditWithData(
            transactions = txs,
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = budgets,
            allocations = emptyList(),
            currentTimeMillis = now
        )

        assertTrue(report.issues.any { it.type == QualityCheckType.UNUSUAL_AMOUNT_SPIKE && it.entityId == 4L })
        assertTrue(report.issues.any { it.type == QualityCheckType.BUDGET_OVERRUN && it.entityId == 1L })
        assertTrue(report.issues.any { it.type == QualityCheckType.UNMATCHED_TRANSFER && it.entityId == 5L })
        assertTrue(report.issues.any { it.type == QualityCheckType.STALE_ACCOUNT })
    }

    @Test
    fun `detects financial event anomalies, old receivables, and orphan mappings`() {
        val txs = listOf(
            TransactionEntity(
                id = 20L, amount = 1000.0, type = "EXPENSE", role = TransactionRole.LENT.name,
                description = "Dinner Split Friend", category = "Dining & Food",
                dateTimestamp = now - 40 * dayMs, accountId = "acc_1"
            ),
            TransactionEntity(
                id = 21L, amount = 500.0, type = "EXPENSE", role = TransactionRole.LENT.name,
                description = "Trip Hotel", category = "Fuel & Transport",
                dateTimestamp = now - 5 * dayMs, transactionLinkId = "evt_trip", accountId = "acc_1"
            ),
            TransactionEntity(
                id = 22L, amount = 750.0, type = "INCOME", role = TransactionRole.REIMBURSEMENT.name,
                description = "Friend Payback", category = "Refunds & Cashback",
                dateTimestamp = now - 2 * dayMs, transactionLinkId = "evt_trip", accountId = "acc_1"
            )
        )
        val allocs = listOf(
            FinancialEventAllocationEntity(id = 1L, transactionId = 21L, transactionLinkId = "evt_a", allocatedAmount = 350.0, createdAt = now),
            FinancialEventAllocationEntity(id = 2L, transactionId = 21L, transactionLinkId = "evt_b", allocatedAmount = 300.0, createdAt = now),
            FinancialEventAllocationEntity(id = 3L, transactionId = 999L, transactionLinkId = "evt_a", allocatedAmount = 100.0, createdAt = now)
        )

        val report = engine.auditWithData(
            transactions = txs,
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = allocs,
            currentTimeMillis = now
        )

        assertTrue(report.issues.any { it.type == QualityCheckType.OLD_RECEIVABLE && it.entityId == 20L })
        assertTrue(report.issues.any { it.type == QualityCheckType.OVER_ALLOCATION && it.entityId == 21L })
        assertTrue(report.issues.any { it.type == QualityCheckType.OVER_SETTLEMENT })
        assertTrue(report.issues.any { it.type == QualityCheckType.ORPHAN_MAPPING && it.entityId == 3L })
    }

    @Test
    fun `dismissing issues excludes them from active list and increases health score`() {
        val txs = listOf(
            TransactionEntity(
                id = 10L, amount = 320.0, type = "EXPENSE", description = "Unknown Merchant",
                category = "Uncategorized", dateTimestamp = now - dayMs, accountId = "acc_1"
            )
        )

        val before = engine.auditWithData(txs, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptySet(), now)
        assertEquals(1, before.totalIssues)

        val dismissedId = before.issues.first().id
        val after = engine.auditWithData(txs, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), setOf(dismissedId), now)
        assertEquals(0, after.totalIssues)
        assertEquals(100, after.healthScore)
        assertEquals(1, after.dismissedCount)
    }
}
