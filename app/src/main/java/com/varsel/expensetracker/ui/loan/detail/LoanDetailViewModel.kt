package com.varsel.expensetracker.ui.loan.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.domain.engine.LoanAmortizationEngine
import com.varsel.expensetracker.domain.model.loan.LoanPayment
import com.varsel.expensetracker.domain.model.loan.PrepaymentReductionType
import com.varsel.expensetracker.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    val amortizationEngine: LoanAmortizationEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val loanId: Long = when (val raw = savedStateHandle.get<Any>("loanId")) {
        is Long -> raw
        is String -> raw.toLongOrNull() ?: 0L
        is Number -> raw.toLong()
        else -> 0L
    }

    private val _uiState = MutableStateFlow(LoanDetailUiState())
    val uiState: StateFlow<LoanDetailUiState> = _uiState.asStateFlow()

    init {
        if (loanId > 0L) {
            loadLoanDetails(loanId)
        }
    }

    fun setLoanId(id: Long) {
        loadLoanDetails(id)
    }

    private fun loadLoanDetails(id: Long) {
        viewModelScope.launch {
            combine(
                loanRepository.getLoanSummary(id),
                loanRepository.getPaymentsForLoan(id)
            ) { summary, payments ->
                Pair(summary, payments)
            }.collect { (summary, payments) ->
                val schedule = loanRepository.getAmortizationSchedule(id)
                _uiState.value = _uiState.value.copy(
                    loanSummary = summary,
                    payments = payments,
                    amortizationSchedule = schedule,
                    isLoading = false
                )
            }
        }
    }

    fun selectTab(tab: LoanDetailTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun recordPayment(
        payment: LoanPayment,
        createBankTransaction: Boolean,
        bankAccountId: String?,
        bankAccountLast4: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            loanRepository.insertPayment(
                payment = payment,
                createBankTransaction = createBankTransaction,
                bankAccountId = bankAccountId,
                bankAccountLast4 = bankAccountLast4
            )
            val currentId = _uiState.value.loanSummary?.loan?.id ?: loanId
            val updatedSchedule = loanRepository.getAmortizationSchedule(currentId)
            _uiState.value = _uiState.value.copy(amortizationSchedule = updatedSchedule)
            onSuccess()
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            loanRepository.deletePayment(paymentId)
            val currentId = _uiState.value.loanSummary?.loan?.id ?: loanId
            val updatedSchedule = loanRepository.getAmortizationSchedule(currentId)
            _uiState.value = _uiState.value.copy(amortizationSchedule = updatedSchedule)
        }
    }

    fun deleteLoan(onDeleted: () -> Unit) {
        val currentId = _uiState.value.loanSummary?.loan?.id ?: loanId
        viewModelScope.launch {
            loanRepository.deleteLoan(currentId)
            onDeleted()
        }
    }

    fun runPrepaymentSimulation(
        extraLumpSum: Double,
        extraMonthly: Double,
        reductionType: PrepaymentReductionType
    ) {
        val currentId = _uiState.value.loanSummary?.loan?.id ?: loanId
        viewModelScope.launch {
            val result = loanRepository.simulatePrepayment(
                loanId = currentId,
                extraLumpSum = extraLumpSum,
                extraMonthly = extraMonthly,
                reductionType = reductionType
            )
            _uiState.value = _uiState.value.copy(simulationResult = result)
        }
    }

    fun updateFloatingRate(
        newAnnualRate: Double,
        newBenchmarkRate: Double?,
        newSpreadRate: Double?,
        recalculateEmi: Boolean,
        onSuccess: () -> Unit
    ) {
        val currentId = _uiState.value.loanSummary?.loan?.id ?: loanId
        viewModelScope.launch {
            loanRepository.updateFloatingRate(
                loanId = currentId,
                newAnnualRate = newAnnualRate,
                newBenchmarkRate = newBenchmarkRate,
                newSpreadRate = newSpreadRate,
                recalculateEmi = recalculateEmi
            )
            val updatedSchedule = loanRepository.getAmortizationSchedule(currentId)
            _uiState.value = _uiState.value.copy(amortizationSchedule = updatedSchedule)
            onSuccess()
        }
    }
}
