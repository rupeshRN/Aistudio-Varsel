package com.varsel.expensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.ui.mapper.DashboardUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(

    private val transactionRepository: TransactionRepository,

    private val statementSnapshotRepository: StatementSnapshotRepository,

    private val dashboardUiMapper: DashboardUiMapper

) : ViewModel() {

    private val _uiState =
        MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> =
        _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {

        viewModelScope.launch(Dispatchers.IO) {

            transactionRepository
                .getAllTransactions()
                .collect { transactions ->

                    val snapshots =
                        statementSnapshotRepository
                            .getAllSnapshots()

                    _uiState.value =
                        dashboardUiMapper.map(
                            transactions = transactions,
                            snapshots = snapshots
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
