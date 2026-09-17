package com.varsel.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.domain.engine.RecurringScheduleEngine
import com.varsel.expensetracker.domain.model.recurring.RecurringFrequency
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.domain.model.recurring.RecurringType
import com.varsel.expensetracker.domain.repository.RecurringRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.usecase.ProcessRecurringTransactionUseCase
import com.varsel.expensetracker.ui.recurring.model.RecurringFilterTab
import com.varsel.expensetracker.ui.recurring.model.RecurringItemUiModel
import com.varsel.expensetracker.ui.recurring.model.RecurringSummaryModel
import com.varsel.expensetracker.ui.recurring.model.RecurringUiState
import com.varsel.expensetracker.ui.transaction.model.AccountOption
import com.varsel.expensetracker.util.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val scheduleEngine: RecurringScheduleEngine,
    private val processRecurringTransactionUseCase: ProcessRecurringTransactionUseCase
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(RecurringFilterTab.ALL)
    private val _editingItem = MutableStateFlow<RecurringItem?>(null)
    private val _isAddEditSheetOpen = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val zoneId = ZoneId.systemDefault()

    val availableAccounts: StateFlow<List<AccountOption>> = statementSnapshotRepository.observeAllSnapshots()
        .combine(MutableStateFlow(Unit)) { snapshots, _ ->
            val cashOption = AccountOption(
                accountId = null,
                accountLast4 = null,
                bankName = "Cash",
                displayName = "Cash Wallet"
            )
            val snapshotOptions = snapshots.map {
                val last4 = it.accountLast4 ?: "Unknown"
                val bank = it.bankName ?: "Account"
                AccountOption(
                    accountId = it.accountId,
                    accountLast4 = it.accountLast4,
                    bankName = it.bankName,
                    displayName = "$bank (•••• $last4)"
                )
            }
            listOf(cashOption) + snapshotOptions
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<RecurringUiState> = combine(
        recurringRepository.getAllRecurringItems(),
        categoryDao.getAllCategories(),
        availableAccounts,
        _selectedTab,
        _isAddEditSheetOpen,
        _editingItem,
        _userMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val rawItems = args[0] as List<RecurringItem>
        @Suppress("UNCHECKED_CAST")
        val categories = args[1] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val accounts = args[2] as List<AccountOption>
        val tab = args[3] as RecurringFilterTab
        val isSheetOpen = args[4] as Boolean
        val editItem = args[5] as RecurringItem?
        val msg = args[6] as String?

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

        // Monthly calculations
        var monthlyExp = 0.0
        var monthlyInc = 0.0
        var monthlySub = 0.0
        var activeSubCount = 0
        var activeRecCount = 0
        var dueCount = 0

        rawItems.filter { it.isActive }.forEach { item ->
            val monthlyMultiplier = when (item.frequency) {
                RecurringFrequency.DAILY -> 30.4
                RecurringFrequency.WEEKLY -> 4.33
                RecurringFrequency.MONTHLY -> 1.0
                RecurringFrequency.YEARLY -> 1.0 / 12.0
            }
            val monthlyAmount = item.amount * monthlyMultiplier

            when (item.type) {
                RecurringType.EXPENSE -> {
                    monthlyExp += monthlyAmount
                    activeRecCount++
                }
                RecurringType.INCOME -> {
                    monthlyInc += monthlyAmount
                    activeRecCount++
                }
                RecurringType.SUBSCRIPTION -> {
                    monthlySub += monthlyAmount
                    monthlyExp += monthlyAmount
                    activeSubCount++
                    activeRecCount++
                }
            }

            if (scheduleEngine.isDue(item)) {
                dueCount++
            }
        }

        RecurringUiState(
            isLoading = false,
            selectedTab = tab,
            allItems = uiModels,
            filteredItems = filtered,
            upcomingItems = upcoming,
            summary = RecurringSummaryModel(
                totalActiveMonthlyExpense = monthlyExp,
                totalActiveMonthlyIncome = monthlyInc,
                totalActiveMonthlySubscriptions = monthlySub,
                activeSubscriptionsCount = activeSubCount,
                activeRecurringCount = activeRecCount,
                dueItemsCount = dueCount
            ),
            availableAccounts = accounts,
            availableCategories = categories,
            editingItem = editItem,
            isAddEditSheetOpen = isSheetOpen,
            userMessage = msg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RecurringUiState(isLoading = true)
    )

    fun setFilterTab(tab: RecurringFilterTab) {
        _selectedTab.value = tab
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
        viewModelScope.launch {
            val result = processRecurringTransactionUseCase.processOccurrence(item)
            if (result.isSuccess) {
                _userMessage.value = "Recorded transaction for \"${item.title}\""
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to record transaction"
            }
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
