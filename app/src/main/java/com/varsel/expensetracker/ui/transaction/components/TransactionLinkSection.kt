package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.category.CategoryMetadata
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import com.varsel.expensetracker.ui.transaction.TransactionEventAllocationUiModel

@Composable
fun TransactionLinkSection(
    allocations: List<TransactionEventAllocationUiModel>,
    totalAllocatedAmount: Double,
    remainingUnallocatedAmount: Double,
    totalTransactionAmount: Double,
    allAvailableEventGroups: List<TransactionLinkGroup>,
    showCreateGroupPrompt: Boolean,
    showAllocateExistingPrompt: Boolean,
    editingAllocation: TransactionEventAllocationUiModel?,
    allocationErrorMessage: String?,
    isSavingGroup: Boolean,
    categories: List<String>,
    onManageFinancialEvent: (transactionLinkId: String) -> Unit,
    onShowCreateFinancialEvent: () -> Unit,
    onDismissCreateGroupPrompt: () -> Unit,
    onCreateReportGroup: (groupName: String, category: String, amount: Double) -> Unit,
    onShowAllocateExisting: () -> Unit,
    onDismissAllocateExisting: () -> Unit,
    onAllocateToExistingGroup: (transactionLinkId: String, amount: Double) -> Unit,
    onStartEditingAllocation: (allocation: TransactionEventAllocationUiModel) -> Unit,
    onDismissEditingAllocation: () -> Unit,
    onUpdateAllocationAmount: (transactionLinkId: String, newAmount: Double) -> Unit,
    onDeleteAllocation: (transactionLinkId: String) -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Financial Event Allocations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (allocations.isNotEmpty()) {
                Text(
                    text = "${allocations.size} linked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Transaction:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "₹%,.2f".format(totalTransactionAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Allocated to Events:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "₹%,.2f".format(totalAllocatedAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Remaining Balance:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (remainingUnallocatedAmount > 0.01) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "₹%,.2f".format(remainingUnallocatedAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingUnallocatedAmount > 0.01) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                    )
                }

                if (totalTransactionAmount > 0.0) {
                    val progress = (totalAllocatedAmount / totalTransactionAmount).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(top = 4.dp)
                    )
                }
            }
        }

        // Error message if any
        if (allocationErrorMessage != null) {
            Text(
                text = allocationErrorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // List of existing allocations
        if (allocations.isNotEmpty()) {
            allocations.forEach { allocation ->
                AllocationItemCard(
                    allocation = allocation,
                    onManage = { onManageFinancialEvent(allocation.transactionLinkId) },
                    onEdit = { onStartEditingAllocation(allocation) },
                    onDelete = { onDeleteAllocation(allocation.transactionLinkId) }
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (allAvailableEventGroups.isNotEmpty()) {
                OutlinedButton(
                    onClick = onShowAllocateExisting,
                    enabled = !isSavingGroup && remainingUnallocatedAmount > 0.009,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Link Existing")
                }
            }

            Button(
                onClick = onShowCreateFinancialEvent,
                enabled = !isSavingGroup && remainingUnallocatedAmount > 0.009,
                modifier = Modifier.weight(1f)
            ) {
                Text("New Event")
            }
        }

        // Dialogs
        if (showCreateGroupPrompt) {
            CreateReportGroupDialog(
                categories = categories,
                initialAmount = if (remainingUnallocatedAmount > 0.0) remainingUnallocatedAmount else totalTransactionAmount,
                maxAmount = if (remainingUnallocatedAmount > 0.0) remainingUnallocatedAmount else totalTransactionAmount,
                isSaving = isSavingGroup,
                onDismiss = onDismissCreateGroupPrompt,
                onCreate = onCreateReportGroup
            )
        }

        if (showAllocateExistingPrompt) {
            AllocateExistingGroupDialog(
                availableGroups = allAvailableEventGroups,
                initialAmount = remainingUnallocatedAmount,
                maxAmount = remainingUnallocatedAmount,
                isSaving = isSavingGroup,
                onDismiss = onDismissAllocateExisting,
                onAllocate = onAllocateToExistingGroup
            )
        }

        if (editingAllocation != null) {
            EditAllocationAmountDialog(
                allocation = editingAllocation,
                maxAmount = totalTransactionAmount - (totalAllocatedAmount - editingAllocation.allocatedAmount),
                onDismiss = onDismissEditingAllocation,
                onSave = { newAmount ->
                    onUpdateAllocationAmount(editingAllocation.transactionLinkId, newAmount)
                }
            )
        }
    }
}

@Composable
private fun AllocationItemCard(
    allocation: TransactionEventAllocationUiModel,
    onManage: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = allocation.groupName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = allocation.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹%,.2f".format(allocation.allocatedAmount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${allocation.percent}% of total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onManage,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text("View Event")
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Amount",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Allocation",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateReportGroupDialog(
    categories: List<String>,
    initialAmount: Double,
    maxAmount: Double,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (groupName: String, category: String, amount: Double) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val availableCategories = remember {
        CategoryMetadata.all
            .map { it.id }
            .filter { it.isNotBlank() }
            .distinct()
    }
    var category by remember(availableCategories) {
        mutableStateOf(availableCategories.firstOrNull() ?: "")
    }
    var amountText by remember {
        mutableStateOf("%.2f".format(initialAmount))
    }
    var categoryExpanded by remember { mutableStateOf(false) }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val isValidAmount = amountValue > 0.0 && amountValue <= (maxAmount + 0.01)

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = {
            Text("Create Event & Allocate")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Event Name") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            Text(if (categoryExpanded) "▲" else "▼")
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = !isSaving) {
                                categoryExpanded = !categoryExpanded
                            }
                    )
                }

                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    availableCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Allocated Amount (₹)") },
                    supportingText = {
                        Text("Max available: ₹%.2f".format(maxAmount))
                    },
                    isError = amountText.isNotBlank() && !isValidAmount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValidAmount && groupName.isNotBlank() && category.isNotBlank()) {
                        onCreate(groupName, category, amountValue)
                    }
                },
                enabled = !isSaving && groupName.isNotBlank() && category.isNotBlank() && isValidAmount
            ) {
                Text(if (isSaving) "Saving..." else "Create & Allocate")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AllocateExistingGroupDialog(
    availableGroups: List<TransactionLinkGroup>,
    initialAmount: Double,
    maxAmount: Double,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAllocate: (transactionLinkId: String, amount: Double) -> Unit
) {
    var selectedGroup by remember(availableGroups) {
        mutableStateOf(availableGroups.firstOrNull())
    }
    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var amountText by remember {
        mutableStateOf("%.2f".format(initialAmount))
    }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val isValidAmount = amountValue > 0.0 && amountValue <= (maxAmount + 0.01)

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = {
            Text("Allocate to Financial Event")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Event selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedGroup?.groupName ?: "Select an Event",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Financial Event") },
                        trailingIcon = {
                            Text(if (groupDropdownExpanded) "▲" else "▼")
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = !isSaving) {
                                groupDropdownExpanded = !groupDropdownExpanded
                            }
                    )
                }

                DropdownMenu(
                    expanded = groupDropdownExpanded,
                    onDismissRequest = { groupDropdownExpanded = false }
                ) {
                    availableGroups.forEach { group ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(group.groupName, fontWeight = FontWeight.SemiBold)
                                    Text(group.category, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            onClick = {
                                selectedGroup = group
                                groupDropdownExpanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Allocated Amount (₹)") },
                    supportingText = {
                        Text("Max available: ₹%.2f".format(maxAmount))
                    },
                    isError = amountText.isNotBlank() && !isValidAmount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val group = selectedGroup
                    if (group != null && isValidAmount) {
                        onAllocate(group.transactionLinkId, amountValue)
                    }
                },
                enabled = !isSaving && selectedGroup != null && isValidAmount
            ) {
                Text(if (isSaving) "Allocating..." else "Allocate")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditAllocationAmountDialog(
    allocation: TransactionEventAllocationUiModel,
    maxAmount: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amountText by remember(allocation) {
        mutableStateOf("%.2f".format(allocation.allocatedAmount))
    }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val isValidAmount = amountValue > 0.0 && amountValue <= (maxAmount + 0.01)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Allocated Amount")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Event: ${allocation.groupName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    supportingText = {
                        Text("Maximum allowed: ₹%.2f".format(maxAmount))
                    },
                    isError = amountText.isNotBlank() && !isValidAmount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValidAmount) {
                        onSave(amountValue)
                    }
                },
                enabled = isValidAmount
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
