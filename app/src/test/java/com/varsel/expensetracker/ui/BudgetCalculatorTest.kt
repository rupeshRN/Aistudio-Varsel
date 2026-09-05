package com.varsel.expensetracker.ui

import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.budget.BudgetCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class BudgetCalculatorTest {

    @Test
    fun testCalculateMonthlyBounds() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 4, 12, 0, 0)
        }
        val bounds = BudgetCalculator.calculatePeriodBounds("MONTHLY", 1, cal.timeInMillis)

        assertEquals(30, bounds.totalDays)
        assertEquals(4, bounds.daysPassed)
        assertEquals(26, bounds.daysRemaining)
    }

    @Test
    fun testComputeBudgetUiModel_WithinBudget() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 4, 12, 0, 0)
        }
        val refTime = cal.timeInMillis

        val budget = BudgetEntity(
            id = 1L,
            name = "Food",
            categoryName = "Dining & Food",
            amount = 3000.0,
            period = "MONTHLY",
            startDayOfMonth = 1
        )

        // Add 1 transaction for 427.0 on Sep 2
        val txCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 2, 10, 0, 0)
        }
        val transactions = listOf(
            Transaction(
                id = 101L,
                amount = 427.0,
                type = TransactionType.EXPENSE,
                category = "Dining & Food",
                dateTimestamp = txCal.timeInMillis,
                description = "Restaurant dinner"
            ),
            Transaction(
                id = 102L,
                amount = 1500.0,
                type = TransactionType.EXPENSE,
                category = "Groceries", // Different category
                dateTimestamp = txCal.timeInMillis,
                description = "Supermarket"
            )
        )

        val uiModel = BudgetCalculator.computeBudgetUiModel(budget, transactions, refTime)

        assertEquals(427.0, uiModel.amountSpent, 0.01)
        assertEquals(2573.0, uiModel.amountLeft, 0.01)
        assertEquals(14, uiModel.percentSpent)
        assertFalse(uiModel.isOverBudget)
        assertEquals(26, uiModel.daysRemaining)
        assertTrue(uiModel.dailyAllowanceText.contains("26 more days"))
        assertTrue(uiModel.dailyAllowanceText.contains("₹99") || uiModel.dailyAllowanceText.contains("₹98"))
    }

    @Test
    fun testComputeBudgetUiModel_OverBudget() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 4, 12, 0, 0)
        }
        val refTime = cal.timeInMillis

        val budget = BudgetEntity(
            id = 2L,
            name = "Entertainment",
            categoryName = "Entertainment",
            amount = 130.0,
            period = "MONTHLY",
            startDayOfMonth = 1
        )

        val txCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 3, 14, 0, 0)
        }
        val transactions = listOf(
            Transaction(
                id = 201L,
                amount = 180.0,
                type = TransactionType.EXPENSE,
                category = "Entertainment",
                dateTimestamp = txCal.timeInMillis,
                description = "Cinema Tickets"
            )
        )

        val uiModel = BudgetCalculator.computeBudgetUiModel(budget, transactions, refTime)

        assertTrue(uiModel.isOverBudget)
        assertEquals(50.0, uiModel.overBudgetAmount, 0.01)
        assertEquals(0.0, uiModel.amountLeft, 0.01)
        assertTrue(uiModel.dailyAllowanceText.contains("Over budget by ₹50"))
    }

    @Test
    fun testComputeBudgetHistory() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 4, 12, 0, 0)
        }
        val refTime = cal.timeInMillis

        val budget = BudgetEntity(
            id = 1L,
            name = "Food",
            categoryName = "Dining & Food",
            amount = 3000.0,
            period = "MONTHLY",
            startDayOfMonth = 1
        )

        val history = BudgetCalculator.computeBudgetHistory(budget, emptyList(), refTime)

        assertEquals(5, history.trendPoints.size)
        assertEquals(5, history.pastPeriods.size)
        assertEquals("Current Period", history.pastPeriods.first().periodTitle)
    }
}
