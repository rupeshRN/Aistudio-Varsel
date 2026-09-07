package com.varsel.expensetracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.data.preference.HomeSection
import com.varsel.expensetracker.ui.components.AppIconLoadingView
import com.varsel.expensetracker.ui.dashboard.components.BalanceCard
import com.varsel.expensetracker.ui.dashboard.components.DashboardLoanWidget
import com.varsel.expensetracker.ui.dashboard.components.DashboardRecentSection
import com.varsel.expensetracker.ui.dashboard.components.GreetingHeader
import com.varsel.expensetracker.ui.dashboard.components.InsightsCard
import com.varsel.expensetracker.ui.dashboard.components.QuickActionBar
import com.varsel.expensetracker.ui.model.TransactionUiModel

private sealed class FeatureDialogState {
    object None : FeatureDialogState()
    data class UnderDevelopment(
        val title: String,
        val message: String,
        val icon: ImageVector
    ) : FeatureDialogState()
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAllTransactions: () -> Unit,
    onNavigateToImport: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToTransactionDetail: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeSections by viewModel.activeHomeSections.collectAsStateWithLifecycle()
    var featureDialog by remember { mutableStateOf<FeatureDialogState>(FeatureDialogState.None) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.isLoading) {
            AppIconLoadingView(
                title = "Varsel",
                subtitle = "Securing your offline ledger..."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                activeSections.forEach { sectionId ->
                    when (sectionId) {
                        HomeSection.BANNER.id -> {
                            item(key = "greeting") {
                                GreetingHeader(
                                    onSettingsClick = onNavigateToSettings
                                )
                            }
                        }

                        HomeSection.NET_WORTH.id -> {
                            item(key = "balance_card") {
                                BalanceCard(
                                    summary = uiState.balanceSummary,
                                    isBalanceHidden = uiState.isBalanceHidden,
                                    onToggleVisibility = { viewModel.toggleBalanceVisibility() }
                                )
                            }
                        }

                        HomeSection.QUICK_ACTIONS.id -> {
                            item(key = "quick_actions") {
                                QuickActionBar(
                                    onImportClick = onNavigateToImport,
                                    onAddTransactionClick = {
                                        featureDialog = FeatureDialogState.UnderDevelopment(
                                            title = "Manual Entry In Development",
                                            message = "Varsel is designed to automatically ingest, categorize, and reconcile transactions directly from your bank statements with zero manual input.\n\nManual expense and income creation is planned for users who prefer manual bookkeeping.",
                                            icon = Icons.Outlined.Construction
                                        )
                                    },
                                    onTransferClick = {
                                        featureDialog = FeatureDialogState.UnderDevelopment(
                                            title = "Account Transfer",
                                            message = "Transfer transactions are automatically identified and linked between your accounts during Statement Import.\n\nDirect manual transfer entry is currently under development.",
                                            icon = Icons.Outlined.SwapHoriz
                                        )
                                    },
                                    onAnalyticsClick = onNavigateToAnalytics
                                )
                            }
                        }

                        HomeSection.INSIGHTS.id -> {
                            if (uiState.insights.isNotEmpty()) {
                                item(key = "insights") {
                                    InsightsCard(
                                        insights = uiState.insights,
                                        onNavigateToAnalytics = onNavigateToAnalytics,
                                        onNavigateToTransactions = onNavigateToAllTransactions
                                    )
                                }
                            }
                        }

                        HomeSection.LOANS.id -> {
                            item(key = "loans_widget") {
                                DashboardLoanWidget(
                                    loans = uiState.loans,
                                    onNavigateToLoans = onNavigateToLoans
                                )
                            }
                        }

                        HomeSection.TRANSACTIONS.id -> {
                            item(key = "recent_transactions") {
                                DashboardRecentSection(
                                    transactions = uiState.recentTransactions,
                                    onViewAll = onNavigateToAllTransactions,
                                    onTransactionClick = { transactionUiModel ->
                                        onNavigateToTransactionDetail(transactionUiModel.id)
                                    }
                                )
                            }
                        }

                        HomeSection.BUDGETS.id -> {
                            item(key = "budgets_widget") {
                                DashboardBudgetsWidget(
                                    visibleBudgets = uiState.visibleBudgets,
                                    allBudgets = uiState.allBudgets,
                                    currentSelection = uiState.homeBudgetsSelection,
                                    onSelectBudgets = { viewModel.setHomeBudgetsSelection(it) },
                                    onNavigateToBudgets = onNavigateToBudgets
                                )
                            }
                        }

                        HomeSection.ACCOUNTS_LIST.id -> {
                            item(key = "accounts_widget") {
                                DashboardAccountsWidget(
                                    snapshots = uiState.balanceSummary.accounts,
                                    isBalanceHidden = uiState.isBalanceHidden
                                )
                            }
                        }

                        HomeSection.GOALS.id -> {
                            item(key = "goals_widget") {
                                DashboardGoalsWidget(
                                    visibleGoals = uiState.visibleGoals,
                                    allGoals = uiState.allGoals,
                                    currentSelection = uiState.homeGoalsSelection,
                                    onSelectGoals = { viewModel.setHomeGoalsSelection(it) },
                                    onNavigateToGoals = onNavigateToBudgets
                                )
                            }
                        }
                    }
                }
            }
        }

        // Under development feature dialog
        when (val state = featureDialog) {
            is FeatureDialogState.UnderDevelopment -> {
                AlertDialog(
                    onDismissRequest = { featureDialog = FeatureDialogState.None },
                    icon = {
                        Icon(
                            imageVector = state.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { featureDialog = FeatureDialogState.None }
                        ) {
                            Text("Got It")
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }
            FeatureDialogState.None -> Unit
        }
    }
}
