package com.varsel.expensetracker.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PieChart
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
import com.varsel.expensetracker.ui.budget.components.BudgetCard
import com.varsel.expensetracker.ui.budget.components.BudgetChooserBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsOverviewScreen(
    viewModel: BudgetViewModel,
    onNavigateToBudgetDetail: (Long) -> Unit,
    onNavigateToBudgetHistory: (Long) -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showChooserSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedBudgetType by remember { mutableStateOf("EXPENSE") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showChooserSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_budget_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Budget"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header (Cashew style large "Budgets" title)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                        Text(
                            text = "Budgets",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { showChooserSheet = true },
                        modifier = Modifier.testTag("manage_budgets_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Manage Budgets",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Budget Cards list (Cashew Screenshot 1)
            items(state.budgets, key = { it.budget.id }) { budgetModel ->
                BudgetCard(
                    budgetUiModel = budgetModel,
                    onClick = { onNavigateToBudgetDetail(budgetModel.budget.id) },
                    onHistoryClick = { onNavigateToBudgetHistory(budgetModel.budget.id) }
                )
            }

            // Add Budget Placeholder Card (Cashew Screenshot 1 style dashed card at bottom)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showChooserSheet = true }
                        .testTag("add_budget_card_placeholder"),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Budget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Empty state suggestion chips (if no budgets created yet)
            if (state.budgets.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Take control of your spending",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Set daily & monthly spending limits for food, entertainment, shopping, and more.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Budget Type Chooser BottomSheet
        if (showChooserSheet) {
            BudgetChooserBottomSheet(
                onDismiss = { showChooserSheet = false },
                onSelectType = { type ->
                    selectedBudgetType = type
                    showChooserSheet = false
                    showAddSheet = true
                }
            )
        }

        // Add Budget BottomSheet
        if (showAddSheet) {
            AddEditBudgetBottomSheet(
                initialBudgetType = selectedBudgetType,
                categories = state.availableCategories,
                onDismiss = { showAddSheet = false },
                onSave = { name, categoryName, amount, period, startDay, limitTotalType, spendingLimitType, budgetType, colorHex, iconName ->
                    viewModel.createBudget(
                        name = name,
                        categoryName = categoryName,
                        amount = amount,
                        period = period,
                        startDayOfMonth = startDay,
                        limitTotalType = limitTotalType,
                        spendingLimitType = spendingLimitType,
                        budgetType = budgetType,
                        colorHex = colorHex,
                        iconName = iconName
                    )
                    showAddSheet = false
                }
            )
        }
    }
}
