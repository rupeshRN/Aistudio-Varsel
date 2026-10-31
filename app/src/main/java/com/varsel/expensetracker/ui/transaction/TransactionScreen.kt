package com.varsel.expensetracker.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.ui.theme.isDark
import com.varsel.expensetracker.ui.transaction.components.AddTransactionBottomSheet
import com.varsel.expensetracker.ui.transaction.components.ManualEntryMode
import com.varsel.expensetracker.ui.transaction.components.MonthSelector
import com.varsel.expensetracker.ui.transaction.components.MonthlySummaryCard
import com.varsel.expensetracker.ui.transaction.components.TransactionFilterBar
import com.varsel.expensetracker.ui.transaction.components.TransactionHeader
import com.varsel.expensetracker.ui.transaction.components.TransactionSearchBar
import com.varsel.expensetracker.ui.transaction.components.transactionList
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.varsel.expensetracker.ui.components.AppErrorBanner
import com.varsel.expensetracker.util.AppErrorMessageMapper
import kotlinx.coroutines.flow.collectLatest
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
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val availableAccounts by viewModel.availableAccounts.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var isMonthMenuExpanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.isDark

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.errorEvents.collectLatest { event ->
            val message = AppErrorMessageMapper.getUserMessage(event.error)
            val action = AppErrorMessageMapper.getActionSuggestion(event.error)
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (event.retryAction != null) (action ?: "Retry") else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.retryAction?.invoke()
            }
        }
    }

    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
        }
    }

    // Month picker in the list is item 0; when scrolled past it, it is hidden
    val isMonthPickerHidden by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 48
        }
    }

    androidx.compose.runtime.LaunchedEffect(isMonthPickerHidden) {
        if (!isMonthPickerHidden) {
            isMonthMenuExpanded = false
        }
    }

    val scrollElevation by animateFloatAsState(
        targetValue = if (isScrolled) 3f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scroll_elevation"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = scrollElevation.dp,
                shadowElevation = (scrollElevation * 0.7f).dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left section: optional back button + Title and context subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (canNavigateBack) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onBackClick() }
                                    .testTag("transaction_back_button"),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Transactions",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = "${uiState.selectedMonth?.displayName ?: "All Time"} • ${uiState.transactions.size} records",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Right tactile action: Month Jumper Dropdown (shown only when scrollable month picker is scrolled out of view)
                    AnimatedVisibility(
                        visible = isMonthPickerHidden,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.85f),
                        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + scaleOut(targetScale = 0.85f)
                    ) {
                        Box {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { isMonthMenuExpanded = true }
                                    .testTag("month_jumper_button"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isMonthMenuExpanded) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = uiState.selectedMonth?.displayName ?: "Month",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Month",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isMonthMenuExpanded,
                                onDismissRequest = { isMonthMenuExpanded = false }
                            ) {
                                uiState.availableMonths.forEach { month ->
                                    val isSelected = month == uiState.selectedMonth
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = month.displayName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.CalendarMonth,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            isMonthMenuExpanded = false
                                            viewModel.updateSelectedMonth(month)
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(0)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Scroll to top"
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showAddTransactionSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Entry"
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 84.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.error != null) {
                item(key = "error_banner") {
                    AppErrorBanner(
                        error = uiState.error!!,
                        onDismiss = { viewModel.dismissError() },
                        onRetry = { viewModel.retryLoading() }
                    )
                }
            }

            // 1. Month Switcher Pills (Horizontal scroll right above summary card)
            item(key = "month_selector") {
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

            // 2. Hero Financial Summary Card (matches BalanceCard styling)
            item(key = "monthly_summary_card") {
                MonthlySummaryCard(
                    monthTitle = uiState.selectedMonth?.displayName ?: "",
                    income = uiState.monthlyIncome,
                    expense = uiState.monthlyExpense,
                    onCreditClick = {
                        viewModel.updateFilter(TransactionFilter.Income)
                    },
                    onDebitClick = {
                        viewModel.updateFilter(TransactionFilter.Expense)
                    }
                )
            }

            // 3. Search Bar
            item(key = "search_bar") {
                TransactionSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery
                )
            }

            // 4. Filter Chips Bar (All, Expenses, Income, Transfers)
            item(key = "filter_bar") {
                TransactionFilterBar(
                    filters = TransactionFilter.entries,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::updateFilter
                )
            }

            // 5. Section Header for Activity Records (Placed directly above the list)
            item(key = "transaction_header") {
                TransactionHeader(
                    transactionCount = uiState.transactions.size
                )
            }

            // 6. Unified Grouped Transaction Ledger
            transactionList(
                transactions = uiState.transactions,
                onTransactionClick = { transaction ->
                    onTransactionClick(transaction.id)
                },
                onResetFilter = if (uiState.searchQuery.isNotEmpty() || uiState.selectedFilter != TransactionFilter.All) {
                    {
                        viewModel.updateSearchQuery("")
                        viewModel.updateFilter(TransactionFilter.All)
                    }
                } else null
            )
        }
    }

    if (showAddTransactionSheet) {
        AddTransactionBottomSheet(
            onDismiss = { showAddTransactionSheet = false },
            categories = categories,
            availableAccounts = availableAccounts,
            initialMode = ManualEntryMode.EXPENSE,
            onSaveTransaction = { amount, type, desc, cat, date, ref, accId, last4, bank ->
                viewModel.addTransaction(
                    amount = amount,
                    type = type,
                    description = desc,
                    category = cat,
                    dateTimestamp = date,
                    referenceNumber = ref,
                    accountId = accId,
                    accountLast4 = last4,
                    bankName = bank
                )
            },
            onSaveTransfer = { amount, desc, date, fromId, fromLast4, fromBank, toId, toLast4, toBank, ref ->
                viewModel.addTransfer(
                    amount = amount,
                    description = desc,
                    dateTimestamp = date,
                    fromAccountId = fromId,
                    fromAccountLast4 = fromLast4,
                    fromBankName = fromBank,
                    toAccountId = toId,
                    toAccountLast4 = toLast4,
                    toBankName = toBank,
                    referenceNumber = ref
                )
            },
            onCreateCategory = { name, isIncome ->
                viewModel.createCategory(name, isIncome)
            }
        )
    }
}
