package com.varsel.expensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.preference.AppearanceRepository
import com.varsel.expensetracker.data.preference.GeneralPreferencesRepository
import com.varsel.expensetracker.data.preference.HomeSection
import com.varsel.expensetracker.domain.engine.AutoTransferReconciliationEngine
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.repository.LoanRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val statementSnapshotRepository: StatementSnapshotRepository,
    private val loanRepository: LoanRepository,
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
                appearanceRepository.appearanceConfig
            ) { transactions, loans, appearanceConfig ->
                Triple(transactions, loans, appearanceConfig)
            }.collect { (transactions, loans, appearanceConfig) ->

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

                _uiState.value = baseDashboard.copy(
                    loans = loans,
                    insights = insights
                )
            }
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
