package com.varsel.expensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.domain.engine.AutoTransferReconciliationEngine
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.repository.LoanRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.ui.mapper.DashboardUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(

    private val transactionRepository: TransactionRepository,

    private val statementSnapshotRepository: StatementSnapshotRepository,

    private val loanRepository: LoanRepository,

    private val dashboardUiMapper: DashboardUiMapper,

    private val autoTransferReconciliationEngine: AutoTransferReconciliationEngine

) : ViewModel() {

    private val _uiState =
        MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> =
        _uiState.asStateFlow()

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
                loanRepository.getAllLoansSummary()
            ) { transactions, loans ->
                Pair(transactions, loans)
            }.collect { (transactions, loans) ->

                val snapshots =
                    statementSnapshotRepository
                        .getAllSnapshots()

                val baseDashboard =
                    dashboardUiMapper.map(
                        transactions = transactions,
                        snapshots = snapshots
                    )

                _uiState.value = baseDashboard.copy(loans = loans)
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
