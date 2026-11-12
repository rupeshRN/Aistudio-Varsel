package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import com.varsel.expensetracker.data.local.entity.LoanAccountEntity
import com.varsel.expensetracker.data.local.entity.LoanPaymentEntity
import com.varsel.expensetracker.data.local.entity.RecurringItemEntity
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import com.varsel.expensetracker.domain.model.quality.QualityCheckType
import com.varsel.expensetracker.domain.model.quality.QualitySeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class DataQualityAuditEngineTest {

    private lateinit var auditEngine: DataQualityAuditEngine
    private val now = 1700000000000L // Reference fixed time for deterministic test

    @Before
    fun setUp() {
        auditEngine = DataQualityAuditEngine()
    }

    private fun createSampleTx(
        id: Long,
        amount: Double = 500.0,
        type: String = "EXPENSE",
        description: String = "Swiggy Order",
        category: String = "Food & Dining",
        dateTimestamp: Long = now - (2 * 24 * 60 * 60 * 1000L),
        referenceNumber: String? = null,
        accountId: String? = "acc_1",
        bankName: String? = "HDFC Bank",
        transferLinkId: String? = null
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            type = type,
            description = description,
            category = category,
            dateTimestamp = dateTimestamp,
            referenceNumber = referenceNumber,
            accountId = accountId,
            bankName = bankName,
            transferLinkId = transferLinkId
        )
    }

    @Test
    fun `test exact duplicates detected`() {
        val tx1 = createSampleTx(id = 1L, amount = 1200.0, referenceNumber = "REF123456", dateTimestamp = now - 10000)
        val tx2 = createSampleTx(id = 2L, amount = 1200.0, referenceNumber = "REF123456", dateTimestamp = now - 10000)

        val report = auditEngine.auditWithData(
            transactions = listOf(tx1, tx2),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val exactDup = report.issues.firstOrNull { it.type == QualityCheckType.EXACT_DUPLICATE }
        assertTrue("Exact duplicate should be detected", exactDup != null)
        assertEquals(2L, exactDup?.entityId)
        assertEquals(1L, exactDup?.secondaryEntityId)
        assertEquals(QualitySeverity.ERROR, exactDup?.severity)
    }

    @Test
    fun `test possible duplicates detected within 24 hours`() {
        val tx1 = createSampleTx(id = 1L, amount = 750.0, description = "Starbucks Coffee", dateTimestamp = now - 50000)
        val tx2 = createSampleTx(id = 2L, amount = 750.0, description = "Starbucks Indiranagar", dateTimestamp = now - 40000)

        val report = auditEngine.auditWithData(
            transactions = listOf(tx1, tx2),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val possibleDup = report.issues.firstOrNull { it.type == QualityCheckType.POSSIBLE_DUPLICATE }
        assertTrue("Possible duplicate should be detected", possibleDup != null)
        assertEquals(2L, possibleDup?.entityId)
        assertEquals(QualitySeverity.WARNING, possibleDup?.severity)
    }

    @Test
    fun `test zero or negative amounts detected`() {
        val zeroTx = createSampleTx(id = 1L, amount = 0.0)
        val negTx = createSampleTx(id = 2L, amount = -50.0)

        val report = auditEngine.auditWithData(
            transactions = listOf(zeroTx, negTx),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val zeroIssues = report.issues.filter { it.type == QualityCheckType.ZERO_AMOUNT_TRANSACTION }
        assertEquals(2, zeroIssues.size)
    }

    @Test
    fun `test future date anomalies flagged`() {
        val futureTx = createSampleTx(id = 1L, dateTimestamp = now + (3 * 24 * 60 * 60 * 1000L))

        val report = auditEngine.auditWithData(
            transactions = listOf(futureTx),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val futureIssue = report.issues.firstOrNull { it.type == QualityCheckType.FUTURE_DATE_ANOMALY }
        assertTrue("Future dated transaction should be flagged", futureIssue != null)
    }

    @Test
    fun `test uncategorised transactions flagged`() {
        val txBlank = createSampleTx(id = 1L, category = "")
        val txUnknown = createSampleTx(id = 2L, category = "Unknown")
        val txUncat = createSampleTx(id = 3L, category = "UNCAT")

        val report = auditEngine.auditWithData(
            transactions = listOf(txBlank, txUnknown, txUncat),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val uncatIssues = report.issues.filter { it.type == QualityCheckType.UNCATEGORISED_ITEM }
        assertEquals(3, uncatIssues.size)
    }

    @Test
    fun `test unusual amount spikes flagged`() {
        val tx1 = createSampleTx(id = 1L, amount = 100.0, category = "Groceries")
        val tx2 = createSampleTx(id = 2L, amount = 150.0, category = "Groceries")
        val tx3 = createSampleTx(id = 3L, amount = 120.0, category = "Groceries")
        val tx4 = createSampleTx(id = 4L, amount = 130.0, category = "Groceries")
        val spikeTx = createSampleTx(id = 5L, amount = 3500.0, category = "Groceries")

        val report = auditEngine.auditWithData(
            transactions = listOf(tx1, tx2, tx3, tx4, spikeTx),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val spike = report.issues.firstOrNull { it.type == QualityCheckType.UNUSUAL_AMOUNT_SPIKE }
        assertTrue("Unusual spike should be flagged", spike != null)
        assertEquals(5L, spike?.entityId)
    }

    @Test
    fun `test balance inconsistency detected against statement snapshot`() {
        // opening: 10000 + credits: 5000 - debits: 2000 = 13000. But ending balance is 11000 (diff 2000)
        val snapshot = StatementSnapshotEntity(
            id = 10L,
            accountId = "acc_1",
            accountLast4 = "1234",
            bankName = "ICICI Bank",
            openingBalance = 10000.0,
            totalCredits = 5000.0,
            totalDebits = 2000.0,
            endingBalance = 11000.0,
            importedAt = now
        )

        val report = auditEngine.auditWithData(
            transactions = emptyList(),
            snapshots = listOf(snapshot),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val balIssue = report.issues.firstOrNull { it.type == QualityCheckType.BALANCE_INCONSISTENCY }
        assertTrue("Balance discrepancy should be detected", balIssue != null)
        assertEquals(2000.0, balIssue?.affectedAmount ?: 0.0, 0.01)
    }

    @Test
    fun `test unmatched transfer leg flagged`() {
        val transferTx = createSampleTx(
            id = 1L,
            amount = 5000.0,
            type = "TRANSFER",
            description = "TRANSFER TO HDFC",
            transferLinkId = null
        )

        val report = auditEngine.auditWithData(
            transactions = listOf(transferTx),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val unmatched = report.issues.firstOrNull { it.type == QualityCheckType.UNMATCHED_TRANSFER }
        assertTrue("Unmatched transfer leg should be flagged", unmatched != null)
        assertEquals(1L, unmatched?.entityId)
    }

    @Test
    fun `test stale account inactivity flagged`() {
        val staleTx = createSampleTx(
            id = 1L,
            accountId = "stale_acc",
            dateTimestamp = now - (90L * 24 * 60 * 60 * 1000L) // 90 days ago
        )

        val report = auditEngine.auditWithData(
            transactions = listOf(staleTx),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val staleIssue = report.issues.firstOrNull { it.type == QualityCheckType.STALE_ACCOUNT }
        assertTrue("Stale account should be flagged", staleIssue != null)
    }

    @Test
    fun `test missing recurring bill flagged when overdue`() {
        val recurringItem = RecurringItemEntity(
            id = 1L,
            title = "Netflix Premium",
            amount = 649.0,
            type = "EXPENSE",
            category = "Entertainment",
            frequency = "MONTHLY",
            startDateTimestamp = now - 60L * 24 * 60 * 60 * 1000L,
            nextOccurrenceTimestamp = now - (10L * 24 * 60 * 60 * 1000L), // 10 days ago
            isActive = true
        )

        val report = auditEngine.auditWithData(
            transactions = emptyList(),
            snapshots = emptyList(),
            recurringItems = listOf(recurringItem),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val billIssue = report.issues.firstOrNull { it.type == QualityCheckType.MISSING_RECURRING_BILL }
        assertTrue("Overdue recurring bill should be flagged", billIssue != null)
        assertEquals(1L, billIssue?.entityId)
    }

    @Test
    fun `test overdue loan with backdated start date and past due 1st EMI without payments`() {
        // Loan started 45 days ago, so 1st EMI was due 15 days ago (in the past)
        val startDate = now - (45L * 24 * 60 * 60 * 1000L)
        val loan = LoanAccountEntity(
            id = 1L,
            name = "Personal Loan",
            loanType = "PERSONAL",
            lenderName = "HDFC Bank",
            principal = 100000.0,
            annualInterestRate = 12.0,
            totalTenureMonths = 12,
            emiAmount = 8885.0,
            startDateTimestamp = startDate,
            status = "ACTIVE"
        )

        val report = auditEngine.auditWithData(
            transactions = emptyList(),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = listOf(loan),
            loanPayments = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val emiIssue = report.issues.firstOrNull { it.type == QualityCheckType.OVERDUE_LOAN_EMI }
        assertTrue("Overdue loan EMI must be flagged for backdated start date with no payments", emiIssue != null)
        assertEquals(1L, emiIssue?.entityId)
        assertEquals(QualitySeverity.ERROR, emiIssue?.severity)
    }

    @Test
    fun `test overdue loan cleared when repayment is recorded`() {
        val startDate = now - (45L * 24 * 60 * 60 * 1000L)
        val loan = LoanAccountEntity(
            id = 1L,
            name = "Personal Loan",
            loanType = "PERSONAL",
            lenderName = "HDFC Bank",
            principal = 100000.0,
            annualInterestRate = 12.0,
            totalTenureMonths = 12,
            emiAmount = 8885.0,
            startDateTimestamp = startDate,
            status = "ACTIVE"
        )

        val payment = LoanPaymentEntity(
            id = 10L,
            loanId = 1L,
            paymentDateTimestamp = now - (14L * 24 * 60 * 60 * 1000L),
            amount = 8885.0,
            principalComponent = 7885.0,
            interestComponent = 1000.0,
            paymentType = "REGULAR_EMI"
        )

        val report = auditEngine.auditWithData(
            transactions = emptyList(),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = listOf(loan),
            loanPayments = listOf(payment),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val emiIssue = report.issues.firstOrNull { it.type == QualityCheckType.OVERDUE_LOAN_EMI }
        assertFalse("Overdue loan EMI should NOT be flagged when repayment is already recorded", emiIssue != null)
    }

    @Test
    fun `test unlinked loan payment does not false positive if already linked in loan payments`() {
        val loanTx = createSampleTx(
            id = 55L,
            amount = 8885.0,
            description = "HDFC LOAN REPAYMENT",
            type = "EXPENSE"
        )
        val loan = LoanAccountEntity(
            id = 1L,
            name = "HDFC Loan",
            loanType = "PERSONAL",
            lenderName = "HDFC Bank",
            principal = 100000.0,
            annualInterestRate = 12.0,
            totalTenureMonths = 12,
            emiAmount = 8885.0,
            startDateTimestamp = now - 60L * 24 * 60 * 60 * 1000L,
            status = "ACTIVE"
        )
        val linkedPayment = LoanPaymentEntity(
            id = 10L,
            loanId = 1L,
            paymentDateTimestamp = now - (10L * 24 * 60 * 60 * 1000L),
            amount = 8885.0,
            principalComponent = 7885.0,
            interestComponent = 1000.0,
            linkedTransactionId = 55L // Explicitly linked to tx 55
        )

        val report = auditEngine.auditWithData(
            transactions = listOf(loanTx),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = listOf(loan),
            loanPayments = listOf(linkedPayment),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )

        val unlinkedIssue = report.issues.firstOrNull { it.type == QualityCheckType.UNLINKED_LOAN_PAYMENT }
        assertFalse("Transaction already linked in loan_payments must not be flagged as unlinked", unlinkedIssue != null)
    }

    @Test
    fun `test broken split allocations flagged when exceeding parent transaction`() {
        val parentTx = createSampleTx(id = 100L, amount = 1000.0)
        val alloc1 = FinancialEventAllocationEntity(
            id = 1L,
            transactionId = 100L,
            transactionLinkId = "event_1",
            allocatedAmount = 600.0,
            createdAt = now
        )
        val alloc2 = FinancialEventAllocationEntity(
            id = 2L,
            transactionId = 100L,
            transactionLinkId = "event_2",
            allocatedAmount = 600.0, // Total 1200 > 1000
            createdAt = now
        )

        val report = auditEngine.auditWithData(
            transactions = listOf(parentTx),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = listOf(alloc1, alloc2),
            currentTimeMillis = now
        )

        val splitIssue = report.issues.firstOrNull { it.type == QualityCheckType.BROKEN_SPLIT }
        assertTrue("Broken split allocation should be flagged", splitIssue != null)
        assertEquals(100L, splitIssue?.entityId)
    }

    @Test
    fun `test orphan allocations flagged when transaction does not exist`() {
        val orphanAlloc = FinancialEventAllocationEntity(
            id = 5L,
            transactionId = 9999L, // Does not exist
            transactionLinkId = "event_x",
            allocatedAmount = 450.0,
            createdAt = now
        )

        val report = auditEngine.auditWithData(
            transactions = emptyList(),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = listOf(orphanAlloc),
            currentTimeMillis = now
        )

        val orphanIssue = report.issues.firstOrNull { it.type == QualityCheckType.ORPHAN_ALLOCATION }
        assertTrue("Orphan allocation should be flagged", orphanIssue != null)
        assertEquals(5L, orphanIssue?.entityId)
    }

    @Test
    fun `test dismissed issues are excluded and score recalculated`() {
        val txBlank = createSampleTx(id = 1L, category = "")

        val report1 = auditEngine.auditWithData(
            transactions = listOf(txBlank),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            currentTimeMillis = now
        )
        assertEquals(1, report1.totalIssues)
        assertTrue(report1.healthScore < 100)

        val issueId = report1.issues.first().id
        val report2 = auditEngine.auditWithData(
            transactions = listOf(txBlank),
            snapshots = emptyList(),
            recurringItems = emptyList(),
            loanAccounts = emptyList(),
            budgets = emptyList(),
            allocations = emptyList(),
            dismissedKeys = setOf(issueId),
            currentTimeMillis = now
        )
        assertEquals(0, report2.totalIssues)
        assertEquals(100, report2.healthScore)
        assertTrue(report2.isHealthy)
    }
}
