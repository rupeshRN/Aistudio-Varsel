package com.varsel.expensetracker.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.ui.transaction.components.BottomActionBar
import com.varsel.expensetracker.ui.transaction.components.CategorySection
import com.varsel.expensetracker.ui.transaction.components.DescriptionSection
import com.varsel.expensetracker.ui.transaction.components.TransactionInfoSection
import com.varsel.expensetracker.ui.transaction.components.TransactionLinkSection
import com.varsel.expensetracker.ui.transaction.components.TransferLinkSection
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    viewModel: TransactionDetailViewModel,
    onBackClick: () -> Unit,
    onFinancialEventClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveCompleted by viewModel.saveCompleted.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var rememberSmartRule by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showImportedLockedInfoDialog by remember { mutableStateOf(false) }

    //--------------------------------------------------
    // Load transaction
    //--------------------------------------------------
    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    //--------------------------------------------------
    // Handle successful save
    //--------------------------------------------------
    LaunchedEffect(saveCompleted) {
        if (saveCompleted) {
            viewModel.consumeSaveCompleted()
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Transaction Details")
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
        },
        bottomBar = {
            val state = uiState as? TransactionDetailUiState.Loaded
            if (state != null) {
                val isImported = state.transaction.isImported
                BottomActionBar(
                    onDeleteClick = {
                        if (isImported) {
                            showImportedLockedInfoDialog = true
                        } else {
                            showDeleteConfirmDialog = true
                        }
                    },
                    onSaveClick = {
                        rememberSmartRule = false
                        viewModel.setApplyToSimilar(false)
                        viewModel.prepareSaveSmartRuleDialog()
                        showSaveConfirmDialog = true
                    },
                    saveEnabled = state.hasChanges && !state.isSaving,
                    isImported = isImported
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = uiState) {
                TransactionDetailUiState.Loading -> {
                    Text("Loading...")
                }
                is TransactionDetailUiState.Error -> {
                    Text(state.message)
                }
                is TransactionDetailUiState.Loaded -> {
                    val transaction = state.transaction
                    val isIncome = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.CREDIT
                    val isTransfer = state.selectedRole == TransactionRole.TRANSFER_IN || state.selectedRole == TransactionRole.TRANSFER_OUT

                    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                    val headerColor = when {
                        isTransfer -> if (isDark) Color(0xFFD1C4E9) else Color(0xFF5E35B1)
                        isIncome -> if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
                        else -> if (isDark) Color(0xFFFF5252) else Color(0xFFC62828)
                    }

                    val typeLabel = if (isTransfer) "(Transfer)" else transaction.type.name

                    // Hero Transaction Header Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = (if (isIncome) "+ ₹" else "- ₹") + "%,.2f".format(kotlin.math.abs(transaction.amount)),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = headerColor
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = headerColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = headerColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (transaction.isImported) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = "Bank Statement",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Description
                    DescriptionSection(
                        description = state.editableDescription,
                        onDescriptionChanged = viewModel::updateDescription
                    )

                    // Category
                    CategorySection(
                        selectedCategory = state.selectedCategory,
                        transactionType = transaction.type,
                        availableCategories = state.categories,
                        onCategorySelected = viewModel::updateCategory,
                        onNewCategoryClick = {
                            newCategoryName = ""
                            showAddCategoryDialog = true
                        }
                    )

                    // Transaction Role
                    TransactionRoleSection(
                        transactionType = transaction.type,
                        selectedRole = state.selectedRole,
                        onRoleSelected = viewModel::updateRole
                    )

                    // Financial Event
                    if (state.selectedRole != TransactionRole.TRANSFER_IN &&
                        state.selectedRole != TransactionRole.TRANSFER_OUT
                    ) {
                        TransactionLinkSection(
                            allocations = state.allocations,
                            totalAllocatedAmount = state.totalAllocatedAmount,
                            remainingUnallocatedAmount = state.remainingUnallocatedAmount,
                            totalTransactionAmount = kotlin.math.abs(transaction.amount),
                            allAvailableEventGroups = state.allAvailableEventGroups,
                            showCreateGroupPrompt = state.showCreateGroupPrompt,
                            showAllocateExistingPrompt = state.showAllocateExistingPrompt,
                            editingAllocation = state.editingAllocation,
                            allocationErrorMessage = state.allocationErrorMessage,
                            isSavingGroup = state.isSavingGroup,
                            categories = state.categories,
                            onManageFinancialEvent = onFinancialEventClick,
                            onShowCreateFinancialEvent = viewModel::showCreateGroupPrompt,
                            onDismissCreateGroupPrompt = viewModel::dismissCreateGroupPrompt,
                            onCreateReportGroup = { groupName, category, amount ->
                                viewModel.createReportGroup(groupName, category, amount)
                            },
                            onShowAllocateExisting = viewModel::showAllocateExistingPrompt,
                            onDismissAllocateExisting = viewModel::dismissAllocateExistingPrompt,
                            onAllocateToExistingGroup = viewModel::allocateToExistingGroup,
                            onStartEditingAllocation = viewModel::startEditingAllocation,
                            onDismissEditingAllocation = viewModel::dismissEditingAllocation,
                            onUpdateAllocationAmount = viewModel::updateAllocationAmount,
                            onDeleteAllocation = viewModel::deleteAllocation,
                            onClearError = viewModel::clearAllocationError
                        )
                    }

                    // Transfer In / Transfer Out
                    if (state.selectedRole == TransactionRole.TRANSFER_IN ||
                        state.selectedRole == TransactionRole.TRANSFER_OUT
                    ) {
                        TransferLinkSection(
                            transaction = transaction,
                            linkedTransfer = state.linkedTransfer,
                            candidateTransactions = state.transferCandidates,
                            isLinking = state.isTransferLinking,
                            transferErrorMessage = state.transferErrorMessage,
                            onLinkTransfer = { candidateId ->
                                viewModel.linkTransfer(candidateId)
                            },
                            onUnlinkTransfer = {
                                viewModel.unlinkTransfer()
                            },
                            onClearError = {
                                viewModel.clearTransferError()
                            }
                        )
                    }

                    // Transaction Information
                    TransactionInfoSection(
                        transaction = transaction
                    )

                    Spacer(modifier = Modifier.padding(bottom = 24.dp))
                }
            }
        }
    }

    //--------------------------------------------------
    // Date Picker Dialog for Custom Cutoff Date
    //--------------------------------------------------
    if (showDatePicker) {
        val state = uiState as? TransactionDetailUiState.Loaded
        val initialDateMillis = state?.customCutoffTimestamp ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMillis = datePickerState.selectedDateMillis
                        if (pickedMillis != null) {
                            viewModel.setPastTimeframe(PastTimeframe.CUSTOM, pickedMillis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    //--------------------------------------------------
    // Save Confirmation Dialog (with Smart Rule toggle & Bulk Apply)
    //--------------------------------------------------
    if (showSaveConfirmDialog) {
        val state = uiState as? TransactionDetailUiState.Loaded
        if (state != null) {
            AlertDialog(
                onDismissRequest = { showSaveConfirmDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Save & Smart Rule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Description: ${state.editableDescription}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Category: ${state.selectedCategory}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Future Smart Rule Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { rememberSmartRule = !rememberSmartRule }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Checkbox(
                                checked = rememberSmartRule,
                                onCheckedChange = { rememberSmartRule = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Learn for future imports",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Auto-rename & categorize future bank imports",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Past Transactions Bulk Apply Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setApplyToSimilar(!state.applyToSimilarTransactions) }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Checkbox(
                                checked = state.applyToSimilarTransactions,
                                onCheckedChange = { viewModel.setApplyToSimilar(it) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Apply to similar past transactions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Update existing transactions up to selected date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Retroactive Filter / Date Configuration Card
                        AnimatedVisibility(
                            visible = state.applyToSimilarTransactions,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Match Count Banner
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (state.isLoadingSimilar) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = "Searching past transactions...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "${state.similarTransactionsCount} matching transactions found",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Apply to transactions from:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Timeframe Filter Chips
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PastTimeframe.entries.forEach { timeframe ->
                                            val isSelected = state.selectedPastTimeframe == timeframe
                                            val chipLabel = when (timeframe) {
                                                PastTimeframe.ALL_TIME -> "All Past"
                                                PastTimeframe.LAST_30_DAYS -> "30 Days"
                                                PastTimeframe.LAST_90_DAYS -> "3 Months"
                                                PastTimeframe.LAST_180_DAYS -> "6 Months"
                                                PastTimeframe.CUSTOM -> {
                                                    if (isSelected && state.customCutoffTimestamp != null) {
                                                        "Since " + SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(state.customCutoffTimestamp))
                                                    } else {
                                                        "Custom Date..."
                                                    }
                                                }
                                            }

                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    if (timeframe == PastTimeframe.CUSTOM) {
                                                        showDatePicker = true
                                                    } else {
                                                        viewModel.setPastTimeframe(timeframe)
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = chipLabel,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                trailingIcon = if (timeframe == PastTimeframe.CUSTOM) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Outlined.CalendarMonth,
                                                            contentDescription = "Pick date",
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                } else null
                                            )
                                        }
                                    }

                                    if (state.similarTransactionsCount > 0) {
                                        Text(
                                            text = "Will update category to \"${state.selectedCategory}\" for ${state.similarTransactionsCount} past transactions.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (state.editableDescription != state.transaction.description && state.editableDescription.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { viewModel.setUpdateDescriptionForSimilar(!state.updateDescriptionForSimilar) }
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Checkbox(
                                                    checked = state.updateDescriptionForSimilar,
                                                    onCheckedChange = { viewModel.setUpdateDescriptionForSimilar(it) }
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Also update description to \"${state.editableDescription}\"",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    } else if (!state.isLoadingSimilar) {
                                        Text(
                                            text = "No other matching past transactions found in this timeframe.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    val updateCount = if (state.applyToSimilarTransactions) state.similarTransactionsCount + 1 else 1
                    val buttonText = if (state.applyToSimilarTransactions && state.similarTransactionsCount > 0) {
                        "Save & Update ($updateCount)"
                    } else {
                        "Save"
                    }

                    Button(
                        onClick = {
                            showSaveConfirmDialog = false
                            viewModel.saveChanges(
                                createSmartRule = rememberSmartRule,
                                applyToSimilar = state.applyToSimilarTransactions,
                                updateDescriptionForSimilar = state.updateDescriptionForSimilar
                            )
                        }
                    ) {
                        Text(buttonText)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSaveConfirmDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    //--------------------------------------------------
    // Add Category Dialog
    //--------------------------------------------------
    if (showAddCategoryDialog) {
        val state = uiState as? TransactionDetailUiState.Loaded
        val isIncome = state?.transaction?.type == TransactionType.INCOME || state?.transaction?.type == TransactionType.CREDIT

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Create New Category")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isIncome) "Add a new Income category" else "Add a new Expense category",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Gym, Pet Care, Freelance") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.createCategory(newCategoryName.trim(), isIncome)
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Add & Select")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddCategoryDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    //--------------------------------------------------
    // Delete Manual Transaction Confirmation Dialog
    //--------------------------------------------------
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete Transaction?")
            },
            text = {
                Text("Are you sure you want to permanently delete this manual transaction?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteTransaction(onDeleted = onBackClick)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    //--------------------------------------------------
    // Imported Bank Transaction Deletion Locked Dialog
    //--------------------------------------------------
    if (showImportedLockedInfoDialog) {
        AlertDialog(
            onDismissRequest = { showImportedLockedInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Imported Bank Transaction",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This transaction was imported from your official bank statement. Deletion is blocked to preserve bank reconciliation, account statement integrity, and historical balance accuracy.")
            },
            confirmButton = {
                Button(
                    onClick = { showImportedLockedInfoDialog = false }
                ) {
                    Text("Understood")
                }
            }
        )
    }
}


