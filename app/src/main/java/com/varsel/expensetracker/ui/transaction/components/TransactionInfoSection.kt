package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionInfoSection(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH)
    val formattedDate = dateFormat.format(Date(transaction.dateTimestamp))

    val reference = transaction.referenceNumber?.takeIf { it.isNotBlank() } ?: "Not available"
    val source = if (transaction.referenceNumber.isNullOrBlank()) "Manual Entry" else "Bank Statement Import"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Transaction Meta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )

            val bankNameDisplay = com.varsel.expensetracker.util.BankInfoHelper.resolveBankName(transaction)
            val bankValue = if (!transaction.accountLast4.isNullOrBlank()) {
                "$bankNameDisplay (•••• ${transaction.accountLast4})"
            } else {
                bankNameDisplay
            }

            InfoRow(
                title = "Bank Name",
                value = bankValue,
                isEmphasized = true
            )

            InfoRow(
                title = "Date & Time",
                value = formattedDate
            )

            InfoRow(
                title = "Source",
                value = source
            )

            InfoRow(
                title = "Reference / UTR",
                value = reference
            )

            InfoRow(
                title = "Transaction ID",
                value = "#TX-${transaction.id}"
            )

            InfoRow(
                title = "Ledger Status",
                value = "Settled & Verified"
            )
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    value: String,
    isEmphasized: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = if (isEmphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


