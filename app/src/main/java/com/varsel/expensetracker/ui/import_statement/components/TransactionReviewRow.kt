package com.varsel.expensetracker.ui.import_statement.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.import_statement.SelectableTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionReviewRow(
    selectable: SelectableTransaction,
    onCheckedChange: (Boolean) -> Unit
) {

    val transaction = selectable.transaction

    val formattedDate =
        SimpleDateFormat(
            "dd MMM yyyy",
            Locale.ENGLISH
        ).format(
            Date(transaction.dateTimestamp)
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onCheckedChange(!selectable.selected)
            }
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = selectable.selected,
                onCheckedChange = onCheckedChange
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = transaction.description,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = transaction.amount.toString(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 52.dp,
                    top = 2.dp
                ),
            horizontalArrangement = Arrangement.Start
        ) {

            Text(
                text = transaction.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
