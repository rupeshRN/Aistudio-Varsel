package com.varsel.expensetracker.ui.heatmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.ui.mapper.TransactionUiMapper
import com.varsel.expensetracker.util.BankInfoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarHeatmapViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionUiMapper: TransactionUiMapper
) : ViewModel() {

    private val selectedYearMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val selectedYear = MutableStateFlow(YearMonth.now().year)
    private val viewMode = MutableStateFlow(HeatmapViewMode.MONTH)
    private val selectedMetric = MutableStateFlow(HeatmapMetric.BOTH)
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val selectedAccount = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CalendarHeatmapUiState> = combine(
        transactionRepository.getAllTransactions(),
        selectedYearMonth,
        selectedDate,
        selectedYear,
        viewMode,
        selectedMetric,
        selectedCategory,
        selectedAccount
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val rawTransactions = args[0] as List<Transaction>
        val ym = args[1] as YearMonth
        val sDate = args[2] as LocalDate
        val sYear = args[3] as Int
        val vMode = args[4] as HeatmapViewMode
        val metric = args[5] as HeatmapMetric
        val category = args[6] as String?
        val account = args[7] as String?

        computeHeatmapState(
            rawTransactions = rawTransactions,
            yearMonth = ym,
            selectedDate = sDate,
            selectedYear = sYear,
            viewMode = vMode,
            metric = metric,
            selectedCategory = category,
            selectedAccount = account
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarHeatmapUiState()
    )

    private fun computeHeatmapState(
        rawTransactions: List<Transaction>,
        yearMonth: YearMonth,
        selectedDate: LocalDate,
        selectedYear: Int,
        viewMode: HeatmapViewMode,
        metric: HeatmapMetric,
        selectedCategory: String?,
        selectedAccount: String?
    ): CalendarHeatmapUiState {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()

        // Extract available categories
        val availableCategories = rawTransactions
            .map { it.category }
            .filter { it.isNotBlank() && !it.equals("Transfer", ignoreCase = true) }
            .distinct()
            .sorted()

        // Extract available accounts
        val availableAccounts = rawTransactions.mapNotNull { tx ->
            when {
                tx.accountLast4 != null -> {
                    val bank = BankInfoHelper.detectBankForTransaction(tx)
                    val shortName = if (bank.isNotBlank()) BankInfoHelper.getBankShortName(bank) else "Bank"
                    "$shortName ••${tx.accountLast4}"
                }
                tx.bankName != null -> tx.bankName
                tx.isImported -> "Imported"
                else -> null
            }
        }.distinct().sorted()

        // Available years
        val txYears = rawTransactions.map {
            Instant.ofEpochMilli(it.dateTimestamp).atZone(zoneId).toLocalDate().year
        }.distinct()
        val availableYears = (txYears + today.year).distinct().sortedDescending()

        // Filter transactions
        val filteredTransactions = rawTransactions.filter { tx ->
            val matchCategory = selectedCategory == null || tx.category.equals(selectedCategory, ignoreCase = true)
            val matchAccount = if (selectedAccount == null) true else {
                val accLabel = when {
                    tx.accountLast4 != null -> {
                        val bank = BankInfoHelper.detectBankForTransaction(tx)
                        val shortName = if (bank.isNotBlank()) BankInfoHelper.getBankShortName(bank) else "Bank"
                        "$shortName ••${tx.accountLast4}"
                    }
                    tx.bankName != null -> tx.bankName
                    tx.isImported -> "Imported"
                    else -> "Manual"
                }
                accLabel == selectedAccount
            }
            matchCategory && matchAccount
        }

        // Group by LocalDate
        val txByDate = filteredTransactions.groupBy {
            Instant.ofEpochMilli(it.dateTimestamp).atZone(zoneId).toLocalDate()
        }

        // Calculate Month Data
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfMonth = yearMonth.atDay(1)
        val leadingDaysCount = firstDayOfMonth.dayOfWeek.value - 1 // Monday = 1 -> 0 leading days
        val prevMonth = yearMonth.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()

        // Precompute raw daily metrics for current month
        val currentMonthDailyData = (1..daysInMonth).map { day ->
            val date = yearMonth.atDay(day)
            val txList = txByDate[date] ?: emptyList()
            // Exclude transfers from expense & income sums for genuine cash flow
            val expenses = txList.filter { it.type == TransactionType.EXPENSE && !it.isTransfer }.sumOf { it.amount }
            val incomes = txList.filter { it.type == TransactionType.INCOME && !it.isTransfer }.sumOf { it.amount }
            val net = incomes - expenses
            val count = txList.size
            Triple(date, Triple(expenses, incomes, net), Pair(count, txList))
        }

        // Determine max threshold for intensity
        val monthMaxExpense = currentMonthDailyData.maxOfOrNull { it.second.first } ?: 0.0
        val monthMaxIncome = currentMonthDailyData.maxOfOrNull { it.second.second } ?: 0.0
        val monthMaxNet = currentMonthDailyData.maxOfOrNull { kotlin.math.abs(it.second.third) } ?: 0.0

        val maxMetricValue = when (metric) {
            HeatmapMetric.BOTH -> maxOf(monthMaxExpense, monthMaxIncome)
            HeatmapMetric.EXPENSE -> monthMaxExpense
            HeatmapMetric.INCOME -> monthMaxIncome
            HeatmapMetric.NET -> monthMaxNet
            HeatmapMetric.ACTIVITY -> 10.0
        }

        // Build 7-column calendar cells with leading and trailing days
        val monthDays = mutableListOf<DayHeatmapEntry>()

        // Leading days from previous month (dimmed)
        for (i in (daysInPrevMonth - leadingDaysCount + 1)..daysInPrevMonth) {
            val date = prevMonth.atDay(i)
            val txList = txByDate[date] ?: emptyList()
            val expenses = txList.filter { it.type == TransactionType.EXPENSE && !it.isTransfer }.sumOf { it.amount }
            val incomes = txList.filter { it.type == TransactionType.INCOME && !it.isTransfer }.sumOf { it.amount }
            val net = incomes - expenses
            monthDays.add(
                DayHeatmapEntry(
                    date = date,
                    dayOfMonth = i,
                    isCurrentMonth = false,
                    isToday = date == today,
                    totalExpense = expenses,
                    totalIncome = incomes,
                    netFlow = net,
                    transactionCount = txList.size,
                    intensityLevel = 0,
                    expenseIntensity = 0,
                    incomeIntensity = 0,
                    transactions = txList.map { transactionUiMapper.map(it) }
                )
            )
        }

        // Current month days
        currentMonthDailyData.forEach { (date, amounts, countAndList) ->
            val (expenses, incomes, net) = amounts
            val (count, txList) = countAndList
            val expIntensity = calculateIntensity(expenses, monthMaxExpense, HeatmapMetric.EXPENSE, count)
            val incIntensity = calculateIntensity(incomes, monthMaxIncome, HeatmapMetric.INCOME, count)
            val metricValue = when (metric) {
                HeatmapMetric.BOTH -> maxOf(expenses, incomes)
                HeatmapMetric.EXPENSE -> expenses
                HeatmapMetric.INCOME -> incomes
                HeatmapMetric.NET -> kotlin.math.abs(net)
                HeatmapMetric.ACTIVITY -> count.toDouble()
            }
            val intensity = calculateIntensity(metricValue, maxMetricValue, metric, count)

            monthDays.add(
                DayHeatmapEntry(
                    date = date,
                    dayOfMonth = date.dayOfMonth,
                    isCurrentMonth = true,
                    isToday = date == today,
                    totalExpense = expenses,
                    totalIncome = incomes,
                    netFlow = net,
                    transactionCount = count,
                    intensityLevel = intensity,
                    expenseIntensity = expIntensity,
                    incomeIntensity = incIntensity,
                    transactions = txList.map { transactionUiMapper.map(it) }
                )
            )
        }

        // Trailing days from next month to complete the grid
        val nextMonth = yearMonth.plusMonths(1)
        val remainingCells = (7 - (monthDays.size % 7)) % 7
        for (i in 1..remainingCells) {
            val date = nextMonth.atDay(i)
            val txList = txByDate[date] ?: emptyList()
            val expenses = txList.filter { it.type == TransactionType.EXPENSE && !it.isTransfer }.sumOf { it.amount }
            val incomes = txList.filter { it.type == TransactionType.INCOME && !it.isTransfer }.sumOf { it.amount }
            val net = incomes - expenses
            monthDays.add(
                DayHeatmapEntry(
                    date = date,
                    dayOfMonth = i,
                    isCurrentMonth = false,
                    isToday = date == today,
                    totalExpense = expenses,
                    totalIncome = incomes,
                    netFlow = net,
                    transactionCount = txList.size,
                    intensityLevel = 0,
                    expenseIntensity = 0,
                    incomeIntensity = 0,
                    transactions = txList.map { transactionUiMapper.map(it) }
                )
            )
        }

        // Compute Month Summary Metrics
        val monthTotalExpense = currentMonthDailyData.sumOf { it.second.first }
        val monthTotalIncome = currentMonthDailyData.sumOf { it.second.second }
        val monthNetSavings = monthTotalIncome - monthTotalExpense

        val daysElapsedInMonth = if (yearMonth == YearMonth.from(today)) {
            today.dayOfMonth
        } else {
            daysInMonth
        }
        val monthDailyAverage = if (daysElapsedInMonth > 0) monthTotalExpense / daysElapsedInMonth else 0.0

        val effectiveDaysForStats = currentMonthDailyData.filter {
            if (yearMonth == YearMonth.from(today)) it.first <= today else true
        }
        val monthZeroSpendDaysCount = effectiveDaysForStats.count { it.second.first <= 0.0 }
        val monthActiveDaysCount = effectiveDaysForStats.count { it.second.first > 0.0 || it.second.second > 0.0 }

        val peakEntry = currentMonthDailyData.maxByOrNull { it.second.first }
        val monthPeakDay = if (peakEntry != null && peakEntry.second.first > 0.0) peakEntry.first else null
        val monthPeakAmount = peakEntry?.second?.first ?: 0.0

        // Zero spend streak calculation
        val (longestStreak, currentStreak) = calculateStreaks(currentMonthDailyData, today, yearMonth)

        // Compute Year Overview (all 12 months for selectedYear)
        val yearMonths = (1..12).map { monthNum ->
            val ym = YearMonth.of(selectedYear, monthNum)
            val mDaysInMonth = ym.lengthOfMonth()
            val mDaily = (1..mDaysInMonth).map { day ->
                val date = ym.atDay(day)
                val txList = txByDate[date] ?: emptyList()
                val exp = txList.filter { it.type == TransactionType.EXPENSE && !it.isTransfer }.sumOf { it.amount }
                val inc = txList.filter { it.type == TransactionType.INCOME && !it.isTransfer }.sumOf { it.amount }
                val net = inc - exp
                DayHeatmapEntry(
                    date = date,
                    dayOfMonth = day,
                    isCurrentMonth = true,
                    isToday = date == today,
                    totalExpense = exp,
                    totalIncome = inc,
                    netFlow = net,
                    transactionCount = txList.size,
                    intensityLevel = 0, // computed below
                    transactions = txList.map { transactionUiMapper.map(it) }
                )
            }
            val mExpTotal = mDaily.sumOf { it.totalExpense }
            val mIncTotal = mDaily.sumOf { it.totalIncome }
            val mZeroDays = mDaily.count { it.totalExpense <= 0.0 && (it.date <= today || ym < YearMonth.from(today)) }
            val mTxTotal = mDaily.sumOf { it.transactionCount }

            // Compute intensity for this month's days relative to year max or month max
            val mMaxExp = mDaily.maxOfOrNull { it.totalExpense } ?: 0.0
            val calibratedDays = mDaily.map { d ->
                val metricVal = when (metric) {
                    HeatmapMetric.BOTH -> maxOf(d.totalExpense, d.totalIncome)
                    HeatmapMetric.EXPENSE -> d.totalExpense
                    HeatmapMetric.INCOME -> d.totalIncome
                    HeatmapMetric.NET -> kotlin.math.abs(d.netFlow)
                    HeatmapMetric.ACTIVITY -> d.transactionCount.toDouble()
                }
                val intensity = calculateIntensity(metricVal, mMaxExp, metric, d.transactionCount)
                d.copy(intensityLevel = intensity)
            }

            MonthOverview(
                yearMonth = ym,
                totalExpense = mExpTotal,
                totalIncome = mIncTotal,
                totalTransactions = mTxTotal,
                zeroSpendDays = mZeroDays,
                days = calibratedDays
            )
        }

        val yearTotalExpense = yearMonths.sumOf { it.totalExpense }
        val yearTotalIncome = yearMonths.sumOf { it.totalIncome }
        val yearZeroSpendDays = yearMonths.sumOf { it.zeroSpendDays }
        val yearActiveDays = yearMonths.sumOf { it.days.count { d -> d.totalExpense > 0.0 || d.totalIncome > 0.0 } }

        // Find Selected Day Data
        val selectedDayEntry = monthDays.firstOrNull { it.date == selectedDate }
            ?: run {
                val txList = txByDate[selectedDate] ?: emptyList()
                val exp = txList.filter { it.type == TransactionType.EXPENSE && !it.isTransfer }.sumOf { it.amount }
                val inc = txList.filter { it.type == TransactionType.INCOME && !it.isTransfer }.sumOf { it.amount }
                DayHeatmapEntry(
                    date = selectedDate,
                    dayOfMonth = selectedDate.dayOfMonth,
                    isCurrentMonth = YearMonth.from(selectedDate) == yearMonth,
                    isToday = selectedDate == today,
                    totalExpense = exp,
                    totalIncome = inc,
                    netFlow = inc - exp,
                    transactionCount = txList.size,
                    intensityLevel = 0,
                    expenseIntensity = calculateIntensity(exp, monthMaxExpense, HeatmapMetric.EXPENSE, txList.size),
                    incomeIntensity = calculateIntensity(inc, monthMaxIncome, HeatmapMetric.INCOME, txList.size),
                    transactions = txList.map { transactionUiMapper.map(it) }
                )
            }

        return CalendarHeatmapUiState(
            isLoading = false,
            selectedYearMonth = yearMonth,
            selectedDate = selectedDate,
            selectedYear = selectedYear,
            viewMode = viewMode,
            selectedMetric = metric,
            selectedCategory = selectedCategory,
            selectedAccount = selectedAccount,
            availableCategories = availableCategories,
            availableAccounts = availableAccounts,
            availableYears = availableYears,
            monthTotalExpense = monthTotalExpense,
            monthTotalIncome = monthTotalIncome,
            monthNetSavings = monthNetSavings,
            monthDailyAverage = monthDailyAverage,
            monthZeroSpendDaysCount = monthZeroSpendDaysCount,
            monthActiveDaysCount = monthActiveDaysCount,
            monthPeakDay = monthPeakDay,
            monthPeakAmount = monthPeakAmount,
            longestZeroSpendStreak = longestStreak,
            currentZeroSpendStreak = currentStreak,
            yearTotalExpense = yearTotalExpense,
            yearTotalIncome = yearTotalIncome,
            yearZeroSpendDaysCount = yearZeroSpendDays,
            yearActiveDaysCount = yearActiveDays,
            monthDays = monthDays,
            yearMonths = yearMonths,
            legendMaxThreshold = maxMetricValue,
            selectedDayData = selectedDayEntry
        )
    }

    private fun calculateIntensity(
        value: Double,
        maxValue: Double,
        metric: HeatmapMetric,
        count: Int
    ): Int {
        if (metric == HeatmapMetric.ACTIVITY) {
            return when {
                count == 0 -> 0
                count in 1..2 -> 1
                count in 3..5 -> 2
                count in 6..8 -> 3
                else -> 4
            }
        }
        if (value <= 0.0) return 0
        if (maxValue <= 0.0) return 0
        val ratio = value / maxValue
        return when {
            ratio <= 0.25 -> 1
            ratio <= 0.50 -> 2
            ratio <= 0.75 -> 3
            else -> 4
        }
    }

    private fun calculateStreaks(
        dailyData: List<Triple<LocalDate, Triple<Double, Double, Double>, Pair<Int, List<Transaction>>>>,
        today: LocalDate,
        yearMonth: YearMonth
    ): Pair<Int, Int> {
        val daysInScope = dailyData.filter {
            if (yearMonth == YearMonth.from(today)) it.first <= today else true
        }
        var longest = 0
        var currentRunning = 0

        for (day in daysInScope) {
            val expense = day.second.first
            if (expense <= 0.0) {
                currentRunning++
                if (currentRunning > longest) {
                    longest = currentRunning
                }
            } else {
                currentRunning = 0
            }
        }

        // Current streak ending at the last observed day in scope
        var currentEnding = 0
        for (day in daysInScope.reversed()) {
            if (day.second.first <= 0.0) {
                currentEnding++
            } else {
                break
            }
        }

        return Pair(longest, currentEnding)
    }

    fun selectYearMonth(ym: YearMonth) {
        selectedYearMonth.value = ym
        selectedYear.value = ym.year
        // Default selected date to 1st of month or today if current month
        val today = LocalDate.now()
        if (ym == YearMonth.from(today)) {
            selectedDate.value = today
        } else {
            selectedDate.value = ym.atDay(1)
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        val ym = YearMonth.from(date)
        if (ym != selectedYearMonth.value) {
            selectedYearMonth.value = ym
            selectedYear.value = ym.year
        }
    }

    fun selectYear(year: Int) {
        selectedYear.value = year
        selectedYearMonth.value = YearMonth.of(year, selectedYearMonth.value.monthValue)
    }

    fun selectMetric(metric: HeatmapMetric) {
        selectedMetric.value = metric
    }

    fun selectViewMode(mode: HeatmapViewMode) {
        viewMode.value = mode
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    fun selectAccount(account: String?) {
        selectedAccount.value = account
    }

    fun goToPreviousMonth() {
        val prev = selectedYearMonth.value.minusMonths(1)
        selectYearMonth(prev)
    }

    fun goToNextMonth() {
        val next = selectedYearMonth.value.plusMonths(1)
        selectYearMonth(next)
    }

    fun goToPreviousYear() {
        selectedYear.value = selectedYear.value - 1
        selectedYearMonth.value = YearMonth.of(selectedYear.value, selectedYearMonth.value.monthValue)
    }

    fun goToNextYear() {
        selectedYear.value = selectedYear.value + 1
        selectedYearMonth.value = YearMonth.of(selectedYear.value, selectedYearMonth.value.monthValue)
    }

    fun goToToday() {
        val today = LocalDate.now()
        selectedYearMonth.value = YearMonth.from(today)
        selectedYear.value = today.year
        selectedDate.value = today
    }
}
