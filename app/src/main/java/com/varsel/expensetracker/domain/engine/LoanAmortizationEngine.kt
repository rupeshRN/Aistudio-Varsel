package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.domain.model.loan.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

@Singleton
class LoanAmortizationEngine @Inject constructor() {

    /**
     * Calculates the standard Equated Monthly Installment (EMI) using the reducing balance method.
     * Formula: E = P * r * (1 + r)^n / ((1 + r)^n - 1)
     * where:
     *   P = Principal
     *   r = Monthly interest rate (Annual rate / 12 / 100)
     *   n = Tenure in months
     */
    fun calculateEmi(
        principal: Double,
        annualInterestRate: Double,
        tenureMonths: Int
    ): Double {
        if (principal <= 0.0 || tenureMonths <= 0) return 0.0
        if (annualInterestRate <= 0.0) {
            return round((principal / tenureMonths) * 100.0) / 100.0
        }

        val monthlyRate = annualInterestRate / (12.0 * 100.0)
        val factor = (1.0 + monthlyRate).pow(tenureMonths.toDouble())
        val emi = principal * monthlyRate * factor / (factor - 1.0)
        return round(emi * 100.0) / 100.0
    }

    /**
     * Generates a full amortization schedule for the entire loan tenure.
     */
    fun generateSchedule(
        principal: Double,
        annualInterestRate: Double,
        emiAmount: Double,
        tenureMonths: Int,
        startDateTimestamp: Long,
        payments: List<LoanPayment> = emptyList()
    ): List<AmortizationScheduleItem> {
        if (principal <= 0.0 || tenureMonths <= 0) return emptyList()

        val monthlyRate = annualInterestRate / (12.0 * 100.0)
        val schedule = mutableListOf<AmortizationScheduleItem>()
        var currentBalance = principal

        val startLocalDate = Instant.ofEpochMilli(startDateTimestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val paymentsByMonth = payments.filter { it.paymentType == LoanPaymentType.REGULAR_EMI }
            .sortedBy { it.paymentDateTimestamp }

        for (monthIndex in 1..tenureMonths) {
            if (currentBalance <= 0.0) break

            val dueLocalDate = startLocalDate.plusMonths(monthIndex.toLong())
            val dueDateTimestamp = dueLocalDate.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val interestComponent = if (annualInterestRate > 0.0) {
                round((currentBalance * monthlyRate) * 100.0) / 100.0
            } else 0.0

            var principalComponent = emiAmount - interestComponent
            if (principalComponent > currentBalance || monthIndex == tenureMonths) {
                principalComponent = currentBalance
            }
            if (principalComponent < 0.0) principalComponent = 0.0

            val effectiveEmi = principalComponent + interestComponent
            val closingBalance = max(0.0, round((currentBalance - principalComponent) * 100.0) / 100.0)

            val isPaid = monthIndex <= paymentsByMonth.size

            schedule.add(
                AmortizationScheduleItem(
                    monthIndex = monthIndex,
                    dueDateTimestamp = dueDateTimestamp,
                    openingBalance = currentBalance,
                    emiAmount = effectiveEmi,
                    principalComponent = principalComponent,
                    interestComponent = interestComponent,
                    closingBalance = closingBalance,
                    isPaid = isPaid
                )
            )

            currentBalance = closingBalance
        }

        return schedule
    }

    /**
     * Computes the real-time summary of a loan including outstanding balance,
     * principal paid, interest paid, remaining interest, and progress %.
     */
    fun computeLoanSummary(
        loan: LoanAccount,
        payments: List<LoanPayment>
    ): LoanSummary {
        val totalPrincipalPaid = payments.sumOf { it.principalComponent }
        val totalInterestPaid = payments.sumOf { it.interestComponent }
        val prepaymentsTotal = payments
            .filter { it.paymentType == LoanPaymentType.PRE_PAYMENT }
            .sumOf { it.amount }

        val currentOutstandingBalance = max(0.0, loan.principal - totalPrincipalPaid)
        val progressPercentage = if (loan.principal > 0.0) {
            min(100f, ((totalPrincipalPaid / loan.principal) * 100f).toFloat())
        } else 0f

        val completedTenureMonths = payments.count { it.paymentType == LoanPaymentType.REGULAR_EMI }
        
        // Calculate projected total interest on original schedule
        val fullSchedule = generateSchedule(
            principal = loan.principal,
            annualInterestRate = loan.annualInterestRate,
            emiAmount = loan.emiAmount,
            tenureMonths = loan.totalTenureMonths,
            startDateTimestamp = loan.startDateTimestamp
        )
        val totalProjectedInterest = fullSchedule.sumOf { it.interestComponent }

        // Compute remaining schedule based on current outstanding balance
        val remainingTenureMonths = if (currentOutstandingBalance > 0.0 && loan.emiAmount > 0.0) {
            val remainingSchedule = generateSchedule(
                principal = currentOutstandingBalance,
                annualInterestRate = loan.annualInterestRate,
                emiAmount = loan.emiAmount,
                tenureMonths = max(1, loan.totalTenureMonths - completedTenureMonths),
                startDateTimestamp = System.currentTimeMillis()
            )
            remainingSchedule.size
        } else 0

        val totalRemainingInterest = if (currentOutstandingBalance > 0.0 && loan.emiAmount > 0.0) {
            val remainingSchedule = generateSchedule(
                principal = currentOutstandingBalance,
                annualInterestRate = loan.annualInterestRate,
                emiAmount = loan.emiAmount,
                tenureMonths = max(1, loan.totalTenureMonths - completedTenureMonths),
                startDateTimestamp = System.currentTimeMillis()
            )
            remainingSchedule.sumOf { it.interestComponent }
        } else 0.0

        // Calculate next EMI due date
        val nextEmiDueDateTimestamp = if (currentOutstandingBalance > 0.0) {
            val startLocalDate = Instant.ofEpochMilli(loan.startDateTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val nextMonthIndex = completedTenureMonths + 1
            val nextDueDate = startLocalDate.plusMonths(nextMonthIndex.toLong())
            nextDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else null

        return LoanSummary(
            loan = loan,
            currentOutstandingBalance = currentOutstandingBalance,
            totalPrincipalPaid = totalPrincipalPaid,
            totalInterestPaid = totalInterestPaid,
            totalProjectedInterest = totalProjectedInterest,
            totalRemainingInterest = totalRemainingInterest,
            completedTenureMonths = completedTenureMonths,
            remainingTenureMonths = remainingTenureMonths,
            nextEmiDueDateTimestamp = nextEmiDueDateTimestamp,
            nextEmiAmount = if (currentOutstandingBalance > 0.0) min(loan.emiAmount, currentOutstandingBalance) else 0.0,
            progressPercentage = progressPercentage,
            paymentsCount = payments.size,
            prepaymentsTotal = prepaymentsTotal
        )
    }

    /**
     * Simulates the impact of making a lump-sum prepayment or extra monthly payment.
     */
    fun simulatePrepayment(
        loan: LoanAccount,
        currentOutstandingBalance: Double,
        extraLumpSum: Double,
        extraMonthly: Double,
        reductionType: PrepaymentReductionType
    ): PrepaymentSimulationResult {
        val balanceAfterLumpSum = max(0.0, currentOutstandingBalance - extraLumpSum)
        val monthlyRate = loan.annualInterestRate / (12.0 * 100.0)

        // Baseline: what would remaining schedule cost with normal EMI?
        val baseSchedule = generateSchedule(
            principal = currentOutstandingBalance,
            annualInterestRate = loan.annualInterestRate,
            emiAmount = loan.emiAmount,
            tenureMonths = max(1, loan.totalTenureMonths),
            startDateTimestamp = System.currentTimeMillis()
        )
        val originalTotalInterest = baseSchedule.sumOf { it.interestComponent }
        val originalTenureMonths = baseSchedule.size

        if (balanceAfterLumpSum <= 0.0) {
            return PrepaymentSimulationResult(
                extraLumpSum = extraLumpSum,
                extraMonthly = extraMonthly,
                originalTenureMonths = originalTenureMonths,
                newTenureMonths = 0,
                monthsSaved = originalTenureMonths,
                originalTotalInterest = originalTotalInterest,
                newTotalInterest = 0.0,
                interestSaved = originalTotalInterest,
                newEmiAmount = 0.0
            )
        }

        return when (reductionType) {
            PrepaymentReductionType.REDUCE_TENURE -> {
                val effectiveMonthlyPayment = loan.emiAmount + extraMonthly
                var runningBal = balanceAfterLumpSum
                var monthsCount = 0
                var simInterest = 0.0

                while (runningBal > 0.0 && monthsCount < 600) { // cap 50 years safety
                    monthsCount++
                    val interest = if (loan.annualInterestRate > 0.0) runningBal * monthlyRate else 0.0
                    val principal = min(runningBal, effectiveMonthlyPayment - interest)
                    simInterest += interest
                    runningBal = max(0.0, runningBal - principal)
                }

                val monthsSaved = max(0, originalTenureMonths - monthsCount)
                val interestSaved = max(0.0, originalTotalInterest - simInterest)

                PrepaymentSimulationResult(
                    extraLumpSum = extraLumpSum,
                    extraMonthly = extraMonthly,
                    originalTenureMonths = originalTenureMonths,
                    newTenureMonths = monthsCount,
                    monthsSaved = monthsSaved,
                    originalTotalInterest = originalTotalInterest,
                    newTotalInterest = simInterest,
                    interestSaved = interestSaved,
                    newEmiAmount = effectiveMonthlyPayment
                )
            }
            PrepaymentReductionType.REDUCE_EMI -> {
                val newEmi = calculateEmi(
                    principal = balanceAfterLumpSum,
                    annualInterestRate = loan.annualInterestRate,
                    tenureMonths = originalTenureMonths
                )
                val newSchedule = generateSchedule(
                    principal = balanceAfterLumpSum,
                    annualInterestRate = loan.annualInterestRate,
                    emiAmount = newEmi,
                    tenureMonths = originalTenureMonths,
                    startDateTimestamp = System.currentTimeMillis()
                )
                val simInterest = newSchedule.sumOf { it.interestComponent }
                val interestSaved = max(0.0, originalTotalInterest - simInterest)

                PrepaymentSimulationResult(
                    extraLumpSum = extraLumpSum,
                    extraMonthly = extraMonthly,
                    originalTenureMonths = originalTenureMonths,
                    newTenureMonths = originalTenureMonths,
                    monthsSaved = 0,
                    originalTotalInterest = originalTotalInterest,
                    newTotalInterest = simInterest,
                    interestSaved = interestSaved,
                    newEmiAmount = newEmi
                )
            }
        }
    }
}
