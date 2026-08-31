package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.category.CategoryMetadata
import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import com.varsel.expensetracker.ui.design.CategoryPalette
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Financial Event Allocations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (allocations.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${allocations.size} linked",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Transaction Amount:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹%,.2f".format(totalAllocatedAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Remaining Unallocated:",
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
                            .clip(CircleShape)
                            .padding(top = 2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Link Existing")
                }
            }

            Button(
                onClick = onShowCreateFinancialEvent,
                enabled = !isSavingGroup && remainingUnallocatedAmount > 0.009,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
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
    val categoryColor = CategoryPalette.colorFor(allocation.category)
    val categoryIcon = CategoryIconCatalog.iconFor(allocation.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = allocation.category,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = allocation.groupName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = allocation.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onManage,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("View Event", fontSize = 12.sp)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Allocation",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val availableCategories = remember(categories) {
        val staticCategories = CategoryMetadata.all
            .map { it.id }
            .filter { it.isNotBlank() }
        (categories + staticCategories).filter { it.isNotBlank() }.distinct()
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
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text("Create Event & Allocate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Event Name") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Event, contentDescription = null)
                    },
                    singleLine = true,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown with ExposedDropdownMenuBox
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { if (!isSaving) categoryExpanded = it },
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
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        availableCategories.forEach { cat ->
                            val catColor = CategoryPalette.colorFor(cat)
                            val catIcon = CategoryIconCatalog.iconFor(cat)

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(catColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = catIcon,
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(cat, fontWeight = if (cat == category) FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quick percentage helper chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "Max").forEach { (ratio, label) ->
                        val target = maxAmount * ratio
                        OutlinedButton(
                            onClick = { amountText = "%.2f".format(target) },
                            enabled = !isSaving,
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
                        Text("Max available: ₹%,.2f".format(maxAmount))
                    },
                    isError = amountText.isNotBlank() && !isValidAmount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
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
                enabled = !isSaving && groupName.isNotBlank() && category.isNotBlank() && isValidAmount,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create & Allocate")
                }
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

@OptIn(ExperimentalMaterial3Api::class)
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
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text("Allocate to Financial Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Event selector with ExposedDropdownMenuBox
                ExposedDropdownMenuBox(
                    expanded = groupDropdownExpanded,
                    onExpandedChange = { if (!isSaving) groupDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val group = selectedGroup
                    val catColor = group?.let { CategoryPalette.colorFor(it.category) } ?: MaterialTheme.colorScheme.primary
                    val catIcon = group?.let { CategoryIconCatalog.iconFor(it.category) } ?: Icons.Default.Event

                    OutlinedTextField(
                        value = selectedGroup?.groupName ?: "Select an Event",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Financial Event") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(catColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = groupDropdownExpanded,
                        onDismissRequest = { groupDropdownExpanded = false }
                    ) {
                        availableGroups.forEach { grp ->
                            val itemColor = CategoryPalette.colorFor(grp.category)
                            val itemIcon = CategoryIconCatalog.iconFor(grp.category)

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
                                                .background(itemColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = itemIcon,
                                                contentDescription = null,
                                                tint = itemColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Column {
                                            Text(grp.groupName, fontWeight = if (grp == selectedGroup) FontWeight.Bold else FontWeight.Normal)
                                            Text(grp.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedGroup = grp
                                    groupDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quick percentage shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "Max").forEach { (ratio, label) ->
                        val target = maxAmount * ratio
                        OutlinedButton(
                            onClick = { amountText = "%.2f".format(target) },
                            enabled = !isSaving,
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
                        Text("Max available: ₹%,.2f".format(maxAmount))
                    },
                    isError = amountText.isNotBlank() && !isValidAmount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
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
                enabled = !isSaving && selectedGroup != null && isValidAmount,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Allocate")
                }
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

    val categoryColor = CategoryPalette.colorFor(allocation.category)
    val categoryIcon = CategoryIconCatalog.iconFor(allocation.category)

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
                Text("Edit Allocated Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                        Column {
                            Text(
                                text = allocation.groupName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = allocation.category,
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
                    listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "Max").forEach { (ratio, label) ->
                        val target = maxAmount * ratio
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
                    label = { Text("Amount (₹)") },
                    supportingText = {
                        Text("Maximum allowed: ₹%,.2f".format(maxAmount))
                    },
                    isError = amountText.isNotBlank() && !isValidAmount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
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
                enabled = isValidAmount,
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
