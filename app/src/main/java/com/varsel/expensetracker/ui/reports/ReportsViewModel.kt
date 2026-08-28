package com.varsel.expensetracker.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import com.varsel.expensetracker.data.repository.FinancialEventAllocationRepository
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.TransactionLinkGroupRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for the Reports feature.
 *
 * Responsibilities:
 * - Observe transactions, link groups, and allocations.
 * - Build available account list and apply account & period filtering.
 * - Calculate money flow, category reports, and Financial Events.
 *
 * FINANCIAL EVENT PASS-THROUGH & REPORTING RULES:
 * 1. Transactions linked to a Financial Event are pass-through clearing flows.
 *    They MUST NOT appear in normal transaction categories, nor inflate normal
 *    monthly income/expenses.
 * 2. Multi-month transactions linked to an event are tallied cumulatively
 *    through the end of the selected reporting month (<= selected month).
 * 3. Net Cost = (Cumulative Expenses <= Month) - (Cumulative Reimbursements <= Month).
 * 4. If Net Cost > 0: Contributes to the Financial Event's category in the Expense report.
 *    If Net Cost < 0: Contributes to the Financial Event's category in the Income report.
 *    If Net Cost == 0: Pass-through is completely settled (0 impact on reports).
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionLinkGroupRepository: TransactionLinkGroupRepository,
    private val financialEventAllocationRepository: FinancialEventAllocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReportsUiState(
            isLoading = true,
            selectedMonth = YearMonth.now()
        )
    )
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val zoneId: ZoneId = ZoneId.systemDefault()

    private var latestTransactions: List<Transaction> = emptyList()
    private var latestGroups: List<TransactionLinkGroup> = emptyList()
    private var latestAllocations: List<FinancialEventAllocationEntity> = emptyList()

    init {
        observeReportData()
    }

    // ------------------------------------------------------------------------
    // Period Actions
    // ------------------------------------------------------------------------

    fun previousMonth() {
        updateSelectedMonth(_uiState.value.selectedMonth.minusMonths(1))
    }

    fun nextMonth() {
        updateSelectedMonth(_uiState.value.selectedMonth.plusMonths(1))
    }

    fun selectMonth(month: YearMonth) {
        updateSelectedMonth(month)
    }

    private fun updateSelectedMonth(month: YearMonth) {
        _uiState.value = _uiState.value.copy(
            period = ReportPeriod.MONTH,
            selectedMonth = month,
            selectedExpenseCategory = null,
            selectedIncomeCategory = null,
            errorMessage = null,
            isLoading = true
        )
        rebuildReport()
    }

    // ------------------------------------------------------------------------
    // Account Filter
    // ------------------------------------------------------------------------

    fun toggleAccount(accountId: String) {
        val current = _uiState.value.selectedAccountIds
        val updated = if (accountId in current) {
            current - accountId
        } else {
            current + accountId
        }
        applyAccountSelection(updated)
    }

    fun selectAccount(accountId: String) {
        applyAccountSelection(setOf(accountId))
    }

    fun selectAllAccounts() {
        applyAccountSelection(emptySet())
    }

    fun setSelectedAccounts(accountIds: Set<String>) {
        applyAccountSelection(accountIds)
    }

    private fun applyAccountSelection(accountIds: Set<String>) {
        _uiState.value = _uiState.value.copy(
            selectedAccountIds = accountIds,
            selectedExpenseCategory = null,
            selectedIncomeCategory = null,
            errorMessage = null,
            isLoading = true
        )
        rebuildReport()
    }

    // ------------------------------------------------------------------------
    // Money Flow & Selection
    // ------------------------------------------------------------------------

    fun selectFlow(flow: ReportsFlow) {
        _uiState.value = _uiState.value.copy(
            selectedFlow = flow,
            selectedExpenseCategory = null,
            selectedIncomeCategory = null
        )
    }

    fun selectExpenseCategory(category: String?) {
        _uiState.value = _uiState.value.copy(
            selectedExpenseCategory = category
        )
    }

    fun selectIncomeCategory(category: String?) {
        _uiState.value = _uiState.value.copy(
            selectedIncomeCategory = category
        )
    }

    fun clearCategorySelection() {
        _uiState.value = _uiState.value.copy(
            selectedExpenseCategory = null,
            selectedIncomeCategory = null
        )
    }

    fun retry() {
        rebuildReport()
    }

    // ------------------------------------------------------------------------
    // Repository Observation
    // ------------------------------------------------------------------------

    private fun observeReportData() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                transactionRepository.getAllTransactions(),
                transactionLinkGroupRepository.getAllGroups(),
                financialEventAllocationRepository.observeAllAllocations()
            ) { transactions, groups, allocations ->
                ReportSourceData(
                    transactions = transactions,
                    groups = groups,
                    allocations = allocations
                )
            }.collect { sourceData ->
                latestTransactions = sourceData.transactions
                latestGroups = sourceData.groups
                latestAllocations = sourceData.allocations
                rebuildReport()
            }
        }
    }

    // ------------------------------------------------------------------------
    // Report Rebuilding
    // ------------------------------------------------------------------------

    private fun rebuildReport() {
        try {
            val state = _uiState.value

            val periodTransactions = latestTransactions.filter { transaction ->
                transaction.belongsToMonth(state.selectedMonth)
            }

            val filteredTransactions = filterByAccounts(
                transactions = periodTransactions,
                selectedAccountIds = state.selectedAccountIds
            )

            val accountFilteredAllTransactions = filterByAccounts(
                transactions = latestTransactions,
                selectedAccountIds = state.selectedAccountIds
            )

            val expenseCategories = buildExpenseCategories(
                transactions = filteredTransactions,
                allAccountTransactions = accountFilteredAllTransactions,
                groups = latestGroups,
                allocations = latestAllocations
            )

            val incomeCategories = buildIncomeCategories(
                transactions = filteredTransactions,
                allAccountTransactions = accountFilteredAllTransactions,
                groups = latestGroups,
                allocations = latestAllocations
            )

            val financialEvents = buildFinancialEvents(
                transactions = filteredTransactions,
                allAccountTransactions = accountFilteredAllTransactions,
                groups = latestGroups,
                allocations = latestAllocations
            )

            val cashFlow = buildCashFlow(
                filteredTransactions = filteredTransactions,
                expenseCategories = expenseCategories,
                incomeCategories = incomeCategories
            )

            val accounts = buildAccounts(latestTransactions)

            _uiState.value = state.copy(
                isLoading = false,
                errorMessage = null,
                accounts = accounts,
                cashFlow = cashFlow,
                expenseCategories = expenseCategories,
                incomeCategories = incomeCategories,
                financialEvents = financialEvents
            )
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = exception.message ?: "Unable to prepare report"
            )
        }
    }

    // ------------------------------------------------------------------------
    // Account Filtering & Helpers
    // ------------------------------------------------------------------------

    private fun filterByAccounts(
        transactions: List<Transaction>,
        selectedAccountIds: Set<String>
    ): List<Transaction> {
        if (selectedAccountIds.isEmpty()) {
            return transactions
        }
        return transactions.filter { transaction ->
            transaction.accountId != null && transaction.accountId in selectedAccountIds
        }
    }

    private fun buildAccounts(transactions: List<Transaction>): List<ReportsAccount> {
        return transactions
            .asSequence()
            .filter { !it.accountId.isNullOrBlank() }
            .groupBy { it.accountId!! }
            .map { (accountId, accountTransactions) ->
                val last4 = accountTransactions.mapNotNull { it.accountLast4 }.firstOrNull()
                ReportsAccount(
                    accountId = accountId,
                    accountLast4 = last4
                )
            }
            .sortedBy { it.accountLast4 ?: "" }
    }

    // ------------------------------------------------------------------------
    // Financial Event Calculations Helper
    // ------------------------------------------------------------------------

    /**
     * Resolves all transactions and allocated amounts for an event.
     * Supports both direct transaction.transactionLinkId and allocation entries.
     */
    private fun getEventTransactionsWithAmounts(
        group: TransactionLinkGroup,
        allAccountTransactions: List<Transaction>,
        allocations: List<FinancialEventAllocationEntity>
    ): Map<Transaction, Double> {
        val eventAllocations = allocations.filter { it.transactionLinkId == group.transactionLinkId }
        val allocationByTransactionId = eventAllocations
            .groupBy { it.transactionId }
            .mapValues { (_, rows) -> rows.sumOf { it.allocatedAmount } }

        return allAccountTransactions
            .filter { transaction ->
                transaction.transactionLinkId == group.transactionLinkId ||
                    transaction.id in allocationByTransactionId.keys
            }
            .associateWith { transaction ->
                allocationByTransactionId[transaction.id] ?: abs(transaction.amount)
            }
    }

    // ------------------------------------------------------------------------
    // Cash Flow
    // ------------------------------------------------------------------------

    private fun buildCashFlow(
        filteredTransactions: List<Transaction>,
        expenseCategories: List<ReportsExpenseCategory>,
        incomeCategories: List<ReportsIncomeCategory>
    ): ReportsCashFlow {
        val totalExpense = expenseCategories.sumOf { it.totalAmount }
        val totalIncome = incomeCategories.sumOf { it.totalAmount }

        return ReportsCashFlow(
            actualIncome = totalIncome,
            effectiveExpense = totalExpense,
            netCashFlow = totalIncome - totalExpense
        )
    }

    // ------------------------------------------------------------------------
    // Expense Categories
    // ------------------------------------------------------------------------

    private fun buildExpenseCategories(
        transactions: List<Transaction>,
        allAccountTransactions: List<Transaction>,
        groups: List<TransactionLinkGroup>,
        allocations: List<FinancialEventAllocationEntity>
    ): List<ReportsExpenseCategory> {
        val allocatedByTransactionId = allocations
            .groupBy { it.transactionId }
            .mapValues { (_, rows) -> rows.sumOf { it.allocatedAmount } }

        // Normal expense transactions (excluding all Financial Event pass-throughs and transfers)
        val normalAmountsByCategory = transactions
            .asSequence()
            .filter { transaction ->
                transaction.type == TransactionType.EXPENSE &&
                    transaction.role == TransactionRole.NORMAL &&
                    transaction.transactionLinkId == null &&
                    transaction.transferLinkId == null
            }
            .mapNotNull { transaction ->
                val allocatedAmount = allocatedByTransactionId[transaction.id] ?: 0.0
                val remainingAmount = (transaction.amount - allocatedAmount).coerceAtLeast(0.0)
                if (remainingAmount <= 0.0) null else transaction.category to remainingAmount
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }

        // Financial Event cumulative net cost additions
        val eventAmountsByCategory = mutableMapOf<String, Double>()
        val eventGrossByCategory = mutableMapOf<String, Double>()
        val eventReimbursementByCategory = mutableMapOf<String, Double>()

        val selectedMonth = _uiState.value.selectedMonth

        groups.forEach { group ->
            val eventTxMap = getEventTransactionsWithAmounts(group, allAccountTransactions, allocations)
            if (eventTxMap.isEmpty()) return@forEach

            // Check if this event touches the selected month or has an unsettled cumulative balance
            val hasTransactionsInMonth = eventTxMap.keys.any { it.belongsToMonth(selectedMonth) }
            val transactionsThroughMonthEnd = eventTxMap.filter { (tx, _) ->
                transactionYearMonth(tx.dateTimestamp) <= selectedMonth
            }

            if (!hasTransactionsInMonth && transactionsThroughMonthEnd.isEmpty()) {
                return@forEach
            }

            val cumulativeExpense = transactionsThroughMonthEnd
                .filter { (tx, _) -> tx.type == TransactionType.EXPENSE && tx.role != TransactionRole.TRANSFER_OUT }
                .values.sum()

            val cumulativeReimbursement = transactionsThroughMonthEnd
                .filter { (tx, _) -> tx.role == TransactionRole.REIMBURSEMENT }
                .values.sum()

            val effectiveCost = cumulativeExpense - cumulativeReimbursement
            val category = group.category.trim()
            if (category.isBlank()) return@forEach

            eventGrossByCategory[category] = (eventGrossByCategory[category] ?: 0.0) + cumulativeExpense
            eventReimbursementByCategory[category] = (eventReimbursementByCategory[category] ?: 0.0) + cumulativeReimbursement

            // If effective net cost > 0, it contributes to out-of-pocket expense in this category
            if (effectiveCost > 0.0) {
                eventAmountsByCategory[category] = (eventAmountsByCategory[category] ?: 0.0) + effectiveCost
            }
        }

        val allCategories = (normalAmountsByCategory.keys + eventAmountsByCategory.keys)
            .filter { it.isNotBlank() }
            .distinct()

        return allCategories
            .map { category ->
                val normalAmount = normalAmountsByCategory[category] ?: 0.0
                val eventAmount = eventAmountsByCategory[category] ?: 0.0
                val reimbursementAmount = eventReimbursementByCategory[category] ?: 0.0
                ReportsExpenseCategory(
                    category = category,
                    totalAmount = normalAmount + eventAmount,
                    normalAmount = normalAmount,
                    financialEventAmount = eventAmount,
                    reimbursedAmount = reimbursementAmount,
                    effectiveFinancialEventAmount = eventAmount
                )
            }
            .filter { it.totalAmount > 0.0 }
            .sortedByDescending { it.totalAmount }
    }

    // ------------------------------------------------------------------------
    // Income Categories
    // ------------------------------------------------------------------------

    private fun buildIncomeCategories(
        transactions: List<Transaction>,
        allAccountTransactions: List<Transaction>,
        groups: List<TransactionLinkGroup>,
        allocations: List<FinancialEventAllocationEntity>
    ): List<ReportsIncomeCategory> {
        // 1. Normal income transactions (excluding linked pass-through reimbursements & transfers)
        val normalIncomeByCategory = transactions
            .asSequence()
            .filter { transaction ->
                transaction.type == TransactionType.INCOME &&
                    transaction.role == TransactionRole.NORMAL &&
                    transaction.transactionLinkId == null &&
                    transaction.transferLinkId == null
            }
            .filter { it.category.isNotBlank() }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toMutableMap()

        // 2. Financial Event surplus (if reimbursements exceed expenses across multi-month)
        val selectedMonth = _uiState.value.selectedMonth

        groups.forEach { group ->
            val eventTxMap = getEventTransactionsWithAmounts(group, allAccountTransactions, allocations)
            if (eventTxMap.isEmpty()) return@forEach

            val transactionsThroughMonthEnd = eventTxMap.filter { (tx, _) ->
                transactionYearMonth(tx.dateTimestamp) <= selectedMonth
            }

            val cumulativeExpense = transactionsThroughMonthEnd
                .filter { (tx, _) -> tx.type == TransactionType.EXPENSE && tx.role != TransactionRole.TRANSFER_OUT }
                .values.sum()

            val cumulativeReimbursement = transactionsThroughMonthEnd
                .filter { (tx, _) -> tx.role == TransactionRole.REIMBURSEMENT }
                .values.sum()

            val effectiveCost = cumulativeExpense - cumulativeReimbursement

            // Negative cost means surplus income for this event category
            if (effectiveCost < 0.0) {
                val category = group.category.trim()
                if (category.isNotBlank()) {
                    val surplus = abs(effectiveCost)
                    normalIncomeByCategory[category] = (normalIncomeByCategory[category] ?: 0.0) + surplus
                }
            }
        }

        return normalIncomeByCategory
            .map { (category, amount) ->
                ReportsIncomeCategory(
                    category = category,
                    totalAmount = amount
                )
            }
            .filter { it.totalAmount > 0.0 }
            .sortedByDescending { it.totalAmount }
    }

    // ------------------------------------------------------------------------
    // Financial Events Section
    // ------------------------------------------------------------------------

    private fun buildFinancialEvents(
        transactions: List<Transaction>,
        allAccountTransactions: List<Transaction>,
        groups: List<TransactionLinkGroup>,
        allocations: List<FinancialEventAllocationEntity>
    ): List<ReportsFinancialEvent> {
        val selectedPeriodTransactionIds = transactions.map { it.id }.toSet()
        val selectedMonth = _uiState.value.selectedMonth

        return groups
            .mapNotNull { group ->
                val eventTxMap = getEventTransactionsWithAmounts(group, allAccountTransactions, allocations)
                if (eventTxMap.isEmpty()) return@mapNotNull null

                val visibleInSelectedPeriod = eventTxMap.keys.any { it.id in selectedPeriodTransactionIds }
                if (!visibleInSelectedPeriod) return@mapNotNull null

                // Multi-month cumulative calculation through selected month
                val transactionsThroughMonthEnd = eventTxMap.filter { (tx, _) ->
                    transactionYearMonth(tx.dateTimestamp) <= selectedMonth
                }

                val cumulativeExpense = transactionsThroughMonthEnd
                    .filter { (tx, _) -> tx.type == TransactionType.EXPENSE && tx.role != TransactionRole.TRANSFER_OUT }
                    .values.sum()

                val cumulativeReimbursement = transactionsThroughMonthEnd
                    .filter { (tx, _) -> tx.role == TransactionRole.REIMBURSEMENT }
                    .values.sum()

                val effectiveCost = cumulativeExpense - cumulativeReimbursement

                val coveredMonths = eventTxMap.keys
                    .asSequence()
                    .filter { it.type == TransactionType.EXPENSE || it.role == TransactionRole.REIMBURSEMENT }
                    .map { transactionYearMonth(it.dateTimestamp) }
                    .distinct()
                    .sorted()
                    .toList()

                ReportsFinancialEvent(
                    transactionLinkId = group.transactionLinkId,
                    groupName = group.groupName,
                    category = group.category,
                    expenseAmount = cumulativeExpense,
                    reimbursedAmount = cumulativeReimbursement,
                    effectiveCost = effectiveCost,
                    coveredMonths = coveredMonths
                )
            }
            .filter {
                it.expenseAmount != 0.0 || it.reimbursedAmount != 0.0 || it.effectiveCost != 0.0
            }
            .sortedByDescending { abs(it.effectiveCost) }
    }

    // ------------------------------------------------------------------------
    // Date Helpers
    // ------------------------------------------------------------------------

    private data class ReportSourceData(
        val transactions: List<Transaction>,
        val groups: List<TransactionLinkGroup>,
        val allocations: List<FinancialEventAllocationEntity>
    )

    private fun transactionYearMonth(dateTimestamp: Long): YearMonth {
        return Instant
            .ofEpochMilli(dateTimestamp)
            .atZone(zoneId)
            .let { YearMonth.from(it.toLocalDate()) }
    }

    private fun Transaction.belongsToMonth(month: YearMonth): Boolean {
        val localDate = Instant
            .ofEpochMilli(dateTimestamp)
            .atZone(zoneId)
            .toLocalDate()
        return YearMonth.from(localDate) == month
    }
}
