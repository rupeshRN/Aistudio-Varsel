package com.varsel.expensetracker.ui.budget.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.domain.model.Transaction

data class DailySpendingPoint(
    val dayNumber: Int,
    val dayLabel: String,
    val amount: Double
)

data class BudgetUiModel(
    val budget: BudgetEntity,
    val amountSpent: Double,
    val amountLeft: Double,
    val percentSpent: Int,
    val spentRatio: Float,
    val periodLabel: String,
    val periodStartFormatted: String,
    val periodEndFormatted: String,
    val periodStartFull: String,
    val periodEndFull: String,
    val todayRatio: Float,
    val daysRemaining: Int,
    val totalDaysInPeriod: Int,
    val dailyAllowance: Double,
    val dailyAllowanceText: String,
    val isOverBudget: Boolean,
    val overBudgetAmount: Double,
    val categoryIcon: ImageVector,
    val categoryColorHex: String,
    val transactions: List<Transaction>,
    val dailySpending: List<DailySpendingPoint>,
    val targetDailyAllowance: Double
)

data class BudgetTrendPoint(
    val monthLabel: String,
    val amount: Double
)

data class BudgetPastPeriodUiModel(
    val periodTitle: String,
    val dateRangeFormatted: String,
    val amountSpent: Double,
    val budgetLimit: Double,
    val amountLeft: Double,
    val percentSpent: Int,
    val spentRatio: Float,
    val isOverBudget: Boolean
)

data class BudgetHistoryUiModel(
    val budget: BudgetEntity,
    val categoryAverageSpent: Double,
    val totalAllTimeSpent: Double,
    val pastPeriods: List<BudgetPastPeriodUiModel>,
    val trendPoints: List<BudgetTrendPoint>,
    val categoryIcon: ImageVector,
    val categoryColorHex: String
)
