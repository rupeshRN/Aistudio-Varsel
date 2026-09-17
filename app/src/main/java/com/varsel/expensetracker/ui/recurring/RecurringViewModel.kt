package com.varsel.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.domain.engine.RecurringScheduleEngine
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.recurring.RecurringFrequency
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.domain.model.recurring.RecurringType
import com.varsel.expensetracker.domain.repository.RecurringRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.domain.usecase.ProcessRecurringTransactionUseCase
import com.varsel.expensetracker.ui.recurring.model.CategorySpendBreakdown
import com.varsel.expensetracker.ui.recurring.model.PriceChangeAlert
import com.varsel.expensetracker.ui.recurring.model.RecurringFilterTab
import com.varsel.expensetracker.ui.recurring.model.RecurringItemUiModel
import com.varsel.expensetracker.ui.recurring.model.RecurringPeriod
import com.varsel.expensetracker.ui.recurring.model.RecurringSummaryModel
import com.varsel.expensetracker.ui.recurring.model.RecurringUiState
import com.varsel.expensetracker.ui.recurring.model.RenewingSubscriptionItem
import com.varsel.expensetracker.ui.recurring.model.SubscriptionInsights
import com.varsel.expensetracker.ui.recurring.model.SubscriptionReviewItem
import com.varsel.expensetracker.ui.transaction.model.AccountOption
import com.varsel.expensetracker.util.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val categoryDao: CategoryDao,
    private val statementSnapshotRepository: StatementSnapshotRepository,
    private val transactionRepository: TransactionRepository,
    private val scheduleEngine: RecurringScheduleEngine,
    private val processRecurringTransactionUseCase: ProcessRecurringTransactionUseCase
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(RecurringFilterTab.ALL)
    private val _selectedPeriod = MutableStateFlow(RecurringPeriod.MONTHLY)
    private val _isRefreshing = MutableStateFlow(false)
    private val _editingItem = MutableStateFlow<RecurringItem?>(null)
    private val _isAddEditSheetOpen = MutableStateFlow(false)
    private val _isInsightsSheetOpen = MutableStateFlow(false)
    private val _isReviewSheetOpen = MutableStateFlow(false)
    private val _variableAmountItem = MutableStateFlow<RecurringItem?>(null)
    private val _variableItemPastPayments = MutableStateFlow<List<Double>>(emptyList())
    private val _catchUpItem = MutableStateFlow<RecurringItem?>(null)
    private val _catchUpMissedDates = MutableStateFlow<List<Long>>(emptyList())
    private val _userMessage = MutableStateFlow<String?>(null)

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val zoneId = ZoneId.systemDefault()

    val availableAccounts: StateFlow<List<AccountOption>> = statementSnapshotRepository.observeAllSnapshots()
        .map { snapshots ->
            val cashOption = AccountOption(
                accountId = null,
                accountLast4 = null,
                bankName = "Cash",
                displayName = "Cash Wallet"
            )
            val snapshotOptions = snapshots.mapNotNull { snap ->
                val last4 = snap.accountLast4 ?: "Unknown"
                val bank = snap.bankName ?: "Account"
                val id = snap.accountId?.takeIf { it.isNotBlank() } ?: "${snap.bankName}_${snap.accountLast4}"
                AccountOption(
                    accountId = snap.accountId ?: id,
                    accountLast4 = snap.accountLast4,
                    bankName = snap.bankName,
                    displayName = "$bank (•••• $last4)"
                )
            }.distinctBy {
                it.accountId?.takeIf { id -> id.isNotBlank() } ?: "${it.bankName}_${it.accountLast4}"
            }
            listOf(cashOption) + snapshotOptions
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class ViewModelInternalState(
        val selectedTab: RecurringFilterTab,
        val selectedPeriod: RecurringPeriod,
        val isRefreshing: Boolean,
        val isAddEditSheetOpen: Boolean,
        val isInsightsSheetOpen: Boolean,
        val isReviewSheetOpen: Boolean,
        val editingItem: RecurringItem?,
        val variableAmountItem: RecurringItem?,
        val variableItemPastPayments: List<Double>,
        val catchUpItem: RecurringItem?,
        val catchUpMissedDates: List<Long>,
        val userMessage: String?
    )

    private val internalState = combine(
        combine(
            _selectedTab,
            _selectedPeriod,
            _isRefreshing,
            _isAddEditSheetOpen,
            _isInsightsSheetOpen
        ) { tab, period, refreshing, isAddEdit, isInsights ->
            Triple(tab, period, refreshing) to (isAddEdit to isInsights)
        },
        combine(
            _isReviewSheetOpen,
            _editingItem,
            _variableAmountItem,
            _variableItemPastPayments,
            _catchUpItem
        ) { isReview, editItem, varItem, pastPayments, catchUp ->
            Triple(isReview, editItem, varItem) to (pastPayments to catchUp)
        },
        _catchUpMissedDates,
        _userMessage
    ) { (t1, p1), (t2, p2), missedDates, msg ->
        val (tab, period, refreshing) = t1
        val (isAddEdit, isInsights) = p1
        val (isReview, editItem, varItem) = t2
        val (pastPayments, catchUp) = p2
        ViewModelInternalState(
            selectedTab = tab,
            selectedPeriod = period,
            isRefreshing = refreshing,
            isAddEditSheetOpen = isAddEdit,
            isInsightsSheetOpen = isInsights,
            isReviewSheetOpen = isReview,
            editingItem = editItem,
            variableAmountItem = varItem,
            variableItemPastPayments = pastPayments,
            catchUpItem = catchUp,
            catchUpMissedDates = missedDates,
            userMessage = msg
        )
    }

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRepository.getAllRecurringItems(),
        categoryDao.getAllCategories(),
        availableAccounts,
        transactionRepository.getAllTransactions(),
        internalState
    ) { rawItems, categories, accounts, transactions, state ->
        val tab = state.selectedTab
        val period = state.selectedPeriod
        val categoryMap = categories.associateBy { it.name.lowercase() }

        val uiModels = rawItems.map { item ->
            val cat = categoryMap[item.category.lowercase()]
            val formattedDate = Instant.ofEpochMilli(item.nextOccurrenceTimestamp)
                .atZone(zoneId)
                .format(dateFormatter)

            val accountName = when {
                item.bankName != null && item.accountLast4 != null -> "${item.bankName} (•••• ${item.accountLast4})"
                item.bankName != null -> item.bankName
                item.accountLast4 != null -> "Account (•••• ${item.accountLast4})"
                else -> "Cash Wallet"
            }

            RecurringItemUiModel(
                item = item,
                formattedAmount = CurrencyFormatter.format(item.amount),
                scheduleText = scheduleEngine.formatFrequencySchedule(item.frequency, item.startDateTimestamp),
                nextOccurrenceFormatted = formattedDate,
                daysUntilNext = scheduleEngine.getDaysUntilNext(item),
                isDue = scheduleEngine.isDue(item),
                categoryIcon = cat?.iconName ?: "ic_help",
                categoryColorHex = cat?.colorHex ?: "#9E9E9E",
                accountDisplayName = accountName
            )
        }

        val filtered = when (tab) {
            RecurringFilterTab.ALL -> uiModels
            RecurringFilterTab.UPCOMING -> uiModels.filter { it.item.isActive }.sortedBy { it.item.nextOccurrenceTimestamp }
            RecurringFilterTab.SUBSCRIPTIONS -> uiModels.filter { it.item.type == RecurringType.SUBSCRIPTION }
            RecurringFilterTab.EXPENSES -> uiModels.filter { it.item.type == RecurringType.EXPENSE }
            RecurringFilterTab.INCOME -> uiModels.filter { it.item.type == RecurringType.INCOME }
        }

        val upcoming = uiModels.filter { it.item.isActive }.sortedBy { it.item.nextOccurrenceTimestamp }

        val today = LocalDate.now(zoneId)
        val currentYear = today.year
        val currentMonth = today.monthValue
        val currentMonthName = today.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        val dayOfMonth = today.dayOfMonth
        val daysInMonth = today.lengthOfMonth()
        val fractionOfMonth = dayOfMonth.toDouble() / daysInMonth.toDouble()
        val elapsedMonthsFraction = (currentMonth - 1) + fractionOfMonth

        val startOfYearMillis = LocalDate.of(currentYear, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfYearMillis = LocalDate.of(currentYear, 12, 31).atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()

        val yearTransactions = transactions.filter { it.dateTimestamp in startOfYearMillis..endOfYearMillis }

        var monthlyExp = 0.0
        var monthlyInc = 0.0
        var monthlySub = 0.0

        var expectedBillsMonthly = 0.0
        var expectedSubsMonthly = 0.0
        var expectedVarMonthly = 0.0
        var expectedIncMonthly = 0.0
        var totalOccurrencesInMonth = 0

        var yearlyExp = 0.0
        var yearlyInc = 0.0
        var yearlySub = 0.0

        var ytdExp = 0.0
        var ytdInc = 0.0
        var ytdSub = 0.0

        var activeSubCount = 0
        var activeRecCount = 0
        var dueCount = 0
        var overdueCount = 0

        val reviewItemsList = mutableListOf<SubscriptionReviewItem>()

        rawItems.filter { it.isActive }.forEach { item ->
            // Accurate monthly estimate based on current month occurrences
            val monthOccurrences = scheduleEngine.countOccurrencesInMonth(item, currentYear, currentMonth)
            totalOccurrencesInMonth += monthOccurrences
            val monthAmountAccurate = item.amount * monthOccurrences

            val monthlyMultiplier = when (item.frequency) {
                RecurringFrequency.DAILY -> 30.4
                RecurringFrequency.WEEKLY -> 4.33
                RecurringFrequency.MONTHLY -> 1.0
                RecurringFrequency.QUARTERLY -> 1.0 / 3.0
                RecurringFrequency.SEMI_ANNUALLY -> 1.0 / 6.0
                RecurringFrequency.YEARLY -> 1.0 / 12.0
            }
            val monthlyBaselineAmount = item.amount * monthlyMultiplier
            val yearlyAmount = monthlyBaselineAmount * 12.0

            // Category and type breakdowns
            when (item.type) {
                RecurringType.EXPENSE -> {
                    monthlyExp += monthAmountAccurate
                    yearlyExp += yearlyAmount
                    if (item.isVariableAmount) {
                        expectedVarMonthly += monthAmountAccurate
                    } else {
                        expectedBillsMonthly += monthAmountAccurate
                    }
                    activeRecCount++
                }
                RecurringType.INCOME -> {
                    monthlyInc += monthAmountAccurate
                    yearlyInc += yearlyAmount
                    expectedIncMonthly += monthAmountAccurate
                    activeRecCount++
                }
                RecurringType.SUBSCRIPTION -> {
                    monthlySub += monthAmountAccurate
                    monthlyExp += monthAmountAccurate
                    yearlySub += yearlyAmount
                    yearlyExp += yearlyAmount
                    if (item.isVariableAmount) {
                        expectedVarMonthly += monthAmountAccurate
                    } else {
                        expectedSubsMonthly += monthAmountAccurate
                    }
                    activeSubCount++
                    activeRecCount++
                }
            }

            // Elapsed YTD commitment calculation
            val itemStartLocalDate = Instant.ofEpochMilli(item.startDateTimestamp).atZone(zoneId).toLocalDate()
            val itemElapsedMonths = when {
                itemStartLocalDate.year < currentYear -> elapsedMonthsFraction
                itemStartLocalDate.year == currentYear -> {
                    val startFraction = (itemStartLocalDate.monthValue - 1) + (itemStartLocalDate.dayOfMonth.toDouble() / itemStartLocalDate.lengthOfMonth().toDouble())
                    (elapsedMonthsFraction - startFraction).coerceAtLeast(0.0)
                }
                else -> 0.0
            }
            val projectedItemYtd = monthlyBaselineAmount * itemElapsedMonths

            // Linked actual transactions in current year
            val recordedItemTxs = yearTransactions.filter { it.recurringItemId == item.id }
            val recordedItemYtd = recordedItemTxs.sumOf { it.amount }
            val itemYtd = maxOf(recordedItemYtd, projectedItemYtd)

            when (item.type) {
                RecurringType.EXPENSE -> ytdExp += itemYtd
                RecurringType.INCOME -> ytdInc += itemYtd
                RecurringType.SUBSCRIPTION -> {
                    ytdSub += itemYtd
                    ytdExp += itemYtd
                }
            }

            if (scheduleEngine.isDue(item)) {
                dueCount++
            }

            // Check overdue missed occurrences
            val missed = scheduleEngine.getMissedOccurrences(item)
            if (missed.isNotEmpty()) {
                overdueCount++
                val oldestFormatted = Instant.ofEpochMilli(missed.first()).atZone(zoneId).format(dateFormatter)
                reviewItemsList.add(
                    SubscriptionReviewItem.OverdueMissedCycles(
                        item = item,
                        missedCount = missed.size,
                        missedTimestamps = missed,
                        oldestMissedDateFormatted = oldestFormatted
                    )
                )
            }

            // Audit: Check for price hike from latest recorded transaction
            val itemTransactions = transactions.filter { it.recurringItemId == item.id }.sortedByDescending { it.dateTimestamp }
            if (itemTransactions.isNotEmpty()) {
                val latestTx = itemTransactions.first()
                if (latestTx.amount > item.amount + 0.01) {
                    reviewItemsList.add(
                        SubscriptionReviewItem.PriceHike(
                            item = item,
                            oldAmount = item.amount,
                            newAmount = latestTx.amount,
                            diff = latestTx.amount - item.amount
                        )
                    )
                }
            } else if (item.lastGeneratedTimestamp == null && item.startDateTimestamp < System.currentTimeMillis() - 60L * 86400000L) {
                reviewItemsList.add(
                    SubscriptionReviewItem.NoRecentConfirmation(
                        item = item,
                        daysSinceLastConfirmation = (System.currentTimeMillis() - item.startDateTimestamp) / 86400000L,
                        message = "No payments recorded in the last 60+ days for this active commitment."
                    )
                )
            }

            // Audit: Check missing account link
            if (item.accountId == null && item.accountLast4 == null && item.bankName == null) {
                reviewItemsList.add(
                    SubscriptionReviewItem.MissingAccountLink(
                        item = item,
                        message = "No payment source account configured for this item."
                    )
                )
            }
        }

        // Also check inactive items in review
        rawItems.filter { !it.isActive }.forEach { inactiveItem ->
            reviewItemsList.add(SubscriptionReviewItem.InactiveItem(inactiveItem))
        }

        // Subscription Insights Calculation
        val activeSubscriptions = rawItems.filter { it.isActive && it.type == RecurringType.SUBSCRIPTION }
        val mostExpensiveSub = activeSubscriptions.maxByOrNull { it.amount }

        val renewingList = activeSubscriptions.mapNotNull { sub ->
            val days = scheduleEngine.getDaysUntilNext(sub)
            if (days in 0..30) {
                val dateStr = Instant.ofEpochMilli(sub.nextOccurrenceTimestamp).atZone(zoneId).format(dateFormatter)
                RenewingSubscriptionItem(
                    item = sub,
                    daysUntilRenewal = days,
                    renewalDateFormatted = dateStr,
                    formattedAmount = CurrencyFormatter.format(sub.amount)
                )
            } else null
        }.sortedBy { it.daysUntilRenewal }

        val totalSubSpendMonthly = activeSubscriptions.sumOf {
            val mult = when (it.frequency) {
                RecurringFrequency.DAILY -> 30.4
                RecurringFrequency.WEEKLY -> 4.33
                RecurringFrequency.MONTHLY -> 1.0
                RecurringFrequency.QUARTERLY -> 1.0 / 3.0
                RecurringFrequency.SEMI_ANNUALLY -> 1.0 / 6.0
                RecurringFrequency.YEARLY -> 1.0 / 12.0
            }
            it.amount * mult
        }

        val categorySpending = activeSubscriptions.groupBy { it.category.ifBlank { "Other" } }
            .map { (catName, itemsInCat) ->
                val catMonthly = itemsInCat.sumOf {
                    val mult = when (it.frequency) {
                        RecurringFrequency.DAILY -> 30.4
                        RecurringFrequency.WEEKLY -> 4.33
                        RecurringFrequency.MONTHLY -> 1.0
                        RecurringFrequency.QUARTERLY -> 1.0 / 3.0
                        RecurringFrequency.SEMI_ANNUALLY -> 1.0 / 6.0
                        RecurringFrequency.YEARLY -> 1.0 / 12.0
                    }
                    it.amount * mult
                }
                val pct = if (totalSubSpendMonthly > 0) (catMonthly / totalSubSpendMonthly).toFloat() else 0f
                CategorySpendBreakdown(
                    categoryName = catName,
                    monthlyAmount = catMonthly,
                    percentage = pct
                )
            }.sortedByDescending { it.monthlyAmount }

        val insights = SubscriptionInsights(
            totalMonthlySubscriptionCost = totalSubSpendMonthly,
            totalYearlySubscriptionCost = totalSubSpendMonthly * 12.0,
            activeSubscriptionsCount = activeSubscriptions.size,
            mostExpensiveSubscription = mostExpensiveSub,
            renewingInNext30Days = renewingList,
            spendingByCategory = categorySpending
        )

        RecurringUiState(
            isLoading = false,
            isRefreshing = state.isRefreshing,
            selectedTab = tab,
            selectedPeriod = period,
            allItems = uiModels,
            filteredItems = filtered,
            upcomingItems = upcoming,
            summary = RecurringSummaryModel(
                totalActiveMonthlyExpense = monthlyExp,
                totalActiveMonthlyIncome = monthlyInc,
                totalActiveMonthlySubscriptions = monthlySub,
                expectedBillsMonthly = expectedBillsMonthly,
                expectedSubscriptionsMonthly = expectedSubsMonthly,
                expectedVariableMonthly = expectedVarMonthly,
                expectedIncomeMonthly = expectedIncMonthly,
                estimatedMonthlyCommitment = expectedBillsMonthly + expectedSubsMonthly + expectedVarMonthly,
                occurrencesInMonthCount = totalOccurrencesInMonth,
                totalActiveYearlyExpense = yearlyExp,
                totalActiveYearlyIncome = yearlyInc,
                totalActiveYearlySubscriptions = yearlySub,
                totalYtdExpense = ytdExp,
                totalYtdIncome = ytdInc,
                totalYtdSubscriptions = ytdSub,
                elapsedMonthsCount = currentMonth,
                currentYear = currentYear,
                currentMonthName = currentMonthName,
                activeSubscriptionsCount = activeSubCount,
                activeRecurringCount = activeRecCount,
                dueItemsCount = dueCount,
                overdueItemsCount = overdueCount
            ),
            insights = insights,
            reviewItems = reviewItemsList,
            availableAccounts = accounts,
            availableCategories = categories,
            editingItem = state.editingItem,
            isAddEditSheetOpen = state.isAddEditSheetOpen,
            isInsightsSheetOpen = state.isInsightsSheetOpen,
            isReviewSheetOpen = state.isReviewSheetOpen,
            variableAmountItem = state.variableAmountItem,
            variableItemPastPayments = state.variableItemPastPayments,
            catchUpItem = state.catchUpItem,
            catchUpMissedDates = state.catchUpMissedDates,
            userMessage = state.userMessage
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RecurringUiState(isLoading = true)
    )

    fun setFilterTab(tab: RecurringFilterTab) {
        _selectedTab.value = tab
    }

    fun setSummaryPeriod(period: RecurringPeriod) {
        _selectedPeriod.value = period
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(350)
            _isRefreshing.value = false
            _userMessage.value = "Recurring schedules & statuses refreshed"
        }
    }

    fun openAddSheet() {
        _editingItem.value = null
        _isAddEditSheetOpen.value = true
    }

    fun openEditSheet(item: RecurringItem) {
        _editingItem.value = item
        _isAddEditSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        _isAddEditSheetOpen.value = false
        _editingItem.value = null
    }

    fun saveRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            if (item.id == 0L) {
                recurringRepository.insertRecurringItem(item)
                _userMessage.value = "Added \"${item.title}\""
            } else {
                recurringRepository.updateRecurringItem(item.copy(updatedAt = System.currentTimeMillis()))
                _userMessage.value = "Updated \"${item.title}\""
            }
            closeAddEditSheet()
        }
    }

    fun deleteRecurringItem(id: Long) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringItemById(id)
            _userMessage.value = "Deleted recurring item"
        }
    }

    fun toggleActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            recurringRepository.setRecurringItemActive(id, isActive)
        }
    }

    fun processOccurrence(item: RecurringItem) {
        if (item.isVariableAmount) {
            openVariableAmountSheet(item)
            return
        }
        viewModelScope.launch {
            val result = processRecurringTransactionUseCase.processOccurrence(item)
            if (result.isSuccess) {
                _userMessage.value = "Recorded transaction for \"${item.title}\""
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to record transaction"
            }
        }
    }

    fun openVariableAmountSheet(item: RecurringItem) {
        viewModelScope.launch {
            val pastTxs = transactionRepository.getTransactionsByRecurringItemId(item.id)
            val pastAmounts = pastTxs.map { it.amount }.distinct().take(4)
            _variableItemPastPayments.value = pastAmounts
            _variableAmountItem.value = item
        }
    }

    fun closeVariableAmountSheet() {
        _variableAmountItem.value = null
        _variableItemPastPayments.value = emptyList()
    }

    fun recordVariableOccurrence(item: RecurringItem, actualAmount: Double, updateBaseline: Boolean) {
        viewModelScope.launch {
            val result = processRecurringTransactionUseCase.processOccurrence(
                item = item,
                actualAmount = actualAmount,
                updateBaselineAmount = updateBaseline
            )
            if (result.isSuccess) {
                _userMessage.value = "Recorded ${CurrencyFormatter.format(actualAmount)} for \"${item.title}\""
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to record transaction"
            }
            closeVariableAmountSheet()
        }
    }

    fun openCatchUpSheet(item: RecurringItem) {
        val missed = scheduleEngine.getMissedOccurrences(item)
        _catchUpItem.value = item
        _catchUpMissedDates.value = missed
    }

    fun closeCatchUpSheet() {
        _catchUpItem.value = null
        _catchUpMissedDates.value = emptyList()
    }

    fun fastForwardSchedule(item: RecurringItem) {
        viewModelScope.launch {
            val result = processRecurringTransactionUseCase.fastForwardToFuture(item)
            if (result.isSuccess) {
                _userMessage.value = "Fast-forwarded \"${item.title}\" to current period"
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to fast-forward"
            }
            closeCatchUpSheet()
        }
    }

    fun recordCatchUpOccurrences(item: RecurringItem, dates: List<Long>) {
        viewModelScope.launch {
            var count = 0
            for (date in dates) {
                val res = processRecurringTransactionUseCase.processOccurrence(
                    item = item,
                    occurrenceTimestamp = date,
                    recordDateTimestamp = date
                )
                if (res.isSuccess) count++
            }
            // Ensure schedule is at least current
            processRecurringTransactionUseCase.fastForwardToFuture(item)
            _userMessage.value = "Recorded $count past transaction(s) for \"${item.title}\""
            closeCatchUpSheet()
        }
    }

    fun openInsightsSheet() {
        _isInsightsSheetOpen.value = true
    }

    fun closeInsightsSheet() {
        _isInsightsSheetOpen.value = false
    }

    fun openReviewSheet() {
        _isReviewSheetOpen.value = true
    }

    fun closeReviewSheet() {
        _isReviewSheetOpen.value = false
    }

    fun confirmPriceHike(item: RecurringItem, newAmount: Double) {
        viewModelScope.launch {
            val updated = item.copy(amount = newAmount, updatedAt = System.currentTimeMillis())
            recurringRepository.updateRecurringItem(updated)
            _userMessage.value = "Updated \"${item.title}\" expected amount to ${CurrencyFormatter.format(newAmount)}"
        }
    }

    fun skipOccurrence(item: RecurringItem) {
        viewModelScope.launch {
            val result = processRecurringTransactionUseCase.skipOccurrence(item)
            if (result.isSuccess) {
                _userMessage.value = "Skipped occurrence for \"${item.title}\""
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to skip occurrence"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
