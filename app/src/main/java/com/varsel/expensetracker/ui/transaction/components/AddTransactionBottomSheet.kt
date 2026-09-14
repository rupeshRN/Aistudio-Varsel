package com.varsel.expensetracker.ui.transaction.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.components.BankLogoBadge
import com.varsel.expensetracker.ui.design.CategoryPalette
import com.varsel.expensetracker.ui.theme.isDark
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

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun resolveCategoryColor(category: CategoryEntity): Color {
    val hex = category.colorHex.trim()
    if (hex.isNotBlank()) {
        try {
            return Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {}
    }
    return CategoryPalette.colorFor(category.name)
}

private fun resolveCategoryColorByName(name: String, categories: List<CategoryEntity>): Color {
    val entity = categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
    if (entity != null) return resolveCategoryColor(entity)
    return CategoryPalette.colorFor(name)
}

/**
 * Modern, Immersive Financial Entry Sheet inspired by Copilot Money & Apple Wallet.
 * Features:
 * - Dynamic atmospheric aura matching current transaction mode (Expense / Income / Transfer)
 * - Hero amount card with quick increment keys (+100, +500, +1k, +2k, +5k)
 * - Quick Date selector (Today / Yesterday / Custom)
 * - Description / Note input placed immediately after Date with mode-specific smart suggestion tags
 * - Collapsible, visually rich Category section with vibrant authentic colors & icons
 * - Tactile Account selection for Expense/Income and Dual-Account flow with Swap for Transfers
 * - Fluid animations and clean visual hierarchy
 */
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
    val isDark = MaterialTheme.colorScheme.isDark

    var currentMode by remember { mutableStateOf(initialMode) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var referenceNumber by remember { mutableStateOf("") }
    var showReferenceField by remember { mutableStateOf(false) }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    // Account state
    val defaultAccount = availableAccounts.firstOrNull() ?: AccountOption(null, null, "Cash", "Cash Wallet")
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

    // Vibrant accent colors for modes
    val targetColor = when (currentMode) {
        ManualEntryMode.EXPENSE -> Color(0xFFEF4444)
        ManualEntryMode.INCOME -> Color(0xFF10B981)
        ManualEntryMode.TRANSFER -> Color(0xFF6366F1)
    }
    val themeColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(300), label = "themeColor")

    // Date picker dialog
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = if (isDark) Color(0xFF0C1222) else Color(0xFFFAFBFD)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Atmospheric subtle gradient aura at the top of the sheet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                themeColor.copy(alpha = if (isDark) 0.18f else 0.09f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar: Sheet Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (currentMode) {
                                ManualEntryMode.EXPENSE -> "Add Expense"
                                ManualEntryMode.INCOME -> "Add Income"
                                ManualEntryMode.TRANSFER -> "New Transfer"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (currentMode) {
                                ManualEntryMode.EXPENSE -> "Record an outgoing transaction"
                                ManualEntryMode.INCOME -> "Record an incoming deposit or revenue"
                                ManualEntryMode.TRANSFER -> "Move funds between accounts"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0).copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1. Sleek Tactile 3-Mode Selector (Revolut / Apple Wallet Inspired)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ManualEntryMode.values().forEach { mode ->
                            val isSelected = currentMode == mode
                            val modeAccent = when (mode) {
                                ManualEntryMode.EXPENSE -> Color(0xFFEF4444)
                                ManualEntryMode.INCOME -> Color(0xFF10B981)
                                ManualEntryMode.TRANSFER -> Color(0xFF6366F1)
                            }
                            val bg = if (isSelected) {
                                if (isDark) modeAccent.copy(alpha = 0.22f) else Color.White
                            } else {
                                Color.Transparent
                            }

                            Surface(
                                onClick = { currentMode = mode },
                                shape = RoundedCornerShape(12.dp),
                                color = bg,
                                shadowElevation = if (isSelected && !isDark) 2.dp else 0.dp,
                                border = if (isSelected && isDark) BorderStroke(1.dp, modeAccent.copy(alpha = 0.4f)) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            ManualEntryMode.EXPENSE -> Icons.Filled.ArrowUpward
                                            ManualEntryMode.INCOME -> Icons.Filled.ArrowDownward
                                            ManualEntryMode.TRANSFER -> Icons.Filled.SwapHoriz
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) modeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (mode) {
                                            ManualEntryMode.EXPENSE -> "Expense"
                                            ManualEntryMode.INCOME -> "Income"
                                            ManualEntryMode.TRANSFER -> "Transfer"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) modeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Hero Amount Canvas (Copilot / Linear Style)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDark) Color(0xFF131D35) else Color.White,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = themeColor.copy(alpha = if (isDark) 0.35f else 0.25f)
                    ),
                    shadowElevation = if (isDark) 0.dp else 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ENTER AMOUNT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                ),
                                color = themeColor
                            )

                            AnimatedVisibility(
                                visible = amountText.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Surface(
                                    onClick = { amountText = "" },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Large Display Amount Input (Stable alignment, no shift)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 38.sp
                                ),
                                color = themeColor
                            )
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = amountText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                        amountText = input
                                    }
                                },
                                textStyle = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 36.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(themeColor),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (amountText.isEmpty()) {
                                            Text(
                                                "0.00",
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 36.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_transaction_amount_input")
                            )
                        }

                        // Tactile Quick Increment Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(100, 500, 1000, 2000, 5000).forEach { increment ->
                                Surface(
                                    onClick = {
                                        val curr = amountText.toDoubleOrNull() ?: 0.0
                                        val res = curr + increment
                                        amountText = if (res % 1.0 == 0.0) res.toInt().toString() else "%.2f".format(Locale.US, res)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "+₹$increment",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Quick Date Selector (Today, Yesterday, Custom Calendar)
                val todayCalendar = Calendar.getInstance()
                val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val isToday = isSameDay(selectedTimestamp, todayCalendar.timeInMillis)
                val isYesterday = isSameDay(selectedTimestamp, yesterdayCalendar.timeInMillis)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TRANSACTION DATE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isToday,
                            onClick = { selectedTimestamp = System.currentTimeMillis() },
                            label = { Text("Today") },
                            leadingIcon = if (isToday) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColor.copy(alpha = 0.16f),
                                selectedLabelColor = themeColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        FilterChip(
                            selected = isYesterday,
                            onClick = {
                                val yCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                selectedTimestamp = yCal.timeInMillis
                            },
                            label = { Text("Yesterday") },
                            leadingIcon = if (isYesterday) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColor.copy(alpha = 0.16f),
                                selectedLabelColor = themeColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        FilterChip(
                            selected = !isToday && !isYesterday,
                            onClick = { datePickerDialog.show() },
                            label = {
                                Text(
                                    if (!isToday && !isYesterday)
                                        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(selectedTimestamp))
                                    else
                                        "Pick Date"
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeColor.copy(alpha = 0.16f),
                                selectedLabelColor = themeColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // 4. Description / Note (Placed immediately after Date as requested!)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DESCRIPTION / NOTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = {
                            Text(
                                when (currentMode) {
                                    ManualEntryMode.EXPENSE -> "e.g. Grocery shopping, Starbucks coffee, Uber ride"
                                    ManualEntryMode.INCOME -> "e.g. Monthly salary, Freelance design, Dividend"
                                    ManualEntryMode.TRANSFER -> "e.g. ATM withdrawal, Credit card payment"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.EditNote,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (description.isNotEmpty()) {
                                IconButton(onClick = { description = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_transaction_description_input")
                    )

                    // Mode-Specific Smart Suggestion Tags
                    val quickTags = when (currentMode) {
                        ManualEntryMode.EXPENSE -> listOf("Groceries", "Food & Dining", "Uber / Cab", "Coffee", "Shopping", "Medical", "Bills", "Fuel")
                        ManualEntryMode.INCOME -> listOf("Salary", "Freelance", "Bonus", "Dividend", "Gift", "Cashback", "Investment Return")
                        ManualEntryMode.TRANSFER -> listOf("ATM Cash", "Credit Card Bill", "Savings Deposit", "Self Transfer")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickTags.forEach { tag ->
                            val isCurrent = description.equals(tag, ignoreCase = true)
                            Surface(
                                onClick = { description = tag },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isCurrent) themeColor.copy(alpha = 0.16f) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) themeColor else (if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0))
                                )
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isCurrent) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Category Section (Pushed down after Description as requested!)
                if (currentMode != ManualEntryMode.TRANSFER) {
                    val selectedCatColor = resolveCategoryColorByName(selectedCategory, categories)
                    val selectedIcon = CategoryIconCatalog.iconFor(
                        categories.firstOrNull { it.name.equals(selectedCategory, ignoreCase = true) }?.iconName ?: selectedCategory
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CATEGORY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )

                            Surface(
                                onClick = { showNewCategoryDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                color = themeColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = themeColor
                                    )
                                    Text(
                                        text = "New Category",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = themeColor
                                    )
                                }
                            }
                        }

                        // Interactive Selected Category Hero Strip
                        Surface(
                            onClick = { isCategoryExpanded = !isCategoryExpanded },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF131D35) else Color.White,
                            border = BorderStroke(
                                1.5.dp,
                                if (isCategoryExpanded) selectedCatColor else selectedCatColor.copy(alpha = 0.35f)
                            ),
                            shadowElevation = if (isDark) 0.dp else 1.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = selectedCatColor.copy(alpha = if (isDark) 0.28f else 0.16f),
                                        border = BorderStroke(1.dp, selectedCatColor.copy(alpha = 0.4f)),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = selectedIcon,
                                                contentDescription = null,
                                                tint = selectedCatColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = selectedCategory,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isCategoryExpanded) "Tap to collapse" else "Tap to change category",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = selectedCatColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (isCategoryExpanded) "Hide" else "Change",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = selectedCatColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isCategoryExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = selectedCatColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Collapsible Category Grid with Authentic Vibrant Colors
                        AnimatedVisibility(
                            visible = isCategoryExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    filteredCategories.forEach { category ->
                                        val isSelected = category.name.equals(selectedCategory, ignoreCase = true)
                                        val catColor = resolveCategoryColor(category)
                                        val catIcon = CategoryIconCatalog.iconFor(category.iconName)

                                        Surface(
                                            onClick = {
                                                selectedCategory = category.name
                                                isCategoryExpanded = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) {
                                                catColor.copy(alpha = if (isDark) 0.28f else 0.18f)
                                            } else {
                                                if (isDark) Color(0xFF1E293B) else Color.White
                                            },
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) catColor else catColor.copy(alpha = if (isDark) 0.35f else 0.25f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = catIcon,
                                                    contentDescription = null,
                                                    tint = catColor,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = category.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Account Selection (Paid From / Deposit To / Two-way Transfer)
                if (currentMode == ManualEntryMode.TRANSFER) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "TRANSFER ACCOUNTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDark) Color(0xFF131D35) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AccountDropdown(
                                    label = "From (Source Account)",
                                    selectedAccount = fromAccount,
                                    options = availableAccounts,
                                    onAccountSelected = { fromAccount = it }
                                )

                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        onClick = {
                                            val temp = fromAccount
                                            fromAccount = toAccount
                                            toAccount = temp
                                        },
                                        shape = CircleShape,
                                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF),
                                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.SwapVert,
                                                contentDescription = "Swap accounts",
                                                tint = Color(0xFF6366F1),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                AccountDropdown(
                                    label = "To (Destination Account)",
                                    selectedAccount = toAccount,
                                    options = availableAccounts,
                                    onAccountSelected = { toAccount = it }
                                )

                                if (fromAccount == toAccount && availableAccounts.size > 1) {
                                    Text(
                                        text = "Source and destination accounts must be different.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Visual Account Carousel for Expense/Income
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (currentMode == ManualEntryMode.INCOME) "DEPOSIT ACCOUNT" else "PAID FROM ACCOUNT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            availableAccounts.forEach { account ->
                                val isSelected = selectedAccount == account
                                Surface(
                                    onClick = { selectedAccount = account },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                    } else {
                                        if (isDark) Color(0xFF131D35) else Color.White
                                    },
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) themeColor else (if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0))
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        BankLogoBadge(
                                            bankName = account.bankName ?: "Bank",
                                            size = 28.dp
                                        )
                                        Column {
                                            Text(
                                                text = account.displayName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!account.accountLast4.isNullOrBlank()) {
                                                Text(
                                                    text = "••${account.accountLast4}",
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
                }

                // 7. Optional Reference / UPI / Cheque Field
                if (!showReferenceField && referenceNumber.isBlank()) {
                    TextButton(
                        onClick = { showReferenceField = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Add Reference / UPI / Cheque # (Optional)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("Reference / UPI / Cheque #") },
                        placeholder = { Text("e.g. UPI/423871923, CHQ-1049") },
                        trailingIcon = {
                            IconButton(onClick = {
                                referenceNumber = ""
                                showReferenceField = false
                            }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(4.dp))

                // 8. Primary CTA Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
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
                            containerColor = themeColor,
                            disabledContainerColor = themeColor.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(2f)
                            .testTag("save_manual_transaction_button")
                    ) {
                        val buttonText = when {
                            !isAmountValid -> "Enter Amount"
                            !isTransferValid -> "Choose Different Accounts"
                            currentMode == ManualEntryMode.EXPENSE -> "Save ₹%,.0f Expense".format(amountValue)
                            currentMode == ManualEntryMode.INCOME -> "Save ₹%,.0f Income".format(amountValue)
                            currentMode == ManualEntryMode.TRANSFER -> "Record ₹%,.0f Transfer".format(amountValue)
                            else -> "Save"
                        }
                        Text(
                            text = buttonText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    // New Category Dialog
    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Create New Category", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Gym, Pet Care, Streaming") },
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
                    enabled = newCategoryName.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("Create")
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
                BankLogoBadge(
                    bankName = selectedAccount.bankName ?: "Bank",
                    size = 24.dp
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
                    leadingIcon = {
                        BankLogoBadge(
                            bankName = option.bankName ?: "Bank",
                            size = 22.dp
                        )
                    },
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
