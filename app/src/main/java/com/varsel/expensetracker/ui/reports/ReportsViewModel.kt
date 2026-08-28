package com.varsel.expensetracker.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.TransactionLinkGroupRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.data.repository.FinancialEventAllocationRepository
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for the production Reports feature.
 *
 * Responsibilities:
 *
 * - Observe transactions.
 * - Observe Financial Event groups.
 * - Build the available account list.
 * - Apply period filtering.
 * - Apply account filtering.
 * - Calculate cash flow.
 * - Calculate category reports.
 * - Calculate Financial Events.
 * - Manage report selections.
 *
 * IMPORTANT REPORTING RULE:
 *
 * A transaction linked to a Financial Event must NOT appear
 * inside its original transaction category.
 *
 * Instead:
 *
 *     linked expense transactions
 *                 +
 *     linked reimbursement transactions
 *                 ↓
 *          Financial Event
 *                 ↓
 *        effective cost
 *                 ↓
 *       Financial Event category
 *
 * Example:
 *
 *     Food transaction       ₹1,000
 *     Travel transaction     ₹1,500
 *     Uncategorized         ₹500
 *     Reimbursement        -₹2,000
 *     --------------------------------
 *     Financial Event        ₹1,000
 *
 * If the Financial Event category is Travel, only ₹1,000
 * is added to Travel.
 *
 * The original Food / Travel / Uncategorized transactions
 * disappear from normal category reporting.
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

    val uiState: StateFlow<ReportsUiState> =
        _uiState.asStateFlow()

    private val zoneId: ZoneId =
        ZoneId.systemDefault()

    private var latestTransactions: List<Transaction> =
        emptyList()

    private var latestGroups: List<TransactionLinkGroup> =
        emptyList()

    private var latestAllocations:
    List<FinancialEventAllocationEntity> =
    emptyList()

    init {
        observeReportData()
    }

    // ------------------------------------------------------------------------
    // Period actions
    // ------------------------------------------------------------------------

    fun previousMonth() {
        updateSelectedMonth(
            _uiState.value.selectedMonth.minusMonths(1)
        )
    }

    fun nextMonth() {
        updateSelectedMonth(
            _uiState.value.selectedMonth.plusMonths(1)
        )
    }

    fun selectMonth(
        month: YearMonth
    ) {
        updateSelectedMonth(month)
    }

    private fun updateSelectedMonth(
        month: YearMonth
    ) {
        _uiState.value =
            _uiState.value.copy(
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
    // Account filter
    // ------------------------------------------------------------------------

    /**
     * Select or deselect an account.
     *
     * Empty selection means All Accounts.
     */
    fun toggleAccount(
        accountId: String
    ) {
        val current =
            _uiState.value.selectedAccountIds

        val updated =
            if (accountId in current) {
                current - accountId
            } else {
                current + accountId
            }

        applyAccountSelection(updated)
    }

    /**
     * Select a single account.
     */
    fun selectAccount(
        accountId: String
    ) {
        applyAccountSelection(
            setOf(accountId)
        )
    }

    /**
     * Show all accounts.
     */
    fun selectAllAccounts() {
        applyAccountSelection(
            emptySet()
        )
    }

    /**
     * Replace the complete account selection.
     */
    fun setSelectedAccounts(
        accountIds: Set<String>
    ) {
        applyAccountSelection(accountIds)
    }

    private fun applyAccountSelection(
        accountIds: Set<String>
    ) {
        _uiState.value =
            _uiState.value.copy(
                selectedAccountIds = accountIds,
                selectedExpenseCategory = null,
                selectedIncomeCategory = null,
                errorMessage = null,
                isLoading = true
            )

        rebuildReport()
    }

    // ------------------------------------------------------------------------
    // Money Flow
    // ------------------------------------------------------------------------

    fun selectFlow(
        flow: ReportsFlow
    ) {
        _uiState.value =
            _uiState.value.copy(
                selectedFlow = flow,
                selectedExpenseCategory = null,
                selectedIncomeCategory = null,
                drillDownState = CategoryDrillDownState()
            )
    }

    fun selectExpenseCategory(
        category: String?
    ) {
        _uiState.value =
            _uiState.value.copy(
                selectedExpenseCategory = category
            )
        if (category != null) {
            openCategoryDrillDown(category, ReportsFlow.EXPENSES)
        }
    }

    fun selectIncomeCategory(
        category: String?
    ) {
        _uiState.value =
            _uiState.value.copy(
                selectedIncomeCategory = category
            )
        if (category != null) {
            openCategoryDrillDown(category, ReportsFlow.INCOME)
        }
    }

    fun openCategoryDrillDown(
        categoryName: String,
        flow: ReportsFlow = _uiState.value.selectedFlow
    ) {
        val state = _uiState.value
        val items = buildCategoryDrillDownItems(
            categoryName = categoryName,
            flow = flow,
            selectedMonth = state.selectedMonth,
            selectedAccountIds = state.selectedAccountIds
        )

        val totalCategoryAmount = when (flow) {
            ReportsFlow.EXPENSES -> {
                state.expenseCategories.firstOrNull { it.category.equals(categoryName, ignoreCase = true) }?.totalAmount
                    ?: items.sumOf { it.amount }
            }
            ReportsFlow.INCOME -> {
                state.incomeCategories.firstOrNull { it.category.equals(categoryName, ignoreCase = true) }?.totalAmount
                    ?: items.sumOf { it.amount }
            }
        }

        val totalFlowAmount = when (flow) {
            ReportsFlow.EXPENSES -> state.cashFlow.effectiveExpense
            ReportsFlow.INCOME -> state.cashFlow.actualIncome
        }

        val percent = if (totalFlowAmount > 0.0) {
            (totalCategoryAmount / totalFlowAmount) * 100.0
        } else {
            0.0
        }

        _uiState.value = _uiState.value.copy(
            drillDownState = CategoryDrillDownState(
                isVisible = true,
                categoryName = categoryName,
                flow = flow,
                totalCategoryAmount = totalCategoryAmount,
                percentOfTotal = percent,
                month = state.selectedMonth,
                items = items,
                searchQuery = ""
            )
        )
    }

    fun updateDrillDownSearch(query: String) {
        _uiState.value = _uiState.value.copy(
            drillDownState = _uiState.value.drillDownState.copy(searchQuery = query)
        )
    }

    fun dismissCategoryDrillDown() {
        _uiState.value = _uiState.value.copy(
            selectedExpenseCategory = null,
            selectedIncomeCategory = null,
            drillDownState = _uiState.value.drillDownState.copy(isVisible = false)
        )
    }

    private fun buildCategoryDrillDownItems(
        categoryName: String,
        flow: ReportsFlow,
        selectedMonth: YearMonth,
        selectedAccountIds: Set<String>
    ): List<CategoryDrillDownItem> {
        val periodTransactions = latestTransactions.filter { it.belongsToMonth(selectedMonth) }
        val filteredTransactions = filterByAccounts(periodTransactions, selectedAccountIds)
        val allFilteredTransactions = filterByAccounts(latestTransactions, selectedAccountIds)
        val resolvedEvents = resolveFinancialEvents(allFilteredTransactions, latestGroups, latestAllocations)

        val result = mutableListOf<CategoryDrillDownItem>()

        if (flow == ReportsFlow.EXPENSES) {
            // 1. Regular expense transactions
            filteredTransactions
                .filter { tx ->
                    (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.DEBIT) &&
                        tx.role == TransactionRole.NORMAL &&
                        tx.transferLinkId == null &&
                        tx.category.equals(categoryName, ignoreCase = true)
                }
                .forEach { tx ->
                    val unallocated = getUnallocatedAmount(tx, latestAllocations)
                    if (unallocated > 0.0) {
                        result.add(
                            CategoryDrillDownItem(
                                transactionId = tx.id,
                                description = tx.description,
                                category = tx.category,
                                dateTimestamp = tx.dateTimestamp,
                                amount = unallocated,
                                isExpense = true,
                                accountLast4 = tx.accountLast4,
                                isEventAllocation = false
                            )
                        )
                    }
                }

            // 2. Financial event net expenses if final month matches
            resolvedEvents.forEach { event ->
                if (event.group.category.equals(categoryName, ignoreCase = true) &&
                    event.finalMonth == selectedMonth &&
                    event.netCost > 0.0
                ) {
                    // Include event root/rep item or top transaction
                    val primaryTx = event.eventTransactions.firstOrNull()
                    result.add(
                        CategoryDrillDownItem(
                            transactionId = primaryTx?.id ?: 0L,
                            description = event.group.groupName,
                            category = event.group.category,
                            dateTimestamp = primaryTx?.dateTimestamp ?: System.currentTimeMillis(),
                            amount = event.netCost,
                            isExpense = true,
                            accountLast4 = primaryTx?.accountLast4,
                            isEventAllocation = true,
                            eventName = event.group.groupName,
                            eventLinkId = event.group.transactionLinkId
                        )
                    )
                }
            }
        } else {
            // Income
            filteredTransactions
                .filter { tx ->
                    tx.type == TransactionType.INCOME &&
                        tx.role == TransactionRole.NORMAL &&
                        tx.transferLinkId == null &&
                        tx.category.equals(categoryName, ignoreCase = true)
                }
                .forEach { tx ->
                    val unallocated = getUnallocatedAmount(tx, latestAllocations)
                    if (unallocated > 0.0) {
                        result.add(
                            CategoryDrillDownItem(
                                transactionId = tx.id,
                                description = tx.description,
                                category = tx.category,
                                dateTimestamp = tx.dateTimestamp,
                                amount = unallocated,
                                isExpense = false,
                                accountLast4 = tx.accountLast4,
                                isEventAllocation = false
                            )
                        )
                    }
                }

            // Surplus events
            resolvedEvents.forEach { event ->
                if (event.group.category.equals(categoryName, ignoreCase = true) &&
                    event.finalMonth == selectedMonth &&
                    event.netCost < 0.0
                ) {
                    val primaryTx = event.eventTransactions.firstOrNull()
                    result.add(
                        CategoryDrillDownItem(
                            transactionId = primaryTx?.id ?: 0L,
                            description = event.group.groupName + " (Surplus)",
                            category = event.group.category,
                            dateTimestamp = primaryTx?.dateTimestamp ?: System.currentTimeMillis(),
                            amount = kotlin.math.abs(event.netCost),
                            isExpense = false,
                            accountLast4 = primaryTx?.accountLast4,
                            isEventAllocation = true,
                            eventName = event.group.groupName,
                            eventLinkId = event.group.transactionLinkId
                        )
                    )
                }
            }
        }

        return result.sortedByDescending { it.dateTimestamp }
    }

    fun clearCategorySelection() {
        _uiState.value =
            _uiState.value.copy(
                selectedExpenseCategory = null,
                selectedIncomeCategory = null,
                drillDownState = CategoryDrillDownState()
            )
    }

    fun retry() {
        rebuildReport()
    }

    // ------------------------------------------------------------------------
    // Repository observation
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

            latestTransactions =
                sourceData.transactions

            latestGroups =
                sourceData.groups

            latestAllocations =
                sourceData.allocations

            rebuildReport()
        }
    }
}

    // ------------------------------------------------------------------------
    // Report rebuilding
    // ------------------------------------------------------------------------

    private fun rebuildReport() {
        try {
            val state =
                _uiState.value

            /*
             * Step 1:
             *
             * Filter transactions by the selected reporting
             * period.
             */
            val periodTransactions =
                latestTransactions.filter { transaction ->
                    transaction.belongsToMonth(
                        state.selectedMonth
                    )
                }

            /*
             * Step 2:
             *
             * Apply account filter.
             *
             * Empty Set means All Accounts.
             */
            val filteredTransactions =
                filterByAccounts(
                    transactions = periodTransactions,
                    selectedAccountIds =
                        state.selectedAccountIds
                )

            /*
             * Account-filtered transactions across the complete
             * transaction history.
             *
             * This is NOT period filtered.
             *
             * It is used to resolve multi-month financial events
             * and overall event balances.
             */
            val accountFilteredAllTransactions =
                filterByAccounts(
                    transactions = latestTransactions,
                    selectedAccountIds =
                        state.selectedAccountIds
                )

            /*
             * Step 3:
             *
             * Resolve all Financial Events across history.
             */
            val resolvedEvents =
                resolveFinancialEvents(
                    allAccountTransactions =
                        accountFilteredAllTransactions,
                    groups =
                        latestGroups,
                    allocations =
                        latestAllocations
                )

            /*
             * Step 4:
             *
             * Build report sections.
             */
            val expenseCategories =
                buildExpenseCategories(
                    transactions =
                        filteredTransactions,
                    selectedMonth =
                        state.selectedMonth,
                    resolvedEvents =
                        resolvedEvents,
                    allocations =
                        latestAllocations
                )

            val incomeCategories =
                buildIncomeCategories(
                    transactions =
                        filteredTransactions,
                    selectedMonth =
                        state.selectedMonth,
                    resolvedEvents =
                        resolvedEvents,
                    allocations =
                        latestAllocations
                )

            val cashFlow =
                buildCashFlow(
                    expenseCategories =
                        expenseCategories,
                    incomeCategories =
                        incomeCategories
                )

            val financialEvents =
                buildFinancialEvents(
                    selectedMonth =
                        state.selectedMonth,
                    transactions =
                        filteredTransactions,
                    resolvedEvents =
                        resolvedEvents
                )

            /*
             * Account list comes from the complete transaction
             * history so accounts remain available to the filter
             * even if they have no transaction in the current
             * month.
             */
            val accounts =
                buildAccounts(
                    latestTransactions
                )

            _uiState.value =
                state.copy(
                    isLoading = false,
                    errorMessage = null,
                    accounts = accounts,
                    cashFlow = cashFlow,
                    expenseCategories =
                        expenseCategories,
                    incomeCategories =
                        incomeCategories,
                    financialEvents =
                        financialEvents
                )

        } catch (exception: Exception) {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    errorMessage =
                        exception.message
                            ?: "Unable to prepare report"
                )
        }
    }

    // ------------------------------------------------------------------------
    // Account filtering
    // ------------------------------------------------------------------------

    private fun filterByAccounts(
        transactions: List<Transaction>,
        selectedAccountIds: Set<String>
    ): List<Transaction> {

        /*
         * Empty means All Accounts.
         */
        if (selectedAccountIds.isEmpty()) {
            return transactions
        }

        return transactions.filter { transaction ->

            transaction.accountId != null &&
                transaction.accountId in
                selectedAccountIds
        }
    }

    /**
     * Builds the account list from the complete transaction
     * history.
     */
    private fun buildAccounts(
        transactions: List<Transaction>
    ): List<ReportsAccount> {

        return transactions
            .asSequence()
            .filter {
                !it.accountId.isNullOrBlank()
            }
            .groupBy {
                it.accountId!!
            }
            .map { (accountId, accountTransactions) ->

                val last4 =
                    accountTransactions
                        .mapNotNull {
                            it.accountLast4
                        }
                        .firstOrNull()

                ReportsAccount(
                    accountId = accountId,
                    accountLast4 = last4
                )
            }
            .sortedBy {
                it.accountLast4 ?: ""
            }
    }

    // ------------------------------------------------------------------------
    // Financial Event Resolution
    // ------------------------------------------------------------------------

    private data class ResolvedFinancialEvent(
        val group: TransactionLinkGroup,
        val eventTransactions: List<Transaction>,
        val allocationMap: Map<Long, Double>,
        val coveredMonths: List<YearMonth>,
        val finalMonth: YearMonth?,
        val totalExpense: Double,
        val totalReimbursement: Double,
        val netCost: Double
    )

    private fun resolveFinancialEvents(
        allAccountTransactions: List<Transaction>,
        groups: List<TransactionLinkGroup>,
        allocations: List<FinancialEventAllocationEntity>
    ): List<ResolvedFinancialEvent> {

        val allocationsByGroup =
            allocations.groupBy {
                it.transactionLinkId
            }

        return groups.mapNotNull { group ->

            val groupAllocations =
                allocationsByGroup[group.transactionLinkId].orEmpty()

            val allocationByTxId =
                groupAllocations
                    .groupBy { it.transactionId }
                    .mapValues { (_, rows) ->
                        rows.sumOf { it.allocatedAmount }
                    }

            /*
             * A transaction belongs to this event if:
             * 1. It has an allocation record for this event, OR
             * 2. It has transactionLinkId matching this event
             */
            val eventTransactions =
                allAccountTransactions.filter { tx ->
                    tx.transactionLinkId == group.transactionLinkId ||
                        tx.id in allocationByTxId.keys
                }

            if (eventTransactions.isEmpty() && groupAllocations.isEmpty()) {
                return@mapNotNull null
            }

            val txAmounts =
                eventTransactions.associate { tx ->
                    val allocAmt =
                        allocationByTxId[tx.id]

                    val amt =
                        if (allocAmt != null && allocAmt > 0.0) {
                            allocAmt
                        } else {
                            kotlin.math.abs(tx.amount)
                        }

                    tx.id to amt
                }

            val coveredMonths =
                eventTransactions
                    .map {
                        transactionYearMonth(it.dateTimestamp)
                    }
                    .distinct()
                    .sorted()

            val finalMonth =
                coveredMonths.maxOrNull()

            val totalExpense =
                eventTransactions
                    .asSequence()
                    .filter { tx ->
                        (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.DEBIT) &&
                            tx.role != TransactionRole.TRANSFER_OUT &&
                            tx.role != TransactionRole.REIMBURSEMENT
                    }
                    .sumOf { tx ->
                        txAmounts[tx.id] ?: 0.0
                    }

            val totalReimbursement =
                eventTransactions
                    .asSequence()
                    .filter { tx ->
                        tx.role == TransactionRole.REIMBURSEMENT ||
                            (tx.type == TransactionType.INCOME &&
                                tx.role != TransactionRole.TRANSFER_IN &&
                                tx.role != TransactionRole.TRANSFER_OUT)
                    }
                    .sumOf { tx ->
                        txAmounts[tx.id] ?: 0.0
                    }

            val netCost =
                totalExpense - totalReimbursement

            ResolvedFinancialEvent(
                group = group,
                eventTransactions = eventTransactions,
                allocationMap = txAmounts,
                coveredMonths = coveredMonths,
                finalMonth = finalMonth,
                totalExpense = totalExpense,
                totalReimbursement = totalReimbursement,
                netCost = netCost
            )
        }
    }

    private fun getUnallocatedAmount(
        transaction: Transaction,
        allocations: List<FinancialEventAllocationEntity>
    ): Double {
        val totalAllocated =
            allocations
                .filter { it.transactionId == transaction.id }
                .sumOf { it.allocatedAmount }

        val effectiveAllocated =
            if (totalAllocated > 0.0) {
                totalAllocated
            } else if (transaction.transactionLinkId != null) {
                kotlin.math.abs(transaction.amount)
            } else {
                0.0
            }

        return (kotlin.math.abs(transaction.amount) - effectiveAllocated)
            .coerceAtLeast(0.0)
    }

    // ------------------------------------------------------------------------
    // Cash Flow
    // ------------------------------------------------------------------------

    private fun buildCashFlow(
        expenseCategories: List<ReportsExpenseCategory>,
        incomeCategories: List<ReportsIncomeCategory>
    ): ReportsCashFlow {

        val actualIncome =
            incomeCategories.sumOf {
                it.totalAmount
            }

        val effectiveExpense =
            expenseCategories.sumOf {
                it.totalAmount
            }

        return ReportsCashFlow(
            actualIncome = actualIncome,
            effectiveExpense = effectiveExpense,
            netCashFlow = actualIncome - effectiveExpense
        )
    }

    // ------------------------------------------------------------------------
    // Expense Categories
    // ------------------------------------------------------------------------

    private fun buildExpenseCategories(
        transactions: List<Transaction>,
        selectedMonth: YearMonth,
        resolvedEvents: List<ResolvedFinancialEvent>,
        allocations: List<FinancialEventAllocationEntity>
    ): List<ReportsExpenseCategory> {

        /*
         * --------------------------------------------------------
         * Ordinary expense categories
         * --------------------------------------------------------
         *
         * Only the unallocated remainder remains in the original
         * transaction category.
         */
        val normalAmountsByCategory =
            transactions
                .asSequence()
                .filter { transaction ->
                    (transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.DEBIT) &&
                        transaction.role == TransactionRole.NORMAL &&
                        transaction.transferLinkId == null
                }
                .mapNotNull { transaction ->
                    val remainingAmount =
                        getUnallocatedAmount(
                            transaction = transaction,
                            allocations = allocations
                        )

                    if (remainingAmount <= 0.0) {
                        null
                    } else {
                        transaction.category to remainingAmount
                    }
                }
                .groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second }
                )
                .mapValues { (_, amounts) ->
                    amounts.sum()
                }

        /*
         * --------------------------------------------------------
         * Financial Event category amounts
         * --------------------------------------------------------
         *
         * Multi-month rule:
         *
         * If an event spans multiple months (e.g. Jun & Jul),
         * it is mapped into the pie chart ONLY in its FINAL month.
         * In interim months, 0 is added to categories/pie chart.
         */
        val eventAmountsByCategory =
            mutableMapOf<String, Double>()

        val eventGrossByCategory =
            mutableMapOf<String, Double>()

        val eventReimbursementByCategory =
            mutableMapOf<String, Double>()

        resolvedEvents.forEach { event ->
            val category =
                event.group.category.trim()

            if (category.isBlank()) {
                return@forEach
            }

            // Only map to pie chart in the event's final month
            if (event.finalMonth == selectedMonth) {
                if (event.netCost > 0.0) {
                    eventAmountsByCategory[category] =
                        (eventAmountsByCategory[category] ?: 0.0) + event.netCost

                    eventGrossByCategory[category] =
                        (eventGrossByCategory[category] ?: 0.0) + event.totalExpense

                    eventReimbursementByCategory[category] =
                        (eventReimbursementByCategory[category] ?: 0.0) + event.totalReimbursement
                }
            }
        }

        /*
         * Combine ordinary categories + Financial Event category
         */
        val allCategories =
            (normalAmountsByCategory.keys + eventAmountsByCategory.keys)
                .filter { it.isNotBlank() }
                .distinct()

        return allCategories
            .map { category ->
                val normalAmount =
                    normalAmountsByCategory[category] ?: 0.0

                val eventAmount =
                    eventAmountsByCategory[category] ?: 0.0

                val reimbursementAmount =
                    eventReimbursementByCategory[category] ?: 0.0

                ReportsExpenseCategory(
                    category = category,
                    totalAmount = normalAmount + eventAmount,
                    normalAmount = normalAmount,
                    financialEventAmount = eventAmount,
                    reimbursedAmount = reimbursementAmount,
                    effectiveFinancialEventAmount = eventAmount
                )
            }
            .filter {
                it.totalAmount > 0.0
            }
            .sortedByDescending {
                it.totalAmount
            }
    }

    // ------------------------------------------------------------------------
    // Income Categories
    // ------------------------------------------------------------------------

    private fun buildIncomeCategories(
        transactions: List<Transaction>,
        selectedMonth: YearMonth,
        resolvedEvents: List<ResolvedFinancialEvent>,
        allocations: List<FinancialEventAllocationEntity>
    ): List<ReportsIncomeCategory> {

        val normalAmountsByCategory =
            transactions
                .asSequence()
                .filter { transaction ->
                    transaction.type == TransactionType.INCOME &&
                        transaction.role == TransactionRole.NORMAL &&
                        transaction.transferLinkId == null
                }
                .mapNotNull { transaction ->
                    val remainingAmount =
                        getUnallocatedAmount(
                            transaction = transaction,
                            allocations = allocations
                        )

                    if (remainingAmount <= 0.0) {
                        null
                    } else {
                        transaction.category to remainingAmount
                    }
                }
                .groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second }
                )
                .mapValues { (_, amounts) ->
                    amounts.sum()
                }

        val eventSurplusByCategory =
            mutableMapOf<String, Double>()

        resolvedEvents.forEach { event ->
            val category =
                event.group.category.trim()

            if (category.isBlank()) {
                return@forEach
            }

            // If an event resulted in a net surplus (reimbursements > expenses),
            // report the net surplus as income in its final month
            if (event.finalMonth == selectedMonth && event.netCost < 0.0) {
                val surplus =
                    kotlin.math.abs(event.netCost)

                eventSurplusByCategory[category] =
                    (eventSurplusByCategory[category] ?: 0.0) + surplus
            }
        }

        val allCategories =
            (normalAmountsByCategory.keys + eventSurplusByCategory.keys)
                .filter { it.isNotBlank() }
                .distinct()

        return allCategories
            .map { category ->
                val normalAmount =
                    normalAmountsByCategory[category] ?: 0.0

                val surplusAmount =
                    eventSurplusByCategory[category] ?: 0.0

                ReportsIncomeCategory(
                    category = category,
                    totalAmount = normalAmount + surplusAmount
                )
            }
            .filter {
                it.totalAmount > 0.0
            }
            .sortedByDescending {
                it.totalAmount
            }
    }

    /**
     * Builds Financial Event summaries.
     */
    private fun buildFinancialEvents(
        selectedMonth: YearMonth,
        transactions: List<Transaction>,
        resolvedEvents: List<ResolvedFinancialEvent>
    ): List<ReportsFinancialEvent> {

        val selectedPeriodTransactionIds =
            transactions
                .map { it.id }
                .toSet()

        return resolvedEvents
            .mapNotNull { event ->
                val hasActivityInSelectedMonth =
                    event.coveredMonths.contains(selectedMonth)

                val isFinalMonth =
                    event.finalMonth == selectedMonth

                // Show event if it has transactions in this month or concludes in this month
                if (!hasActivityInSelectedMonth && !isFinalMonth) {
                    return@mapNotNull null
                }

                val selectedPeriodTransactions =
                    event.eventTransactions.filter {
                        it.id in selectedPeriodTransactionIds
                    }

                val selectedPeriodExpense =
                    selectedPeriodTransactions
                        .asSequence()
                        .filter { transaction ->
                            (transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.DEBIT) &&
                                transaction.role != TransactionRole.TRANSFER_OUT &&
                                transaction.role != TransactionRole.REIMBURSEMENT
                        }
                        .sumOf { transaction ->
                            event.allocationMap[transaction.id] ?: 0.0
                        }

                val selectedPeriodReimbursement =
                    selectedPeriodTransactions
                        .asSequence()
                        .filter { transaction ->
                            transaction.role == TransactionRole.REIMBURSEMENT ||
                                (transaction.type == TransactionType.INCOME &&
                                    transaction.role != TransactionRole.TRANSFER_IN &&
                                    transaction.role != TransactionRole.TRANSFER_OUT)
                        }
                        .sumOf { transaction ->
                            event.allocationMap[transaction.id] ?: 0.0
                        }

                ReportsFinancialEvent(
                    transactionLinkId = event.group.transactionLinkId,
                    groupName = event.group.groupName,
                    category = event.group.category,
                    expenseAmount = selectedPeriodExpense,
                    reimbursedAmount = selectedPeriodReimbursement,
                    effectiveCost = event.netCost,
                    totalEventExpense = event.totalExpense,
                    totalEventReimbursement = event.totalReimbursement,
                    isFinalMonth = isFinalMonth,
                    coveredMonths = event.coveredMonths
                )
            }
            .filter {
                it.expenseAmount != 0.0 ||
                    it.reimbursedAmount != 0.0 ||
                    it.effectiveCost != 0.0 ||
                    it.coveredMonths.contains(selectedMonth)
            }
            .sortedByDescending {
                kotlin.math.abs(it.effectiveCost)
            }
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private data class ReportSourceData(
        val transactions: List<Transaction>,
        val groups: List<TransactionLinkGroup>,
        val allocations: List<FinancialEventAllocationEntity>
    )

    private fun transactionYearMonth(
    dateTimestamp: Long
): YearMonth {

    return Instant
        .ofEpochMilli(dateTimestamp)
        .atZone(zoneId)
        .let {
            YearMonth.from(
                it.toLocalDate()
            )
        }
}
    private fun Transaction.belongsToMonth(
        month: YearMonth
    ): Boolean {

        val localDate =
            Instant
                .ofEpochMilli(dateTimestamp)
                .atZone(zoneId)
                .toLocalDate()

        return YearMonth.from(localDate) ==
            month
    }
}
