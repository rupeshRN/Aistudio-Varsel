package com.varsel.expensetracker.domain

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.domain.repository.TransferLinkResult
import com.varsel.expensetracker.domain.usecase.AddManualTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ManualTransactionEntryTest {

    private class FakeTransactionRepository : TransactionRepository {
        val transactions = mutableListOf<Transaction>()
        private var nextId = 1L

        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(transactions)

        override suspend fun getTransactionById(id: Long): Transaction? =
            transactions.firstOrNull { it.id == id }

        override suspend fun insertTransactions(newTransactions: List<Transaction>) {
            for (t in newTransactions) {
                val assignedId = if (t.id == 0L) nextId++ else t.id
                transactions.add(t.copy(id = assignedId))
            }
        }

        override suspend fun insertTransaction(transaction: Transaction) {
            val assignedId = if (transaction.id == 0L) nextId++ else transaction.id
            transactions.add(transaction.copy(id = assignedId))
        }

        override suspend fun updateTransaction(transaction: Transaction) {
            val index = transactions.indexOfFirst { it.id == transaction.id }
            if (index != -1) transactions[index] = transaction
        }

        override suspend fun updateTransactions(transactions: List<Transaction>) {
            transactions.forEach { updateTransaction(it) }
        }

        override suspend fun deleteTransaction(transaction: Transaction) {
            transactions.removeAll { it.id == transaction.id }
        }

        override suspend fun findExistingFingerprints(fingerprints: List<String>): Set<String> = emptySet()

        override suspend fun linkTransactions(transactionIds: List<Long>, transactionLinkId: String) {}

        override suspend fun unlinkTransaction(transactionId: Long) {}

        override suspend fun linkTransfer(transferOutTransactionId: Long, transferInTransactionId: Long): TransferLinkResult =
            TransferLinkResult.Success

        override suspend fun unlinkTransfer(transactionId: Long) {}

        override suspend fun getLinkedTransferTransactions(transferLinkId: String): List<Transaction> =
            transactions.filter { it.transferLinkId == transferLinkId }

        override suspend fun findSimilarTransactions(excludeId: Long, pattern: String, isIncome: Boolean, sinceTimestamp: Long): List<Transaction> =
            emptyList()
    }

    private lateinit var repository: FakeTransactionRepository
    private lateinit var useCase: AddManualTransactionUseCase

    @Before
    fun setUp() {
        repository = FakeTransactionRepository()
        useCase = AddManualTransactionUseCase(repository)
    }

    @Test
    fun isImported_onlyTrueForBankStatementTransactions() {
        // Bank statement transaction has fingerprint and/or rawDescription
        val importedTxn = Transaction(
            id = 1,
            amount = 1500.0,
            type = TransactionType.DEBIT,
            description = "UPI/MERCHANT/STORE",
            category = "Shopping",
            dateTimestamp = 1700000000000L,
            referenceNumber = "UPI-99238472",
            rawDescription = "UPI/MERCHANT/STORE/12345/P2M",
            transactionFingerprint = "hdfc_fp_987654321"
        )
        assertTrue("Bank statement transaction must be recognized as imported", importedTxn.isImported)

        // Manual transaction with reference number must NOT be marked as imported
        val manualWithRef = Transaction(
            id = 2,
            amount = 450.0,
            type = TransactionType.EXPENSE,
            description = "Dinner with friends",
            category = "Dining & Food",
            dateTimestamp = 1700000000000L,
            referenceNumber = "UPI/123456789",
            rawDescription = null,
            transactionFingerprint = null
        )
        assertFalse("Manual transaction with reference number must NOT be imported", manualWithRef.isImported)

        // Manual transaction without reference number
        val manualPlain = Transaction(
            id = 3,
            amount = 50.0,
            type = TransactionType.EXPENSE,
            description = "Chai",
            category = "Dining & Food",
            dateTimestamp = 1700000000000L,
            referenceNumber = null,
            rawDescription = null,
            transactionFingerprint = null
        )
        assertFalse("Manual plain transaction must NOT be imported", manualPlain.isImported)
    }

    @Test
    fun addManualTransaction_createsNonImportedTransaction() = runBlocking {
        val result = useCase.addTransaction(
            amount = 850.0,
            type = TransactionType.EXPENSE,
            description = "Grocery Store",
            category = "Groceries",
            dateTimestamp = 1700000000000L,
            referenceNumber = "REF-12345",
            bankName = "Cash"
        )

        assertTrue("UseCase addTransaction should succeed", result.isSuccess)
        assertEquals(1, repository.transactions.size)
        val created = repository.transactions.first()
        assertEquals(850.0, created.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, created.type)
        assertEquals("Grocery Store", created.description)
        assertEquals("Groceries", created.category)
        assertEquals("REF-12345", created.referenceNumber)
        assertNull(created.transactionFingerprint)
        assertNull(created.rawDescription)
        assertFalse("Created transaction must not be imported", created.isImported)
    }

    @Test
    fun addManualTransaction_rejectsNonPositiveAmount() = runBlocking {
        val resultZero = useCase.addTransaction(
            amount = 0.0,
            type = TransactionType.EXPENSE,
            description = "Free item",
            category = "Other",
            dateTimestamp = 1700000000000L,
            referenceNumber = null
        )
        assertTrue("Zero amount must fail validation", resultZero.isFailure)

        val resultNegative = useCase.addTransaction(
            amount = -100.0,
            type = TransactionType.EXPENSE,
            description = "Negative item",
            category = "Other",
            dateTimestamp = 1700000000000L,
            referenceNumber = null
        )
        assertTrue("Negative amount must fail validation", resultNegative.isFailure)
        assertEquals(0, repository.transactions.size)
    }

    @Test
    fun addManualTransfer_createsLinkedPair() = runBlocking {
        val result = useCase.addTransfer(
            amount = 5000.0,
            description = "Savings Transfer",
            dateTimestamp = 1700000000000L,
            fromAccountId = "acc_hdfc",
            fromAccountLast4 = "1234",
            fromBankName = "HDFC Bank",
            toAccountId = "acc_sbi",
            toAccountLast4 = "5678",
            toBankName = "SBI",
            referenceNumber = "TXN-TRANSFER-99"
        )

        assertTrue("AddTransfer should succeed", result.isSuccess)
        assertEquals(2, repository.transactions.size)

        val debitLeg = repository.transactions.first { it.role == TransactionRole.TRANSFER_OUT }
        val creditLeg = repository.transactions.first { it.role == TransactionRole.TRANSFER_IN }

        assertEquals(5000.0, debitLeg.amount, 0.001)
        assertEquals(5000.0, creditLeg.amount, 0.001)
        assertEquals(TransactionRole.TRANSFER_OUT, debitLeg.role)
        assertEquals(TransactionRole.TRANSFER_IN, creditLeg.role)
        assertNotNull(debitLeg.transferLinkId)
        assertEquals(debitLeg.transferLinkId, creditLeg.transferLinkId)

        assertFalse("Debit transfer leg must not be imported", debitLeg.isImported)
        assertFalse("Credit transfer leg must not be imported", creditLeg.isImported)
        assertTrue("Debit transfer leg recognized as transfer", debitLeg.isTransfer)
        assertTrue("Credit transfer leg recognized as transfer", creditLeg.isTransfer)
    }
}
