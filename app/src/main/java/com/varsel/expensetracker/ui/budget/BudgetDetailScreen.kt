package com.varsel.expensetracker.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
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
import com.varsel.expensetracker.ui.budget.components.AddEditBudgetBottomSheet
import com.varsel.expensetracker.ui.budget.components.BudgetCircularGauge
import com.varsel.expensetracker.ui.budget.components.BudgetProgressTrack
import com.varsel.expensetracker.ui.budget.components.DailySpendingChart
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetId: Long,
    viewModel: BudgetViewModel,
    onBackClick: () -> Unit,
    onNavigateToHistory: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val detailFlow = remember(budgetId) { viewModel.getBudgetDetail(budgetId) }
    val budgetUiModel by detailFlow.collectAsState()
    val state by viewModel.uiState.collectAsState()

    var showEditSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentModel = budgetUiModel

    if (currentModel == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val accentColor = try {
        Color(android.graphics.Color.parseColor(currentModel.categoryColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("budget_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToHistory(budgetId) },
                        modifier = Modifier.testTag("budget_detail_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "History"
                        )
                    }

                    IconButton(
                        onClick = { showEditSheet = true },
                        modifier = Modifier.testTag("budget_detail_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Budget"
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Budget", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Category Title, Left of total, Date range (Cashew Screenshot 2)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = currentModel.budget.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val leftOrOverText = if (currentModel.isOverBudget) {
                        "${BudgetCalculator.formatCurrency(currentModel.overBudgetAmount, round = true)} over of ${BudgetCalculator.formatCurrency(currentModel.budget.amount, round = true)}"
                    } else {
                        "${BudgetCalculator.formatCurrency(currentModel.amountLeft, round = true)} left of ${BudgetCalculator.formatCurrency(currentModel.budget.amount, round = true)}"
                    }

                    Text(
                        text = leftOrOverText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (currentModel.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress track with "Today" pin
                    BudgetProgressTrack(
                        spentRatio = currentModel.spentRatio,
                        todayRatio = currentModel.todayRatio,
                        percentSpent = currentModel.percentSpent,
                        periodStartLabel = currentModel.periodStartFull,
                        periodEndLabel = currentModel.periodEndFull,
                        dailyAllowanceText = currentModel.dailyAllowanceText,
                        fillColor = accentColor,
                        isOverBudget = currentModel.isOverBudget
                    )
                }
            }

            // Circular Gauge Visual (Cashew Screenshot 2 Centerpiece)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BudgetCircularGauge(
                        spentRatio = currentModel.spentRatio,
                        percentSpent = currentModel.percentSpent,
                        amountSpent = currentModel.amountSpent,
                        amountTotal = currentModel.budget.amount,
                        accentColor = accentColor,
                        isOverBudget = currentModel.isOverBudget
                    )
                }
            }

            // Category Summary Row (Cashew Screenshot 2 style)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = currentModel.categoryIcon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentModel.budget.categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val txCount = currentModel.transactions.size
                            Text(
                                text = "${currentModel.percentSpent}% of spending • $txCount ${if (txCount == 1) "transaction" else "transactions"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = BudgetCalculator.formatCurrency(currentModel.amountSpent, round = true),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Daily Spending Chart (Cashew Screenshot 2 style)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        DailySpendingChart(
                            dailyPoints = currentModel.dailySpending,
                            targetDailyAllowance = currentModel.targetDailyAllowance,
                            barColor = accentColor
                        )
                    }
                }
            }

            // Contributing Transactions Section Header
            item {
                Text(
                    text = "Transactions this period",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Contributing Transactions List
            if (currentModel.transactions.isEmpty()) {
                item {
                    Text(
                        text = "No expenses recorded in this period yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                items(currentModel.transactions, key = { it.id }) { tx ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onTransactionClick(tx.id) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.description.ifBlank { tx.category },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateFormat.format(tx.dateTimestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "- ${BudgetCalculator.formatCurrency(tx.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Edit Budget Bottom Sheet
        if (showEditSheet) {
            AddEditBudgetBottomSheet(
                existingBudget = currentModel.budget,
                categories = state.availableCategories,
                onDismiss = { showEditSheet = false },
                onSave = { name, categoryName, amount, period, startDay, limitTotalType, spendingLimitType, budgetType, colorHex, iconName ->
                    viewModel.updateBudget(
                        currentModel.budget.copy(
                            name = name,
                            categoryName = categoryName,
                            amount = amount,
                            period = period,
                            startDayOfMonth = startDay,
                            limitTotalType = limitTotalType,
                            spendingLimitType = spendingLimitType,
                            budgetType = budgetType,
                            colorHex = colorHex ?: currentModel.budget.colorHex,
                            iconName = iconName ?: currentModel.budget.iconName
                        )
                    )
                    showEditSheet = false
                },
                onDelete = {
                    showEditSheet = false
                    showDeleteConfirm = true
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Budget") },
                text = { Text("Are you sure you want to delete the budget for ${currentModel.budget.name}? Transactions will not be deleted.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBudget(budgetId)
                            showDeleteConfirm = false
                            onBackClick()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
