package com.varsel.expensetracker.ui.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.ui.budget.components.AddEditBudgetBottomSheet
import com.varsel.expensetracker.ui.budget.components.BudgetCard
import com.varsel.expensetracker.ui.budget.components.BudgetChooserBottomSheet
import com.varsel.expensetracker.ui.budget.components.ManageBudgetsBottomSheet

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
    val listState = rememberLazyListState()

    var showChooserSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showManageSheet by remember { mutableStateOf(false) }
    var editBudgetTarget by remember { mutableStateOf<BudgetEntity?>(null) }
    var selectedBudgetType by remember { mutableStateOf("EXPENSE") }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 30
        }
    }

    val scrollFraction by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "budget_header_scroll_fraction"
    )

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = (4 * scrollFraction).dp,
                shadowElevation = (3 * scrollFraction).dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = (18 - 8 * scrollFraction).dp,
                            bottom = (14 - 6 * scrollFraction).dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (onBackClick != null) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Budgets",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = (26 - 6 * scrollFraction).sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (state.budgets.isNotEmpty()) {
                                AnimatedVisibility(
                                    visible = !isScrolled,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Text(
                                        text = "${state.budgets.size} active • ${BudgetCalculator.formatCurrency(state.totalAmountSpent, round = true)} spent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { showManageSheet = true },
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
        },
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
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Budget Cards list
            items(state.budgets, key = { it.budget.id }) { budgetModel ->
                BudgetCard(
                    budgetUiModel = budgetModel,
                    onClick = { onNavigateToBudgetDetail(budgetModel.budget.id) },
                    onHistoryClick = { onNavigateToBudgetHistory(budgetModel.budget.id) }
                )
            }

            // Add Budget Placeholder Card (Dashed card at bottom)
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

        // Manage Budgets Bottom Sheet (Reorder, Hide, Edit)
        if (showManageSheet) {
            ManageBudgetsBottomSheet(
                budgets = state.allBudgets,
                hiddenBudgetIds = state.hiddenBudgetIds,
                onDismiss = { showManageSheet = false },
                onToggleHide = viewModel::toggleHideBudget,
                onMoveUp = viewModel::moveBudgetUp,
                onMoveDown = viewModel::moveBudgetDown,
                onEditBudget = { budget ->
                    editBudgetTarget = budget
                    showManageSheet = false
                },
                onDeleteBudget = viewModel::deleteBudget,
                onAddNewBudget = {
                    showManageSheet = false
                    showChooserSheet = true
                }
            )
        }

        // Edit Existing Budget BottomSheet
        if (editBudgetTarget != null) {
            AddEditBudgetBottomSheet(
                existingBudget = editBudgetTarget,
                initialBudgetType = editBudgetTarget?.budgetType ?: "EXPENSE",
                categories = state.availableCategories,
                onDismiss = { editBudgetTarget = null },
                onSave = { name, categoryName, amount, period, startDay, limitTotalType, spendingLimitType, budgetType, colorHex, iconName ->
                    editBudgetTarget?.let { existing ->
                        viewModel.updateBudget(
                            existing.copy(
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
                        )
                    }
                    editBudgetTarget = null
                },
                onDelete = {
                    editBudgetTarget?.let { viewModel.deleteBudget(it.id) }
                    editBudgetTarget = null
                }
            )
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

