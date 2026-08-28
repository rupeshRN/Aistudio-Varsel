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
                selectedIncomeCategory = null
            )
    }

    fun selectExpenseCategory(
        category: String?
    ) {
        _uiState.value =
            _uiState.value.copy(
                selectedExpenseCategory = category
            )
    }

    fun selectIncomeCategory(
        category: String?
    ) {
        _uiState.value =
            _uiState.value.copy(
                selectedIncomeCategory = category
            )
    }

    fun clearCategorySelection() {
        _uiState.value =
            _uiState.value.copy(
                selectedExpenseCategory = null,
                selectedIncomeCategory = null
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
 * It is used only to determine whether a Financial Event
 * spans multiple months.
 */
val accountFilteredAllTransactions =
    filterByAccounts(
        transactions = latestTransactions,
        selectedAccountIds =
            state.selectedAccountIds
    )

            /*
             * Step 4:
             *
             * Build report sections from the same filtered
             * transaction collection.
             */
            val cashFlow =
                buildCashFlow(
                    filteredTransactions
                )

val expenseCategories =
    buildExpenseCategories(
        transactions =
            filteredTransactions,

        groups =
            latestGroups,

        allocations =
            latestAllocations
    )

            val incomeCategories =
                buildIncomeCategories(
                    filteredTransactions
                )

val financialEvents =
    buildFinancialEvents(
        transactions =
            filteredTransactions,

        allAccountTransactions =
            accountFilteredAllTransactions,

        groups =
            latestGroups,

        allocations =
            latestAllocations
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
    // Cash Flow
    // ------------------------------------------------------------------------

private fun buildCashFlow(
    transactions: List<Transaction>
): ReportsCashFlow {

    /*
     * --------------------------------------------------------
     * Actual income
     * --------------------------------------------------------
     *
     * A transaction allocated to a Financial Event is NOT
     * ordinary income.
     *
     * Reimbursements are also NOT income.
     *
     * Transfers are NOT income.
     */
    val actualIncome =
        transactions
            .asSequence()
            .filter { transaction ->

                transaction.type ==
                    TransactionType.INCOME &&

                    transaction.role ==
                    TransactionRole.NORMAL &&

                    transaction.transactionLinkId == null &&

                    transaction.transferLinkId == null
            }
            .sumOf {
                it.amount
            }

    /*
     * --------------------------------------------------------
     * Ordinary expenses
     * --------------------------------------------------------
     *
     * Linked Financial Event transactions are excluded.
     *
     * Financial Event expenses are represented separately
     * through their effective event cost.
     */
    val ordinaryExpense =
        transactions
            .asSequence()
            .filter { transaction ->

                transaction.type ==
                    TransactionType.EXPENSE &&

                    transaction.role !=
                    TransactionRole.TRANSFER_OUT &&

                    transaction.transactionLinkId == null &&

                    transaction.transferLinkId == null
            }
            .sumOf {
                it.amount
            }

    return ReportsCashFlow(
        actualIncome =
            actualIncome,

        effectiveExpense =
            ordinaryExpense,

        netCashFlow =
            actualIncome -
                ordinaryExpense
    )
}

    // ------------------------------------------------------------------------
    // Expense Categories
    // ------------------------------------------------------------------------

private fun buildExpenseCategories(
    transactions: List<Transaction>,
    groups: List<TransactionLinkGroup>,
    allocations: List<FinancialEventAllocationEntity>
): List<ReportsExpenseCategory> {

    /*
     * --------------------------------------------------------
     * Allocation lookup
     * --------------------------------------------------------
     *
     * A transaction may now be allocated to multiple
     * Financial Events.
     *
     * Therefore we NEVER use transactionLinkId to determine
     * how much of a transaction remains available for normal
     * reporting.
     */
    val allocatedByTransactionId =
        allocations
            .groupBy {
                it.transactionId
            }
            .mapValues { (_, rows) ->
                rows.sumOf {
                    it.allocatedAmount
                }
            }

    /*
     * --------------------------------------------------------
     * Ordinary expense categories
     * --------------------------------------------------------
     *
     * Only the unallocated remainder remains in the original
     * transaction category.
     *
     * Example:
     *
     * Transaction = ₹1,000
     * Event A     = ₹600
     * Event B     = ₹400
     *
     * Ordinary category = ₹0
     */
    val normalAmountsByCategory =
        transactions
            .asSequence()
            .filter { transaction ->

                transaction.type ==
                    TransactionType.EXPENSE &&

                    transaction.role ==
                    TransactionRole.NORMAL &&

                    transaction.transferLinkId == null
            }
            .mapNotNull { transaction ->

                val allocatedAmount =
                    allocatedByTransactionId[
                        transaction.id
                    ] ?: 0.0

                val remainingAmount =
                    (
                        transaction.amount -
                            allocatedAmount
                        )
                        .coerceAtLeast(0.0)

                if (remainingAmount <= 0.0) {
                    null
                } else {
                    transaction.category to
                        remainingAmount
                }
            }
            .groupBy(
                keySelector = {
                    it.first
                },
                valueTransform = {
                    it.second
                }
            )
            .mapValues { (_, amounts) ->
                amounts.sum()
            }

    /*
     * --------------------------------------------------------
     * Financial Event category amounts
     * --------------------------------------------------------
     *
     * IMPORTANT:
     *
     * We calculate each event through the END of the
     * currently selected month.
     *
     * This is intentionally different from the ordinary
     * transaction list, which contains only the selected
     * reporting period.
     *
     * Therefore a June reimbursement remains relevant when
     * July is selected.
     */
    val eventAmountsByCategory =
        mutableMapOf<String, Double>()

    val eventGrossByCategory =
        mutableMapOf<String, Double>()

    val eventReimbursementByCategory =
        mutableMapOf<String, Double>()

    /*
     * All transactions available to the report calculation.
     *
     * `latestTransactions` contains the complete transaction
     * stream observed by ReportsViewModel.
     *
     * We use it here so Financial Events can be calculated
     * cumulatively through month-end.
     */
    val allTransactions =
        latestTransactions

    val selectedMonth =
        _uiState.value.selectedMonth

    groups.forEach { group ->

        val eventAllocations =
            allocations.filter {
                it.transactionLinkId ==
                    group.transactionLinkId
            }

        if (eventAllocations.isEmpty()) {
            return@forEach
        }

        /*
         * transactionId -> amount allocated to THIS event
         *
         * This is important for the future multi-event case.
         */
        val allocationByTransactionId =
            eventAllocations
                .groupBy {
                    it.transactionId
                }
                .mapValues { (_, rows) ->
                    rows.sumOf {
                        it.allocatedAmount
                    }
                }

        /*
         * All transactions belonging to this event.
         *
         * We deliberately use allTransactions instead of the
         * currently filtered transaction list.
         */
        val eventTransactions =
            allTransactions
                .filter { transaction ->

                    transaction.id in
                        allocationByTransactionId.keys
                }

        /*
         * Only transactions occurring on or before the end
         * of the selected month contribute to the event's
         * effective cost.
         */
        val transactionsThroughMonthEnd =
            eventTransactions.filter { transaction ->

                transactionYearMonth(
                    transaction.dateTimestamp
                ) <= selectedMonth
            }

        /*
         * ----------------------------------------------------
         * Cumulative Financial Event expenses
         * ----------------------------------------------------
         */
        val cumulativeExpense =
            transactionsThroughMonthEnd
                .asSequence()
                .filter { transaction ->

                    transaction.type ==
                        TransactionType.EXPENSE &&

                        transaction.role !=
                        TransactionRole.TRANSFER_OUT
                }
                .sumOf { transaction ->

                    allocationByTransactionId[
                        transaction.id
                    ] ?: 0.0
                }

        /*
         * ----------------------------------------------------
         * Cumulative Financial Event reimbursements
         * ----------------------------------------------------
         */
        val cumulativeReimbursement =
            transactionsThroughMonthEnd
                .asSequence()
                .filter { transaction ->

                    transaction.role ==
                        TransactionRole.REIMBURSEMENT
                }
                .sumOf { transaction ->

                    allocationByTransactionId[
                        transaction.id
                    ] ?: 0.0
                }

        /*
         * ----------------------------------------------------
         * Effective Financial Event cost
         * ----------------------------------------------------
         */
        val effectiveCost =
            cumulativeExpense -
                cumulativeReimbursement

        val category =
            group.category.trim()

        if (category.isBlank()) {
            return@forEach
        }

        /*
         * Keep these values available for the existing
         * ReportsExpenseCategory model / UI.
         */
        eventGrossByCategory[category] =
            (
                eventGrossByCategory[category]
                    ?: 0.0
                ) + cumulativeExpense

        eventReimbursementByCategory[category] =
            (
                eventReimbursementByCategory[category]
                    ?: 0.0
                ) + cumulativeReimbursement

        /*
         * A Financial Event with a positive effective cost
         * contributes to expenses.
         *
         * If reimbursements exceed expenses, it must NOT
         * create a negative expense category.
         */
        if (effectiveCost > 0.0) {

            eventAmountsByCategory[category] =
                (
                    eventAmountsByCategory[category]
                        ?: 0.0
                    ) + effectiveCost
        }
    }

    /*
     * --------------------------------------------------------
     * Combine ordinary categories + Financial Event category
     * --------------------------------------------------------
     */
    val allCategories =
        (
            normalAmountsByCategory.keys +
                eventAmountsByCategory.keys
            )
            .filter {
                it.isNotBlank()
            }
            .distinct()

    return allCategories
        .map { category ->

            val normalAmount =
                normalAmountsByCategory[
                    category
                ] ?: 0.0

            val eventAmount =
                eventAmountsByCategory[
                    category
                ] ?: 0.0

            val reimbursementAmount =
                eventReimbursementByCategory[
                    category
                ] ?: 0.0

            ReportsExpenseCategory(

                category =
                    category,

                totalAmount =
                    normalAmount +
                        eventAmount,

                normalAmount =
                    normalAmount,

                financialEventAmount =
                    eventAmount,

                reimbursedAmount =
                    reimbursementAmount,

                effectiveFinancialEventAmount =
                    eventAmount
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

    /**
     * Builds actual income categories.
     *
     * Financial Event reimbursements are deliberately excluded.
     *
     * Account transfers are deliberately excluded.
     *
     * This means the Income chart represents genuine income,
     * not money received to recover a Financial Event expense
     * and not money moved between the user's own accounts.
     */
private fun buildIncomeCategories(
    transactions: List<Transaction>
): List<ReportsIncomeCategory> {

    return transactions
        .asSequence()
        .filter { transaction ->

            transaction.type ==
                TransactionType.INCOME &&

                transaction.role ==
                TransactionRole.NORMAL &&

                transaction.transactionLinkId == null &&

                transaction.transferLinkId == null
        }
        .filter {
            it.category.isNotBlank()
        }
        .groupBy {
            it.category
        }
        .map { (category, categoryTransactions) ->

            ReportsIncomeCategory(

                category =
                    category,

                totalAmount =
                    categoryTransactions.sumOf {
                        it.amount
                    }
            )
        }
        .sortedByDescending {
            it.totalAmount
        }
}

/**
 * Builds Financial Event summaries.
 *
 * The transaction list is already filtered by period and
 * account before reaching this function.
 *
 * IMPORTANT:
 *
 * The financial amounts shown in the report remain restricted
 * to the selected reporting period.
 *
 * The complete account-filtered transaction history is used
 * only to determine whether an event spans multiple months.
 */
 private fun buildFinancialEvents(
    transactions: List<Transaction>,
    allAccountTransactions: List<Transaction>,
    groups: List<TransactionLinkGroup>,
    allocations: List<FinancialEventAllocationEntity>
): List<ReportsFinancialEvent> {

    /*
     * --------------------------------------------------------
     * Account-filtered historical allocation data
     * --------------------------------------------------------
     */
    val allocationsByEvent =
        allocations
            .groupBy {
                it.transactionLinkId
            }

    /*
     * Only show Financial Events that have at least one
     * relevant transaction in the selected period.
     */
    val selectedPeriodTransactionIds =
        transactions
            .map {
                it.id
            }
            .toSet()

    return groups
        .mapNotNull { group ->

            val eventAllocations =
                allocationsByEvent[
                    group.transactionLinkId
                ]
                    .orEmpty()

            if (eventAllocations.isEmpty()) {
                return@mapNotNull null
            }

            /*
             * ------------------------------------------------
             * All transactions belonging to this event.
             * ------------------------------------------------
             */
            val allocationByTransactionId =
                eventAllocations
                    .groupBy {
                        it.transactionId
                    }
                    .mapValues { (_, rows) ->
                        rows.sumOf {
                            it.allocatedAmount
                        }
                    }

            val eventTransactions =
                allAccountTransactions
                    .filter { transaction ->

                        transaction.id in
                            allocationByTransactionId
                                .keys
                    }

            /*
             * ------------------------------------------------
             * Determine whether this event is visible for
             * the selected reporting month.
             * ------------------------------------------------
             *
             * We show the event if at least one linked
             * transaction occurs in the selected month.
             */
            val visibleInSelectedPeriod =
                eventTransactions.any {
                    it.id in
                        selectedPeriodTransactionIds
                }

            if (!visibleInSelectedPeriod) {
                return@mapNotNull null
            }

            /*
             * ------------------------------------------------
             * Cumulative month-end calculation
             * ------------------------------------------------
             *
             * This is the key rule:
             *
             * selected month = July
             *
             * include ALL linked transactions whose date is
             * <= July 31.
             *
             * Therefore:
             *
             * June reimbursement ₹1,465
             * July expenses      ₹26,950.45
             * July reimbursements ₹24,384
             *
             * are all included.
             *
             * Effective cost =
             *
             * cumulative expenses
             * -
             * cumulative reimbursements
             */
            val selectedMonth =
                _uiState.value.selectedMonth

            val transactionsThroughMonthEnd =
                eventTransactions.filter { transaction ->

                    transactionYearMonth(
                        transaction.dateTimestamp
                    ) <= selectedMonth
                }

            val cumulativeExpense =
                transactionsThroughMonthEnd
                    .asSequence()
                    .filter { transaction ->

                        transaction.type ==
                            TransactionType.EXPENSE &&

                            transaction.role !=
                            TransactionRole.TRANSFER_OUT
                    }
                    .sumOf { transaction ->

                        allocationByTransactionId[
                            transaction.id
                        ] ?: 0.0
                    }

            val cumulativeReimbursement =
                transactionsThroughMonthEnd
                    .asSequence()
                    .filter { transaction ->

                        transaction.role ==
                            TransactionRole.REIMBURSEMENT
                    }
                    .sumOf { transaction ->

                        allocationByTransactionId[
                            transaction.id
                        ] ?: 0.0
                    }

            val effectiveCost =
                cumulativeExpense -
                    cumulativeReimbursement

            /*
             * ------------------------------------------------
             * Selected-period display amounts
             * ------------------------------------------------
             *
             * These remain useful for the Financial Event
             * card's transaction/month display.
             */
            val selectedPeriodTransactions =
                eventTransactions.filter {
                    it.id in
                        selectedPeriodTransactionIds
                }

            val selectedPeriodExpense =
                selectedPeriodTransactions
                    .asSequence()
                    .filter { transaction ->

                        transaction.type ==
                            TransactionType.EXPENSE &&

                            transaction.role !=
                            TransactionRole.TRANSFER_OUT
                    }
                    .sumOf { transaction ->

                        allocationByTransactionId[
                            transaction.id
                        ] ?: 0.0
                    }

            val selectedPeriodReimbursement =
                selectedPeriodTransactions
                    .asSequence()
                    .filter { transaction ->

                        transaction.role ==
                            TransactionRole.REIMBURSEMENT
                    }
                    .sumOf { transaction ->

                        allocationByTransactionId[
                            transaction.id
                        ] ?: 0.0
                    }

            /*
             * ------------------------------------------------
             * Multi-month detection
             * ------------------------------------------------
             */
            val coveredMonths =
                eventTransactions
                    .asSequence()
                    .filter { transaction ->

                        transaction.type ==
                            TransactionType.EXPENSE ||

                            transaction.role ==
                            TransactionRole.REIMBURSEMENT
                    }
                    .map {
                        transactionYearMonth(
                            it.dateTimestamp
                        )
                    }
                    .distinct()
                    .sorted()
                    .toList()

            ReportsFinancialEvent(

                transactionLinkId =
                    group.transactionLinkId,

                groupName =
                    group.groupName,

                category =
                    group.category,

                /*
                 * Keep these as the selected month's amounts.
                 */
                expenseAmount =
                    selectedPeriodExpense,

                reimbursedAmount =
                    selectedPeriodReimbursement,

                /*
                 * IMPORTANT:
                 *
                 * This is now the cumulative month-end
                 * effective cost.
                 */
                effectiveCost =
                    effectiveCost,

                coveredMonths =
                    coveredMonths
            )
        }
        .filter {
            it.expenseAmount != 0.0 ||
                it.reimbursedAmount != 0.0 ||
                it.effectiveCost != 0.0
        }
        .sortedByDescending {
            kotlin.math.abs(
                it.effectiveCost
            )
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
