package com.varsel.expensetracker.ui.loan.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.domain.model.loan.LoanPayment
import com.varsel.expensetracker.domain.model.loan.LoanPaymentType
import com.varsel.expensetracker.domain.model.loan.LoanSummary
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    loanSummary: LoanSummary,
    onDismiss: () -> Unit,
    onConfirm: (payment: LoanPayment, createBankTx: Boolean, accountId: String?, accountLast4: String?) -> Unit
) {
    val loan = loanSummary.loan
    var paymentType by remember { mutableStateOf(LoanPaymentType.REGULAR_EMI) }

    val defaultEmiAmount = if (loanSummary.currentOutstandingBalance > 0) {
        min(loan.emiAmount, loanSummary.currentOutstandingBalance)
    } else 0.0

    var amountString by remember {
        mutableStateOf(if (defaultEmiAmount > 0) defaultEmiAmount.toLong().toString() else "")
    }

    val monthlyRate = loan.annualInterestRate / (12.0 * 100.0)
    val defaultInterest = round(loanSummary.currentOutstandingBalance * monthlyRate * 100.0) / 100.0
    val defaultPrincipal = max(0.0, (amountString.toDoubleOrNull() ?: 0.0) - defaultInterest)

    var principalString by remember {
        mutableStateOf(if (defaultPrincipal > 0) defaultPrincipal.toLong().toString() else "0")
    }
    var interestString by remember {
        mutableStateOf(if (defaultInterest > 0) defaultInterest.toLong().toString() else "0")
    }

    var selectedDateTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var createBankTx by remember { mutableStateOf(loan.linkedBankAccountId != null) }

    fun updateSplits(total: Double, type: LoanPaymentType) {
        when (type) {
            LoanPaymentType.REGULAR_EMI -> {
                val interest = min(total, round(loanSummary.currentOutstandingBalance * monthlyRate * 100.0) / 100.0)
                val principal = max(0.0, total - interest)
                interestString = interest.toLong().toString()
                principalString = principal.toLong().toString()
            }
            LoanPaymentType.PRE_PAYMENT -> {
                // 100% principal prepayment
                principalString = total.toLong().toString()
                interestString = "0"
            }
            LoanPaymentType.CLOSURE -> {
                val interest = min(total, round(loanSummary.currentOutstandingBalance * monthlyRate * 100.0) / 100.0)
                val principal = max(0.0, total - interest)
                interestString = interest.toLong().toString()
                principalString = principal.toLong().toString()
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateTimestamp
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateTimestamp = it
                    }
                    showDatePicker = false
                }) {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Loan Payment",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Loan: ${loan.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                // Payment Type Selector
                Text(
                    text = "Payment Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = paymentType == LoanPaymentType.REGULAR_EMI,
                        onClick = {
                            paymentType = LoanPaymentType.REGULAR_EMI
                            amountString = if (defaultEmiAmount > 0) defaultEmiAmount.toLong().toString() else ""
                            updateSplits(amountString.toDoubleOrNull() ?: 0.0, LoanPaymentType.REGULAR_EMI)
                        },
                        label = { Text("Regular EMI", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paymentType == LoanPaymentType.PRE_PAYMENT,
                        onClick = {
                            paymentType = LoanPaymentType.PRE_PAYMENT
                            updateSplits(amountString.toDoubleOrNull() ?: 0.0, LoanPaymentType.PRE_PAYMENT)
                        },
                        label = { Text("Pre-payment", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Total Amount
                OutlinedTextField(
                    value = amountString,
                    onValueChange = {
                        amountString = it
                        val total = it.toDoubleOrNull() ?: 0.0
                        updateSplits(total, paymentType)
                    },
                    label = { Text("Payment Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Outlined.CurrencyRupee, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Split Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = principalString,
                        onValueChange = { principalString = it },
                        label = { Text("Principal (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = interestString,
                        onValueChange = { interestString = it },
                        label = { Text("Interest (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Payment Date
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Payment Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormat.format(Date(selectedDateTimestamp)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Outlined.CalendarToday, contentDescription = "Pick date", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reference (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Checkbox: Create Debit in Linked Bank Account
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { createBankTx = !createBankTx }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = createBankTx,
                        onCheckedChange = { createBankTx = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Add expense to bank account",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (loan.bankAccountLast4 != null) "Linked to •••• ${loan.bankAccountLast4}" else "Adds a transaction to account ledger",
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
                    val amount = amountString.toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) {
                        val principal = principalString.toDoubleOrNull() ?: amount
                        val interest = interestString.toDoubleOrNull() ?: 0.0

                        val payment = LoanPayment(
                            loanId = loan.id,
                            paymentDateTimestamp = selectedDateTimestamp,
                            amount = amount,
                            principalComponent = principal,
                            interestComponent = interest,
                            paymentType = paymentType,
                            notes = notes.trim().ifEmpty { null }
                        )
                        onConfirm(
                            payment,
                            createBankTx,
                            loan.linkedBankAccountId,
                            loan.bankAccountLast4
                        )
                    }
                },
                enabled = (amountString.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
