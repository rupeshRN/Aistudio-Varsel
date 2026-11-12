package com.varsel.expensetracker.ui.reports
 
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class CategoryDrillDownStateTest {

    private val sampleItems = listOf(
        CategoryDrillDownItem(
            transactionId = 1L,
            description = "Swiggy Food Delivery",
            category = "Food & Dining",
            dateTimestamp = 1700000000000L,
            amount = 450.0,
            isExpense = true,
            accountLast4 = "1234"
        ),
        CategoryDrillDownItem(
            transactionId = 2L,
            description = "Zomato Restaurant",
            category = "Food & Dining",
            dateTimestamp = 1700010000000L,
            amount = 850.0,
            isExpense = true,
            accountLast4 = "1234"
        ),
        CategoryDrillDownItem(
            transactionId = 3L,
            description = "Starbucks Coffee",
            category = "Food & Dining",
            dateTimestamp = 1700020000000L,
            amount = 300.0,
            isExpense = true,
            accountLast4 = "5678"
        ),
        CategoryDrillDownItem(
            transactionId = 4L,
            description = "Swiggy Instamart Snacks",
            category = "Food & Dining",
            dateTimestamp = 1700030000000L,
            amount = 400.0,
            isExpense = true,
            accountLast4 = "5678"
        )
    )

    private val totalCategory = 2000.0 // 450 + 850 + 300 + 400 = 2000.0

    @Test
    fun testDefaultStateWithoutSearch() {
        val state = CategoryDrillDownState(
            isVisible = true,
            categoryName = "Food & Dining",
            flow = ReportsFlow.EXPENSES,
            totalCategoryAmount = totalCategory,
            percentOfTotal = 25.0, // 25% of all expenses
            periodLabel = "March 2026",
            month = YearMonth.of(2026, 3),
            items = sampleItems,
            searchQuery = ""
        )

        assertFalse(state.isSearching)
        assertEquals(4, state.filteredItems.size)
        assertEquals(2000.0, state.displayAmount, 0.001)
        assertEquals(100.0, state.searchPercentOfCategory, 0.001)
    }

    @Test
    fun testDynamicFilteringAndAmountWhenSearching() {
        val state = CategoryDrillDownState(
            isVisible = true,
            categoryName = "Food & Dining",
            flow = ReportsFlow.EXPENSES,
            totalCategoryAmount = totalCategory,
            percentOfTotal = 25.0,
            periodLabel = "March 2026",
            month = YearMonth.of(2026, 3),
            items = sampleItems,
            searchQuery = "swiggy"
        )

        assertTrue(state.isSearching)
        // Matches item 1 (450.0) and item 4 (400.0) -> total 850.0
        assertEquals(2, state.filteredItems.size)
        assertEquals(850.0, state.displayAmount, 0.001)

        // Percentage within that category: (850 / 2000) * 100 = 42.5%
        assertEquals(42.5, state.searchPercentOfCategory, 0.001)
    }

    @Test
    fun testSingleItemSearchContribution() {
        val state = CategoryDrillDownState(
            isVisible = true,
            categoryName = "Food & Dining",
            flow = ReportsFlow.EXPENSES,
            totalCategoryAmount = totalCategory,
            percentOfTotal = 25.0,
            periodLabel = "March 2026",
            month = YearMonth.of(2026, 3),
            items = sampleItems,
            searchQuery = "starbucks"
        )

        assertTrue(state.isSearching)
        assertEquals(1, state.filteredItems.size)
        assertEquals(300.0, state.displayAmount, 0.001)

        // (300 / 2000) * 100 = 15.0%
        assertEquals(15.0, state.searchPercentOfCategory, 0.001)
    }

    @Test
    fun testNoMatchesSearchQuery() {
        val state = CategoryDrillDownState(
            isVisible = true,
            categoryName = "Food & Dining",
            flow = ReportsFlow.EXPENSES,
            totalCategoryAmount = totalCategory,
            percentOfTotal = 25.0,
            periodLabel = "March 2026",
            month = YearMonth.of(2026, 3),
            items = sampleItems,
            searchQuery = "nonexistent merchant"
        )

        assertTrue(state.isSearching)
        assertEquals(0, state.filteredItems.size)
        assertEquals(0.0, state.displayAmount, 0.001)
        assertEquals(0.0, state.searchPercentOfCategory, 0.001)
    }

    @Test
    fun testSearchByAccountLast4() {
        val state = CategoryDrillDownState(
            isVisible = true,
            categoryName = "Food & Dining",
            flow = ReportsFlow.EXPENSES,
            totalCategoryAmount = totalCategory,
            percentOfTotal = 25.0,
            periodLabel = "March 2026",
            month = YearMonth.of(2026, 3),
            items = sampleItems,
            searchQuery = "5678"
        )

        assertTrue(state.isSearching)
        // Items 3 and 4 have accountLast4 = "5678" -> 300 + 400 = 700.0
        assertEquals(2, state.filteredItems.size)
        assertEquals(700.0, state.displayAmount, 0.001)
        assertEquals(35.0, state.searchPercentOfCategory, 0.001)
    }

    @Test
    fun testComparisonModeMonthGroupingAndSubtotals() {
        val multiMonthItems = listOf(
            CategoryDrillDownItem(
                transactionId = 1L,
                description = "Swiggy Delivery",
                category = "Food & Dining",
                dateTimestamp = 1730000000000L,
                amount = 500.0,
                isExpense = true,
                month = YearMonth.of(2026, 11)
            ),
            CategoryDrillDownItem(
                transactionId = 2L,
                description = "Grocery Mart",
                category = "Food & Dining",
                dateTimestamp = 1729000000000L,
                amount = 1500.0,
                isExpense = true,
                month = YearMonth.of(2026, 11)
            ),
            CategoryDrillDownItem(
                transactionId = 3L,
                description = "Dine Out",
                category = "Food & Dining",
                dateTimestamp = 1727000000000L,
                amount = 1200.0,
                isExpense = true,
                month = YearMonth.of(2026, 10)
            ),
            CategoryDrillDownItem(
                transactionId = 4L,
                description = "Cafe Coffee",
                category = "Food & Dining",
                dateTimestamp = 1725000000000L,
                amount = 300.0,
                isExpense = true,
                month = YearMonth.of(2026, 9)
            )
        )

        val state = CategoryDrillDownState(
            isVisible = true,
            categoryName = "Food & Dining",
            flow = ReportsFlow.EXPENSES,
            totalCategoryAmount = 3500.0,
            percentOfTotal = 20.0,
            periodLabel = "Sep 2026 – Nov 2026 (3 Months)",
            month = YearMonth.of(2026, 11),
            items = multiMonthItems,
            isComparisonMode = true,
            comparisonMonths = listOf(
                YearMonth.of(2026, 9),
                YearMonth.of(2026, 10),
                YearMonth.of(2026, 11)
            )
        )

        val groups = state.itemsByMonth
        assertEquals(3, groups.size)

        // Latest month first (Nov 2026)
        val novItems = groups[YearMonth.of(2026, 11)]
        assertEquals(2, novItems?.size)
        assertEquals(2000.0, novItems?.sumOf { it.amount } ?: 0.0, 0.001)

        // Oct 2026
        val octItems = groups[YearMonth.of(2026, 10)]
        assertEquals(1, octItems?.size)
        assertEquals(1200.0, octItems?.sumOf { it.amount } ?: 0.0, 0.001)

        // Sep 2026
        val sepItems = groups[YearMonth.of(2026, 9)]
        assertEquals(1, sepItems?.size)
        assertEquals(300.0, sepItems?.sumOf { it.amount } ?: 0.0, 0.001)
    }

    @Test
    fun testComparisonModeSelectedMonthFilter() {
        val multiMonthItems = listOf(
            CategoryDrillDownItem(
                transactionId = 1L,
                description = "Swiggy Delivery",
                category = "Food & Dining",
                dateTimestamp = 1730000000000L,
                amount = 500.0,
                isExpense = true,
                month = YearMonth.of(2026, 11)
            ),
            CategoryDrillDownItem(
                transactionId = 2L,
                description = "Dine Out",
                category = "Food & Dining",
                dateTimestamp = 1727000000000L,
                amount = 1200.0,
                isExpense = true,
                month = YearMonth.of(2026, 10)
            )
        )

        val state = CategoryDrillDownState(
            isVisible = true,
            categoryName = "Food & Dining",
            flow = ReportsFlow.EXPENSES,
            totalCategoryAmount = 1700.0,
            percentOfTotal = 15.0,
            periodLabel = "Oct 2026 – Nov 2026 (2 Months)",
            month = YearMonth.of(2026, 11),
            items = multiMonthItems,
            isComparisonMode = true,
            selectedMonthFilter = YearMonth.of(2026, 10)
        )

        // Filtered items should only contain October transactions
        assertEquals(1, state.filteredItems.size)
        assertEquals(1200.0, state.displayAmount, 0.001)
        assertEquals("Dine Out", state.filteredItems.first().description)
    }
}
