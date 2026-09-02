package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.local.dao.TransactionDao
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AutoTransferReconciliationEngineTest {

    private lateinit var engine: AutoTransferReconciliationEngine

    private class FakeTransactionDao : TransactionDao {
        override fun getAllTransactions(): Flow<List<TransactionEntity>> = emptyFlow()
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {}
        override suspend fun insertTransaction(transaction: TransactionEntity) {}
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override suspend fun getTransactionById(id: Long): TransactionEntity? = null
        override suspend fun linkTransactions(transactionIds: List<Long>, transactionLinkId: String) {}
        override suspend fun linkTransferTransactions(transferOutTransactionId: Long, transferInTransactionId: Long, transferLinkId: String) {}
        override suspend fun unlinkTransfer(transactionId: Long) {}
        override suspend fun unlinkTransaction(transactionId: Long) {}
        override suspend fun getLinkedTransfer(transferLinkId: String, currentTransactionId: Long): TransactionEntity? = null
        override suspend fun getLinkedTransactions(transactionLinkId: String): List<TransactionEntity> = emptyList()
        override suspend fun getUnlinkedReimbursements(currentTransactionId: Long): List<TransactionEntity> = emptyList()
        override suspend fun updateTransactions(transactions: List<TransactionEntity>) {}
        override suspend fun findExistingFingerprints(fingerprints: List<String>): List<String> = emptyList()
        override suspend fun getTransactionsSince(excludeId: Long, sinceTimestamp: Long): List<TransactionEntity> = emptyList()
        override suspend fun findUnlinkedTransferCandidates(type: String, amount: Double, minDate: Long, maxDate: Long): List<TransactionEntity> = emptyList()
        override suspend fun findUnlinkedTransferCandidatesByReference(type: String, referenceNumber: String): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsByIds(ids: List<Long>): List<TransactionEntity> = emptyList()
        override suspend fun getRecentUnlinkedTransactions(limit: Int): List<TransactionEntity> = emptyList()
    }

    private class FakeStatementSnapshotDao : StatementSnapshotDao {
        override suspend fun insertSnapshot(snapshot: StatementSnapshotEntity) {}
        override suspend fun getLatestSnapshot(accountId: String): StatementSnapshotEntity? = null
        override suspend fun getAllSnapshots(): List<StatementSnapshotEntity> = emptyList()
        override fun observeAllSnapshots(): Flow<List<StatementSnapshotEntity>> = emptyFlow()
        override suspend fun deleteSnapshotById(id: Long) {}
        override suspend fun deleteTransactionsForAccountPeriod(accountId: String, startDate: Long, endDate: Long) {}
    }

    @Before
    fun setUp() {
        engine = AutoTransferReconciliationEngine(FakeTransactionDao(), FakeStatementSnapshotDao())
    }

    @Test
    fun `test matching by 12-digit UTR reference number scores 100`() {
        val now = System.currentTimeMillis()
        val debit = TransactionEntity(
            id = 1L,
            amount = 5000.0,
            type = "EXPENSE",
            description = "Transfer to Indian Bank",
            category = "General",
            dateTimestamp = now,
            referenceNumber = "423512345678",
            bankName = "ICICI Bank",
            accountId = "acc_icici_1",
            accountLast4 = "1234"
        )

        val credit = TransactionEntity(
            id = 2L,
            amount = 5000.0,
            type = "INCOME",
            description = "UPI/423512345678/From ICICI",
            category = "Income",
            dateTimestamp = now + 3600000L,
            referenceNumber = "423512345678",
            bankName = "Indian Bank",
            accountId = "acc_ib_1",
            accountLast4 = "9012"
        )

        val score = engine.scorePair(debit, credit, emptyList())
        assertEquals(100, score)
    }

    @Test
    fun `test Indian Bank to ICICI transfer matching by target account last4 scores 90`() {
        val now = System.currentTimeMillis()
        // Indian Bank debit narration: IFSC/receiver/XXXXXXXX1234/...
        val debit = TransactionEntity(
            id = 10L,
            amount = 12500.0,
            type = "EXPENSE",
            description = "Self Transfer",
            category = "General",
            dateTimestamp = now,
            referenceNumber = null,
            bankName = "Indian Bank",
            accountId = "acc_ib_1",
            accountLast4 = "9012",
            rawDescription = "ICIC0000001/MYSELF/XXXXXXXX1234/user@icici/UPI/423599999999/Savings"
        )

        val credit = TransactionEntity(
            id = 20L,
            amount = 12500.0,
            type = "INCOME",
            description = "Funds Received",
            category = "Income",
            dateTimestamp = now,
            referenceNumber = "DIFF_REF_OR_EMPTY",
            bankName = "ICICI Bank",
            accountId = "acc_icici_1",
            accountLast4 = "1234"
        )

        val score = engine.scorePair(debit, credit, emptyList())
        assertEquals(90, score)
    }

    @Test
    fun `test ICICI to Indian Bank matching by counterparty IFSC code scores 80 or 85`() {
        val now = System.currentTimeMillis()
        // ICICI Bank narration: MMT/IMPS/611234567895/Rent/Myself/IDIB0001234
        val debit = TransactionEntity(
            id = 100L,
            amount = 20000.0,
            type = "EXPENSE",
            description = "Myself - Rent",
            category = "General",
            dateTimestamp = now,
            referenceNumber = "611234567895",
            bankName = "ICICI Bank",
            accountId = "acc_icici_1",
            accountLast4 = "1234",
            rawDescription = "MMT/IMPS/611234567895/Rent/Myself/IDIB0001234"
        )

        val credit = TransactionEntity(
            id = 200L,
            amount = 20000.0,
            type = "INCOME",
            description = "IMPS Deposit",
            category = "Income",
            dateTimestamp = now + 1800000L,
            referenceNumber = "OTHER_REF",
            bankName = "Indian Bank",
            accountId = "acc_ib_1",
            accountLast4 = "9012"
        )

        val snapshots = listOf(
            StatementSnapshotEntity(
                id = 1L,
                accountId = "acc_ib_1",
                accountLast4 = "9012",
                bankName = "Indian Bank",
                ifscCode = "IDIB0001234",
                statementStartDate = null,
                statementEndDate = null,
                openingBalance = null,
                totalCredits = null,
                totalDebits = null,
                endingBalance = null,
                importedAt = now
            )
        )

        // Matches exact IFSC from snapshot
        val scoreExactIfsc = engine.scorePair(debit, credit, snapshots)
        assertEquals(85, scoreExactIfsc)

        // Matches bank prefix IDIB -> Indian Bank even without snapshot IFSC
        val scorePrefix = engine.scorePair(debit, credit, emptyList())
        assertEquals(80, scorePrefix)
    }

    @Test
    fun `test rejects same account transfers or mismatched amounts`() {
        val now = System.currentTimeMillis()
        val debit = TransactionEntity(
            id = 1L,
            amount = 5000.0,
            type = "EXPENSE",
            description = "ATM Withdrawal",
            category = "Cash",
            dateTimestamp = now,
            bankName = "ICICI Bank",
            accountId = "acc_icici_1",
            accountLast4 = "1234"
        )

        val sameAccountCredit = TransactionEntity(
            id = 2L,
            amount = 5000.0,
            type = "INCOME",
            description = "Reversal",
            category = "Refund",
            dateTimestamp = now,
            bankName = "ICICI Bank",
            accountId = "acc_icici_1",
            accountLast4 = "1234"
        )

        val differentAmountCredit = TransactionEntity(
            id = 3L,
            amount = 5001.0,
            type = "INCOME",
            description = "Deposit",
            category = "General",
            dateTimestamp = now,
            bankName = "Indian Bank",
            accountId = "acc_ib_1",
            accountLast4 = "9012"
        )

        assertEquals(0, engine.scorePair(debit, sameAccountCredit, emptyList()))
        assertEquals(0, engine.scorePair(debit, differentAmountCredit, emptyList()))
    }
}
