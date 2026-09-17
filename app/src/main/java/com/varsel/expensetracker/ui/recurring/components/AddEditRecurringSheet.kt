package com.varsel.expensetracker.ui.recurring.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.category.CategoryMetadata
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.domain.model.recurring.RecurringFrequency
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.domain.model.recurring.RecurringType
import com.varsel.expensetracker.ui.components.BankLogoBadge
import com.varsel.expensetracker.ui.design.CategoryPalette
import com.varsel.expensetracker.ui.theme.isDark
import com.varsel.expensetracker.ui.transaction.model.AccountOption
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringSheet(
    item: RecurringItem?,
    availableAccounts: List<AccountOption>,
    availableCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (RecurringItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.isDark
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val zoneId = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var title by remember(item) { mutableStateOf(item?.title ?: "") }
    var notes by remember(item) { mutableStateOf(item?.notes ?: "") }
    var amountText by remember(item) {
        mutableStateOf(
            item?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else "%.2f".format(Locale.US, it)
            } ?: ""
        )
    }
    var selectedType by remember(item) { mutableStateOf(item?.type ?: RecurringType.EXPENSE) }
    var selectedFrequency by remember(item) { mutableStateOf(item?.frequency ?: RecurringFrequency.MONTHLY) }

    var nextOccurrenceTimestamp by remember(item) {
        mutableStateOf(item?.nextOccurrenceTimestamp ?: System.currentTimeMillis())
    }

    var isActive by remember(item) { mutableStateOf(item?.isActive ?: true) }
    var isVariableAmount by remember(item) { mutableStateOf(item?.isVariableAmount ?: false) }

    // Initial category selection
    var selectedCategory by remember(item, availableCategories, selectedType) {
        mutableStateOf(
            item?.category ?: when (selectedType) {
                RecurringType.INCOME -> "Salary"
                RecurringType.SUBSCRIPTION -> "Entertainment"
                RecurringType.EXPENSE -> availableCategories.firstOrNull()?.name ?: "Utilities"
            }
        )
    }

    // Deduplicate accounts defensively by unique account identifier
    val dedupedAccounts = remember(availableAccounts) {
        val unique = availableAccounts.distinctBy {
            it.accountId?.takeIf { id -> id.isNotBlank() } ?: "${it.bankName}_${it.accountLast4}_${it.displayName}"
        }
        unique.ifEmpty {
            listOf(AccountOption(null, null, "Cash", "Cash Wallet"))
        }
    }

    // Default to matching account or first account or fallback cash
    var selectedAccount by remember(item, dedupedAccounts) {
        mutableStateOf(
            dedupedAccounts.firstOrNull { acc ->
                (item?.accountId != null && acc.accountId == item.accountId) ||
                (!item?.accountLast4.isNullOrBlank() && acc.accountLast4 == item.accountLast4 && acc.bankName == item.bankName)
            }
                ?: dedupedAccounts.firstOrNull()
                ?: AccountOption(null, null, "Cash", "Cash Wallet")
        )
    }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isTitleEntered = title.trim().isNotBlank()
    val isAmountEntered = amountText.isNotBlank()
    val isValidAmount = parsedAmount > 0.0
    val canSave = isTitleEntered && isValidAmount

    val themeColor = when (selectedType) {
        RecurringType.EXPENSE -> MaterialTheme.colorScheme.error
        RecurringType.SUBSCRIPTION -> MaterialTheme.colorScheme.secondary
        RecurringType.INCOME -> if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
    }

    val buttonContainerColor = when {
        !canSave -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        selectedType == RecurringType.EXPENSE -> MaterialTheme.colorScheme.primary
        selectedType == RecurringType.SUBSCRIPTION -> MaterialTheme.colorScheme.secondary
        selectedType == RecurringType.INCOME -> if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.primary
    }

    ModalBottomSheet(
        onDismissRequest = {
            keyboardController?.hide()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("add_recurring_bottom_sheet")
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
            // Header Row with Title and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (item == null) "Add Recurring Item" else "Edit Recurring Item",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (selectedType) {
                            RecurringType.SUBSCRIPTION -> "Manage digital streaming, software & regular memberships"
                            RecurringType.EXPENSE -> "Automate rent, utility bills, EMI & recurring charges"
                            RecurringType.INCOME -> "Schedule salary, dividends & routine incoming funds"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onDismiss()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Segmented Type Selector (Matching AddEntryBottomSheet SingleChoiceSegmentedRow)
            RecurringSegmentedRow(
                selectedType = selectedType,
                onTypeSelected = { newType ->
                    selectedType = newType
                    if (newType == RecurringType.INCOME && (selectedCategory == "Utilities" || selectedCategory == "Entertainment")) {
                        selectedCategory = "Salary"
                    } else if (newType == RecurringType.SUBSCRIPTION && selectedCategory == "Salary") {
                        selectedCategory = "Entertainment"
                    }
                }
            )

            // Title / Merchant Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Merchant") },
                placeholder = {
                    Text(
                        when (selectedType) {
                            RecurringType.SUBSCRIPTION -> "e.g. Netflix, Spotify, iCloud, Gym"
                            RecurringType.EXPENSE -> "e.g. House Rent, WiFi, Electricity Bill"
                            RecurringType.INCOME -> "e.g. Monthly Salary, Freelance Retainer"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = when (selectedType) {
                            RecurringType.SUBSCRIPTION -> Icons.Outlined.Subscriptions
                            RecurringType.INCOME -> Icons.Outlined.AccountBalance
                            RecurringType.EXPENSE -> Icons.Outlined.ReceiptLong
                        },
                        contentDescription = null,
                        tint = themeColor
                    )
                },
                trailingIcon = {
                    if (title.isNotEmpty()) {
                        IconButton(
                            onClick = { title = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = "Clear title",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Compact Amount Input (No big card as explicitly requested)
            OutlinedTextField(
                value = amountText,
                onValueChange = { newText ->
                    if (newText.isEmpty() || newText.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = newText
                    }
                },
                label = {
                    Text(
                        if (isVariableAmount) "Estimated Amount (Variable)" else "Amount per Cycle"
                    )
                },
                placeholder = { Text("0.00") },
                leadingIcon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(start = 14.dp, end = 6.dp)
                    ) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                    }
                },
                trailingIcon = {
                    if (amountText.isNotEmpty()) {
                        IconButton(
                            onClick = { amountText = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = "Clear amount",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_amount_input")
            )

            // Frequency Selector Row
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
                        text = "Billing Frequency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Repeats ${selectedFrequency.displayName.lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurringFrequency.values().forEach { freq ->
                        val isSelected = selectedFrequency == freq
                        Surface(
                            onClick = { selectedFrequency = freq },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = freq.displayName,
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

            // Next Due Date Selector (Interactive surface with relative status)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Next Due Date",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = nextOccurrenceTimestamp }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val localDate = LocalDate.of(year, month + 1, dayOfMonth)
                                nextOccurrenceTimestamp = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = dateFormatter.format(Date(nextOccurrenceTimestamp)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatRelativeDueDate(nextOccurrenceTimestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditCalendar,
                                contentDescription = "Change date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Account / Source Picker (Matching AddEntryBottomSheet AccountPickerSection)
            AccountPickerRow(
                selectedAccount = selectedAccount,
                accounts = dedupedAccounts,
                onAccountSelected = { selectedAccount = it }
            )

            // Category Dropdown
            CategoryDropdownField(
                selectedType = selectedType,
                selectedCategory = selectedCategory,
                availableCategories = availableCategories,
                onCategorySelected = { selectedCategory = it }
            )

            // Options Surface (Variable Amount & Active Status Toggles)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Variable Amount Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Variable Amount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isVariableAmount) "Amount varies each cycle (e.g. Electricity, Credit Card)" else "Fixed exact amount every cycle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isVariableAmount,
                            onCheckedChange = { isVariableAmount = it }
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Active Schedule Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Active Schedule",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isActive) "Generates due reminders and auto-matches statements" else "Paused / Inactive (no automated alerts)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }
            }

            // Notes / Memo Field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Memo (Optional)") },
                placeholder = { Text("e.g. Plan renewal date, account #, login email") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Validation Helper Banner
            AnimatedVisibility(
                visible = !canSave && (isTitleEntered || isAmountEntered),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when {
                                !isTitleEntered -> "Please enter a title or merchant name"
                                !isAmountEntered -> "Please enter an amount"
                                !isValidAmount -> "Amount must be greater than ₹0"
                                else -> "Please fill all required fields"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Primary CTA Save Button (Matching AddEntryBottomSheet style)
            val buttonText = when {
                !isTitleEntered -> "Enter Title / Merchant"
                !isAmountEntered -> "Enter Amount"
                !isValidAmount -> "Invalid Amount"
                item != null -> "Update ${selectedType.displayName} (₹${formatAmountPreview(parsedAmount)})"
                selectedType == RecurringType.SUBSCRIPTION -> "Save Subscription (₹${formatAmountPreview(parsedAmount)})"
                selectedType == RecurringType.INCOME -> "Save Recurring Income (₹${formatAmountPreview(parsedAmount)})"
                else -> "Save Recurring Expense (₹${formatAmountPreview(parsedAmount)})"
            }

            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    if (canSave) {
                        val recurringItem = RecurringItem(
                            id = item?.id ?: 0L,
                            title = title.trim(),
                            notes = notes.trim().ifBlank { null },
                            amount = parsedAmount,
                            type = selectedType,
                            frequency = selectedFrequency,
                            startDateTimestamp = item?.startDateTimestamp ?: nextOccurrenceTimestamp,
                            nextOccurrenceTimestamp = nextOccurrenceTimestamp,
                            endDateTimestamp = item?.endDateTimestamp,
                            isActive = isActive,
                            isVariableAmount = isVariableAmount,
                            accountId = selectedAccount.accountId,
                            accountLast4 = selectedAccount.accountLast4,
                            bankName = selectedAccount.bankName,
                            category = selectedCategory.ifBlank { "Other" },
                            lastGeneratedTimestamp = item?.lastGeneratedTimestamp,
                            createdAt = item?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(recurringItem)
                        onDismiss()
                    }
                },
                enabled = canSave,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = if (canSave) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_recurring_button")
            ) {
                Icon(
                    imageVector = if (item != null) Icons.Outlined.Check else Icons.Outlined.EventRepeat,
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

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RecurringSegmentedRow(
    selectedType: RecurringType,
    onTypeSelected: (RecurringType) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark

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
            RecurringType.values().forEach { type ->
                val isSelected = selectedType == type
                val containerColor = if (!isSelected) {
                    Color.Transparent
                } else {
                    when (type) {
                        RecurringType.EXPENSE -> MaterialTheme.colorScheme.errorContainer
                        RecurringType.SUBSCRIPTION -> MaterialTheme.colorScheme.secondaryContainer
                        RecurringType.INCOME -> if (isDark) Color(0xFF1B5E20).copy(alpha = 0.5f) else Color(0xFFC8E6C9)
                    }
                }
                val contentColor = if (!isSelected) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    when (type) {
                        RecurringType.EXPENSE -> MaterialTheme.colorScheme.onErrorContainer
                        RecurringType.SUBSCRIPTION -> MaterialTheme.colorScheme.onSecondaryContainer
                        RecurringType.INCOME -> if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
                    }
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
                                RecurringType.EXPENSE -> Icons.Outlined.ArrowDownward
                                RecurringType.SUBSCRIPTION -> Icons.Outlined.Subscriptions
                                RecurringType.INCOME -> Icons.Outlined.ArrowUpward
                            },
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = type.displayName,
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
private fun AccountPickerRow(
    selectedAccount: AccountOption,
    accounts: List<AccountOption>,
    onAccountSelected: (AccountOption) -> Unit
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
                text = "Pay From / Account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = selectedAccount.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(accounts) { account ->
                val isSelected = (selectedAccount.accountId != null && selectedAccount.accountId == account.accountId) ||
                        (selectedAccount.accountId == null && account.accountId == null && selectedAccount.displayName == account.displayName)

                Surface(
                    onClick = { onAccountSelected(account) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(
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
                            bankName = account.bankName ?: account.displayName,
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
                                text = if (!account.accountLast4.isNullOrBlank()) "•••• ${account.accountLast4}" else "Default",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdownField(
    selectedType: RecurringType,
    selectedCategory: String,
    availableCategories: List<CategoryEntity>,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val categories = remember(selectedType, availableCategories) {
        val baseCategories = if (selectedType == RecurringType.INCOME) {
            CategoryMetadata.incomeCategories.map { it.id }
        } else if (selectedType == RecurringType.SUBSCRIPTION) {
            listOf("Entertainment", "Utilities", "Internet & Mobile", "Software", "Gym & Fitness", "Education", "Shopping", "Other")
        } else {
            CategoryMetadata.expenseCategories.map { it.id }
        }

        val availableNames = availableCategories.map { it.name }
        (baseCategories + availableNames).distinct()
    }

    val displayedCategory = selectedCategory.ifBlank { "Select Category" }
    val isIncome = selectedType == RecurringType.INCOME
    val selectedEmoji = remember(selectedCategory, isIncome) {
        if (selectedCategory.isNotBlank()) CategoryMetadata.emojiForCategory(selectedCategory, isIncome) else "🏷️"
    }
    val selectedColor = remember(selectedCategory) {
        if (selectedCategory.isNotBlank()) CategoryPalette.colorFor(selectedCategory) else null
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background((selectedColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedEmoji,
                        fontSize = 14.sp
                    )
                }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { catName ->
                val emoji = CategoryMetadata.emojiForCategory(catName, isIncome)
                val catColor = CategoryPalette.colorFor(catName)
                val isSelected = selectedCategory.equals(catName, ignoreCase = true)

                DropdownMenuItem(
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 14.sp
                            )
                        }
                    },
                    text = {
                        Text(
                            text = catName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    onClick = {
                        onCategorySelected(catName)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatRelativeDueDate(timestamp: Long): String {
    val calNow = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val calTarget = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffDays = ((calTarget.timeInMillis - calNow.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt()
    return when {
        diffDays < 0 -> "Overdue by ${-diffDays} day${if (-diffDays > 1) "s" else ""}"
        diffDays == 0 -> "Due today"
        diffDays == 1 -> "Due tomorrow"
        diffDays in 2..30 -> "Due in $diffDays days"
        else -> "Due in ${diffDays / 30} month${if (diffDays / 30 > 1) "s" else ""}"
    }
}

private fun formatAmountPreview(value: Double): String {
    return if (value % 1.0 == 0.0) {
        "%,d".format(Locale.US, value.toLong())
    } else {
        "%,.2f".format(Locale.US, value)
    }
}
