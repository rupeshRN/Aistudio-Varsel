package com.varsel.expensetracker.ui.import_statement.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.developer.ParserDiagnostics

@Composable
fun DeveloperDiagnosticsCard(
    enabled: Boolean,
    diagnostics: ParserDiagnostics
) {
    if (!enabled) return

    var expanded by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            TextButton(
                onClick = {
                    expanded = !expanded
                }
            ) {
                Text(
                    text = if (expanded) {
                        "Developer Diagnostics ▼"
                    } else {
                        "Developer Diagnostics ▶"
                    }
                )
            }

            if (expanded) {

                Spacer(modifier = Modifier.height(8.dp))

                // Normalization

                Text("Raw Lines : ${diagnostics.rawLines}")

                Text(
                    "Normalized Lines : ${diagnostics.normalizedLines}"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Detection and parsing

                Text(
                    "Dates Detected : ${diagnostics.datesDetected}"
                )

                Text(
                    "Blocks Built : ${diagnostics.blocksBuilt}"
                )

                Text(
                    "Transactions Parsed : " +
                        diagnostics.transactionsParsed
                )

                Text(
                    "Rejected Blocks : " +
                        diagnostics.rejectedBlocks
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date

                Text(
                    "Last Parsed Date : " +
                        diagnostics.lastParsedDate
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reconciliation

                Text(
                    text = "Reconciliation",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Calculated Credits : " +
                        diagnostics.calculatedCredits
                )

                Text(
                    "Statement Credits : " +
                        (diagnostics.statementCredits ?: "—")
                )

                Text(
                    "Credit Difference : " +
                        diagnostics.creditDifference
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "Calculated Debits : " +
                        diagnostics.calculatedDebits
                )

                Text(
                    "Statement Debits : " +
                        (diagnostics.statementDebits ?: "—")
                )

                Text(
                    "Debit Difference : " +
                        diagnostics.debitDifference
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stop reason

                Text(
                    text = "Stop Reason",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    diagnostics.stopReason
                )

                // Missed date lines

                if (diagnostics.missedDateLines.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Missed Date Lines",
                        style = MaterialTheme.typography.titleSmall
                    )

                    diagnostics.missedDateLines.forEach { line ->

                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
