package com.varsel.expensetracker.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.ui.transaction.components.MonthSelector
import com.varsel.expensetracker.ui.transaction.components.MonthlySummaryCard
import com.varsel.expensetracker.ui.transaction.components.TransactionFilterBar
import com.varsel.expensetracker.ui.transaction.components.TransactionHeader
import com.varsel.expensetracker.ui.transaction.components.TransactionSearchBar
import com.varsel.expensetracker.ui.transaction.components.transactionList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    canNavigateBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onTransactionClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 30
        }
    }

    val scrollFraction by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "header_scroll_fraction"
    )

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = (3 * scrollFraction).dp,
                shadowElevation = (2 * scrollFraction).dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = (16 - 6 * scrollFraction).dp,
                            bottom = (10 - 4 * scrollFraction).dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                ) {
                    // Left-aligned header with dynamic scale on scroll and optional back navigation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canNavigateBack) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.testTag("transaction_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = (22 - 3 * scrollFraction).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height((10 - 4 * scrollFraction).dp))

                    // Dynamically moving Month Selector
                    MonthSelector(
                        months = uiState.availableMonths,
                        selectedMonth = uiState.selectedMonth,
                        onMonthSelected = { month ->
                            viewModel.updateSelectedMonth(month)
                            coroutineScope.launch {
                                listState.scrollToItem(0)
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top"
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TransactionHeader(
                    transactionCount = uiState.transactions.size
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
                TransactionSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery
                )
            }

            item {
                TransactionFilterBar(
                    filters = TransactionFilter.entries,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::updateFilter
                )
            }

            transactionList(
                transactions = uiState.transactions,
                onTransactionClick = { transaction ->
                    onTransactionClick(transaction.id)
                }
            )
        }
    }
}

