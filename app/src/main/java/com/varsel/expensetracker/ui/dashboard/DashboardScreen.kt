package com.varsel.expensetracker.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.varsel.expensetracker.ui.model.TransactionUiModel
import com.varsel.expensetracker.ui.dashboard.components.BalanceCard
import com.varsel.expensetracker.ui.dashboard.components.GreetingHeader
import com.varsel.expensetracker.ui.dashboard.components.InsightsCard
import com.varsel.expensetracker.ui.dashboard.components.DashboardRecentSection

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAllTransactions: () -> Unit
) {
    // Lifecycle-aware flow collection to prevent background CPU cycles
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    
    Box(
    modifier = Modifier.fillMaxSize()
) {

    if (uiState.isLoading) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

    } else {

        LazyColumn(

            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {

            item(key = "greeting") {

                GreetingHeader()

            }

            item(key = "balance_card") {

                BalanceCard(
                    summary = uiState.balanceSummary
                )

            }

            item(key = "insights") {

                InsightsCard()

            }

            item(
                key = "recent_transactions"
            ) {

                DashboardRecentSection(

                    transactions = uiState.recentTransactions,

                    onViewAll = onNavigateToAllTransactions,

                    onTransactionClick = {

                        // TODO: Reconnect edit dialog
                        // after UI → Domain migration.

                    }
                )
            }
        }
    }
}
}

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)
    val dateStr = try {
        val date = Instant.ofEpochMilli(transaction.dateTimestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        date.format(dateFormatter)
    } catch (e: Exception) {
        "Recent"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { "Transaction" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = transaction.category.ifEmpty { "Uncategorized" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• $dateStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            val isIncome = transaction.type == TransactionType.INCOME
            val amountPrefix = if (isIncome) "+" else "-"
            val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)

            Text(
                text = "$amountPrefix₹%.2f".format(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTransactionCategoryDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var categoryInput by remember { mutableStateOf(transaction.category) }
    var descriptionInput by remember { mutableStateOf(transaction.description) }

    val suggestedCategories = listOf("Food & Drink", "Groceries", "Transport", "Shopping", "Utilities", "Salary", "Income", "Subscriptions", "Uncategorized")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Description / Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Quick Categories:", style = MaterialTheme.typography.labelMedium)
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestedCategories.forEach { cat ->
                        SuggestionChip(
                            onClick = { categoryInput = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = transaction.copy(
                        description = descriptionInput.ifEmpty { transaction.description },
                        category = categoryInput.ifEmpty { "Uncategorized" }
                    )
                    onSave(updated)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
