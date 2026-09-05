package com.varsel.expensetracker.ui.budget

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.budget.model.BudgetHistoryUiModel
import com.varsel.expensetracker.ui.budget.model.BudgetPastPeriodUiModel
import com.varsel.expensetracker.ui.budget.model.BudgetTrendPoint
import com.varsel.expensetracker.ui.budget.model.BudgetUiModel
import com.varsel.expensetracker.ui.budget.model.DailySpendingPoint
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object BudgetCalculator {

    private val currencyFormat = DecimalFormat("₹#,##0.##")
    private val wholeCurrencyFormat = DecimalFormat("₹#,##0")

    fun formatCurrency(amount: Double, round: Boolean = false): String {
        return if (round) {
            wholeCurrencyFormat.format(amount.roundToInt())
        } else {
            currencyFormat.format(amount)
        }
    }

    data class PeriodBounds(
        val startMillis: Long,
        val endMillis: Long,
        val totalDays: Int,
        val daysPassed: Int,
        val daysRemaining: Int,
        val startFormatted: String,
        val endFormatted: String,
        val startFull: String,
        val endFull: String
    )

    fun calculatePeriodBounds(
        period: String,
        startDayOfMonth: Int,
        referenceTime: Long = System.currentTimeMillis()
    ): PeriodBounds {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceTime
        }

        when (period.uppercase()) {
            "WEEKLY" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                val totalDays = 7
                val daysPassed = (((referenceTime - start).coerceAtLeast(0L)) / TimeUnit.DAYS.toMillis(1)).toInt() + 1
                val daysRemaining = (totalDays - daysPassed).coerceAtLeast(0)

                val shortDf = SimpleDateFormat("MMM d", Locale.getDefault())
                val fullDf = SimpleDateFormat("MMMM d", Locale.getDefault())

                return PeriodBounds(
                    startMillis = start,
                    endMillis = end,
                    totalDays = totalDays,
                    daysPassed = daysPassed.coerceIn(1, totalDays),
                    daysRemaining = daysRemaining,
                    startFormatted = shortDf.format(start),
                    endFormatted = shortDf.format(end),
                    startFull = fullDf.format(start),
                    endFull = fullDf.format(end)
                )
            }
            "YEARLY" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                val totalDays = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
                cal.set(Calendar.DAY_OF_YEAR, totalDays)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                val daysPassed = (((referenceTime - start).coerceAtLeast(0L)) / TimeUnit.DAYS.toMillis(1)).toInt() + 1
                val daysRemaining = (totalDays - daysPassed).coerceAtLeast(0)

                val shortDf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val fullDf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

                return PeriodBounds(
                    startMillis = start,
                    endMillis = end,
                    totalDays = totalDays,
                    daysPassed = daysPassed.coerceIn(1, totalDays),
                    daysRemaining = daysRemaining,
                    startFormatted = shortDf.format(start),
                    endFormatted = shortDf.format(end),
                    startFull = fullDf.format(start),
                    endFull = fullDf.format(end)
                )
            }
            else -> {
                // Monthly
                val safeStartDay = startDayOfMonth.coerceIn(1, 28)
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)

                val startCal = Calendar.getInstance().apply {
                    timeInMillis = referenceTime
                    if (currentDay < safeStartDay) {
                        add(Calendar.MONTH, -1)
                    }
                    set(Calendar.DAY_OF_MONTH, safeStartDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val endCal = (startCal.clone() as Calendar).apply {
                    add(Calendar.MONTH, 1)
                    add(Calendar.MILLISECOND, -1)
                }

                val start = startCal.timeInMillis
                val end = endCal.timeInMillis

                val totalDays = ((end - start) / TimeUnit.DAYS.toMillis(1)).toInt() + 1
                val daysPassed = (((referenceTime - start).coerceAtLeast(0L)) / TimeUnit.DAYS.toMillis(1)).toInt() + 1
                val daysRemaining = (totalDays - daysPassed).coerceAtLeast(0)

                val shortDf = SimpleDateFormat("MMM d", Locale.getDefault())
                val fullDf = SimpleDateFormat("MMMM d", Locale.getDefault())

                return PeriodBounds(
                    startMillis = start,
                    endMillis = end,
                    totalDays = totalDays,
                    daysPassed = daysPassed.coerceIn(1, totalDays),
                    daysRemaining = daysRemaining,
                    startFormatted = shortDf.format(start),
                    endFormatted = shortDf.format(end),
                    startFull = fullDf.format(start),
                    endFull = fullDf.format(end)
                )
            }
        }
    }

    fun computeBudgetUiModel(
        budget: BudgetEntity,
        transactions: List<Transaction>,
        referenceTime: Long = System.currentTimeMillis()
    ): BudgetUiModel {
        val bounds = calculatePeriodBounds(
            period = budget.period,
            startDayOfMonth = budget.startDayOfMonth,
            referenceTime = referenceTime
        )

        // Filter contributing transactions
        val isSavings = budget.budgetType.equals("SAVINGS", ignoreCase = true)
        val isAllCategories = budget.categoryName.equals("ALL", ignoreCase = true) ||
                budget.categoryName.equals("All Expenses", ignoreCase = true) ||
                budget.categoryName.equals("All Savings", ignoreCase = true) ||
                budget.categoryName.equals("Total Savings", ignoreCase = true)

        val contributingTransactions = transactions.filter { tx ->
            val matchesType = if (isSavings) {
                tx.type == TransactionType.INCOME || tx.category.contains("Saving", ignoreCase = true) || tx.category.contains("Investment", ignoreCase = true)
            } else {
                tx.type == TransactionType.EXPENSE
            }
            matchesType &&
                    !tx.isTransfer &&
                    tx.dateTimestamp in bounds.startMillis..bounds.endMillis &&
                    (isAllCategories || tx.category.equals(budget.categoryName, ignoreCase = true))
        }.sortedByDescending { it.dateTimestamp }

        val amountSpent = contributingTransactions.sumOf { it.amount }
        val amountLeft = (budget.amount - amountSpent).coerceAtLeast(0.0)
        val isOverBudget = if (isSavings) false else (amountSpent > budget.amount)
        val overBudgetAmount = if (isOverBudget) amountSpent - budget.amount else 0.0

        val percentSpent = if (budget.amount > 0) {
            ((amountSpent / budget.amount) * 100).toInt()
        } else 0

        val spentRatio = if (budget.amount > 0) {
            (amountSpent / budget.amount).toFloat()
        } else 0f

        val todayRatio = if (bounds.totalDays > 0) {
            (bounds.daysPassed.toFloat() / bounds.totalDays.toFloat()).coerceIn(0f, 1f)
        } else 0f

        val dailyAllowance = if (bounds.daysRemaining > 0) {
            amountLeft / bounds.daysRemaining
        } else 0.0

        val targetDailyAllowance = if (bounds.totalDays > 0) {
            budget.amount / bounds.totalDays
        } else 0.0

        val dailyAllowanceText = if (isSavings) {
            when {
                amountSpent >= budget.amount -> "Savings goal reached! 🎉"
                bounds.daysRemaining == 0 -> "Last day of period"
                else -> "Save ${formatCurrency(dailyAllowance, round = true)}/day for ${bounds.daysRemaining} more days"
            }
        } else {
            when {
                isOverBudget -> "Over budget by ${formatCurrency(overBudgetAmount, round = true)}"
                bounds.daysRemaining == 0 -> "Last day of period"
                else -> "You can spend ${formatCurrency(dailyAllowance, round = true)}/day for ${bounds.daysRemaining} more days"
            }
        }

        // Daily spending points for trend
        val dailyMap = mutableMapOf<Int, Double>()
        val cal = Calendar.getInstance()
        contributingTransactions.forEach { tx ->
            cal.timeInMillis = tx.dateTimestamp
            val day = cal.get(Calendar.DAY_OF_MONTH)
            dailyMap[day] = (dailyMap[day] ?: 0.0) + tx.amount
        }

        val dailyPoints = (1..bounds.daysPassed).map { day ->
            DailySpendingPoint(
                dayNumber = day,
                dayLabel = "Day $day",
                amount = dailyMap[day] ?: 0.0
            )
        }

        val categoryIconKey = budget.iconName
            ?: CategoryIconCatalog.iconKeyForCategory(budget.categoryName)
        val categoryIcon = CategoryIconCatalog.iconFor(categoryIconKey)
        val categoryColorHex = budget.colorHex
            ?: CategoryIconCatalog.getCategory(budget.categoryName)?.colorHex
            ?: "#3F51B5"

        return BudgetUiModel(
            budget = budget,
            amountSpent = amountSpent,
            amountLeft = amountLeft,
            percentSpent = percentSpent,
            spentRatio = spentRatio,
            periodLabel = "${bounds.startFormatted} - ${bounds.endFormatted}",
            periodStartFormatted = bounds.startFormatted,
            periodEndFormatted = bounds.endFormatted,
            periodStartFull = bounds.startFull,
            periodEndFull = bounds.endFull,
            todayRatio = todayRatio,
            daysRemaining = bounds.daysRemaining,
            totalDaysInPeriod = bounds.totalDays,
            dailyAllowance = dailyAllowance,
            dailyAllowanceText = dailyAllowanceText,
            isOverBudget = isOverBudget,
            overBudgetAmount = overBudgetAmount,
            categoryIcon = categoryIcon,
            categoryColorHex = categoryColorHex,
            transactions = contributingTransactions,
            dailySpending = dailyPoints,
            targetDailyAllowance = targetDailyAllowance
        )
    }

    fun computeBudgetHistory(
        budget: BudgetEntity,
        transactions: List<Transaction>,
        referenceTime: Long = System.currentTimeMillis()
    ): BudgetHistoryUiModel {
        val isSavings = budget.budgetType.equals("SAVINGS", ignoreCase = true)
        val isAllCategories = budget.categoryName.equals("ALL", ignoreCase = true) ||
                budget.categoryName.equals("All Expenses", ignoreCase = true) ||
                budget.categoryName.equals("All Savings", ignoreCase = true) ||
                budget.categoryName.equals("Total Savings", ignoreCase = true)

        val allCategoryExpenses = transactions.filter { tx ->
            val matchesType = if (isSavings) {
                tx.type == TransactionType.INCOME || tx.category.contains("Saving", ignoreCase = true) || tx.category.contains("Investment", ignoreCase = true)
            } else {
                tx.type == TransactionType.EXPENSE
            }
            matchesType &&
                    !tx.isTransfer &&
                    (isAllCategories || tx.category.equals(budget.categoryName, ignoreCase = true))
        }

        val totalAllTimeSpent = allCategoryExpenses.sumOf { it.amount }

        // Compute past 5 monthly periods
        val pastPeriods = mutableListOf<BudgetPastPeriodUiModel>()
        val trendPoints = mutableListOf<BudgetTrendPoint>()

        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val fullMonthFormat = SimpleDateFormat("MMMM 1", Locale.getDefault())
        val rangeFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceTime
        }

        // We generate 5 months backwards from current month: e.g. -4, -3, -2, -1, 0
        for (i in 4 downTo 0) {
            val monthCal = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, -i)
            }
            val ref = monthCal.timeInMillis
            val bounds = calculatePeriodBounds(
                period = "MONTHLY",
                startDayOfMonth = budget.startDayOfMonth,
                referenceTime = ref
            )

            val monthExpenses = allCategoryExpenses.filter {
                it.dateTimestamp in bounds.startMillis..bounds.endMillis
            }
            val spent = monthExpenses.sumOf { it.amount }
            val left = (budget.amount - spent).coerceAtLeast(0.0)
            val percent = if (budget.amount > 0) ((spent / budget.amount) * 100).toInt() else 0
            val ratio = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
            val isOver = spent > budget.amount

            val title = if (i == 0) "Current Period" else fullMonthFormat.format(bounds.startMillis)
            val range = "${rangeFormat.format(bounds.startMillis)} - ${rangeFormat.format(bounds.endMillis)}"

            val periodModel = BudgetPastPeriodUiModel(
                periodTitle = title,
                dateRangeFormatted = range,
                amountSpent = spent,
                budgetLimit = budget.amount,
                amountLeft = left,
                percentSpent = percent,
                spentRatio = ratio,
                isOverBudget = isOver
            )
            pastPeriods.add(periodModel)

            trendPoints.add(
                BudgetTrendPoint(
                    monthLabel = monthFormat.format(bounds.startMillis),
                    amount = spent
                )
            )
        }

        // Reverse pastPeriods so most recent is at the top
        val sortedPastPeriods = pastPeriods.reversed()

        val avgSpent = if (trendPoints.isNotEmpty()) {
            trendPoints.map { it.amount }.average()
        } else 0.0

        val categoryIconKey = budget.iconName
            ?: CategoryIconCatalog.iconKeyForCategory(budget.categoryName)
        val categoryIcon = CategoryIconCatalog.iconFor(categoryIconKey)
        val categoryColorHex = budget.colorHex
            ?: CategoryIconCatalog.getCategory(budget.categoryName)?.colorHex
            ?: "#3F51B5"

        return BudgetHistoryUiModel(
            budget = budget,
            categoryAverageSpent = avgSpent,
            totalAllTimeSpent = totalAllTimeSpent,
            pastPeriods = sortedPastPeriods,
            trendPoints = trendPoints,
            categoryIcon = categoryIcon,
            categoryColorHex = categoryColorHex
        )
    }
}
