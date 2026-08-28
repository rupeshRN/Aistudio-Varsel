package com.varsel.expensetracker.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType

@Composable
fun TransactionRoleSection(

    transactionType: TransactionType,

    selectedRole: TransactionRole,

    onRoleSelected:
        (TransactionRole) -> Unit

) {

    //--------------------------------------------------
    // Determine roles available for this transaction.
    //
    // EXPENSE / DEBIT
    // ----------------
    // Normal
    // Lent
    // Transfer Out
    //
    // INCOME / CREDIT
    // ----------------
    // Normal
    // Reimbursement
    // Transfer In
    //
    // Transfer roles are intentionally tied to the
    // direction of the transaction.
    //--------------------------------------------------

    val availableRoles =
        when (transactionType) {

            TransactionType.EXPENSE,
            TransactionType.DEBIT -> {

                listOf(

                    TransactionRole.NORMAL,

                    TransactionRole.LENT,

                    TransactionRole.TRANSFER_OUT
                )
            }

            TransactionType.INCOME,
            TransactionType.CREDIT -> {

                listOf(

                    TransactionRole.NORMAL,

                    TransactionRole.REIMBURSEMENT,

                    TransactionRole.TRANSFER_IN
                )
            }
        }

    Column(

        modifier =
            Modifier.fillMaxWidth(),

        verticalArrangement =
            Arrangement.spacedBy(
                8.dp
            )

    ) {

        Text(

            text =
                "Transaction Role",

            style =
                MaterialTheme
                    .typography
                    .titleSmall
        )

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )

        ) {

            availableRoles.forEach { role ->

                FilterChip(

                    selected =
                        selectedRole ==
                            role,

                    onClick = {

                        onRoleSelected(
                            role
                        )
                    },

                    label = {

                        Text(

                            when (role) {

                                TransactionRole.NORMAL ->
                                    "Normal"

                                TransactionRole.LENT ->
                                    "Lent"

                                TransactionRole.REIMBURSEMENT ->
                                    "Reimbursement"

                                TransactionRole.TRANSFER_IN ->
                                    "Transfer In"

                                TransactionRole.TRANSFER_OUT ->
                                    "Transfer Out"
                            }
                        )
                    }
                )
            }
        }
    }
}
