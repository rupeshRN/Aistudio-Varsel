package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.domain.model.loan.InterestRateType
import com.varsel.expensetracker.domain.model.loan.LoanAccount
import com.varsel.expensetracker.domain.model.loan.LoanPayment
import com.varsel.expensetracker.domain.model.loan.LoanPaymentType
import com.varsel.expensetracker.domain.model.loan.LoanStatus
import com.varsel.expensetracker.domain.model.loan.LoanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoanAmortizationEngineTest {

    private lateinit var engine: LoanAmortizationEngine

    @Before
    fun setup() {
        engine = LoanAmortizationEngine()
    }

    @Test
    fun calculateEmi_standardValues_computesAccurateEmi() {
        // Principal 1,000,000, 8.5% annual rate, 120 months (10 years)
        val emi = engine.calculateEmi(1_000_000.0, 8.5, 120)
        // Expected approx: 12,398
        assertTrue(emi > 12390.0 && emi < 12410.0)
    }

    @Test
    fun calculateTenureMonths_whenRateChanges_computesAccurateDuration() {
        // Outstanding 500,000, new rate 9.0%, current EMI 10,000 -> approx 63 months
        val tenure = engine.calculateTenureMonths(500_000.0, 9.0, 10_000.0)
        assertTrue(tenure in 60..70)
    }

    @Test
    fun generateSchedule_withFloatingRateUpdate_projectsFuturePaymentsCorrectly() {
        val loanPrincipal = 1_200_000.0
        val annualRate = 8.5
        val emi = 15_000.0
        val tenureMonths = 120
        val startDate = 1672531199000L

        // Simulate 2 historical payments
        val payments = listOf(
            LoanPayment(
                id = 1L,
                loanId = 1L,
                paymentDateTimestamp = 1675209599000L,
                amount = 15_000.0,
                principalComponent = 6_500.0,
                interestComponent = 8_500.0,
                paymentType = LoanPaymentType.REGULAR_EMI
            ),
            LoanPayment(
                id = 2L,
                loanId = 1L,
                paymentDateTimestamp = 1677628799000L,
                amount = 15_000.0,
                principalComponent = 6_546.0,
                interestComponent = 8_454.0,
                paymentType = LoanPaymentType.REGULAR_EMI
            )
        )

        val schedule = engine.generateSchedule(
            principal = loanPrincipal,
            annualInterestRate = annualRate,
            emiAmount = emi,
            tenureMonths = tenureMonths,
            startDateTimestamp = startDate,
            payments = payments
        )
        assertEquals(2, schedule.filter { it.isPaid }.size)
        assertTrue(schedule.any { !it.isPaid })

        // Check that unpaid installments start from monthIndex 3
        val unpaidFirst = schedule.first { !it.isPaid }
        assertEquals(3, unpaidFirst.monthIndex)
        assertTrue(unpaidFirst.closingBalance < unpaidFirst.openingBalance)
    }
}
