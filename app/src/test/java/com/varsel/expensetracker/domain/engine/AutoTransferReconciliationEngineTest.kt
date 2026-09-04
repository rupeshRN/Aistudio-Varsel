package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.local.dao.TransactionDao
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        override suspend fun getRecentUnlinkedTransactionsSince(minDateTimestamp: Long, limit: Int): List<TransactionEntity> = emptyList()
        override suspend fun getAllUnlinkedTransactions(): List<TransactionEntity> = emptyList()
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

    @Test
    fun `test matching by reference number across 4 days weekend clearing delay scores 100`() {
        val juneDebitDate = 1718956800000L // Friday June 21, 2024
        val juneCreditDate = juneDebitDate + (4L * 24 * 60 * 60 * 1000L) // Tuesday June 25, 2024 (4 days later)

        val debit = TransactionEntity(
            id = 501L,
            amount = 15000.0,
            type = "EXPENSE",
            description = "Transfer out to Indian Bank",
            category = "General",
            dateTimestamp = juneDebitDate,
            referenceNumber = "417312345678",
            bankName = "ICICI Bank",
            accountId = "acc_icici_1",
            accountLast4 = "1234"
        )

        val credit = TransactionEntity(
            id = 502L,
            amount = 15000.0,
            type = "INCOME",
            description = "UPI/417312345678/From ICICI",
            category = "Income",
            dateTimestamp = juneCreditDate,
            referenceNumber = "417312345678",
            bankName = "Indian Bank",
            accountId = "acc_ib_1",
            accountLast4 = "9012"
        )

        val score = engine.scorePair(debit, credit, emptyList())
        assertEquals(100, score)
    }

    @Test
    fun `test hasTransferIndicators filters non-transfer ordinary expenses`() {
        val ordinaryCoffee = TransactionEntity(
            id = 601L,
            amount = 150.0,
            type = "EXPENSE",
            description = "Starbucks Coffee",
            category = "Food & Dining",
            dateTimestamp = System.currentTimeMillis()
        )
        assertFalse(engine.hasTransferIndicators(ordinaryCoffee))

        val transferExpense = TransactionEntity(
            id = 602L,
            amount = 5000.0,
            type = "EXPENSE",
            description = "NEFT transfer to account XXXXXXXX9012",
            category = "Transfer",
            dateTimestamp = System.currentTimeMillis()
        )
        assertTrue(engine.hasTransferIndicators(transferExpense))

        val txWithUtr = TransactionEntity(
            id = 603L,
            amount = 2000.0,
            type = "EXPENSE",
            description = "Payment",
            category = "General",
            dateTimestamp = System.currentTimeMillis(),
            referenceNumber = "417312345678"
        )
        assertTrue(engine.hasTransferIndicators(txWithUtr))
    }

    @Test
    fun `test Rule 4 matches transfer when debit narration mentions destination bank name`() {
        val now = System.currentTimeMillis()
        val debit = TransactionEntity(
            id = 701L,
            amount = 25000.0,
            type = "EXPENSE",
            description = "Transfer to Indian Bank",
            rawDescription = "FND TRF TO INDIAN BANK",
            category = "Transfer",
            dateTimestamp = now,
            bankName = "ICICI Bank",
            accountId = "acc_icici",
            accountLast4 = "1111"
        )

        val credit = TransactionEntity(
            id = 702L,
            amount = 25000.0,
            type = "INCOME",
            description = "By Transfer - ICICI",
            rawDescription = "TRANSFER FROM ICICI BANK",
            category = "Income",
            dateTimestamp = now + (24 * 60 * 60 * 1000L), // next day
            bankName = "Indian Bank",
            accountId = "acc_ib",
            accountLast4 = "2222"
        )

        val score = engine.scorePair(debit, credit, emptyList())
        assertEquals(85, score)
    }

    @Test
    fun `test matches alphanumeric NEFT UTR in narration`() {
        val now = System.currentTimeMillis()
        val debit = TransactionEntity(
            id = 801L,
            amount = 10000.0,
            type = "EXPENSE",
            description = "NEFT-N123456789012-Self Transfer",
            rawDescription = "NEFT-N123456789012-USER NAME",
            category = "Transfer",
            dateTimestamp = now,
            bankName = "ICICI Bank",
            accountId = "acc_icici",
            accountLast4 = "1111"
        )

        val credit = TransactionEntity(
            id = 802L,
            amount = 10000.0,
            type = "INCOME",
            description = "NEFT / N123456789012 / ICICI BANK",
            rawDescription = "NEFT / N123456789012 / ICICI",
            category = "Income",
            dateTimestamp = now,
            bankName = "Indian Bank",
            accountId = "acc_ib",
            accountLast4 = "2222"
        )

        val score = engine.scorePair(debit, credit, emptyList())
        assertEquals(100, score)
    }
}
