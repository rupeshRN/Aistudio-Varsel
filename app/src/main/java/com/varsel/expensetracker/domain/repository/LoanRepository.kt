package com.varsel.expensetracker.domain.repository

import com.varsel.expensetracker.domain.model.loan.AmortizationScheduleItem
import com.varsel.expensetracker.domain.model.loan.LoanAccount
import com.varsel.expensetracker.domain.model.loan.LoanPayment
import com.varsel.expensetracker.domain.model.loan.LoanSummary
import com.varsel.expensetracker.domain.model.loan.PrepaymentReductionType
import com.varsel.expensetracker.domain.model.loan.PrepaymentSimulationResult
import kotlinx.coroutines.flow.Flow

interface LoanRepository {

    fun getAllLoans(): Flow<List<LoanAccount>>

    fun getLoanById(id: Long): Flow<LoanAccount?>

    suspend fun getLoanByIdSync(id: Long): LoanAccount?

    suspend fun insertLoan(loan: LoanAccount): Long

    suspend fun updateLoan(loan: LoanAccount)

    suspend fun deleteLoan(loanId: Long)

    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPayment>>

    suspend fun insertPayment(
        payment: LoanPayment,
        createBankTransaction: Boolean = false,
        bankAccountId: String? = null,
        bankAccountLast4: String? = null
    ): Long

    suspend fun updatePayment(payment: LoanPayment)

    suspend fun deletePayment(paymentId: Long)

    fun getLoanSummary(loanId: Long): Flow<LoanSummary?>

    fun getAllLoansSummary(): Flow<List<LoanSummary>>

    suspend fun getAmortizationSchedule(loanId: Long): List<AmortizationScheduleItem>

    suspend fun simulatePrepayment(
        loanId: Long,
        extraLumpSum: Double,
        extraMonthly: Double,
        reductionType: PrepaymentReductionType
    ): PrepaymentSimulationResult?

    suspend fun updateFloatingRate(
        loanId: Long,
        newAnnualRate: Double,
        newBenchmarkRate: Double?,
        newSpreadRate: Double?,
        recalculateEmi: Boolean
    ): LoanAccount?

    suspend fun onTransactionDeleted(transactionId: Long)
}
