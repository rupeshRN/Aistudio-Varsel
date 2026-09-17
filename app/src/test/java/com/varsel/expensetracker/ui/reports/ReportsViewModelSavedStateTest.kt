package com.varsel.expensetracker.ui.reports

import androidx.lifecycle.SavedStateHandle
import com.varsel.expensetracker.data.local.dao.FinancialEventAllocationDao
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import com.varsel.expensetracker.data.repository.FinancialEventAllocationRepository
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import com.varsel.expensetracker.domain.repository.TransactionLinkGroupRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReportsViewModelSavedStateTest {

    private class FakeTransactionRepository : TransactionRepository {
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun insertTransactions(transactions: List<Transaction>) {}
        override suspend fun insertTransaction(transaction: Transaction) {}
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun getTransactionById(id: Long): Transaction? = null
        override suspend fun findExistingFingerprints(fingerprints: List<String>): Set<String> = emptySet()
        override suspend fun linkTransactions(transactionIds: List<Long>, transactionLinkId: String) {}
        override suspend fun unlinkTransaction(transactionId: Long) {}
        override suspend fun linkTransfer(transferOutTransactionId: Long, transferInTransactionId: Long): com.varsel.expensetracker.domain.repository.TransferLinkResult =
            com.varsel.expensetracker.domain.repository.TransferLinkResult.Success
        override suspend fun unlinkTransfer(transactionId: Long) {}
        override suspend fun getLinkedTransferTransactions(transferLinkId: String): List<Transaction> = emptyList()
        override suspend fun updateTransactions(transactions: List<Transaction>) {}
        override suspend fun findSimilarTransactions(excludeId: Long, pattern: String, isIncome: Boolean, sinceTimestamp: Long): List<Transaction> = emptyList()
        override suspend fun getTransactionsByRecurringItemId(recurringItemId: Long): List<Transaction> = emptyList()
        override suspend fun hasTransactionWithReference(referenceNumber: String): Boolean = false
    }

    private class FakeTransactionLinkGroupRepository : TransactionLinkGroupRepository {
        override fun getAllGroups(): Flow<List<TransactionLinkGroup>> = flowOf(emptyList())
        override suspend fun getGroup(transactionLinkId: String): TransactionLinkGroup? = null
        override suspend fun saveGroup(group: TransactionLinkGroup) {}
        override suspend fun deleteGroup(transactionLinkId: String) {}
    }

    private class FakeFinancialEventAllocationDao : FinancialEventAllocationDao {
        override fun getAllAllocations(): Flow<List<FinancialEventAllocationEntity>> = flowOf(emptyList())
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
        override fun observeAllocationsForTransaction(transactionId: Long): Flow<List<FinancialEventAllocationEntity>> = flowOf(emptyList())
        override fun observeAllocationsForFinancialEvent(transactionLinkId: String): Flow<List<FinancialEventAllocationEntity>> = flowOf(emptyList())
        override suspend fun updateAllocationAmount(transactionId: Long, transactionLinkId: String, newAmount: Double) {}
        override suspend fun deleteAllocationForTransactionAndEvent(transactionId: Long, transactionLinkId: String) {}
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle): ReportsViewModel {
        val fakeTxRepo = FakeTransactionRepository()
        val fakeGroupRepo = FakeTransactionLinkGroupRepository()
        val fakeAllocationRepo = FinancialEventAllocationRepository(FakeFinancialEventAllocationDao())
        return ReportsViewModel(
            savedStateHandle = savedStateHandle,
            transactionRepository = fakeTxRepo,
            transactionLinkGroupRepository = fakeGroupRepo,
            financialEventAllocationRepository = fakeAllocationRepo
        )
    }

    @Test
    fun testDefaultStateInitializesWhenNoSavedState() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)

        val state = viewModel.uiState.value
        assertEquals(PeriodFilter.THIS_MONTH, state.periodFilter)
        assertEquals(ReportPeriod.MONTH, state.period)
        assertEquals(YearMonth.now(), state.selectedMonth)
        assertEquals(ReportsTab.OVERVIEW, state.currentTab)
        assertEquals(ReportsFlow.EXPENSES, state.selectedFlow)
        assertTrue(state.selectedAccountIds.isEmpty())
    }

    @Test
    fun testMutationsPersistToSavedStateHandle() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)

        // 1. Tab and flow
        viewModel.selectReportsTab(ReportsTab.COMPARE)
        assertEquals(ReportsTab.COMPARE.name, savedStateHandle.get<String>(ReportsViewModel.KEY_CURRENT_TAB))

        viewModel.selectComparisonWindow(ComparisonWindow.SIX_MONTHS)
        assertEquals(ComparisonWindow.SIX_MONTHS.name, savedStateHandle.get<String>(ReportsViewModel.KEY_COMPARISON_WINDOW))

        viewModel.selectComparisonFlow(ReportsFlow.INCOME)
        assertEquals(ReportsFlow.INCOME.name, savedStateHandle.get<String>(ReportsViewModel.KEY_COMPARISON_FLOW))

        viewModel.selectFlow(ReportsFlow.INCOME)
        assertEquals(ReportsFlow.INCOME.name, savedStateHandle.get<String>(ReportsViewModel.KEY_SELECTED_FLOW))

        // 2. Period and Month
        viewModel.selectPeriod(PeriodFilter.LAST_3_MONTHS)
        assertEquals(PeriodFilter.LAST_3_MONTHS.name, savedStateHandle.get<String>(ReportsViewModel.KEY_PERIOD_FILTER))
        assertEquals(ReportPeriod.MONTH.name, savedStateHandle.get<String>(ReportsViewModel.KEY_REPORT_PERIOD))

        val customStart = LocalDate.of(2026, 1, 15)
        val customEnd = LocalDate.of(2026, 3, 20)
        viewModel.setCustomDateRange(customStart, customEnd)
        assertEquals(PeriodFilter.CUSTOM.name, savedStateHandle.get<String>(ReportsViewModel.KEY_PERIOD_FILTER))
        assertEquals(ReportPeriod.CUSTOM.name, savedStateHandle.get<String>(ReportsViewModel.KEY_REPORT_PERIOD))
        assertEquals(customStart.toString(), savedStateHandle.get<String>(ReportsViewModel.KEY_CUSTOM_START_DATE))
        assertEquals(customEnd.toString(), savedStateHandle.get<String>(ReportsViewModel.KEY_CUSTOM_END_DATE))

        val selectedMonth = YearMonth.of(2026, 7)
        viewModel.selectMonth(selectedMonth)
        assertEquals(selectedMonth.toString(), savedStateHandle.get<String>(ReportsViewModel.KEY_SELECTED_MONTH))

        // 3. Account filter
        viewModel.selectAccount("acc_1")
        assertEquals(listOf("acc_1"), savedStateHandle.get<List<String>>(ReportsViewModel.KEY_SELECTED_ACCOUNT_IDS))

        viewModel.toggleAccount("acc_2")
        val savedAccounts = savedStateHandle.get<List<String>>(ReportsViewModel.KEY_SELECTED_ACCOUNT_IDS)
        assertEquals(setOf("acc_1", "acc_2"), savedAccounts?.toSet())
    }

    @Test
    fun testViewModelRestoresFromSavedStateHandle() {
        val initialMap = mapOf(
            ReportsViewModel.KEY_PERIOD_FILTER to PeriodFilter.CUSTOM.name,
            ReportsViewModel.KEY_REPORT_PERIOD to ReportPeriod.CUSTOM.name,
            ReportsViewModel.KEY_SELECTED_MONTH to "2025-11",
            ReportsViewModel.KEY_CUSTOM_START_DATE to "2025-10-01",
            ReportsViewModel.KEY_CUSTOM_END_DATE to "2025-11-15",
            ReportsViewModel.KEY_CURRENT_TAB to ReportsTab.COMPARE.name,
            ReportsViewModel.KEY_SELECTED_FLOW to ReportsFlow.INCOME.name,
            ReportsViewModel.KEY_COMPARISON_WINDOW to ComparisonWindow.SIX_MONTHS.name,
            ReportsViewModel.KEY_COMPARISON_FLOW to ReportsFlow.INCOME.name,
            ReportsViewModel.KEY_SELECTED_ACCOUNT_IDS to arrayListOf("acc_savings", "acc_wallet")
        )
        val savedStateHandle = SavedStateHandle(initialMap)
        val viewModel = createViewModel(savedStateHandle)

        val state = viewModel.uiState.value

        // Assert Period / Month / Date Range
        assertEquals(PeriodFilter.CUSTOM, state.periodFilter)
        assertEquals(ReportPeriod.CUSTOM, state.period)
        assertEquals(YearMonth.of(2025, 11), state.selectedMonth)
        assertEquals(LocalDate.of(2025, 10, 1), state.customStartDate)
        assertEquals(LocalDate.of(2025, 11, 15), state.customEndDate)

        // Assert Tab and Flow
        assertEquals(ReportsTab.COMPARE, state.currentTab)
        assertEquals(ReportsFlow.INCOME, state.selectedFlow)
        assertEquals(ComparisonWindow.SIX_MONTHS, state.comparisonWindow)
        assertEquals(ReportsFlow.INCOME, state.comparisonFlow)

        // Assert Account Filter
        assertEquals(setOf("acc_savings", "acc_wallet"), state.selectedAccountIds)
    }
}
