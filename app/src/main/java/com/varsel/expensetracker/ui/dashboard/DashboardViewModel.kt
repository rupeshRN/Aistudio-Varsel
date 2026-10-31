package com.varsel.expensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.data.preference.AppearanceConfig
import com.varsel.expensetracker.data.preference.AppearanceRepository
import com.varsel.expensetracker.data.preference.GeneralConfig
import com.varsel.expensetracker.data.preference.GeneralPreferencesRepository
import com.varsel.expensetracker.data.preference.HomeSection
import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.domain.engine.AutoTransferReconciliationEngine
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.model.loan.LoanSummary
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.domain.repository.BudgetRepository
import com.varsel.expensetracker.domain.repository.LoanRepository
import com.varsel.expensetracker.domain.repository.RecurringRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.domain.usecase.AddManualTransactionUseCase
import com.varsel.expensetracker.ui.budget.BudgetCalculator
import com.varsel.expensetracker.ui.mapper.DashboardUiMapper
import com.varsel.expensetracker.ui.model.AccountBalanceUiModel
import com.varsel.expensetracker.ui.transaction.model.AccountOption
import com.varsel.expensetracker.ui.util.UiErrorEvent
import com.varsel.expensetracker.util.AppError
import com.varsel.expensetracker.util.SafeErrorHandler
import com.varsel.expensetracker.util.SafeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val statementSnapshotRepository: StatementSnapshotRepository,
    private val loanRepository: LoanRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringRepository: RecurringRepository,
    private val dashboardUiMapper: DashboardUiMapper,
    private val autoTransferReconciliationEngine: AutoTransferReconciliationEngine,
    private val appearanceRepository: AppearanceRepository,
    private val generalPreferencesRepository: GeneralPreferencesRepository,
    private val addManualTransactionUseCase: AddManualTransactionUseCase,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> =
        _uiState.asStateFlow()

    private val _errorEvents = Channel<UiErrorEvent>(Channel.BUFFERED)
    val errorEvents = _errorEvents.receiveAsFlow()

    private var dashboardLoadJob: Job? = null

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .catch { e ->
            if (e is CancellationException) throw e
            SafeLog.e("DashboardViewModel", "Failed to stream categories", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableAccounts: StateFlow<List<AccountOption>> = statementSnapshotRepository.observeAllSnapshots()
        .map { snapshots ->
            val cashOption = AccountOption(
                accountId = null,
                accountLast4 = null,
                bankName = "Cash",
                displayName = "Cash / General"
            )
            val bankAccounts = snapshots.mapNotNull { snap ->
                val id = snap.accountId ?: return@mapNotNull null
                val last4 = snap.accountLast4 ?: "••••"
                val name = snap.bankName ?: "Bank Account"
                AccountOption(
                    accountId = id,
                    accountLast4 = last4,
                    bankName = name,
                    displayName = "$name (•••• $last4)"
                )
            }.distinctBy { it.accountId }
            listOf(cashOption) + bankAccounts
        }
        .catch { e ->
            if (e is CancellationException) throw e
            SafeLog.e("DashboardViewModel", "Failed to stream account snapshots", e)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            listOf(
                AccountOption(
                    accountId = null,
                    accountLast4 = null,
                    bankName = "Cash",
                    displayName = "Cash / General"
                )
            )
        )

    val activeHomeSections: StateFlow<List<String>> = generalPreferencesRepository.generalConfig
        .map { it.activeHomeSections }
        .catch { e ->
            if (e is CancellationException) throw e
            SafeLog.e("DashboardViewModel", "Failed to stream active home sections", e)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeSection.DEFAULT_ACTIVE
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                autoTransferReconciliationEngine.reconcileTransfers()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SafeLog.w("DashboardViewModel", "Auto-reconciliation could not complete", e)
            }
        }
        loadDashboard()
    }

    fun retryLoadDashboard() {
        loadDashboard()
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadDashboard() {
        dashboardLoadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }

        dashboardLoadJob = viewModelScope.launch(Dispatchers.IO) {
            combine(
                transactionRepository.getAllTransactions(),
                loanRepository.getAllLoansSummary(),
                appearanceRepository.appearanceConfig,
                budgetRepository.getAllBudgets(),
                recurringRepository.getActiveRecurringItems(),
                generalPreferencesRepository.generalConfig
            ) { args: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val transactions = args[0] as List<Transaction>
                @Suppress("UNCHECKED_CAST")
                val loans = args[1] as List<LoanSummary>
                val appearanceConfig = args[2] as AppearanceConfig
                @Suppress("UNCHECKED_CAST")
                val rawBudgets = args[3] as List<BudgetEntity>
                @Suppress("UNCHECKED_CAST")
                val activeRecurring = args[4] as List<RecurringItem>
                val generalConfig = args[5] as GeneralConfig

                val snapshots =
                    statementSnapshotRepository
                        .getAllSnapshots()

                val baseDashboard =
                    dashboardUiMapper.map(
                        transactions = transactions,
                        snapshots = snapshots,
                        period = generalConfig.netWorthWidgetPeriod
                    )

                val insights = if (appearanceConfig.actionableInsights) {
                    baseDashboard.insights
                } else {
                    emptyList()
                }

                val allBudgets = rawBudgets.map { budget ->
                    BudgetCalculator.computeBudgetUiModel(
                        budget = budget,
                        transactions = transactions
                    )
                }

                val expenseBudgets = allBudgets.filter { !it.budget.budgetType.equals("SAVINGS", ignoreCase = true) }
                val savingsGoals = allBudgets.filter { it.budget.budgetType.equals("SAVINGS", ignoreCase = true) }

                val homeBudgetsSelection = generalConfig.homeBudgetsSelection
                val homeGoalsSelection = generalConfig.homeGoalsSelection

                val visibleBudgets = if (homeBudgetsSelection == "ALL" || homeBudgetsSelection.isBlank()) {
                    expenseBudgets
                } else {
                    val selectedIds = homeBudgetsSelection.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
                    val filtered = expenseBudgets.filter { it.budget.id in selectedIds }
                    if (filtered.isNotEmpty()) filtered else expenseBudgets
                }

                val visibleGoals = if (homeGoalsSelection == "ALL" || homeGoalsSelection.isBlank()) {
                    savingsGoals
                } else {
                    val selectedIds = homeGoalsSelection.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
                    val filtered = savingsGoals.filter { it.budget.id in selectedIds }
                    if (filtered.isNotEmpty()) filtered else savingsGoals
                }

                val totalBudgetLimit = visibleBudgets.sumOf { it.budget.amount }
                val totalBudgetSpent = visibleBudgets.sumOf { it.amountSpent }
                val totalGoalTarget = visibleGoals.sumOf { it.budget.amount }
                val totalGoalSaved = visibleGoals.sumOf { it.amountSpent }
                val rawAccounts = baseDashboard.balanceSummary.accounts

                val pinnedAccounts = generalConfig.pinnedAccounts
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val primaryAccount = generalConfig.primaryAccount.trim()

                fun accountMatchesName(
                    account: AccountBalanceUiModel,
                    configuredName: String
                ): Boolean {
                    val configured = configuredName.trim().lowercase()

                    if (configured.isBlank()) return false

                    val shortName = account.bankShortName
                        .trim()
                        .lowercase()

                    val bankName = account.bankName
                        .trim()
                        .lowercase()

                    val displayName = account.accountDisplayName
                        .trim()
                        .lowercase()

                    return configured == shortName ||
                        configured == bankName ||
                        configured == displayName
                }

                val configuredAccounts =
                    rawAccounts.sortedWith(
                        compareBy<AccountBalanceUiModel> { account ->

                            val pinnedIndex = pinnedAccounts.indexOfFirst { pinnedName ->
                                accountMatchesName(account, pinnedName)
                            }

                            when {
                                pinnedIndex >= 0 -> pinnedIndex

                                primaryAccount != "First Select" &&
                                    primaryAccount.isNotBlank() &&
                                    accountMatchesName(account, primaryAccount) -> -1

                                else -> Int.MAX_VALUE
                            }
                        }
                    )

                val recentTxns = when (generalConfig.homeTransactionsFilter) {
                    "EXPENSE" -> baseDashboard.recentTransactions.filter { !it.isIncome }
                    "INCOME" -> baseDashboard.recentTransactions.filter { it.isIncome }
                    else -> baseDashboard.recentTransactions
                }.take(if (generalConfig.homeTransactionsCount > 0) generalConfig.homeTransactionsCount else 5)

                _uiState.update { current ->
                    baseDashboard.copy(
                        balanceSummary = baseDashboard.balanceSummary.copy(accounts = configuredAccounts),
                        recentTransactions = recentTxns,
                        loans = loans,
                        recurringItems = activeRecurring,
                        insights = insights,
                        allBudgets = expenseBudgets,
                        allGoals = savingsGoals,
                        visibleBudgets = visibleBudgets,
                        visibleGoals = visibleGoals,
                        homeBudgetsSelection = homeBudgetsSelection,
                        homeGoalsSelection = homeGoalsSelection,
                        totalBudgetLimit = totalBudgetLimit,
                        totalBudgetSpent = totalBudgetSpent,
                        totalGoalTarget = totalGoalTarget,
                        totalGoalSaved = totalGoalSaved,
                        isBalanceHidden = current.isBalanceHidden,
                        showNetWorthBreakdown = generalConfig.showNetWorthBreakdown,
                        isLoading = false,
                        error = null
                    )
                }
            }
            .catch { throwable ->
                if (throwable is CancellationException) throw throwable
                val appError = SafeErrorHandler.handle("DashboardViewModel", throwable, "Load Dashboard")
                _uiState.update { it.copy(isLoading = false, error = appError) }
            }
            .collect {}
        }
    }

    fun toggleBalanceVisibility() {
        _uiState.update { it.copy(isBalanceHidden = !it.isBalanceHidden) }
    }

    fun setHomeBudgetsSelection(selection: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                generalPreferencesRepository.setHomeBudgetsSelection(selection)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SafeLog.w("DashboardViewModel", "Failed to update home budget preference", e)
            }
        }
    }

    fun setHomeGoalsSelection(selection: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                generalPreferencesRepository.setHomeGoalsSelection(selection)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SafeLog.w("DashboardViewModel", "Failed to update home goal preference", e)
            }
        }
    }

    fun updateTransaction(
        transaction: Transaction
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionRepository.updateTransaction(transaction)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val appError = SafeErrorHandler.handle("DashboardViewModel", e, "Update Transaction")
                _errorEvents.send(UiErrorEvent(error = appError))
            }
        }
    }

    fun addTransaction(
        amount: Double,
        type: TransactionType,
        description: String,
        category: String,
        dateTimestamp: Long,
        referenceNumber: String?,
        accountId: String? = null,
        accountLast4: String? = null,
        bankName: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val result = addManualTransactionUseCase.addTransaction(
                    amount = amount,
                    type = type,
                    description = description,
                    category = category,
                    dateTimestamp = dateTimestamp,
                    referenceNumber = referenceNumber,
                    accountId = accountId,
                    accountLast4 = accountLast4,
                    bankName = bankName
                )
                if (result.isSuccess) {
                    onComplete?.invoke(true)
                } else {
                    val throwable = result.exceptionOrNull() ?: Exception("Transaction saving failed")
                    val appError = SafeErrorHandler.handle("DashboardViewModel", throwable, "Add Transaction")
                    _errorEvents.send(UiErrorEvent(error = appError))
                    onComplete?.invoke(false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val appError = SafeErrorHandler.handle("DashboardViewModel", e, "Add Transaction")
                _errorEvents.send(UiErrorEvent(error = appError))
                onComplete?.invoke(false)
            }
        }
    }

    fun addTransfer(
        amount: Double,
        description: String,
        dateTimestamp: Long,
        fromAccountId: String?,
        fromAccountLast4: String?,
        fromBankName: String?,
        toAccountId: String?,
        toAccountLast4: String?,
        toBankName: String?,
        referenceNumber: String?,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val result = addManualTransactionUseCase.addTransfer(
                    amount = amount,
                    description = description,
                    dateTimestamp = dateTimestamp,
                    fromAccountId = fromAccountId,
                    fromAccountLast4 = fromAccountLast4,
                    fromBankName = fromBankName,
                    toAccountId = toAccountId,
                    toAccountLast4 = toAccountLast4,
                    toBankName = toBankName,
                    referenceNumber = referenceNumber
                )
                if (result.isSuccess) {
                    onComplete?.invoke(true)
                } else {
                    val throwable = result.exceptionOrNull() ?: Exception("Transfer saving failed")
                    val appError = SafeErrorHandler.handle("DashboardViewModel", throwable, "Add Transfer")
                    _errorEvents.send(UiErrorEvent(error = appError))
                    onComplete?.invoke(false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val appError = SafeErrorHandler.handle("DashboardViewModel", e, "Add Transfer")
                _errorEvents.send(UiErrorEvent(error = appError))
                onComplete?.invoke(false)
            }
        }
    }

    fun createCategory(name: String, isIncome: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trimmed = name.trim()
                if (trimmed.isBlank()) return@launch
                val iconKey = com.varsel.expensetracker.category.CategoryIconCatalog.iconKeyForCategory(trimmed, isIncome)
                val colorHex = if (isIncome) "#4CAF50" else "#E91E63"
                val entity = CategoryEntity(
                    name = trimmed,
                    type = if (isIncome) "INCOME" else "EXPENSE",
                    iconName = iconKey,
                    colorHex = colorHex
                )
                categoryDao.insertCategory(entity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val appError = SafeErrorHandler.handle("DashboardViewModel", e, "Create Category")
                _errorEvents.send(UiErrorEvent(error = appError))
            }
        }
    }
}

