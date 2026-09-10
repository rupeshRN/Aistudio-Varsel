package com.varsel.expensetracker.ui.mapper

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DashboardUiMapperPeriodTest {

    private val transactionUiMapper = TransactionUiMapper()
    private val mapper = DashboardUiMapper(transactionUiMapper)

    @Test
    fun testPeriodFilteringAllTime() {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        cal.add(Calendar.MONTH, -5)
        val fiveMonthsAgo = cal.timeInMillis

        val txns = listOf(
            Transaction(
                id = 1L,
                amount = 1000.0,
                type = TransactionType.INCOME,
                role = TransactionRole.NORMAL,
                dateTimestamp = now,
                description = "Salary Now",
                category = "Income"
            ),
            Transaction(
                id = 2L,
                amount = 2000.0,
                type = TransactionType.INCOME,
                role = TransactionRole.NORMAL,
                dateTimestamp = fiveMonthsAgo,
                description = "Salary Past",
                category = "Income"
            )
        )

        val resultAllTime = mapper.map(txns, emptyList(), period = "All Time")
        assertEquals(3000.0, resultAllTime.balanceSummary.totalIncome, 0.01)
        assertEquals("All Time", resultAllTime.balanceSummary.periodLabel)

        val resultThisMonth = mapper.map(txns, emptyList(), period = "This Month")
        assertEquals(1000.0, resultThisMonth.balanceSummary.totalIncome, 0.01)
        assertEquals("This Month", resultThisMonth.balanceSummary.periodLabel)

        // Net Liquid Balance must stay the same (3000.0) regardless of selected period!
        assertEquals(3000.0, resultAllTime.balanceSummary.totalBalance, 0.01)
        assertEquals(3000.0, resultThisMonth.balanceSummary.totalBalance, 0.01)
    }

    @Test
    fun testHistoricalTransactionsWithoutCurrentMonthData() {
        // Historical statement: August 2025
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2025)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        val aug2025 = cal.timeInMillis

        cal.set(Calendar.MONTH, Calendar.FEBRUARY)
        val feb2025 = cal.timeInMillis

        val txns = listOf(
            Transaction(
                id = 1L,
                amount = 1000.0,
                type = TransactionType.INCOME,
                role = TransactionRole.NORMAL,
                dateTimestamp = aug2025,
                description = "Aug Income",
                category = "Income"
            ),
            Transaction(
                id = 2L,
                amount = 2000.0,
                type = TransactionType.INCOME,
                role = TransactionRole.NORMAL,
                dateTimestamp = feb2025,
                description = "Feb Income",
                category = "Income"
            )
        )

        val resultThisYear = mapper.map(txns, emptyList(), period = "This Year")
        assertEquals(3000.0, resultThisYear.balanceSummary.totalIncome, 0.01)
        assertEquals("This Year", resultThisYear.balanceSummary.periodLabel)

        val resultLast6Months = mapper.map(txns, emptyList(), period = "Last 6 Months")
        assertEquals(1000.0, resultLast6Months.balanceSummary.totalIncome, 0.01)
        assertEquals("Last 6 Months", resultLast6Months.balanceSummary.periodLabel)

        val resultLast3Months = mapper.map(txns, emptyList(), period = "Last 3 Months")
        assertEquals(1000.0, resultLast3Months.balanceSummary.totalIncome, 0.01)
        assertEquals("Last 3 Months", resultLast3Months.balanceSummary.periodLabel)
    }
}
