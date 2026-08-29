package com.varsel.expensetracker.ui.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.domain.model.loan.LoanStatus
import com.varsel.expensetracker.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoansViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState())
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    init {
        observeLoans()
    }

    private fun observeLoans() {
        viewModelScope.launch {
            loanRepository.getAllLoansSummary().collect { loanSummaries ->
                val activeLoans = loanSummaries.filter { it.loan.status == LoanStatus.ACTIVE }
                val closedLoans = loanSummaries.filter { it.loan.status == LoanStatus.CLOSED }

                val totalOutstanding = activeLoans.sumOf { it.currentOutstandingBalance }
                val totalEmi = activeLoans.sumOf { it.nextEmiAmount }
                val totalPrincipalPaid = loanSummaries.sumOf { it.totalPrincipalPaid }
                val totalInterestPaid = loanSummaries.sumOf { it.totalInterestPaid }

                _uiState.value = LoansUiState(
                    loans = loanSummaries,
                    totalOutstandingDebt = totalOutstanding,
                    totalMonthlyEmi = totalEmi,
                    totalPrincipalRepaid = totalPrincipalPaid,
                    totalInterestPaid = totalInterestPaid,
                    activeLoansCount = activeLoans.size,
                    closedLoansCount = closedLoans.size,
                    isLoading = false
                )
            }
        }
    }
}
