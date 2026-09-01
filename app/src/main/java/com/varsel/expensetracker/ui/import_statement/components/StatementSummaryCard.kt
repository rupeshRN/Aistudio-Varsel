package com.varsel.expensetracker.ui.import_statement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.import_statement.ImportSummary
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn

/**
 * Premium summary displayed immediately after parsing.
 *
 * Gives confidence before the user reviews transactions.
 */
@Composable
fun StatementSummaryCard(

    summary: ImportSummary,

    onContinue: () -> Unit

) {

    Card(

        modifier = Modifier.fillMaxWidth(),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),

        shape = MaterialTheme.shapes.extraLarge

    ) {

        Column(

            modifier = Modifier.padding(20.dp)

        ) {

            //--------------------------------------------------
            // Header
            //--------------------------------------------------

            Row(

                verticalAlignment = Alignment.CenterVertically

            ) {

                Icon(

                    imageVector = Icons.Default.CheckCircle,

                    contentDescription = null,

                    tint = Color(0xFF2E7D32)

                )

                Spacer(Modifier.width(12.dp))

                Column {

                    Text(

                        "Statement Parsed",

                        style = MaterialTheme.typography.headlineSmall,

                        fontWeight = FontWeight.Bold

                    )

                    Text(

                        summary.bankName,

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                }

            }

            Spacer(Modifier.height(20.dp))

            //--------------------------------------------------
            // Statement Period
            //--------------------------------------------------

Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {

    Text(
        text = "Statement Period",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(
        modifier = Modifier.width(12.dp)
    )

    Text(
        text = summary.statementPeriod,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.weight(1f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

            Spacer(Modifier.height(24.dp))

            //--------------------------------------------------
            // Statistics
            //--------------------------------------------------

            Row(

                horizontalArrangement = Arrangement.spacedBy(12.dp),

                modifier = Modifier.fillMaxWidth()

            ) {

                StatisticCard(

                    title = "Transactions",

                    value =

                    "${summary.transactionsParsed} / ${summary.transactionsDetected}",

                    modifier = Modifier.weight(1f)

                )

                StatisticCard(

                    title = "Review",

                    value = summary.needsReview.toString(),

                    modifier = Modifier.weight(1f)

                )

            }

            Spacer(Modifier.height(12.dp))

            Row(

                horizontalArrangement = Arrangement.spacedBy(12.dp),

                modifier = Modifier.fillMaxWidth()

            ) {

                StatisticCard(

                    title = "Debits",

                    value = summary.debits.toString(),

                    modifier = Modifier.weight(1f)

                )

         StatisticCard(

                    title = "Credits",

                    value = summary.credits.toString(),

                    modifier = Modifier.weight(1f)

                )
                

            }

            Spacer(Modifier.height(12.dp))

            Row(

                horizontalArrangement = Arrangement.spacedBy(12.dp),

                modifier = Modifier.fillMaxWidth()

            ) {

                StatisticCard(

                    title = "Learned",

                    value = summary.learnedMatches.toString(),

                    modifier = Modifier.weight(1f)

                )

            StatisticCard(

                    title = "Duplicates",

                    value = summary.duplicates.toString(),

                    modifier = Modifier.weight(1f)

                )

            }

            Spacer(Modifier.height(24.dp))

            Divider()

            Spacer(Modifier.height(20.dp))

            //--------------------------------------------------
            // Reconciliation
            //--------------------------------------------------

            Row(

                verticalAlignment = Alignment.CenterVertically

            ) {

                Icon(

                    imageVector =

                    if (summary.reconciliationPassed)

                        Icons.Default.CheckCircle

                    else

                        Icons.Default.Warning,

                    contentDescription = null,

                    tint =

                    if (summary.reconciliationPassed)

                        Color(0xFF2E7D32)

                    else

                        MaterialTheme.colorScheme.error

                )

                Spacer(Modifier.width(12.dp))

                Column {

                    Text(

                        if (summary.reconciliationPassed)

                            "Reconciliation Passed"

                        else

                            "Reconciliation Failed",

                        style = MaterialTheme.typography.titleMedium,

                        fontWeight = FontWeight.SemiBold

                    )

                    Text(

                        summary.reconciliationStatusText,

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                }

            }

            Spacer(Modifier.height(28.dp))

            //--------------------------------------------------
            // Continue
            //--------------------------------------------------

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    onClick = onContinue,
                    contentPadding = PaddingValues(
                        horizontal = 20.dp,
                        vertical = 14.dp
                    )
                ) {
                    Text(
                        text = "Continue to Transaction Review",
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
        }

    }

}
