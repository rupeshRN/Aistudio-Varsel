package com.varsel.expensetracker.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.ui.transaction.components.MonthSelector
import com.varsel.expensetracker.ui.transaction.components.MonthlySummaryCard
import com.varsel.expensetracker.ui.transaction.components.TransactionFilterBar
import com.varsel.expensetracker.ui.transaction.components.TransactionHeader
import com.varsel.expensetracker.ui.transaction.components.TransactionSearchBar
import com.varsel.expensetracker.ui.transaction.components.transactionList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    onBackClick: () -> Unit,
    onTransactionClick: (Long) -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val months = listOf(
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec"
    )

    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = {

                    Text("Transactions")

                },

                navigationIcon = {

                    IconButton(

                        onClick = onBackClick

                    ) {

                        Icon(

                            imageVector = Icons.Default.ArrowBack,

                            contentDescription = "Back"

                        )

                    }

                }

            )

        }

    ) { padding ->

        LazyColumn(

            modifier = Modifier,

            contentPadding = PaddingValues(16.dp),

            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {

            item {

                TransactionHeader(

                    transactionCount =
                        uiState.transactions.size

                )

            }

            item {

                MonthlySummaryCard(

                  monthTitle = uiState.selectedMonth?.displayName ?: "",

                income = uiState.monthlyIncome,

                expense = uiState.monthlyExpense

                )

            }

            item {

                MonthSelector(

                    months = uiState.availableMonths,

                    selectedMonth = uiState.selectedMonth,

                    onMonthSelected = viewModel::updateSelectedMonth

                )

            }

            item {

                TransactionSearchBar(

                    query =
                        uiState.searchQuery,

                    onQueryChange =

                        viewModel::updateSearchQuery

                )

            }

            item {

                TransactionFilterBar(

                    filters =
                        TransactionFilter.entries,

                    selectedFilter =
                        uiState.selectedFilter,

                    onFilterSelected =

                        viewModel::updateFilter

                )

            }

transactionList(

    transactions =
        uiState.transactions,

    onTransactionClick = { transaction ->

        onTransactionClick(
            transaction.id
        )

    }

)

        }

    }

}
