package com.varsel.expensetracker.ui.entry

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.category.CategoryMetadata
import com.varsel.expensetracker.ui.components.BankLogoBadge
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryBottomSheet(
    accounts: List<ManualEntryAccount>,
    initialType: ManualEntryType = ManualEntryType.EXPENSE,
    onDismiss: () -> Unit,
    onSaveExpenseOrIncome: (
        type: ManualEntryType,
        amount: Double,
        category: String,
        note: String,
        dateTimestamp: Long,
        account: ManualEntryAccount
    ) -> Unit,
    onSaveTransfer: (
        amount: Double,
        fromAccount: ManualEntryAccount,
        toAccount: ManualEntryAccount,
        note: String,
        dateTimestamp: Long
    ) -> Unit,
    onAdjustCashBalance: (
        actualBalance: Double,
        currentBalance: Double,
        dateTimestamp: Long
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current

    // State
    var selectedType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(
            if (initialType == ManualEntryType.INCOME) "Salary" else "Food & Dining"
        )
    }

    // Default to Cash Wallet if present, else first account
    val cashWallet = remember(accounts) {
        accounts.firstOrNull { it.isCashWallet }
            ?: ManualEntryAccount(
                id = "cash_wallet",
                bankName = "Cash Wallet",
                displayName = "Cash Wallet",
                last4 = "Cash",
                isCashWallet = true,
                balance = 0.0
            )
    }

    val bankAccounts = remember(accounts) {
        accounts.filter { !it.isCashWallet }
    }

    var selectedAccount by remember(accounts) {
        mutableStateOf(cashWallet)
    }

    var fromAccount by remember(accounts) {
        mutableStateOf(bankAccounts.firstOrNull() ?: cashWallet)
    }

    var toAccount by remember(accounts) {
        mutableStateOf(cashWallet)
    }

    // Date
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Cash Wallet Reconciliation Toggle
    var showCashReconciliation by remember { mutableStateOf(false) }
    var actualCashBalanceText by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isValidAmount = amount > 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("add_entry_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row with Title and Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (selectedType) {
                            ManualEntryType.EXPENSE -> "Add Expense"
                            ManualEntryType.INCOME -> "Add Income"
                            ManualEntryType.TRANSFER -> "Internal Transfer"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (selectedType) {
                            ManualEntryType.EXPENSE -> "Record manual expense or pending card spend"
                            ManualEntryType.INCOME -> "Record cash earnings or incoming funds"
                            ManualEntryType.TRANSFER -> "Move between Bank & Cash without skewing stats"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Segmented Tab Selector
            SingleChoiceSegmentedRow(
                selectedType = selectedType,
                onTypeSelected = { type ->
                    selectedType = type
                    if (type == ManualEntryType.INCOME && selectedCategory == "Food & Dining") {
                        selectedCategory = "Salary"
                    } else if (type == ManualEntryType.EXPENSE && selectedCategory == "Salary") {
                        selectedCategory = "Food & Dining"
                    }
                }
            )

            // Hero Amount Card
            HeroAmountCard(
                amountText = amountText,
                onAmountChange = { newText ->
                    if (newText.isEmpty() || newText.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = newText
                    }
                },
                selectedType = selectedType,
                onAddIncrement = { increment ->
                    val current = amountText.toDoubleOrNull() ?: 0.0
                    val updated = current + increment
                    amountText = if (updated % 1.0 == 0.0) {
                        updated.toLong().toString()
                    } else {
                        "%.2f".format(Locale.US, updated)
                    }
                },
                onClear = { amountText = "" }
            )

            // Account Selector
            if (selectedType == ManualEntryType.TRANSFER) {
                TransferAccountSelector(
                    fromAccount = fromAccount,
                    toAccount = toAccount,
                    allAccounts = listOf(cashWallet) + bankAccounts,
                    onFromAccountSelected = { fromAccount = it },
                    onToAccountSelected = { toAccount = it },
                    onSwap = {
                        val temp = fromAccount
                        fromAccount = toAccount
                        toAccount = temp
                    }
                )
            } else {
                AccountPickerSection(
                    selectedAccount = selectedAccount,
                    allAccounts = listOf(cashWallet) + bankAccounts,
                    onAccountSelected = { selectedAccount = it }
                )

                // Cash Reconciliation card if Cash Wallet is selected
                if (selectedAccount.isCashWallet) {
                    CashReconciliationCard(
                        isExpanded = showCashReconciliation,
                        onToggle = { showCashReconciliation = !showCashReconciliation },
                        currentCashBalance = selectedAccount.balance,
                        actualBalanceText = actualCashBalanceText,
                        onActualBalanceChange = { newText ->
                            if (newText.isEmpty() || newText.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                actualCashBalanceText = newText
                            }
                        },
                        onApplyAdjustment = {
                            val actual = actualCashBalanceText.toDoubleOrNull()
                            if (actual != null) {
                                onAdjustCashBalance(actual, selectedAccount.balance, selectedDateMillis)
                                onDismiss()
                            }
                        }
                    )
                }
            }

            // Category Picker (for Expense & Income)
            if (selectedType != ManualEntryType.TRANSFER && !showCashReconciliation) {
                CategoryPickerSection(
                    selectedType = selectedType,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            // Note & Date Row (hidden if cash reconciliation is open)
            if (!showCashReconciliation) {
                NoteAndDateSection(
                    noteText = noteText,
                    onNoteChange = { noteText = it },
                    selectedDateMillis = selectedDateMillis,
                    onDateClick = { showDatePicker = true }
                )
            }

            // Primary CTA Button
            if (!showCashReconciliation) {
                val buttonText = when (selectedType) {
                    ManualEntryType.EXPENSE -> if (isValidAmount) "Record Expense (₹${formatAmountPreview(amount)})" else "Enter Amount"
                    ManualEntryType.INCOME -> if (isValidAmount) "Record Income (₹${formatAmountPreview(amount)})" else "Enter Amount"
                    ManualEntryType.TRANSFER -> if (isValidAmount) "Confirm Transfer (₹${formatAmountPreview(amount)})" else "Enter Amount"
                }

                val buttonColor = when (selectedType) {
                    ManualEntryType.EXPENSE -> MaterialTheme.colorScheme.primary
                    ManualEntryType.INCOME -> Color(0xFF2E7D32)
                    ManualEntryType.TRANSFER -> MaterialTheme.colorScheme.secondary
                }

                Button(
                    onClick = {
                        if (isValidAmount) {
                            when (selectedType) {
                                ManualEntryType.EXPENSE, ManualEntryType.INCOME -> {
                                    onSaveExpenseOrIncome(
                                        selectedType,
                                        amount,
                                        selectedCategory,
                                        noteText.trim(),
                                        selectedDateMillis,
                                        selectedAccount
                                    )
                                }
                                ManualEntryType.TRANSFER -> {
                                    onSaveTransfer(
                                        amount,
                                        fromAccount,
                                        toAccount,
                                        noteText.trim(),
                                        selectedDateMillis
                                    )
                                }
                            }
                            onDismiss()
                        }
                    },
                    enabled = isValidAmount,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_entry_button")
                ) {
                    Icon(
                        imageVector = when (selectedType) {
                            ManualEntryType.EXPENSE -> Icons.Outlined.ArrowDownward
                            ManualEntryType.INCOME -> Icons.Outlined.ArrowUpward
                            ManualEntryType.TRANSFER -> Icons.Outlined.SwapHoriz
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
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
}

@Composable
private fun SingleChoiceSegmentedRow(
    selectedType: ManualEntryType,
    onTypeSelected: (ManualEntryType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ManualEntryType.values().forEach { type ->
                val isSelected = selectedType == type
                val containerColor = when {
                    !isSelected -> Color.Transparent
                    type == ManualEntryType.EXPENSE -> MaterialTheme.colorScheme.errorContainer
                    type == ManualEntryType.INCOME -> Color(0xFFC8E6C9)
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                val contentColor = when {
                    !isSelected -> MaterialTheme.colorScheme.onSurfaceVariant
                    type == ManualEntryType.EXPENSE -> MaterialTheme.colorScheme.onErrorContainer
                    type == ManualEntryType.INCOME -> Color(0xFF1B5E20)
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }

                Surface(
                    onClick = { onTypeSelected(type) },
                    shape = RoundedCornerShape(12.dp),
                    color = containerColor,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (type) {
                                ManualEntryType.EXPENSE -> Icons.Outlined.ArrowDownward
                                ManualEntryType.INCOME -> Icons.Outlined.ArrowUpward
                                ManualEntryType.TRANSFER -> Icons.Outlined.SwapHoriz
                            },
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroAmountCard(
    amountText: String,
    onAmountChange: (String) -> Unit,
    selectedType: ManualEntryType,
    onAddIncrement: (Double) -> Unit,
    onClear: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Large Currency Amount Field
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = when (selectedType) {
                        ManualEntryType.EXPENSE -> MaterialTheme.colorScheme.error
                        ManualEntryType.INCOME -> Color(0xFF2E7D32)
                        ManualEntryType.TRANSFER -> MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))

                TextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    placeholder = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    },
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .widthIn(min = 100.dp, max = 220.dp)
                        .testTag("amount_input_field")
                )

                if (amountText.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = "Clear amount",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick Increment Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(100.0, 500.0, 1000.0, 2000.0).forEach { inc ->
                    Surface(
                        onClick = { onAddIncrement(inc) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "+₹${inc.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountPickerSection(
    selectedAccount: ManualEntryAccount,
    allAccounts: List<ManualEntryAccount>,
    onAccountSelected: (ManualEntryAccount) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Account / Source",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (selectedAccount.isCashWallet) {
                Text(
                    text = "Cash in Hand",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Pending Statement Sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(allAccounts) { account ->
                val isSelected = selectedAccount.id == account.id
                Surface(
                    onClick = { onAccountSelected(account) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier.widthIn(min = 140.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BankLogoBadge(
                            bankName = account.bankName,
                            size = 32.dp
                        )
                        Column {
                            Text(
                                text = account.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "₹${formatAmountPreview(account.balance)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferAccountSelector(
    fromAccount: ManualEntryAccount,
    toAccount: ManualEntryAccount,
    allAccounts: List<ManualEntryAccount>,
    onFromAccountSelected: (ManualEntryAccount) -> Unit,
    onToAccountSelected: (ManualEntryAccount) -> Unit,
    onSwap: () -> Unit
) {
    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Transfer Route",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // From Account
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showFromMenu = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BankLogoBadge(bankName = fromAccount.bankName, size = 32.dp)
                        Column {
                            Text(
                                text = "From (Source)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = fromAccount.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        text = "₹${formatAmountPreview(fromAccount.balance)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showFromMenu,
                    onDismissRequest = { showFromMenu = false }
                ) {
                    allAccounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.displayName} (₹${formatAmountPreview(acc.balance)})") },
                            onClick = {
                                onFromAccountSelected(acc)
                                showFromMenu = false
                            }
                        )
                    }
                }

                // Swap Divider Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onSwap,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapVert,
                            contentDescription = "Swap accounts",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // To Account
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showToMenu = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BankLogoBadge(bankName = toAccount.bankName, size = 32.dp)
                        Column {
                            Text(
                                text = "To (Destination)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = toAccount.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        text = "₹${formatAmountPreview(toAccount.balance)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showToMenu,
                    onDismissRequest = { showToMenu = false }
                ) {
                    allAccounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.displayName} (₹${formatAmountPreview(acc.balance)})") },
                            onClick = {
                                onToAccountSelected(acc)
                                showToMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Explainer Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (fromAccount.isCashWallet || toAccount.isCashWallet) {
                        "ATM Cash Withdrawal / Deposit: Moves funds into your physical cash wallet without double-counting expenses in your monthly reports."
                    } else {
                        "Bank Transfer: Moves funds directly between your bank accounts without inflating monthly spend."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun CashReconciliationCard(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    currentCashBalance: Double,
    actualBalanceText: String,
    onActualBalanceChange: (String) -> Unit,
    onApplyAdjustment: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2E7D32).copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Counted your cash? Adjust Wallet Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enter the exact total cash physically in your wallet right now. Varsel will automatically calculate the difference and record a reconciliation entry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = actualBalanceText,
                        onValueChange = onActualBalanceChange,
                        label = { Text("Physical Cash in Hand (₹)") },
                        placeholder = { Text("e.g. 850") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val actual = actualBalanceText.toDoubleOrNull()
                    if (actual != null) {
                        val diff = actual - currentCashBalance
                        val diffText = if (diff >= 0) "+₹${formatAmountPreview(diff)}" else "-₹${formatAmountPreview(-diff)}"
                        Text(
                            text = "Adjustment: $diffText (from current ₹${formatAmountPreview(currentCashBalance)} to ₹${formatAmountPreview(actual)})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (diff >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )

                        Button(
                            onClick = onApplyAdjustment,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reconcile Cash Balance to ₹${formatAmountPreview(actual)}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerSection(
    selectedType: ManualEntryType,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = remember(selectedType) {
        if (selectedType == ManualEntryType.INCOME) {
            CategoryMetadata.incomeCategories
        } else {
            CategoryMetadata.expenseCategories
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory.equals(cat.id, ignoreCase = true)
                val emoji = CategoryMetadata.emojiForCategory(cat.id, cat.isIncome)

                Surface(
                    onClick = { onCategorySelected(cat.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = emoji, fontSize = 14.sp)
                        Text(
                            text = cat.id,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteAndDateSection(
    noteText: String,
    onNoteChange: (String) -> Unit,
    selectedDateMillis: Long,
    onDateClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isToday = remember(selectedDateMillis) {
        val calNow = Calendar.getInstance()
        val calSelected = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        calNow.get(Calendar.YEAR) == calSelected.get(Calendar.YEAR) &&
        calNow.get(Calendar.DAY_OF_YEAR) == calSelected.get(Calendar.DAY_OF_YEAR)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Note Input
        OutlinedTextField(
            value = noteText,
            onValueChange = onNoteChange,
            label = { Text("Description / Note") },
            placeholder = { Text("e.g. Chai with friends, Grocery store") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Date Picker Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onDateClick() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isToday) "Today (${dateFormatter.format(Date(selectedDateMillis))})" else dateFormatter.format(Date(selectedDateMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Change Date",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatAmountPreview(value: Double): String {
    return if (value % 1.0 == 0.0) {
        "%,d".format(Locale.US, value.toLong())
    } else {
        "%,.2f".format(Locale.US, value)
    }
}
