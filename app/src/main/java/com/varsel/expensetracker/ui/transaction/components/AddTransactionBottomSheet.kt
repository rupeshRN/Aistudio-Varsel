package com.varsel.expensetracker.ui.transaction.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.transaction.model.AccountOption
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ManualEntryMode {
    EXPENSE,
    INCOME,
    TRANSFER
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionBottomSheet(
    onDismiss: () -> Unit,
    categories: List<CategoryEntity>,
    availableAccounts: List<AccountOption>,
    initialMode: ManualEntryMode = ManualEntryMode.EXPENSE,
    onSaveTransaction: (
        amount: Double,
        type: TransactionType,
        description: String,
        category: String,
        dateTimestamp: Long,
        referenceNumber: String?,
        accountId: String?,
        accountLast4: String?,
        bankName: String?
    ) -> Unit,
    onSaveTransfer: (
        amount: Double,
        description: String,
        dateTimestamp: Long,
        fromAccountId: String?,
        fromAccountLast4: String?,
        fromBankName: String?,
        toAccountId: String?,
        toAccountLast4: String?,
        toBankName: String?,
        referenceNumber: String?
    ) -> Unit,
    onCreateCategory: (name: String, isIncome: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var currentMode by remember { mutableStateOf(initialMode) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Account state
    val defaultAccount = availableAccounts.firstOrNull() ?: AccountOption(null, null, "Cash", "Cash / General")
    var selectedAccount by remember(availableAccounts) { mutableStateOf(defaultAccount) }
    var fromAccount by remember(availableAccounts) { mutableStateOf(defaultAccount) }
    var toAccount by remember(availableAccounts) {
        mutableStateOf(availableAccounts.getOrNull(1) ?: defaultAccount)
    }

    // Category state
    val filteredCategories = remember(categories, currentMode) {
        val typeFilter = when (currentMode) {
            ManualEntryMode.INCOME -> "INCOME"
            ManualEntryMode.EXPENSE -> "EXPENSE"
            ManualEntryMode.TRANSFER -> "BOTH"
        }
        categories.filter { it.type == typeFilter || it.type == "BOTH" }
    }

    var selectedCategory by remember(filteredCategories) {
        mutableStateOf(filteredCategories.firstOrNull()?.name ?: if (currentMode == ManualEntryMode.INCOME) "Salary" else "Other")
    }

    // Custom category dialog state
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val isAmountValid = amountValue > 0.0
    val isTransferValid = currentMode != ManualEntryMode.TRANSFER || (fromAccount != toAccount || availableAccounts.size <= 1)
    val canSave = isAmountValid && isTransferValid

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header & Tabs
            Text(
                text = when (currentMode) {
                    ManualEntryMode.EXPENSE -> "Add Expense"
                    ManualEntryMode.INCOME -> "Add Income"
                    ManualEntryMode.TRANSFER -> "Record Transfer"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            TabRow(
                selectedTabIndex = currentMode.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = currentMode == ManualEntryMode.EXPENSE,
                    onClick = { currentMode = ManualEntryMode.EXPENSE },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (currentMode == ManualEntryMode.EXPENSE) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Expense", fontWeight = if (currentMode == ManualEntryMode.EXPENSE) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = currentMode == ManualEntryMode.INCOME,
                    onClick = { currentMode = ManualEntryMode.INCOME },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (currentMode == ManualEntryMode.INCOME) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Income", fontWeight = if (currentMode == ManualEntryMode.INCOME) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = currentMode == ManualEntryMode.TRANSFER,
                    onClick = { currentMode = ManualEntryMode.TRANSFER },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (currentMode == ManualEntryMode.TRANSFER) Color(0xFF5E35B1) else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Transfer", fontWeight = if (currentMode == ManualEntryMode.TRANSFER) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            // Amount Input Card
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                amountText = input
                            }
                        },
                        prefix = {
                            Text(
                                text = "₹ ",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = when (currentMode) {
                                    ManualEntryMode.EXPENSE -> Color(0xFFC62828)
                                    ManualEntryMode.INCOME -> Color(0xFF2E7D32)
                                    ManualEntryMode.TRANSFER -> Color(0xFF5E35B1)
                                }
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        placeholder = { Text("0.00", style = MaterialTheme.typography.headlineMedium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_transaction_amount_input")
                    )

                    // Quick increment chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(100, 500, 1000, 2000).forEach { chipAmount ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val currentNum = amountText.toDoubleOrNull() ?: 0.0
                                    val newNum = currentNum + chipAmount
                                    amountText = if (newNum % 1.0 == 0.0) newNum.toInt().toString() else "%.2f".format(Locale.US, newNum)
                                },
                                label = { Text("+$chipAmount") },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            // Description / Note
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Narration") },
                placeholder = {
                    Text(
                        when (currentMode) {
                            ManualEntryMode.EXPENSE -> "e.g., Grocery shopping, Dinner with team"
                            ManualEntryMode.INCOME -> "e.g., Monthly salary, Freelance dividend"
                            ManualEntryMode.TRANSFER -> "e.g., Monthly savings, ATM cash withdrawal"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_transaction_description_input")
            )

            // Categories (for Expense and Income)
            if (currentMode != ManualEntryMode.TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        TextButton(onClick = { showNewCategoryDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Category")
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredCategories.forEach { category ->
                            val isSelected = category.name.equals(selectedCategory, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category.name },
                                label = { Text(category.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconCatalog.iconFor(category.iconName),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Account Selection
            if (currentMode == ManualEntryMode.TRANSFER) {
                // From Account and To Account
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Transfer Accounts",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    AccountDropdown(
                        label = "From (Debit)",
                        selectedAccount = fromAccount,
                        options = availableAccounts,
                        onAccountSelected = { fromAccount = it }
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                val temp = fromAccount
                                fromAccount = toAccount
                                toAccount = temp
                            }
                        ) {
                            Icon(Icons.Filled.SwapVert, contentDescription = "Swap accounts", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    AccountDropdown(
                        label = "To (Credit)",
                        selectedAccount = toAccount,
                        options = availableAccounts,
                        onAccountSelected = { toAccount = it }
                    )

                    if (fromAccount == toAccount && availableAccounts.size > 1) {
                        Text(
                            text = "Source and destination accounts must be different.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                // Single Account Picker
                AccountDropdown(
                    label = "Account / Payment Mode",
                    selectedAccount = selectedAccount,
                    options = availableAccounts,
                    onAccountSelected = { selectedAccount = it }
                )
            }

            // Date Picker Row
            val calendar = remember { Calendar.getInstance().apply { timeInMillis = selectedTimestamp } }
            val datePickerDialog = remember(context) {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        }
                        selectedTimestamp = cal.timeInMillis
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            }

            OutlinedCard(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(Date(selectedTimestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Reference Number (Optional)
            OutlinedTextField(
                value = referenceNumber,
                onValueChange = { referenceNumber = it },
                label = { Text("Reference / UPI / Cheque # (Optional)") },
                placeholder = { Text("e.g., UPI/423871923, CHQ-1049") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (!canSave) return@Button
                        if (currentMode == ManualEntryMode.TRANSFER) {
                            onSaveTransfer(
                                amountValue,
                                description.trim().ifBlank { "Transfer" },
                                selectedTimestamp,
                                fromAccount.accountId,
                                fromAccount.accountLast4,
                                fromAccount.bankName,
                                toAccount.accountId,
                                toAccount.accountLast4,
                                toAccount.bankName,
                                referenceNumber.trim().takeIf { it.isNotBlank() }
                            )
                        } else {
                            val type = if (currentMode == ManualEntryMode.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
                            val cat = selectedCategory.ifBlank { if (type == TransactionType.INCOME) "Income" else "Expense" }
                            val desc = description.trim().ifBlank { cat }
                            onSaveTransaction(
                                amountValue,
                                type,
                                desc,
                                cat,
                                selectedTimestamp,
                                referenceNumber.trim().takeIf { it.isNotBlank() },
                                selectedAccount.accountId,
                                selectedAccount.accountLast4,
                                selectedAccount.bankName
                            )
                        }
                        onDismiss()
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (currentMode) {
                            ManualEntryMode.EXPENSE -> Color(0xFFC62828)
                            ManualEntryMode.INCOME -> Color(0xFF2E7D32)
                            ManualEntryMode.TRANSFER -> Color(0xFF5E35B1)
                        }
                    ),
                    modifier = Modifier
                        .weight(2f)
                        .testTag("save_manual_transaction_button")
                ) {
                    Text(
                        text = when (currentMode) {
                            ManualEntryMode.EXPENSE -> "Save Expense"
                            ManualEntryMode.INCOME -> "Save Income"
                            ManualEntryMode.TRANSFER -> "Record Transfer"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // New Category Dialog
    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Create New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryName.trim()
                        if (trimmed.isNotBlank()) {
                            onCreateCategory(trimmed, currentMode == ManualEntryMode.INCOME)
                            selectedCategory = trimmed
                            newCategoryName = ""
                            showNewCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.trim().isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    label: String,
    selectedAccount: AccountOption,
    options: List<AccountOption>,
    onAccountSelected: (AccountOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedAccount.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onAccountSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
