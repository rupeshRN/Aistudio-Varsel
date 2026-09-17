package com.varsel.expensetracker.ui.recurring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.recurring.components.AddEditRecurringSheet
import com.varsel.expensetracker.ui.recurring.components.RecurringItemCard
import com.varsel.expensetracker.ui.recurring.components.RecurringSummaryCard
import com.varsel.expensetracker.ui.recurring.model.RecurringFilterTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringSubscriptionsScreen(
    viewModel: RecurringViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recurring & Subscriptions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openAddSheet() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Recurring Item"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddSheet() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Item") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Summary Card
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        RecurringSummaryCard(summary = uiState.summary)
                    }
                }

                // Filter Tabs
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(RecurringFilterTab.values()) { tab ->
                            FilterChip(
                                selected = uiState.selectedTab == tab,
                                onClick = { viewModel.setFilterTab(tab) },
                                label = {
                                    val count = when (tab) {
                                        RecurringFilterTab.ALL -> uiState.allItems.size
                                        RecurringFilterTab.UPCOMING -> uiState.upcomingItems.size
                                        RecurringFilterTab.SUBSCRIPTIONS -> uiState.allItems.count { it.item.type == com.varsel.expensetracker.domain.model.recurring.RecurringType.SUBSCRIPTION }
                                        RecurringFilterTab.EXPENSES -> uiState.allItems.count { it.item.type == com.varsel.expensetracker.domain.model.recurring.RecurringType.EXPENSE }
                                        RecurringFilterTab.INCOME -> uiState.allItems.count { it.item.type == com.varsel.expensetracker.domain.model.recurring.RecurringType.INCOME }
                                    }
                                    Text("${tab.title} ($count)")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }

                // List Items or Empty State
                if (uiState.filteredItems.isEmpty()) {
                    item {
                        EmptyRecurringState(
                            tab = uiState.selectedTab,
                            onAddClick = { viewModel.openAddSheet() },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
                        )
                    }
                } else {
                    item {
                        Text(
                            text = when (uiState.selectedTab) {
                                RecurringFilterTab.ALL -> "All Recurring Commitments"
                                RecurringFilterTab.UPCOMING -> "Upcoming in Chronological Order"
                                RecurringFilterTab.SUBSCRIPTIONS -> "Active Subscriptions"
                                RecurringFilterTab.EXPENSES -> "Recurring Expenses & Bills"
                                RecurringFilterTab.INCOME -> "Recurring Income & Salaries"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(
                        items = uiState.filteredItems,
                        key = { it.item.id }
                    ) { uiModel ->
                        Column(
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            RecurringItemCard(
                                uiModel = uiModel,
                                onToggleActive = { active -> viewModel.toggleActive(uiModel.item.id, active) },
                                onRecordOccurrence = { viewModel.processOccurrence(uiModel.item) },
                                onSkipOccurrence = { viewModel.skipOccurrence(uiModel.item) },
                                onEdit = { viewModel.openEditSheet(uiModel.item) },
                                onDelete = { viewModel.deleteRecurringItem(uiModel.item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddEditSheetOpen) {
        AddEditRecurringSheet(
            item = uiState.editingItem,
            availableAccounts = uiState.availableAccounts,
            availableCategories = uiState.availableCategories,
            onDismiss = { viewModel.closeAddEditSheet() },
            onSave = { item -> viewModel.saveRecurringItem(item) }
        )
    }
}

@Composable
private fun EmptyRecurringState(
    tab: RecurringFilterTab,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.EventRepeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when (tab) {
                RecurringFilterTab.ALL -> "No recurring payments yet"
                RecurringFilterTab.UPCOMING -> "No upcoming recurring items"
                RecurringFilterTab.SUBSCRIPTIONS -> "No subscriptions added"
                RecurringFilterTab.EXPENSES -> "No recurring expenses"
                RecurringFilterTab.INCOME -> "No recurring income"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Track Netflix, rent, bills, salary, and more with automated schedules and occurrence tracking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onAddClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text("Add Recurring Item")
        }
    }
}
