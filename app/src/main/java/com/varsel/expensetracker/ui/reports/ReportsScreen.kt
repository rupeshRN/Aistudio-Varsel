package com.varsel.expensetracker.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.ui.reports.components.CategoryDrillDownBottomSheet
import com.varsel.expensetracker.ui.reports.components.ExpenseCategoryChart
import com.varsel.expensetracker.ui.reports.components.ExpenseCategoryList
import com.varsel.expensetracker.ui.reports.components.FinancialEventsCard
import com.varsel.expensetracker.ui.reports.components.IncomeCategoryChart
import com.varsel.expensetracker.ui.reports.components.IncomeCategoryList
import com.varsel.expensetracker.ui.reports.components.MoneyFlowCard
import com.varsel.expensetracker.ui.reports.components.NetCashFlowCard
import com.varsel.expensetracker.ui.reports.components.ReportFilterSheet
import com.varsel.expensetracker.ui.reports.components.ReportsHeader
import kotlinx.coroutines.launch

/**
 * Production Reports screen.
 *
 * The complete report page uses one vertical scroll container.
 *
 * This is intentional:
 * - Header
 * - Net Cash Flow
 * - Money Flow
 * - Donut chart
 * - Category list
 * - Financial Events
 *
 * all belong to the same report page and should scroll together.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    onTransactionClick: (Long) -> Unit = {},
    onFinancialEventClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var filterSheetVisible by remember {
        mutableStateOf(false)
    }

    val filterSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    val drillDownSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    val scope = rememberCoroutineScope()

    ReportsScreenContent(
        uiState = uiState,

        onPreviousMonth =
            viewModel::previousMonth,

        onNextMonth =
            viewModel::nextMonth,

        onFilterClick = {
            filterSheetVisible = true
        },

        onFlowSelected =
            viewModel::selectFlow,

        onExpenseCategorySelected = {
            if (it == null) {
                viewModel.clearCategorySelection()
            } else {
                viewModel.selectExpenseCategory(it)
            }
        },

        onIncomeCategorySelected = {
            if (it == null) {
                viewModel.clearCategorySelection()
            } else {
                viewModel.selectIncomeCategory(it)
            }
        },

        onFinancialEventClick =
            onFinancialEventClick
    )

    if (filterSheetVisible) {

        ReportFilterSheet(
            accounts =
                uiState.accounts,

            selectedAccountIds =
                uiState.selectedAccountIds,

                selectedPeriod =
        uiState.periodFilter,

    customStartDate =
        uiState.customStartDate,

    customEndDate =
        uiState.customEndDate,

    onPeriodSelected =
        viewModel::selectPeriod,

    onCustomDateRangeSelected =
        viewModel::setCustomDateRange,

            sheetState =
                filterSheetState,

            onDismiss = {
                filterSheetVisible = false
            },

            onApply = { selectedAccounts ->

                viewModel.setSelectedAccounts(
                    selectedAccounts
                )

                scope.launch {
                    filterSheetState.hide()
                    filterSheetVisible = false
                }
            }
        )
    }

    if (uiState.drillDownState.isVisible) {
        CategoryDrillDownBottomSheet(
            state = uiState.drillDownState,
            sheetState = drillDownSheetState,
            onDismiss = {
                scope.launch {
                    drillDownSheetState.hide()
                    viewModel.dismissCategoryDrillDown()
                }
            },
            onSearchQueryChange = viewModel::updateDrillDownSearch,
            onTransactionClick = onTransactionClick,
            onFinancialEventClick = onFinancialEventClick
        )
    }
}

@Composable
private fun ReportsScreenContent(
    uiState: ReportsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onFilterClick: () -> Unit,
    onFlowSelected: (ReportsFlow) -> Unit,
    onExpenseCategorySelected: (String?) -> Unit,
    onIncomeCategorySelected: (String?) -> Unit,
    onFinancialEventClick: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        when {

            uiState.isLoading -> {

                CircularProgressIndicator(
                    modifier =
                        Modifier.align(
                            Alignment.Center
                        )
                )
            }

            uiState.errorMessage != null -> {

                Text(
                    text =
                        uiState.errorMessage,

                    modifier =
                        Modifier.align(
                            Alignment.Center
                        )
                )
            }

            else -> {

                val scrollState =
                    rememberScrollState()

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(
                                scrollState
                            )
                            .padding(
                                16.dp
                            ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            16.dp
                        )
                ) {

                    ReportsHeader(
                        periodLabel =
                            uiState.formattedPeriodLabel,

                        isPreviousEnabled =
                            uiState.isPreviousPeriodEnabled,

                        isNextEnabled =
                            uiState.isNextPeriodEnabled,

                        accountFilterLabel =
                            uiState.accountFilterLabel,

                        hasActiveAccountFilter =
                            !uiState.isAllAccountsSelected,

                        onPreviousPeriod =
                            onPreviousMonth,

                        onNextPeriod =
                            onNextMonth,

                        onFilterClick =
                            onFilterClick
                    )

                    NetCashFlowCard(
                        actualIncome =
                            uiState.cashFlow.actualIncome,

                        effectiveExpense =
                            uiState.cashFlow.effectiveExpense,

                        netCashFlow =
                            uiState.cashFlow.netCashFlow
                    )

                    MoneyFlowCard(
                        selectedFlow =
                            uiState.selectedFlow,

                        onFlowSelected =
                            onFlowSelected
                    ) {

                        when (uiState.selectedFlow) {

                            ReportsFlow.EXPENSES -> {

                                Column(
                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            16.dp
                                        )
                                ) {

                                    ExpenseCategoryChart(
                                        categories =
                                            uiState.expenseCategories,

                                        selectedCategory =
                                            uiState.selectedExpenseCategory,

                                        onCategoryClick =
                                            onExpenseCategorySelected
                                    )

                                    ExpenseCategoryList(
                                        categories =
                                            uiState.expenseCategories,

                                        selectedCategory =
                                            uiState.selectedExpenseCategory,

                                        onCategorySelected =
                                            onExpenseCategorySelected
                                    )
                                }
                            }

                            ReportsFlow.INCOME -> {

                                Column(
                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            16.dp
                                        )
                                ) {

                                    IncomeCategoryChart(
                                        categories =
                                            uiState.incomeCategories,

                                        selectedCategory =
                                            uiState.selectedIncomeCategory,

                                        onCategoryClick =
                                            onIncomeCategorySelected
                                    )

                                    IncomeCategoryList(
                                        categories =
                                            uiState.incomeCategories,

                                        selectedCategory =
                                            uiState.selectedIncomeCategory,

                                        onCategorySelected =
                                            onIncomeCategorySelected
                                    )
                                }
                            }
                        }
                    }

                    FinancialEventsCard(
                        financialEvents =
                            uiState.financialEvents,

                        onFinancialEventClick =
                            onFinancialEventClick
                    )
                }
            }
        }
    }
}
