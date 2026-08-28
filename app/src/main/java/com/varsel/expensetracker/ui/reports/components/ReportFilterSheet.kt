package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.reports.ReportsAccount

/**
 * Account/report filter sheet.
 *
 * The sheet owns only temporary UI selection.
 *
 * Changes are not applied to the report until the user
 * presses Apply.
 */
@Composable
fun ReportFilterSheet(
    accounts: List<ReportsAccount>,
    selectedAccountIds: Set<String>,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    var temporarySelectedAccounts by remember(
        selectedAccountIds
    ) {
        mutableStateOf(selectedAccountIds)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Report Filters",
                style =
                    MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Accounts",
                style =
                    MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            /*
             * All Accounts
             *
             * Empty selection represents All Accounts.
             */
            FilterAccountRow(
                label = "All Accounts",
                selected =
                    temporarySelectedAccounts.isEmpty(),
                onClick = {
                    temporarySelectedAccounts =
                        emptySet()
                }
            )

            HorizontalDivider()

            if (accounts.isEmpty()) {

                Text(
                    text = "No accounts available",
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    modifier = Modifier.padding(
                        vertical = 8.dp
                    )
                )

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(
                            1f,
                            fill = false
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    items(
                        items = accounts,
                        key = {
                            it.accountId
                        }
                    ) { account ->

                        FilterAccountRow(
                            label =
                                account.displayName,
                            selected =
                                account.accountId in
                                    temporarySelectedAccounts,
                            onClick = {

                                temporarySelectedAccounts =
                                    if (
                                        account.accountId in
                                            temporarySelectedAccounts
                                    ) {
                                        temporarySelectedAccounts -
                                            account.accountId
                                    } else {
                                        temporarySelectedAccounts +
                                            account.accountId
                                    }
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.End,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }

                Spacer(
                    modifier = Modifier.size(8.dp)
                )

                Button(
                    onClick = {
                        onApply(
                            temporarySelectedAccounts
                        )
                    }
                ) {
                    Text("Apply")
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }
    }
}

/**
 * Individual account filter row.
 */
@Composable
private fun FilterAccountRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 4.dp,
                vertical = 8.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Checkbox(
            checked = selected,
            onCheckedChange = {
                onClick()
            }
        )

        Spacer(
            modifier = Modifier.size(8.dp)
        )

        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyLarge
        )
    }
}
