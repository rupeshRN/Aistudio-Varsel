package com.varsel.expensetracker.ui.transaction.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.domain.model.TransactionType
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
    val context = LocalContext.current
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Manual Entry Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Transaction Type Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val isExpense = type == TransactionType.EXPENSE || type == TransactionType.DEBIT
                val isIncome = type == TransactionType.INCOME || type == TransactionType.CREDIT

                FilterChip(
                    selected = isExpense,
                    onClick = { onTypeChanged(TransactionType.EXPENSE) },
                    label = { Text("Expense") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isExpense) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFEBEE),
                        selectedLabelColor = Color(0xFFC62828)
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = isIncome,
                    onClick = { onTypeChanged(TransactionType.INCOME) },
                    label = { Text("Income") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isIncome) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE8F5E9),
                        selectedLabelColor = Color(0xFF2E7D32)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Editable Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                        onAmountChanged(input)
                    }
                },
                label = { Text("Amount") },
                prefix = {
                    Text(
                        text = "₹ ",
                        fontWeight = FontWeight.Bold,
                        color = if (type == TransactionType.INCOME || type == TransactionType.CREDIT) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_edit_amount_input")
            )

            // Date Picker Card
            OutlinedCard(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
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
                            text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(Date(dateTimestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pick Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Reference Number
            OutlinedTextField(
                value = referenceNumber,
                onValueChange = onReferenceNumberChanged,
                label = { Text("Reference / UPI / Cheque #") },
                placeholder = { Text("e.g. UPI/283918239, CHQ-201") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_edit_reference_input")
            )
        }
    }
}
