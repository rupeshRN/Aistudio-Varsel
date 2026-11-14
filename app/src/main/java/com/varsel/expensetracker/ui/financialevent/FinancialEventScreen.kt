package com.varsel.expensetracker.ui.financialevent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Switch
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.domain.model.FinancialEventStatus
import com.varsel.expensetracker.domain.model.FinancialEventStatusSummary
import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import com.varsel.expensetracker.ui.components.FinancialEventStatusBadge
import com.varsel.expensetracker.ui.components.rememberFinancialEventStatusPalette
import com.varsel.expensetracker.ui.design.CategoryPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class AddTransactionMode {
    EXPENSE,
    REIMBURSEMENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialEventScreen(
    transactionLinkId: String,
    onBackClick: () -> Unit,
    viewModel: FinancialEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var addMode by remember { mutableStateOf<AddTransactionMode?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(transactionLinkId) {
        viewModel.loadFinancialEvent(transactionLinkId)
    }

    LaunchedEffect(uiState) {
        if (uiState is FinancialEventUiState.EventDeleted) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Financial Event",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                FinancialEventUiState.EventDeleted -> {
                    // Handled by LaunchedEffect navigation
                }

                FinancialEventUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                is FinancialEventUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onBackClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Go Back")
                                }
                            }
                        }
                    }
                }

                is FinancialEventUiState.Loaded -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hero Header Card
                        EventHeroHeaderCard(
                            group = state.group,
                            totalExpenses = state.totalExpenses,
                            totalReimbursements = state.totalReimbursements,
                            actualExpense = state.actualExpense,
                            statusSummary = state.statusSummary,
                            onEditGroup = viewModel::startEditingGroup,
                            onToggleSettled = viewModel::toggleManualSettled,
                            enabled = !state.isUpdating
                        )

                        // Quick Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { addMode = AddTransactionMode.EXPENSE },
                                enabled = !state.isUpdating && state.availableExpenses.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Expense", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            FilledTonalButton(
                                onClick = { addMode = AddTransactionMode.REIMBURSEMENT },
                                enabled = !state.isUpdating && state.availableReimbursements.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Reimbursement", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Tab Selection
                        val tabTitles = listOf(
                            "All (${state.allocatedExpenses.size + state.allocatedReimbursements.size})",
                            "Expenses (${state.allocatedExpenses.size})",
                            "Reimbursements (${state.allocatedReimbursements.size})"
                        )

                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            edgePadding = 0.dp,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            tabTitles.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }

                        // Transactions List Section based on selected tab
                        when (selectedTab) {
                            0 -> {
                                // All Tab
                                if (state.allocatedExpenses.isEmpty() && state.allocatedReimbursements.isEmpty()) {
                                    EmptyEventSection(
                                        message = "No transactions linked to this financial event yet.",
                                        actionText = if (state.availableExpenses.isNotEmpty()) "Add Expenses" else null,
                                        onAction = { addMode = AddTransactionMode.EXPENSE }
                                    )
                                } else {
                                    if (state.allocatedExpenses.isNotEmpty()) {
                                        SectionSubheader(
                                            title = "Allocated Expenses",
                                            count = state.allocatedExpenses.size,
                                            total = state.totalExpenses,
                                            isExpense = true
                                        )
                                        state.allocatedExpenses.forEach { item ->
                                            FinancialEventAllocatedRow(
                                                item = item,
                                                isIncome = false,
                                                onEdit = { viewModel.startEditingItem(item) },
                                                onRemove = { viewModel.removeTransaction(item.transaction.id) },
                                                enabled = !state.isUpdating
                                            )
                                        }
                                    }

                                    if (state.allocatedReimbursements.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        SectionSubheader(
                                            title = "Allocated Reimbursements",
                                            count = state.allocatedReimbursements.size,
                                            total = state.totalReimbursements,
                                            isExpense = false
                                        )
                                        state.allocatedReimbursements.forEach { item ->
                                            FinancialEventAllocatedRow(
                                                item = item,
                                                isIncome = true,
                                                onEdit = { viewModel.startEditingItem(item) },
                                                onRemove = { viewModel.removeTransaction(item.transaction.id) },
                                                enabled = !state.isUpdating
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // Expenses Tab
                                if (state.allocatedExpenses.isEmpty()) {
                                    EmptyEventSection(
                                        message = "No expenses linked to this event.",
                                        actionText = if (state.availableExpenses.isNotEmpty()) "Add Expenses" else null,
                                        onAction = { addMode = AddTransactionMode.EXPENSE }
                                    )
                                } else {
                                    state.allocatedExpenses.forEach { item ->
                                        FinancialEventAllocatedRow(
                                            item = item,
                                            isIncome = false,
                                            onEdit = { viewModel.startEditingItem(item) },
                                            onRemove = { viewModel.removeTransaction(item.transaction.id) },
                                            enabled = !state.isUpdating
                                        )
                                    }
                                }
                            }

                            2 -> {
                                // Reimbursements Tab
                                if (state.allocatedReimbursements.isEmpty()) {
                                    EmptyEventSection(
                                        message = "No reimbursements linked to this event.",
                                        actionText = if (state.availableReimbursements.isNotEmpty()) "Add Reimbursements" else null,
                                        onAction = { addMode = AddTransactionMode.REIMBURSEMENT }
                                    )
                                } else {
                                    state.allocatedReimbursements.forEach { item ->
                                        FinancialEventAllocatedRow(
                                            item = item,
                                            isIncome = true,
                                            onEdit = { viewModel.startEditingItem(item) },
                                            onRemove = { viewModel.removeTransaction(item.transaction.id) },
                                            enabled = !state.isUpdating
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    // Month-wise transaction picker dialog
                    if (addMode != null) {
                        val availableList = when (addMode) {
                            AddTransactionMode.EXPENSE -> state.availableExpenses
                            AddTransactionMode.REIMBURSEMENT -> state.availableReimbursements
                            null -> emptyList()
                        }

                        MonthWiseTransactionPickerDialog(
                            title = when (addMode) {
                                AddTransactionMode.EXPENSE -> "Add Expenses to Event"
                                AddTransactionMode.REIMBURSEMENT -> "Add Reimbursements to Event"
                                null -> ""
                            },
                            isIncome = addMode == AddTransactionMode.REIMBURSEMENT,
                            transactions = availableList,
                            isUpdating = state.isUpdating,
                            onDismiss = { addMode = null },
                            onConfirm = { allocationsMap ->
                                when (addMode) {
                                    AddTransactionMode.EXPENSE -> viewModel.addExpensesWithAmounts(allocationsMap)
                                    AddTransactionMode.REIMBURSEMENT -> viewModel.addReimbursementsWithAmounts(allocationsMap)
                                    null -> Unit
                                }
                                addMode = null
                            }
                        )
                    }

                    // Edit Group Dialog
                    if (state.isEditingGroup) {
                        EditFinancialEventDialog(
                            initialName = state.group.groupName,
                            initialCategory = state.group.category,
                            initialExpectedReimbursement = state.group.expectedReimbursement,
                            initialIsManuallySettled = state.group.isManuallySettled,
                            totalExpenses = state.totalExpenses,
                            categories = state.categories,
                            onDismiss = viewModel::cancelEditingGroup,
                            onSave = viewModel::saveGroup
                        )
                    }

                    // Edit Item Allocation Dialog
                    if (state.editingItem != null) {
                        val item = state.editingItem
                        EditItemAmountDialog(
                            item = item,
                            onDismiss = viewModel::cancelEditingItem,
                            onSave = { newAmount ->
                                viewModel.updateItemAllocationAmount(item.transaction.id, newAmount)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionSubheader(
    title: String,
    count: Int,
    total: Double,
    isExpense: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "₹%,.2f".format(total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
        )
    }
}

@Composable
private fun EventHeroHeaderCard(
    group: TransactionLinkGroup,
    totalExpenses: Double,
    totalReimbursements: Double,
    actualExpense: Double,
    statusSummary: FinancialEventStatusSummary,
    onEditGroup: () -> Unit,
    onToggleSettled: () -> Unit,
    enabled: Boolean
) {
    val categoryColor = CategoryPalette.colorFor(group.category)
    val categoryIcon = CategoryIconCatalog.iconFor(group.category)
    val statusPalette = rememberFinancialEventStatusPalette(statusSummary.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Category Badge + Title + Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = group.category,
                            tint = categoryColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.groupName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                            Text(
                                text = group.category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                FilledTonalIconButton(
                    onClick = onEditGroup,
                    enabled = enabled,
                    modifier = Modifier.testTag("edit_financial_event_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Financial Event"
                    )
                }
            }

            // Settlement Status Strip + Quick Toggle
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = statusPalette.containerColor,
                border = BorderStroke(1.dp, statusPalette.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FinancialEventStatusBadge(
                            summary = statusSummary,
                            modifier = Modifier.testTag("event_detail_status_badge")
                        )
                        if (statusSummary.statusMessage.isNotBlank()) {
                            Text(
                                text = statusSummary.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (statusSummary.isManuallySettled || statusSummary.status != FinancialEventStatus.SETTLED) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onToggleSettled,
                            enabled = enabled,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("toggle_event_settled_button"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (statusSummary.isManuallySettled) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    statusPalette.contentColor.copy(alpha = 0.18f)
                                },
                                contentColor = if (statusSummary.isManuallySettled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    statusPalette.contentColor
                                }
                            )
                        ) {
                            Icon(
                                imageVector = if (statusSummary.isManuallySettled) Icons.Default.Edit else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (statusSummary.isManuallySettled) "Reopen" else "Mark Settled",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Net Calculation Overview Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary Net Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            )
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "My Actual Expense",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Net Out-of-Pocket",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "₹%,.2f".format(actualExpense),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Two sub-cards: Gross Expenses & Reimbursements
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Expenses Mini Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Gross Expenses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "₹%,.2f".format(totalExpenses),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Total Reimbursements Mini Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Reimbursements",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "₹%,.2f".format(totalReimbursements),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Reimbursement offset progress toward expected roommates' share or gross expense
                if (statusSummary.targetReimbursement > 0.0 || totalExpenses > 0.0) {
                    val progress = statusSummary.recoveryRatio
                    val offsetPercent = statusSummary.recoveryPercent
                    val leftLabel = if (statusSummary.hasExpectedReimbursement) {
                        "Target: ₹%,.0f (My share: ₹%,.0f)".format(
                            statusSummary.targetReimbursement,
                            statusSummary.personalShare ?: 0.0
                        )
                    } else {
                        "Reimbursement Offset"
                    }

                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = leftLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$offsetPercent% covered",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusPalette.contentColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = statusPalette.contentColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialEventAllocatedRow(
    item: FinancialEventItemUiModel,
    isIncome: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    val date = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(item.transaction.dateTimestamp))
    val categoryColor = CategoryPalette.colorFor(item.transaction.category)
    val categoryIcon = CategoryIconCatalog.iconFor(item.transaction.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = item.transaction.category,
                        tint = categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Description + Date
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.transaction.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Allocated Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹%,.2f".format(item.allocatedAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                    if (item.isPartial) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${item.percent}% of ₹" + "%,.0f".format(item.totalTransactionAmount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isPartial) {
                    Text(
                        text = "Partial Allocation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Full Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onEdit,
                        enabled = enabled,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Amount", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onRemove,
                        enabled = enabled,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove from Event",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyEventSection(
    message: String,
    actionText: String?,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionText != null) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
private fun MonthWiseTransactionPickerDialog(
    title: String,
    isIncome: Boolean,
    transactions: List<AvailableTransactionUiModel>,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Map<Long, Double>) -> Unit
) {
    val selectedAmounts = remember { mutableStateMapOf<Long, Double>() }

    var expandedMonth by remember {
        mutableStateOf(transactions.firstOrNull()?.let { monthKey(it.transaction.dateTimestamp) })
    }

    val groupedTransactions = remember(transactions) {
        transactions
            .sortedByDescending { it.transaction.dateTimestamp }
            .groupBy { monthKey(it.transaction.dateTimestamp) }
    }

    Dialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isIncome) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isIncome) Icons.Default.Savings else Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${transactions.size} transactions available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Grouped Month List
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedTransactions.forEach { (mKey, monthTransactions) ->
                        val firstItem = monthTransactions.first()
                        val isMonthExpanded = expandedMonth == mKey
                        val monthSelectedCount = monthTransactions.count { it.transaction.id in selectedAmounts }

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isMonthExpanded) MaterialTheme.colorScheme.surfaceContainerLow
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isUpdating) {
                                            expandedMonth = if (isMonthExpanded) null else mKey
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = monthLabel(firstItem.transaction.dateTimestamp),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = monthTransactions.size.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (monthSelectedCount > 0) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    text = "$monthSelectedCount selected",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = if (isMonthExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isMonthExpanded,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        monthTransactions.forEach { avail ->
                                            TransactionPickerRow(
                                                avail = avail,
                                                allocatedAmount = selectedAmounts[avail.transaction.id],
                                                enabled = !isUpdating,
                                                onToggle = { isSelected ->
                                                    if (isSelected) {
                                                        selectedAmounts[avail.transaction.id] = avail.remainingAmount
                                                    } else {
                                                        selectedAmounts.remove(avail.transaction.id)
                                                    }
                                                },
                                                onAmountChange = { newAmt ->
                                                    selectedAmounts[avail.transaction.id] = newAmt
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary & Action Bar
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selectedAmounts.isNotEmpty()) {
                        val total = selectedAmounts.values.sum()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedAmounts.size} Selected",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Total: ₹%,.2f".format(total),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isUpdating,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = { onConfirm(selectedAmounts.toMap()) },
                            enabled = selectedAmounts.isNotEmpty() && !isUpdating,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Link Selected")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionPickerRow(
    avail: AvailableTransactionUiModel,
    allocatedAmount: Double?,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onAmountChange: (Double) -> Unit
) {
    val isSelected = allocatedAmount != null
    var isEditingCustomAmount by remember { mutableStateOf(false) }
    var customAmountText by remember(allocatedAmount) {
        mutableStateOf("%.2f".format(allocatedAmount ?: avail.remainingAmount))
    }

    val categoryColor = CategoryPalette.colorFor(avail.transaction.category)
    val categoryIcon = CategoryIconCatalog.iconFor(avail.transaction.category)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (isSelected) listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                else listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onToggle(!isSelected) },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle(it) },
                    enabled = enabled
                )

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = avail.transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(avail.transaction.dateTimestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹%,.2f".format(avail.remainingAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (avail.isPartiallyAllocated) {
                        Text(
                            text = "of ₹%,.0f".format(avail.totalAmount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanded customization controls when checked
            if (isSelected) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp, end = 4.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick percentage chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "100%").forEach { (ratio, label) ->
                            val targetAmt = avail.remainingAmount * ratio
                            val isCurrent = (allocatedAmount ?: 0.0) == targetAmt || (ratio == 1.0 && (allocatedAmount ?: 0.0) == avail.remainingAmount)
                            Surface(
                                shape = CircleShape,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    onAmountChange(targetAmt)
                                    customAmountText = "%.2f".format(targetAmt)
                                }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (isEditingCustomAmount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = customAmountText,
                                onValueChange = {
                                    customAmountText = it
                                    val parsed = it.toDoubleOrNull()
                                    if (parsed != null && parsed > 0 && parsed <= avail.remainingAmount + 0.01) {
                                        onAmountChange(parsed)
                                    }
                                },
                                label = { Text("Allocate (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            TextButton(onClick = { isEditingCustomAmount = false }) {
                                Text("Done")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Allocated: ₹%,.2f".format(allocatedAmount ?: 0.0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = { isEditingCustomAmount = true }) {
                                Text("Custom Amount", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditItemAmountDialog(
    item: FinancialEventItemUiModel,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amountText by remember(item) {
        mutableStateOf("%.2f".format(item.allocatedAmount))
    }
    val amountVal = amountText.toDoubleOrNull() ?: 0.0
    val isValid = amountVal > 0.0 && amountVal <= (item.totalTransactionAmount + 0.01)

    val categoryColor = CategoryPalette.colorFor(item.transaction.category)
    val categoryIcon = CategoryIconCatalog.iconFor(item.transaction.category)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text("Edit Allocation Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(categoryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.transaction.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Total: ₹%,.2f".format(item.totalTransactionAmount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quick percentage shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "100%").forEach { (ratio, label) ->
                        val target = item.totalTransactionAmount * ratio
                        OutlinedButton(
                            onClick = { amountText = "%.2f".format(target) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Allocated Amount (₹)") },
                    supportingText = {
                        Text("Max allowed: ₹%,.2f".format(item.totalTransactionAmount))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountText.isNotBlank() && !isValid,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onSave(amountVal) },
                enabled = isValid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFinancialEventDialog(
    initialName: String,
    initialCategory: String,
    initialExpectedReimbursement: Double?,
    initialIsManuallySettled: Boolean,
    totalExpenses: Double,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, Double?, Boolean) -> Unit
) {
    var groupName by remember(initialName) { mutableStateOf(initialName) }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var expectedText by remember(initialExpectedReimbursement) {
        mutableStateOf(
            initialExpectedReimbursement?.let { value ->
                if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(Locale.US, value)
            }.orEmpty()
        )
    }
    var isManuallySettled by remember(initialIsManuallySettled) { mutableStateOf(initialIsManuallySettled) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val parsedExpected: Double? = expectedText.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
    val isExpectedValid = expectedText.isBlank() || (parsedExpected != null && parsedExpected >= 0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text("Edit Financial Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Event Name") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Event, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selection with ExposedDropdownMenuBox
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentCategoryColor = CategoryPalette.colorFor(category)
                    val currentCategoryIcon = CategoryIconCatalog.iconFor(category)

                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(currentCategoryColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = currentCategoryIcon,
                                    contentDescription = null,
                                    tint = currentCategoryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { availableCategory ->
                            val catColor = CategoryPalette.colorFor(availableCategory)
                            val catIcon = CategoryIconCatalog.iconFor(availableCategory)

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(catColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = catIcon,
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(availableCategory, fontWeight = if (availableCategory == category) FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                onClick = {
                                    category = availableCategory
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Expected Reimbursements from roommates (Optional)
                OutlinedTextField(
                    value = expectedText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            expectedText = input
                        }
                    },
                    label = { Text("Expected Reimbursements (Optional)") },
                    placeholder = { Text("Roommates' total share") },
                    prefix = { Text("₹") },
                    trailingIcon = {
                        if (expectedText.isNotEmpty()) {
                            IconButton(onClick = { expectedText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear expected share"
                                )
                            }
                        }
                    },
                    isError = !isExpectedValid,
                    supportingText = {
                        val help = when {
                            !isExpectedValid -> "Enter a valid non-negative amount"
                            parsedExpected != null && totalExpenses > parsedExpected ->
                                "Your personal share: ₹%,.0f".format(totalExpenses - parsedExpected)
                            parsedExpected != null ->
                                "Roommates' share target: ₹%,.0f".format(parsedExpected)
                            totalExpenses > 0.0 ->
                                "Leave blank to compare against full ₹%,.0f".format(totalExpenses)
                            else -> "Optional target reimbursement amount"
                        }
                        Text(help)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expected_reimbursement_input")
                )

                // Quick Roommate Split Presets
                if (totalExpenses > 0.0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "1/2 Share" to (totalExpenses * 0.5),
                            "2/3 Share" to (totalExpenses * (2.0 / 3.0)),
                            "3/4 Share" to (totalExpenses * 0.75)
                        )
                        presets.forEach { (label, share) ->
                            val roundedShare = (share * 100.0).roundToInt() / 100.0
                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        expectedText = if (roundedShare % 1.0 == 0.0) {
                                            roundedShare.toLong().toString()
                                        } else {
                                            "%.2f".format(Locale.US, roundedShare)
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Manual Settled Switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isManuallySettled = !isManuallySettled }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Mark as Settled",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Use when settled in cash or your own share remains",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isManuallySettled,
                            onCheckedChange = { isManuallySettled = it },
                            modifier = Modifier.testTag("edit_event_settled_switch")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(groupName, category, parsedExpected, isManuallySettled) },
                enabled = groupName.isNotBlank() && category.isNotBlank() && isExpectedValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_financial_event_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun monthKey(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date(timestamp))
}

private fun monthLabel(timestamp: Long): String {
    return SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date(timestamp))
}
