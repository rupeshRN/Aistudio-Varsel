package com.varsel.expensetracker.ui.loan.add_edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.domain.engine.LoanAmortizationEngine
import com.varsel.expensetracker.domain.model.loan.InterestRateType
import com.varsel.expensetracker.domain.model.loan.LoanAccount
import com.varsel.expensetracker.domain.model.loan.LoanStatus
import com.varsel.expensetracker.domain.model.loan.LoanType
import com.varsel.expensetracker.domain.repository.LoanRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class BankAccountOption(
    val accountId: String,
    val accountLast4: String,
    val bankName: String
)

data class AddEditLoanUiState(
    val loanId: Long = 0L,
    val name: String = "",
    val loanType: LoanType = LoanType.HOME_LOAN,
    val principalString: String = "",
    val interestType: InterestRateType = InterestRateType.FIXED,
    val benchmarkRateString: String = "",
    val spreadRateString: String = "",
    val interestRateString: String = "",
    val tenureMonthsString: String = "",
    val emiAmountString: String = "",
    val isAutoEmi: Boolean = true,
    val startDateTimestamp: Long = System.currentTimeMillis(),
    val collateralOrNotes: String = "",
    val lenderName: String = "",
    val loanAccountNumber: String = "",
    val selectedBankAccountId: String? = null,
    val selectedBankAccountLast4: String? = null,
    val bankAccounts: List<BankAccountOption> = emptyList(),
    val isEditing: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class AddEditLoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val statementSnapshotRepository: StatementSnapshotRepository,
    private val amortizationEngine: LoanAmortizationEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editLoanId: Long = when (val raw = savedStateHandle.get<Any>("loanId")) {
        is Long -> raw
        is String -> raw.toLongOrNull() ?: 0L
        is Number -> raw.toLong()
        else -> 0L
    }

    private val _uiState = MutableStateFlow(AddEditLoanUiState(loanId = editLoanId))
    val uiState: StateFlow<AddEditLoanUiState> = _uiState.asStateFlow()

    init {
        loadBankAccounts()
        if (editLoanId > 0L) {
            loadExistingLoan(editLoanId)
        }
    }

    private fun loadBankAccounts() {
        viewModelScope.launch {
            try {
                val snapshots = statementSnapshotRepository.getAllSnapshots()
                val accounts = snapshots.mapNotNull { snap ->
                    val id = snap.accountId ?: return@mapNotNull null
                    val last4 = snap.accountLast4 ?: "••••"
                    BankAccountOption(accountId = id, accountLast4 = last4, bankName = "Account (•••• $last4)")
                }.distinctBy { it.accountId }

                _uiState.value = _uiState.value.copy(bankAccounts = accounts)
            } catch (e: Exception) {
                // If snapshots are not available, continue with empty bank accounts list
            }
        }
    }

    private fun loadExistingLoan(id: Long) {
        viewModelScope.launch {
            loanRepository.getLoanById(id).collect { loan ->
                if (loan != null) {
                    _uiState.value = _uiState.value.copy(
                        loanId = loan.id,
                        name = loan.name,
                        loanType = loan.loanType,
                        principalString = if (loan.principal > 0) loan.principal.toLong().toString() else "",
                        interestRateString = if (loan.annualInterestRate > 0) loan.annualInterestRate.toString() else "",
                        interestType = loan.interestType,
                        benchmarkRateString = loan.benchmarkRate?.toString().orEmpty(),
                        spreadRateString = loan.spreadRate?.toString().orEmpty(),
                        tenureMonthsString = if (loan.totalTenureMonths > 0) loan.totalTenureMonths.toString() else "",
                        emiAmountString = if (loan.emiAmount > 0) loan.emiAmount.toLong().toString() else "",
                        startDateTimestamp = loan.startDateTimestamp,
                        collateralOrNotes = loan.collateralOrNotes.orEmpty(),
                        lenderName = loan.lenderName.orEmpty(),
                        loanAccountNumber = loan.loanAccountNumber.orEmpty(),
                        selectedBankAccountId = loan.linkedBankAccountId,
                        selectedBankAccountLast4 = loan.bankAccountLast4,
                        isEditing = true,
                        isAutoEmi = false
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name, errorMessage = null)
    }

    fun onLoanTypeChange(type: LoanType) {
        _uiState.value = _uiState.value.copy(loanType = type)
    }

    fun onPrincipalChange(principal: String) {
        _uiState.value = _uiState.value.copy(principalString = principal, errorMessage = null)
        recalculateEmiIfAuto()
    }

    fun onInterestRateChange(rate: String) {
        _uiState.value = _uiState.value.copy(interestRateString = rate, errorMessage = null)
        recalculateEmiIfAuto()
    }

    fun onInterestTypeChange(type: InterestRateType) {
        _uiState.value = _uiState.value.copy(interestType = type)
        if (type == InterestRateType.FLOATING) {
            updateFloatingTotalRate()
        }
    }

    fun onBenchmarkRateChange(rate: String) {
        _uiState.value = _uiState.value.copy(benchmarkRateString = rate, errorMessage = null)
        updateFloatingTotalRate()
    }

    fun onSpreadRateChange(spread: String) {
        _uiState.value = _uiState.value.copy(spreadRateString = spread, errorMessage = null)
        updateFloatingTotalRate()
    }

    private fun updateFloatingTotalRate() {
        val state = _uiState.value
        val benchmark = state.benchmarkRateString.toDoubleOrNull()
        val spread = state.spreadRateString.toDoubleOrNull()
        if (benchmark != null && spread != null) {
            val total = benchmark + spread
            _uiState.value = _uiState.value.copy(
                interestRateString = (kotlin.math.round(total * 100.0) / 100.0).toString()
            )
            recalculateEmiIfAuto()
        }
    }

    fun onTenureMonthsChange(tenure: String) {
        _uiState.value = _uiState.value.copy(tenureMonthsString = tenure, errorMessage = null)
        recalculateEmiIfAuto()
    }

    fun onEmiAmountChange(emi: String) {
        _uiState.value = _uiState.value.copy(
            emiAmountString = emi,
            isAutoEmi = false,
            errorMessage = null
        )
    }

    fun onToggleAutoEmi(auto: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoEmi = auto)
        if (auto) {
            recalculateEmiIfAuto()
        }
    }

    fun onStartDateChange(timestamp: Long) {
        _uiState.value = _uiState.value.copy(startDateTimestamp = timestamp)
    }

    fun onCollateralOrNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(collateralOrNotes = notes)
    }

    fun onLenderNameChange(lender: String) {
        _uiState.value = _uiState.value.copy(lenderName = lender)
    }

    fun onLoanAccountNumberChange(accountNum: String) {
        _uiState.value = _uiState.value.copy(loanAccountNumber = accountNum)
    }

    fun onBankAccountSelected(option: BankAccountOption?) {
        _uiState.value = _uiState.value.copy(
            selectedBankAccountId = option?.accountId,
            selectedBankAccountLast4 = option?.accountLast4
        )
    }

    private fun recalculateEmiIfAuto() {
        val state = _uiState.value
        if (!state.isAutoEmi) return

        val p = state.principalString.toDoubleOrNull() ?: 0.0
        val r = state.interestRateString.toDoubleOrNull() ?: 0.0
        val n = state.tenureMonthsString.toIntOrNull() ?: 0

        if (p > 0 && n > 0) {
            val calculatedEmi = amortizationEngine.calculateEmi(p, r, n)
            _uiState.value = _uiState.value.copy(
                emiAmountString = if (calculatedEmi > 0) calculatedEmi.toLong().toString() else ""
            )
        }
    }

    fun saveLoan(onSuccess: (Long) -> Unit) {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Please enter a loan name")
            return
        }

        val principal = state.principalString.toDoubleOrNull() ?: 0.0
        if (principal <= 0.0) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid principal amount")
            return
        }

        val rate = state.interestRateString.toDoubleOrNull() ?: 0.0
        val tenure = state.tenureMonthsString.toIntOrNull() ?: 0
        if (tenure <= 0) {
            _uiState.value = state.copy(errorMessage = "Please enter tenure in months")
            return
        }

        var emi = state.emiAmountString.toDoubleOrNull() ?: 0.0
        if (emi <= 0.0) {
            emi = amortizationEngine.calculateEmi(principal, rate, tenure)
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val loanAccount = LoanAccount(
                id = state.loanId,
                name = name,
                loanType = state.loanType,
                principal = principal,
                annualInterestRate = rate,
                emiAmount = emi,
                totalTenureMonths = tenure,
                startDateTimestamp = state.startDateTimestamp,
                collateralOrNotes = state.collateralOrNotes.trim().ifEmpty { null },
                status = LoanStatus.ACTIVE,
                linkedBankAccountId = state.selectedBankAccountId,
                bankAccountLast4 = state.selectedBankAccountLast4,
                lenderName = state.lenderName.trim().ifEmpty { null },
                loanAccountNumber = state.loanAccountNumber.trim().ifEmpty { null },
                interestType = state.interestType,
                benchmarkRate = state.benchmarkRateString.toDoubleOrNull(),
                spreadRate = state.spreadRateString.toDoubleOrNull()
            )

            if (state.isEditing) {
                loanRepository.updateLoan(loanAccount)
                onSuccess(loanAccount.id)
            } else {
                val newId = loanRepository.insertLoan(loanAccount)
                onSuccess(newId)
            }
        }
    }
}
