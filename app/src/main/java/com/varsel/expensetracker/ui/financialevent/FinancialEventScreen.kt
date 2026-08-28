package com.varsel.expensetracker.ui.financialevent

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    LaunchedEffect(transactionLinkId) {
        viewModel.loadFinancialEvent(transactionLinkId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Financial Event")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                FinancialEventUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is FinancialEventUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                is FinancialEventUiState.Loaded -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Card
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.group.groupName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Category: ${state.group.category}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = viewModel::startEditingGroup,
                                        enabled = !state.isUpdating
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Event Name / Category",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Expenses Section
                        Text(
                            text = "Expenses (${state.allocatedExpenses.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (state.allocatedExpenses.isEmpty()) {
                            Text(
                                text = "No expenses allocated to this event.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.allocatedExpenses.forEach { item ->
                                FinancialEventAllocatedRow(
                                    item = item,
                                    onEdit = { viewModel.startEditingItem(item) },
                                    onRemove = { viewModel.removeTransaction(item.transaction.id) },
                                    enabled = !state.isUpdating
                                )
                            }
                        }

                        if (state.availableExpenses.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { addMode = AddTransactionMode.EXPENSE },
                                enabled = !state.isUpdating,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add / Allocate Expenses")
                            }
                        }

                        // Reimbursements Section
                        Text(
                            text = "Reimbursements (${state.allocatedReimbursements.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (state.allocatedReimbursements.isEmpty()) {
                            Text(
                                text = "No reimbursements allocated to this event.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.allocatedReimbursements.forEach { item ->
                                FinancialEventAllocatedRow(
                                    item = item,
                                    onEdit = { viewModel.startEditingItem(item) },
                                    onRemove = { viewModel.removeTransaction(item.transaction.id) },
                                    enabled = !state.isUpdating
                                )
                            }
                        }

                        if (state.availableReimbursements.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { addMode = AddTransactionMode.REIMBURSEMENT },
                                enabled = !state.isUpdating,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add / Allocate Reimbursements")
                            }
                        }

                        // Event Summary Card
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Event Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Allocated Expenses:")
                                    Text("₹%,.2f".format(state.totalExpenses), fontWeight = FontWeight.SemiBold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Allocated Reimbursements:")
                                    Text("₹%,.2f".format(state.totalReimbursements), fontWeight = FontWeight.SemiBold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("My Actual Expense:", fontWeight = FontWeight.Bold)
                                    Text(
                                        "₹%,.2f".format(state.actualExpense),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Month-wise transaction picker
                        if (addMode != null) {
                            val availableList = when (addMode) {
                                AddTransactionMode.EXPENSE -> state.availableExpenses
                                AddTransactionMode.REIMBURSEMENT -> state.availableReimbursements
                                null -> emptyList()
                            }

                            MonthWiseTransactionPicker(
                                title = when (addMode) {
                                    AddTransactionMode.EXPENSE -> "Add Expenses"
                                    AddTransactionMode.REIMBURSEMENT -> "Add Reimbursements"
                                    null -> ""
                                },
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

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialEventAllocatedRow(
    item: FinancialEventItemUiModel,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    val date = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(item.transaction.dateTimestamp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹%,.2f".format(item.allocatedAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.isPartial) {
                        Text(
                            text = "of ₹%,.2f (%d%%)".format(item.totalTransactionAmount, item.percent),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = enabled,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Edit Amount")
                }

                IconButton(
                    onClick = onRemove,
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from Event",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthWiseTransactionPicker(
    title: String,
    transactions: List<AvailableTransactionUiModel>,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Map<Long, Double>) -> Unit
) {
    val selectedAmounts = remember { mutableStateMapOf<Long, Double>() }

    var expandedMonth by remember {
        mutableStateOf(transactions.firstOrNull()?.let { monthKey(it.transaction.dateTimestamp) })
    }

    val groupedTransactions = transactions
        .sortedByDescending { it.transaction.dateTimestamp }
        .groupBy { monthKey(it.transaction.dateTimestamp) }

    AlertDialog(
        onDismissRequest = {
            if (!isUpdating) onDismiss()
        },
        title = {
            Text(title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select transactions and optionally customize allocated amounts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                groupedTransactions.forEach { (_, monthTransactions) ->
                    val firstItem = monthTransactions.first()
                    val key = monthKey(firstItem.transaction.dateTimestamp)
                    val monthSelectedCount = monthTransactions.count { it.transaction.id in selectedAmounts }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            OutlinedButton(
                                onClick = {
                                    expandedMonth = if (expandedMonth == key) null else key
                                },
                                enabled = !isUpdating,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(monthLabel(firstItem.transaction.dateTimestamp))
                                    if (monthSelectedCount > 0) {
                                        Text(
                                            "$monthSelectedCount selected",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (expandedMonth == key) {
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

                if (selectedAmounts.isNotEmpty()) {
                    val total = selectedAmounts.values.sum()
                    Text(
                        text = "${selectedAmounts.size} selected | Total: ₹%,.2f".format(total),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedAmounts.toMap()) },
                enabled = selectedAmounts.isNotEmpty() && !isUpdating
            ) {
                Text(if (isUpdating) "Adding..." else "Add Selected")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text("Cancel")
            }
        }
    )
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { checked ->
                    onToggle(checked)
                },
                enabled = enabled
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = avail.transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(avail.transaction.dateTimestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Avail: ₹%,.2f".format(avail.remainingAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (avail.remainingAmount < avail.totalAmount - 0.01) {
                    Text(
                        text = "Total: ₹%,.2f".format(avail.totalAmount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isSelected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditingCustomAmount) {
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
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { isEditingCustomAmount = false }) {
                        Text("Done")
                    }
                } else {
                    Text(
                        text = "Allocated: ₹%,.2f".format(allocatedAmount ?: 0.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { isEditingCustomAmount = true }) {
                        Text("Custom Amount")
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Allocation Amount")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Total transaction amount: ₹%,.2f".format(item.totalTransactionAmount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Allocated Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountText.isNotBlank() && !isValid,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) onSave(amountVal)
                },
                enabled = isValid
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

private fun monthKey(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date(timestamp))
}

private fun monthLabel(timestamp: Long): String {
    return SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFinancialEventDialog(
    initialName: String,
    initialCategory: String,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var groupName by remember(initialName) { mutableStateOf(initialName) }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Financial Event") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Event name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            Text(if (categoryExpanded) "▲" else "▼")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { categoryExpanded = !categoryExpanded }
                    )
                }

                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { availableCategory ->
                        DropdownMenuItem(
                            text = { Text(availableCategory) },
                            onClick = {
                                category = availableCategory
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(groupName, category) },
                enabled = groupName.isNotBlank() && category.isNotBlank() && category in categories
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
