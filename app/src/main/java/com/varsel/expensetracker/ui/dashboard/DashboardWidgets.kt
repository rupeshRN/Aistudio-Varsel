package com.varsel.expensetracker.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.budget.model.BudgetUiModel
import com.varsel.expensetracker.ui.components.BankLogoBadge
import com.varsel.expensetracker.ui.model.AccountBalanceUiModel
import java.util.Locale

private fun formatMoney(amount: Double): String {
    return String.format(Locale.getDefault(), "₹%,.2f", amount)
}

@Composable
fun DashboardBudgetsWidget(
    visibleBudgets: List<BudgetUiModel>,
    allBudgets: List<BudgetUiModel>,
    currentSelection: String,
    onSelectBudgets: (String) -> Unit,
    onNavigateToBudgets: () -> Unit
) {
    var showSelectionDialog by remember { mutableStateOf(false) }

    if (showSelectionDialog) {
        HomeWidgetItemsSelectionDialog(
            title = "Monthly Budgets on Home",
            items = allBudgets,
            currentSelection = currentSelection,
            onSave = { onSelectBudgets(it) },
            onDismiss = { showSelectionDialog = false }
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onNavigateToBudgets)
            .testTag("dashboard_budgets_widget")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Monthly Budgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (allBudgets.isNotEmpty()) {
                        val countText = if (currentSelection == "ALL" || currentSelection.isBlank() || visibleBudgets.size == allBudgets.size) {
                            "(${allBudgets.size})"
                        } else {
                            "(${visibleBudgets.size}/${allBudgets.size})"
                        }
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (allBudgets.size > 1) {
                        IconButton(
                            onClick = { showSelectionDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Select budgets to display",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "View Budgets",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (allBudgets.isEmpty()) {
                Text(
                    text = "No monthly budgets set yet. Create spending limits to manage your finances.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onNavigateToBudgets,
                    modifier = Modifier.padding(top = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Budget")
                }
            } else {
                // Concise Budget items
                visibleBudgets.forEachIndexed { index, budgetItem ->
                    val isOver = budgetItem.isOverBudget
                    val spentRatio = budgetItem.spentRatio.coerceIn(0f, 1f)
                    val progressColor = when {
                        isOver -> Color(0xFFE53935)
                        spentRatio >= 0.85f -> Color(0xFFFFA000)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = budgetItem.budget.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${formatMoney(budgetItem.amountSpent)} / ${formatMoney(budgetItem.budget.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${budgetItem.percentSpent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOver) Color(0xFFD32F2F) else progressColor
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { spentRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val remainingText = if (isOver) {
                                "₹%,.0f over limit".format(budgetItem.overBudgetAmount)
                            } else {
                                "₹%,.0f left".format(budgetItem.amountLeft)
                            }
                            Text(
                                text = remainingText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isOver) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isOver) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${budgetItem.daysRemaining} days left",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (index < visibleBudgets.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardAccountsWidget(
    snapshots: List<AccountBalanceUiModel>,
    modifier: Modifier = Modifier
) {
    var isBalanceHidden by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_accounts_widget"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header - Unboxed, clean styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                val sectionTitle = if (snapshots.isEmpty()) {
                    "Linked Accounts"
                } else {
                    "Linked Accounts (${snapshots.size})"
                }
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (snapshots.isNotEmpty()) {
                IconButton(
                    onClick = { isBalanceHidden = !isBalanceHidden },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isBalanceHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isBalanceHidden) "Show balance" else "Hide balance",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (snapshots.isEmpty()) {
            // Clean, unboxed minimal placeholder without card background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Import bank statements to view your account balances.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Clean, borderless account rows without any card background
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                snapshots.forEachIndexed { index, account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BankLogoBadge(
                            bankName = account.bankName,
                            size = 40.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            val displayName = if (account.bankShortName.isNotBlank() && account.bankShortName != "Bank") {
                                account.bankShortName
                            } else if (account.bankName.isNotBlank() && account.bankName != "Bank Account") {
                                com.varsel.expensetracker.util.BankInfoHelper.getBankShortName(account.bankName)
                            } else {
                                "Bank Account"
                            }
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = account.accountDisplayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isBalanceHidden) "₹ •••••" else formatMoney(account.balance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Available",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (index < snapshots.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp, end = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardGoalsWidget(
    visibleGoals: List<BudgetUiModel>,
    allGoals: List<BudgetUiModel>,
    currentSelection: String,
    onSelectGoals: (String) -> Unit,
    onNavigateToGoals: () -> Unit
) {
    var showSelectionDialog by remember { mutableStateOf(false) }

    if (showSelectionDialog) {
        HomeWidgetItemsSelectionDialog(
            title = "Savings Goals on Home",
            items = allGoals,
            currentSelection = currentSelection,
            onSave = { onSelectGoals(it) },
            onDismiss = { showSelectionDialog = false }
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onNavigateToGoals)
            .testTag("dashboard_goals_widget")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Savings,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Savings Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (allGoals.isNotEmpty()) {
                        val countText = if (currentSelection == "ALL" || currentSelection.isBlank() || visibleGoals.size == allGoals.size) {
                            "(${allGoals.size})"
                        } else {
                            "(${visibleGoals.size}/${allGoals.size})"
                        }
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (allGoals.size > 1) {
                        IconButton(
                            onClick = { showSelectionDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Select goals to display",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "View Goals",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (allGoals.isEmpty()) {
                Text(
                    text = "No savings goals created yet. Set savings targets to track your milestones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onNavigateToGoals,
                    modifier = Modifier.padding(top = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Savings Goal")
                }
            } else {
                visibleGoals.forEachIndexed { index, goal ->
                    val ratio = if (goal.budget.amount > 0) (goal.amountSpent / goal.budget.amount).toFloat().coerceIn(0f, 1f) else 0f
                    val isCompleted = goal.amountSpent >= goal.budget.amount

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = goal.budget.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${formatMoney(goal.amountSpent)} / ${formatMoney(goal.budget.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${goal.percentSpent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF4CAF50),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isCompleted) {
                                Text(
                                    text = "Goal reached! 🎉",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            } else {
                                Text(
                                    text = "₹%,.0f to go".format(goal.amountLeft.coerceAtLeast(0.0)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Text(
                                text = "${goal.percentSpent}% saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                    if (index < visibleGoals.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeWidgetItemsSelectionDialog(
    title: String,
    items: List<BudgetUiModel>,
    currentSelection: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isShowAll by remember { mutableStateOf(currentSelection == "ALL" || currentSelection.isBlank()) }
    val initialSelectedIds = remember {
        if (currentSelection == "ALL" || currentSelection.isBlank()) {
            items.map { it.budget.id }.toSet()
        } else {
            currentSelection.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
        }
    }
    var selectedIds by remember { mutableStateOf(initialSelectedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Select which items to display on your home screen:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Show All Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val newShowAll = !isShowAll
                            isShowAll = newShowAll
                            if (newShowAll) {
                                selectedIds = items.map { it.budget.id }.toSet()
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isShowAll,
                        onCheckedChange = { checked ->
                            isShowAll = checked
                            if (checked) {
                                selectedIds = items.map { it.budget.id }.toSet()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Show All (${items.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )

                // Individual item checkboxes
                items.forEach { item ->
                    val isChecked = isShowAll || selectedIds.contains(item.budget.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isShowAll = false
                                selectedIds = if (selectedIds.contains(item.budget.id)) {
                                    selectedIds - item.budget.id
                                } else {
                                    selectedIds + item.budget.id
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                isShowAll = false
                                selectedIds = if (checked) {
                                    selectedIds + item.budget.id
                                } else {
                                    selectedIds - item.budget.id
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.budget.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${formatMoney(item.amountSpent)} / ${formatMoney(item.budget.amount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isShowAll || selectedIds.size == items.size || selectedIds.isEmpty()) {
                        onSave("ALL")
                    } else {
                        onSave(selectedIds.joinToString(","))
                    }
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
