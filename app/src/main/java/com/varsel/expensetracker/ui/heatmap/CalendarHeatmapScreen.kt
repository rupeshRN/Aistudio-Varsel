package com.varsel.expensetracker.ui.heatmap

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.ui.heatmap.components.HeatmapCalendarView
import com.varsel.expensetracker.ui.heatmap.components.HeatmapDayDetailSection
import com.varsel.expensetracker.ui.heatmap.components.HeatmapSummaryCards
import com.varsel.expensetracker.ui.heatmap.components.HeatmapYearView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHeatmapScreen(
    onBackClick: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: CalendarHeatmapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calendar Heatmap",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("heatmap_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Today shortcut button
                    IconButton(
                        onClick = { viewModel.goToToday() },
                        modifier = Modifier.testTag("heatmap_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Today,
                            contentDescription = "Jump to Today",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Month vs Year View toggle
                    IconButton(
                        onClick = {
                            viewModel.selectViewMode(
                                if (uiState.viewMode == HeatmapViewMode.MONTH) HeatmapViewMode.YEAR else HeatmapViewMode.MONTH
                            )
                        },
                        modifier = Modifier.testTag("heatmap_toggle_view_mode")
                    ) {
                        Icon(
                            imageVector = if (uiState.viewMode == HeatmapViewMode.MONTH) Icons.Outlined.GridView else Icons.Outlined.CalendarMonth,
                            contentDescription = if (uiState.viewMode == HeatmapViewMode.MONTH) "Switch to Year View" else "Switch to Month View"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Metric & Filter Chips Row
                item(key = "filter_chips") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Metric Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeatmapMetric.entries.forEach { metric ->
                                FilterChip(
                                    selected = uiState.selectedMetric == metric,
                                    onClick = { viewModel.selectMetric(metric) },
                                    label = { Text(metric.label) },
                                    leadingIcon = if (uiState.selectedMetric == metric) {
                                        {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("filter_metric_${metric.name.lowercase()}")
                                )
                            }
                        }

                        // Category & Account Secondary Filters
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Category Filter Chip
                            Box {
                                FilterChip(
                                    selected = uiState.selectedCategory != null,
                                    onClick = { showCategoryMenu = true },
                                    label = { Text(uiState.selectedCategory ?: "All Categories") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("filter_category_chip")
                                )

                                DropdownMenu(
                                    expanded = showCategoryMenu,
                                    onDismissRequest = { showCategoryMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Categories") },
                                        onClick = {
                                            viewModel.selectCategory(null)
                                            showCategoryMenu = false
                                        }
                                    )
                                    uiState.availableCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                viewModel.selectCategory(cat)
                                                showCategoryMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Account Filter Chip
                            if (uiState.availableAccounts.isNotEmpty()) {
                                Box {
                                    FilterChip(
                                        selected = uiState.selectedAccount != null,
                                        onClick = { showAccountMenu = true },
                                        label = { Text(uiState.selectedAccount ?: "All Accounts") },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("filter_account_chip")
                                    )

                                    DropdownMenu(
                                        expanded = showAccountMenu,
                                        onDismissRequest = { showAccountMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("All Accounts") },
                                            onClick = {
                                                viewModel.selectAccount(null)
                                                showAccountMenu = false
                                            }
                                        )
                                        uiState.availableAccounts.forEach { acc ->
                                            DropdownMenuItem(
                                                text = { Text(acc) },
                                                onClick = {
                                                    viewModel.selectAccount(acc)
                                                    showAccountMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary Hero & Stats
                item(key = "summary_cards") {
                    HeatmapSummaryCards(uiState = uiState)
                }

                // Main Calendar Heatmap or Year Matrix
                if (uiState.viewMode == HeatmapViewMode.MONTH) {
                    item(key = "month_calendar") {
                        HeatmapCalendarView(
                            uiState = uiState,
                            onDateSelected = { date -> viewModel.selectDate(date) },
                            onPreviousMonth = { viewModel.goToPreviousMonth() },
                            onNextMonth = { viewModel.goToNextMonth() },
                            onTodayClick = { viewModel.goToToday() }
                        )
                    }

                    // Selected Day Transactions Drilldown
                    item(key = "day_details") {
                        HeatmapDayDetailSection(
                            dayEntry = uiState.selectedDayData,
                            onTransactionClick = onTransactionClick
                        )
                    }
                } else {
                    item(key = "year_matrix") {
                        HeatmapYearView(
                            uiState = uiState,
                            onMonthSelected = { ym ->
                                viewModel.selectYearMonth(ym)
                                viewModel.selectViewMode(HeatmapViewMode.MONTH)
                            },
                            onPreviousYear = { viewModel.goToPreviousYear() },
                            onNextYear = { viewModel.goToNextYear() }
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
