package com.varsel.expensetracker.ui.transaction

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var rememberSmartRule by remember { mutableStateOf(true) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                        if (!isImported) {
                            showDeleteConfirmDialog = true
                        }
                    },
                    onSaveClick = {
                        showSaveConfirmDialog = true
                    },
                    saveEnabled = state.hasChanges && !state.isSaving,
                    deleteEnabled = !isImported
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
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

                    if (transaction.isImported) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Imported bank transaction • Deletion is locked to preserve statement integrity.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                        amount = "₹%.2f".format(transaction.amount),
                        date = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(transaction.dateTimestamp)),
                        type = transaction.type.name
                    )

                    Spacer(modifier = Modifier.padding(bottom = 24.dp))
                }
            }
        }
    }

    //--------------------------------------------------
    // Save Confirmation Dialog (with Smart Rule toggle)
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
                        text = "Save Transaction Changes?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "You are about to save changes for this transaction.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = rememberSmartRule,
                                onCheckedChange = { rememberSmartRule = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Learn for future imports",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Auto-rename & categorize matching bank narrations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSaveConfirmDialog = false
                            viewModel.saveChanges(createSmartRule = rememberSmartRule)
                        }
                    ) {
                        Text("Save")
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
}

