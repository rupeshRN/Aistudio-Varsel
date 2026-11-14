package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.domain.model.FinancialEventStatus
import com.varsel.expensetracker.domain.model.FinancialEventStatusFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialEventStatusCalculatorTest {

    @Test
    fun `expense with zero reimbursement returns PENDING`() {
        val summary = FinancialEventStatusCalculator.calculate(
            totalExpense = 12000.0,
            totalReimbursement = 0.0
        )

        assertEquals(FinancialEventStatus.PENDING, summary.status)
        assertEquals("Pending", summary.badgeLabel)
        assertEquals(12000.0, summary.remainingDue, 0.01)
        assertEquals(0f, summary.recoveryRatio, 0.001f)
        assertTrue(summary.statusMessage.contains("12,000"))
    }

    @Test
    fun `shared room expense with expected roommates share shows roommate due`() {
        val summary = FinancialEventStatusCalculator.calculate(
            totalExpense = 12000.0,
            totalReimbursement = 0.0,
            expectedReimbursement = 8000.0
        )

        assertEquals(FinancialEventStatus.PENDING, summary.status)
        assertEquals(8000.0, summary.targetReimbursement, 0.01)
        assertEquals(8000.0, summary.remainingDue, 0.01)
        assertEquals(4000.0, summary.personalShare ?: 0.0, 0.01)
        assertTrue(summary.statusMessage.contains("8,000"))
        assertTrue(summary.statusMessage.contains("roommates"))
    }

    @Test
    fun `partial reimbursement from one roommate returns PARTIAL with progress and remaining`() {
        val summary = FinancialEventStatusCalculator.calculate(
            totalExpense = 12000.0,
            totalReimbursement = 4000.0,
            expectedReimbursement = 8000.0
        )

        assertEquals(FinancialEventStatus.PARTIAL, summary.status)
        assertEquals("Partial", summary.badgeLabel)
        assertEquals(50, summary.recoveryPercent)
        assertEquals(0.5f, summary.recoveryRatio, 0.01f)
        assertEquals(4000.0, summary.remainingDue, 0.01)
        assertEquals(4000.0, summary.personalShare ?: 0.0, 0.01)
        assertTrue(summary.statusMessage.contains("50%"))
        assertTrue(summary.statusMessage.contains("4,000"))
    }

    @Test
    fun `all roommates paid expected share resolves to SETTLED even when personal share remains`() {
        val summary = FinancialEventStatusCalculator.calculate(
            totalExpense = 15000.0,
            totalReimbursement = 10000.0,
            expectedReimbursement = 10000.0
        )

        assertEquals(FinancialEventStatus.SETTLED, summary.status)
        assertEquals("Settled", summary.badgeLabel)
        assertEquals(0.0, summary.remainingDue, 0.01)
        assertEquals(100, summary.recoveryPercent)
        assertEquals(5000.0, summary.personalShare ?: 0.0, 0.01)
        assertTrue(summary.statusMessage.contains("My share"))
        assertTrue(summary.statusMessage.contains("5,000"))
    }

    @Test
    fun `uneven split within one rupee rounding tolerance auto resolves to SETTLED`() {
        val summary = FinancialEventStatusCalculator.calculate(
            totalExpense = 1000.0,
            totalReimbursement = 666.0,
            expectedReimbursement = 666.67
        )

        assertEquals(FinancialEventStatus.SETTLED, summary.status)
        assertEquals(0.0, summary.remainingDue, 0.01)
    }

    @Test
    fun `manual settled override resolves partial event to SETTLED with zero due`() {
        val summary = FinancialEventStatusCalculator.calculate(
            totalExpense = 9000.0,
            totalReimbursement = 3000.0,
            isManuallySettled = true
        )

        assertEquals(FinancialEventStatus.SETTLED, summary.status)
        assertTrue(summary.isManuallySettled)
        assertEquals(0.0, summary.remainingDue, 0.01)
        assertTrue(summary.statusMessage.contains("Marked settled"))
    }

    @Test
    fun `reimbursement exceeding expected share returns OVERPAID with surplus`() {
        val summary = FinancialEventStatusCalculator.calculate(
            totalExpense = 12000.0,
            totalReimbursement = 8500.0,
            expectedReimbursement = 8000.0
        )

        assertEquals(FinancialEventStatus.OVERPAID, summary.status)
        assertEquals("Overpaid", summary.badgeLabel)
        assertEquals(500.0, summary.surplusAmount, 0.01)
        assertEquals(0.0, summary.remainingDue, 0.01)
        assertTrue(summary.statusMessage.contains("500"))
    }

    @Test
    fun `advance payment before logging room expense returns ADVANCE_RECEIVED`() {
        val summary = FinancialEventStatusCalculator.calculate(
            totalExpense = 0.0,
            totalReimbursement = 3000.0
        )

        assertEquals(FinancialEventStatus.ADVANCE_RECEIVED, summary.status)
        assertEquals("Advance", summary.badgeLabel)
        assertEquals(3000.0, summary.surplusAmount, 0.01)
        assertNull(summary.personalShare)
        assertTrue(summary.statusMessage.contains("Advance"))
    }

    @Test
    fun `status filter matches expected statuses accurately`() {
        assertTrue(FinancialEventStatusFilter.ALL.matches(FinancialEventStatus.PENDING))
        assertTrue(FinancialEventStatusFilter.PENDING.matches(FinancialEventStatus.PENDING))
        assertFalse(FinancialEventStatusFilter.PENDING.matches(FinancialEventStatus.PARTIAL))
        assertTrue(FinancialEventStatusFilter.PARTIAL.matches(FinancialEventStatus.PARTIAL))
        assertTrue(FinancialEventStatusFilter.SETTLED.matches(FinancialEventStatus.SETTLED))
        assertTrue(FinancialEventStatusFilter.OVERPAID.matches(FinancialEventStatus.OVERPAID))
        assertTrue(FinancialEventStatusFilter.ADVANCE.matches(FinancialEventStatus.ADVANCE_RECEIVED))
    }
}
