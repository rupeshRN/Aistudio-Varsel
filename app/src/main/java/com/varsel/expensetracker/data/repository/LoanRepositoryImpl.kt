package com.varsel.expensetracker.data.repository

import com.varsel.expensetracker.data.local.dao.LoanAccountDao
import com.varsel.expensetracker.data.local.dao.LoanPaymentDao
import com.varsel.expensetracker.data.local.dao.TransactionDao
import com.varsel.expensetracker.data.local.entity.LoanAccountEntity
import com.varsel.expensetracker.data.local.entity.LoanPaymentEntity
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import com.varsel.expensetracker.domain.engine.LoanAmortizationEngine
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.model.loan.*
import com.varsel.expensetracker.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepositoryImpl @Inject constructor(
    private val loanAccountDao: LoanAccountDao,
    private val loanPaymentDao: LoanPaymentDao,
    private val transactionDao: TransactionDao,
    private val amortizationEngine: LoanAmortizationEngine
) : LoanRepository {

    override fun getAllLoans(): Flow<List<LoanAccount>> {
        return loanAccountDao.getAllLoanAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLoanById(id: Long): Flow<LoanAccount?> {
        return loanAccountDao.getLoanAccountById(id).map { it?.toDomain() }
    }

    override suspend fun getLoanByIdSync(id: Long): LoanAccount? {
        return loanAccountDao.getLoanAccountByIdSync(id)?.toDomain()
    }

    override suspend fun insertLoan(loan: LoanAccount): Long {
        val emi = if (loan.emiAmount > 0.0) {
            loan.emiAmount
        } else {
            amortizationEngine.calculateEmi(
                principal = loan.principal,
                annualInterestRate = loan.annualInterestRate,
                tenureMonths = loan.totalTenureMonths
            )
        }
        val entity = loan.copy(emiAmount = emi).toEntity()
        return loanAccountDao.insertLoanAccount(entity)
    }

    override suspend fun updateLoan(loan: LoanAccount) {
        loanAccountDao.updateLoanAccount(loan.toEntity())
    }

    override suspend fun deleteLoan(loanId: Long) {
        loanPaymentDao.deletePaymentsForLoan(loanId)
        loanAccountDao.deleteLoanAccountById(loanId)
    }

    override fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPayment>> {
        return loanPaymentDao.getPaymentsForLoan(loanId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertPayment(
        payment: LoanPayment,
        createBankTransaction: Boolean,
        bankAccountId: String?,
        bankAccountLast4: String?
    ): Long {
        var linkedTxId = payment.linkedTransactionId

        if (createBankTransaction) {
            val loanEntity = loanAccountDao.getLoanAccountByIdSync(payment.loanId)
            val loanName = loanEntity?.name ?: "Loan"
            val paymentDesc = when (payment.paymentType) {
                LoanPaymentType.REGULAR_EMI -> "EMI Payment: $loanName"
                LoanPaymentType.PRE_PAYMENT -> "Pre-payment: $loanName"
                LoanPaymentType.CLOSURE -> "Loan Closure: $loanName"
            }

            val newTx = TransactionEntity(
                amount = payment.amount,
                type = TransactionType.EXPENSE.name,
                description = paymentDesc,
                category = "Loan Repayment",
                dateTimestamp = payment.paymentDateTimestamp,
                accountId = bankAccountId ?: loanEntity?.linkedBankAccountId,
                accountLast4 = bankAccountLast4 ?: loanEntity?.bankAccountLast4,
                role = TransactionRole.NORMAL.name
            )
            transactionDao.insertTransaction(newTx)
        }

        val entity = payment.copy(linkedTransactionId = linkedTxId).toEntity()
        val paymentId = loanPaymentDao.insertPayment(entity)

        // Auto-check if loan should be marked CLOSED if balance <= 0
        checkAndAutoUpdateLoanStatus(payment.loanId)

        return paymentId
    }

    override suspend fun updatePayment(payment: LoanPayment) {
        loanPaymentDao.updatePayment(payment.toEntity())
        checkAndAutoUpdateLoanStatus(payment.loanId)
    }

    override suspend fun deletePayment(paymentId: Long) {
        loanPaymentDao.deletePaymentById(paymentId)
    }

    override fun getLoanSummary(loanId: Long): Flow<LoanSummary?> {
        return combine(
            loanAccountDao.getLoanAccountById(loanId),
            loanPaymentDao.getPaymentsForLoan(loanId)
        ) { loanEntity, paymentEntities ->
            if (loanEntity == null) return@combine null
            val loan = loanEntity.toDomain()
            val payments = paymentEntities.map { it.toDomain() }
            amortizationEngine.computeLoanSummary(loan, payments)
        }
    }

    override fun getAllLoansSummary(): Flow<List<LoanSummary>> {
        return combine(
            loanAccountDao.getAllLoanAccounts(),
            loanPaymentDao.getAllPayments()
        ) { loanEntities, paymentEntities ->
            val paymentsByLoan = paymentEntities.groupBy { it.loanId }
            loanEntities.map { loanEntity ->
                val loan = loanEntity.toDomain()
                val payments = paymentsByLoan[loan.id]?.map { it.toDomain() } ?: emptyList()
                amortizationEngine.computeLoanSummary(loan, payments)
            }
        }
    }

    override suspend fun getAmortizationSchedule(loanId: Long): List<AmortizationScheduleItem> {
        val loanEntity = loanAccountDao.getLoanAccountByIdSync(loanId) ?: return emptyList()
        val payments = loanPaymentDao.getPaymentsForLoanAscSync(loanId).map { it.toDomain() }
        val loan = loanEntity.toDomain()
        return amortizationEngine.generateSchedule(
            principal = loan.principal,
            annualInterestRate = loan.annualInterestRate,
            emiAmount = loan.emiAmount,
            tenureMonths = loan.totalTenureMonths,
            startDateTimestamp = loan.startDateTimestamp,
            payments = payments
        )
    }

    override suspend fun simulatePrepayment(
        loanId: Long,
        extraLumpSum: Double,
        extraMonthly: Double,
        reductionType: PrepaymentReductionType
    ): PrepaymentSimulationResult? {
        val loanEntity = loanAccountDao.getLoanAccountByIdSync(loanId) ?: return null
        val payments = loanPaymentDao.getPaymentsForLoanAscSync(loanId).map { it.toDomain() }
        val loan = loanEntity.toDomain()
        val summary = amortizationEngine.computeLoanSummary(loan, payments)
        return amortizationEngine.simulatePrepayment(
            loan = loan,
            currentOutstandingBalance = summary.currentOutstandingBalance,
            extraLumpSum = extraLumpSum,
            extraMonthly = extraMonthly,
            reductionType = reductionType
        )
    }

    override suspend fun updateFloatingRate(
        loanId: Long,
        newAnnualRate: Double,
        newBenchmarkRate: Double?,
        newSpreadRate: Double?,
        recalculateEmi: Boolean
    ): LoanAccount? {
        val loanEntity = loanAccountDao.getLoanAccountByIdSync(loanId) ?: return null
        val payments = loanPaymentDao.getPaymentsForLoanAscSync(loanId).map { it.toDomain() }
        val loan = loanEntity.toDomain()
        val summary = amortizationEngine.computeLoanSummary(loan, payments)
        val outstanding = summary.currentOutstandingBalance
        val completedMonths = summary.completedTenureMonths

        val updatedLoan = if (recalculateEmi) {
            val remainingMonths = kotlin.math.max(1, loan.totalTenureMonths - completedMonths)
            val newEmi = amortizationEngine.calculateEmi(
                principal = outstanding,
                annualInterestRate = newAnnualRate,
                tenureMonths = remainingMonths
            )
            loan.copy(
                annualInterestRate = newAnnualRate,
                emiAmount = newEmi,
                benchmarkRate = newBenchmarkRate,
                spreadRate = newSpreadRate,
                interestType = InterestRateType.FLOATING
            )
        } else {
            val newRemainingMonths = amortizationEngine.calculateTenureMonths(
                principal = outstanding,
                annualInterestRate = newAnnualRate,
                emiAmount = loan.emiAmount
            )
            val newTotalTenure = completedMonths + newRemainingMonths
            loan.copy(
                annualInterestRate = newAnnualRate,
                totalTenureMonths = kotlin.math.max(1, newTotalTenure),
                benchmarkRate = newBenchmarkRate,
                spreadRate = newSpreadRate,
                interestType = InterestRateType.FLOATING
            )
        }

        loanAccountDao.updateLoanAccount(updatedLoan.toEntity())
        return updatedLoan
    }

    override suspend fun onTransactionDeleted(transactionId: Long) {
        loanPaymentDao.deletePaymentByTransactionId(transactionId)
    }

    private suspend fun checkAndAutoUpdateLoanStatus(loanId: Long) {
        val loanEntity = loanAccountDao.getLoanAccountByIdSync(loanId) ?: return
        val payments = loanPaymentDao.getPaymentsForLoanAscSync(loanId).map { it.toDomain() }
        val loan = loanEntity.toDomain()
        val summary = amortizationEngine.computeLoanSummary(loan, payments)
        val targetStatus = if (summary.currentOutstandingBalance <= 0.01) {
            LoanStatus.CLOSED
        } else {
            LoanStatus.ACTIVE
        }
        if (loan.status != targetStatus) {
            loanAccountDao.updateLoanAccount(loanEntity.copy(status = targetStatus.name))
        }
    }

    private fun LoanAccountEntity.toDomain(): LoanAccount {
        return LoanAccount(
            id = id,
            name = name,
            loanType = try { LoanType.valueOf(loanType) } catch (e: Exception) { LoanType.OTHER },
            principal = principal,
            annualInterestRate = annualInterestRate,
            emiAmount = emiAmount,
            totalTenureMonths = totalTenureMonths,
            startDateTimestamp = startDateTimestamp,
            collateralOrNotes = collateralOrNotes,
            status = try { LoanStatus.valueOf(status) } catch (e: Exception) { LoanStatus.ACTIVE },
            linkedBankAccountId = linkedBankAccountId,
            bankAccountLast4 = bankAccountLast4,
            lenderName = lenderName,
            loanAccountNumber = loanAccountNumber,
            interestType = try { InterestRateType.valueOf(interestType) } catch (e: Exception) { InterestRateType.FIXED },
            benchmarkRate = benchmarkRate,
            spreadRate = spreadRate,
            createdAt = createdAt
        )
    }

    private fun LoanAccount.toEntity(): LoanAccountEntity {
        return LoanAccountEntity(
            id = id,
            name = name,
            loanType = loanType.name,
            principal = principal,
            annualInterestRate = annualInterestRate,
            emiAmount = emiAmount,
            totalTenureMonths = totalTenureMonths,
            startDateTimestamp = startDateTimestamp,
            collateralOrNotes = collateralOrNotes,
            status = status.name,
            linkedBankAccountId = linkedBankAccountId,
            bankAccountLast4 = bankAccountLast4,
            lenderName = lenderName,
            loanAccountNumber = loanAccountNumber,
            interestType = interestType.name,
            benchmarkRate = benchmarkRate,
            spreadRate = spreadRate,
            createdAt = createdAt
        )
    }

    private fun LoanPaymentEntity.toDomain(): LoanPayment {
        return LoanPayment(
            id = id,
            loanId = loanId,
            paymentDateTimestamp = paymentDateTimestamp,
            amount = amount,
            principalComponent = principalComponent,
            interestComponent = interestComponent,
            paymentType = try { LoanPaymentType.valueOf(paymentType) } catch (e: Exception) { LoanPaymentType.REGULAR_EMI },
            linkedTransactionId = linkedTransactionId,
            notes = notes,
            createdAt = createdAt
        )
    }

    private fun LoanPayment.toEntity(): LoanPaymentEntity {
        return LoanPaymentEntity(
            id = id,
            loanId = loanId,
            paymentDateTimestamp = paymentDateTimestamp,
            amount = amount,
            principalComponent = principalComponent,
            interestComponent = interestComponent,
            paymentType = paymentType.name,
            linkedTransactionId = linkedTransactionId,
            notes = notes,
            createdAt = createdAt
        )
    }
}
