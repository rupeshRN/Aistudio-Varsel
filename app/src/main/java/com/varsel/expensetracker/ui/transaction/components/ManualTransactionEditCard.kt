package com.varsel.expensetracker.ui.transaction.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.theme.isDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ManualTransactionEditCard(
    amount: String,
    onAmountChanged: (String) -> Unit,
    type: TransactionType,
    onTypeChanged: (TransactionType) -> Unit,
    dateTimestamp: Long,
    onDateChanged: (Long) -> Unit,
    referenceNumber: String,
    onReferenceNumberChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val context = LocalContext.current
    val isExpense = type == TransactionType.EXPENSE || type == TransactionType.DEBIT
    val themeColor = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)

    val calendar = remember { Calendar.getInstance().apply { timeInMillis = dateTimestamp } }
    val datePickerDialog = remember(context, dateTimestamp) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onDateChanged(cal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val todayCalendar = Calendar.getInstance()
    val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isToday = isSameDay(dateTimestamp, todayCalendar.timeInMillis)
    val isYesterday = isSameDay(dateTimestamp, yesterdayCalendar.timeInMillis)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sleek Tactile Mode Selector (Expense vs Income)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFF1F5F9),
            border = BorderStroke(
                1.dp,
                if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Expense Pill
                val expenseAccent = Color(0xFFEF4444)
                val expenseBg = if (isExpense) {
                    if (isDark) expenseAccent.copy(alpha = 0.22f) else Color.White
                } else {
                    Color.Transparent
                }

                Surface(
                    onClick = { onTypeChanged(TransactionType.EXPENSE) },
                    shape = RoundedCornerShape(12.dp),
                    color = expenseBg,
                    shadowElevation = if (isExpense && !isDark) 2.dp else 0.dp,
                    border = if (isExpense && isDark) BorderStroke(1.dp, expenseAccent.copy(alpha = 0.4f)) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            tint = if (isExpense) expenseAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isExpense) expenseAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Income Pill
                val incomeAccent = Color(0xFF10B981)
                val incomeBg = if (!isExpense) {
                    if (isDark) incomeAccent.copy(alpha = 0.22f) else Color.White
                } else {
                    Color.Transparent
                }

                Surface(
                    onClick = { onTypeChanged(TransactionType.INCOME) },
                    shape = RoundedCornerShape(12.dp),
                    color = incomeBg,
                    shadowElevation = if (!isExpense && !isDark) 2.dp else 0.dp,
                    border = if (!isExpense && isDark) BorderStroke(1.dp, incomeAccent.copy(alpha = 0.4f)) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint = if (!isExpense) incomeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (!isExpense) incomeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Hero Amount Canvas (Linear / Copilot Style)
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
                        visible = amount.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            onClick = { onAmountChanged("") },
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

                // Large Display Amount Input
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
                        value = amount,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                onAmountChanged(input)
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
                                if (amount.isEmpty()) {
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
                            .testTag("detail_edit_amount_input")
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
                                val curr = amount.toDoubleOrNull() ?: 0.0
                                val res = curr + increment
                                onAmountChanged(
                                    if (res % 1.0 == 0.0) res.toInt().toString() else "%.2f".format(Locale.US, res)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Text(
                                text = "+₹%,d".format(increment),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Quick Date Selector (Today, Yesterday, Custom Calendar)
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
                    onClick = { onDateChanged(System.currentTimeMillis()) },
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
                        onDateChanged(yCal.timeInMillis)
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
                                SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(dateTimestamp))
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

        // 4. Reference Number / UPI / Cheque
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "REFERENCE / UPI / CHEQUE #",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            OutlinedTextField(
                value = referenceNumber,
                onValueChange = onReferenceNumberChanged,
                placeholder = { Text("e.g. UPI/283918239, CHQ-201 (Optional)") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_edit_reference_input")
            )
        }
    }
}

private fun isSameDay(cal1Millis: Long, cal2Millis: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = cal1Millis }
    val c2 = Calendar.getInstance().apply { timeInMillis = cal2Millis }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
