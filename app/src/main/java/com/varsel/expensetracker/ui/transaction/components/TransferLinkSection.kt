package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransferLinkSection(

    transaction:
        Transaction,

    linkedTransfer:
        Transaction?,

    candidateTransactions:
        List<Transaction>,

    isLinking:
        Boolean,

    transferErrorMessage:
        String?,

    onLinkTransfer:
        (Long) -> Unit,

    onUnlinkTransfer:
        () -> Unit,

    onClearError:
        () -> Unit

) {

    //--------------------------------------------------
    // Transfer section
    //--------------------------------------------------

    Column(

        modifier =
            Modifier.fillMaxWidth(),

        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )

    ) {

        Text(

            text =
                "Transfer",

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        //--------------------------------------------------
        // Existing transfer
        //--------------------------------------------------

        if (
            linkedTransfer != null
        ) {

            Text(

                text =
                    "Linked transfer",

                style =
                    MaterialTheme
                        .typography
                        .titleSmall
            )

            TransferTransactionRow(
                transaction =
                    linkedTransfer
            )

            OutlinedButton(

                onClick =
                    onUnlinkTransfer,

                enabled =
                    !isLinking,

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(
                    "Unlink Transfer"
                )
            }

        } else {

            //--------------------------------------------------
            // No transfer yet
            //--------------------------------------------------

            if (
                candidateTransactions
                    .isNotEmpty()
            ) {

                Text(

                    text =
                        if (
                            transaction.role ==
                                TransactionRole.TRANSFER_OUT
                        ) {
                            "Select the Transfer In transaction"
                        } else {
                            "Select the Transfer Out transaction"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )

                candidateTransactions.forEach {
                    candidate ->

                    TransferCandidateRow(

                        transaction =
                            candidate,

                        enabled =
                            !isLinking,

                        onClick = {

                            onLinkTransfer(
                                candidate.id
                            )
                        }
                    )
                }

            } else {

                Text(

                    text =
                        "No matching transfer transaction " +
                            "is currently available.",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }

    //--------------------------------------------------
    // Transfer validation error
    //--------------------------------------------------

    if (
        transferErrorMessage != null
    ) {

        AlertDialog(

            onDismissRequest = {
                onClearError()
            },

            title = {

                Text(
                    "Transfer could not be linked"
                )
            },

            text = {

                Text(
                    transferErrorMessage
                )
            },

            confirmButton = {

                TextButton(

                    onClick =
                        onClearError

                ) {

                    Text(
                        "OK"
                    )
                }
            }
        )
    }
}

@Composable
private fun TransferCandidateRow(

    transaction:
        Transaction,

    enabled:
        Boolean,

    onClick:
        () -> Unit

) {

    OutlinedButton(

        onClick =
            onClick,

        enabled =
            enabled,

        modifier =
            Modifier.fillMaxWidth()

    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween

        ) {

            Column(

                modifier =
                    Modifier.weight(1f)

            ) {

                Text(

                    text =
                        transaction.description,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )

                Text(

                    text =
                        formatDate(
                            transaction.dateTimestamp
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Text(

                text =
                    "₹%,.2f"
                        .format(
                            transaction.amount
                        ),

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }
    }
}

@Composable
private fun TransferTransactionRow(

    transaction:
        Transaction

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween

    ) {

        Column(

            modifier =
                Modifier.weight(1f)

        ) {

            Text(

                text =
                    transaction.description,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )

            Text(

                text =
                    formatDate(
                        transaction.dateTimestamp
                    ),

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        Text(

            text =
                "₹%,.2f"
                    .format(
                        transaction.amount
                    ),

            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )
    }
}

private fun formatDate(
    timestamp: Long
): String {

    return SimpleDateFormat(

        "dd MMM yyyy",

        Locale.ENGLISH

    ).format(

        Date(
            timestamp
        )
    )
}
