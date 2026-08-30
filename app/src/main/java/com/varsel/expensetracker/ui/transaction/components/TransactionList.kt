package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.dashboard.components.RecentTransactionCard
import com.varsel.expensetracker.ui.model.TransactionUiModel

fun LazyListScope.transactionList(

    transactions: List<TransactionUiModel>,

    onTransactionClick: (TransactionUiModel) -> Unit = {}

) {

    if (transactions.isEmpty()) {

        item(
            key = "empty_transactions"
        ) {

            TransactionEmptyState()

        }

    } else {

        items(

            count = transactions.size,

            key = {

                transactions[it].id

            }

        ) { index ->

            RecentTransactionCard(

                transaction = transactions[index],

                onClick = {

                    onTransactionClick(

                        transactions[index]

                    )

                }

            )

        }

    }

}

@Composable
private fun TransactionEmptyState() {

    Box(

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),

        contentAlignment = Alignment.Center

    ) {

        Text(

            text = "No transactions found.",

            style = MaterialTheme.typography.bodyLarge

        )

    }

}
