package com.varsel.expensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.preference.AppearanceRepository
import com.varsel.expensetracker.data.preference.GeneralPreferencesRepository
import com.varsel.expensetracker.data.preference.HomeSection
import com.varsel.expensetracker.domain.engine.AutoTransferReconciliationEngine
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.repository.BudgetRepository
import com.varsel.expensetracker.domain.repository.LoanRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.ui.budget.BudgetCalculator
import com.varsel.expensetracker.ui.mapper.DashboardUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val dashboardUiMapper: DashboardUiMapper,
    private val autoTransferReconciliationEngine: AutoTransferReconciliationEngine,
    private val appearanceRepository: AppearanceRepository,
    private val generalPreferencesRepository: GeneralPreferencesRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> =
        _uiState.asStateFlow()

    val activeHomeSections: StateFlow<List<String>> = generalPreferencesRepository.generalConfig
        .map { it.activeHomeSections }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeSection.DEFAULT_ACTIVE
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                autoTransferReconciliationEngine.reconcileTransfers()
            } catch (_: Exception) {}
        }
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                transactionRepository.getAllTransactions(),
                loanRepository.getAllLoansSummary(),
                appearanceRepository.appearanceConfig,
                budgetRepository.getAllBudgets(),
                generalPreferencesRepository.generalConfig
            ) { transactions, loans, appearanceConfig, rawBudgets, generalConfig ->
                val snapshots =
                    statementSnapshotRepository
                        .getAllSnapshots()

                val baseDashboard =
                    dashboardUiMapper.map(
                        transactions = transactions,
                        snapshots = snapshots
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
                val configuredAccounts = if (generalConfig.pinnedAccounts.isNotEmpty()) {
                    val pinned = generalConfig.pinnedAccounts
                    val matched = rawAccounts.filter { acc ->
                        pinned.any { p ->
                            p.equals(acc.bankShortName, ignoreCase = true) ||
                            p.equals(acc.bankName, ignoreCase = true) ||
                            acc.accountDisplayName.contains(p, ignoreCase = true)
                        }
                    }
                    val listToOrder = if (matched.isNotEmpty()) matched else rawAccounts
                    if (generalConfig.primaryAccount != "First Select" && generalConfig.primaryAccount.isNotBlank()) {
                        val primary = generalConfig.primaryAccount
                        listToOrder.sortedByDescending { acc ->
                            primary.equals(acc.bankShortName, ignoreCase = true) ||
                            primary.equals(acc.bankName, ignoreCase = true) ||
                            acc.accountDisplayName.contains(primary, ignoreCase = true)
                        }
                    } else {
                        listToOrder.sortedBy { acc ->
                            val idx = pinned.indexOfFirst { p ->
                                p.equals(acc.bankShortName, ignoreCase = true) ||
                                p.equals(acc.bankName, ignoreCase = true) ||
                                acc.accountDisplayName.contains(p, ignoreCase = true)
                            }
                            if (idx >= 0) idx else 999
                        }
                    }
                } else {
                    rawAccounts
                }

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
                        isBalanceHidden = current.isBalanceHidden
                    )
                }
            }.collect {}
        }
    }

    fun toggleBalanceVisibility() {
        _uiState.update { it.copy(isBalanceHidden = !it.isBalanceHidden) }
    }

    fun setHomeBudgetsSelection(selection: String) {
        viewModelScope.launch(Dispatchers.IO) {
            generalPreferencesRepository.setHomeBudgetsSelection(selection)
        }
    }

    fun setHomeGoalsSelection(selection: String) {
        viewModelScope.launch(Dispatchers.IO) {
            generalPreferencesRepository.setHomeGoalsSelection(selection)
        }
    }

    fun updateTransaction(
        transaction: Transaction
    ) {

        viewModelScope.launch(Dispatchers.IO) {

            transactionRepository
                .updateTransaction(transaction)
        }
    }
}
