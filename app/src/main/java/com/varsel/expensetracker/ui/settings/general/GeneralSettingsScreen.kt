package com.varsel.expensetracker.ui.settings.general

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.data.preference.BiometricTimeout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToEditHome: () -> Unit,
    viewModel: GeneralSettingsViewModel = hiltViewModel()
) {
    val generalConfig by viewModel.generalConfig.collectAsStateWithLifecycle()
    var showBiometricTimeoutMenu by remember { mutableStateOf(false) }
    var showNavTabsSheet by remember { mutableStateOf(false) }

    // Dialogs for widgets
    var showNetWorthDialog by remember { mutableStateOf(false) }
    var showIncomeExpenseDialog by remember { mutableStateOf(false) }
    var showBudgetWidgetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("general_settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Large Title matching Screenshot 1
            Text(
                text = "General",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 1. Biometric Lock Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = "Biometric Lock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Timeout Dropdown Button
                Box {
                    Button(
                        onClick = { showBiometricTimeoutMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("biometric_timeout_dropdown")
                    ) {
                        Text(
                            text = generalConfig.biometricTimeout.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showBiometricTimeoutMenu,
                        onDismissRequest = { showBiometricTimeoutMenu = false }
                    ) {
                        BiometricTimeout.entries.forEach { timeout ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = timeout.label,
                                        fontWeight = if (timeout == generalConfig.biometricTimeout) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setBiometricTimeout(timeout)
                                    showBiometricTimeoutMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Edit Home Page Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToEditHome)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = "Edit Home Page",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Edit Navigation Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNavTabsSheet = true }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ViewStream,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Edit Navigation Tabs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${generalConfig.navigationTabs.size} shortcuts configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Section: Widgets
            Text(
                text = "Widgets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Net Worth Widget Row
            WidgetSettingRow(
                icon = Icons.Outlined.TrendingUp,
                title = "Net Worth Widget",
                subtitle = "Period: ${generalConfig.netWorthWidgetPeriod}",
                onClick = { showNetWorthDialog = true }
            )

            // Income/Expense Widget Row
            WidgetSettingRow(
                icon = Icons.Outlined.SwapVert,
                title = "Income/Expense Widget",
                subtitle = "Period: ${generalConfig.incomeExpenseWidgetPeriod}",
                onClick = { showIncomeExpenseDialog = true }
            )

            // Budget Widget Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBudgetWidgetDialog = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = "Budget Widget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = generalConfig.budgetWidgetCategory,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Widget Theme Row
            WidgetSettingRow(
                icon = Icons.Outlined.Palette,
                title = "Widget Theme",
                subtitle = "Follows active app theme & accents",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Navigation Tabs Sheet
    if (showNavTabsSheet) {
        EditNavigationTabsSheet(
            currentTabs = generalConfig.navigationTabs,
            floatingNavBar = generalConfig.floatingNavBar,
            showNavLabels = generalConfig.showNavLabels,
            onTabSelected = { slotIndex, route ->
                viewModel.updateNavigationSlot(slotIndex, route)
            },
            onFloatingNavBarChange = { viewModel.setFloatingNavBar(it) },
            onShowNavLabelsChange = { viewModel.setShowNavLabels(it) },
            onDismiss = { showNavTabsSheet = false }
        )
    }

    // Widget Customization Dialogs
    if (showNetWorthDialog) {
        val periods = listOf("This Month", "Last 3 Months", "Last 6 Months", "This Year", "All Time")
        AlertDialog(
            onDismissRequest = { showNetWorthDialog = false },
            title = { Text("Net Worth Time Period") },
            text = {
                Column {
                    periods.forEach { period ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setNetWorthWidgetPeriod(period)
                                    showNetWorthDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = generalConfig.netWorthWidgetPeriod == period,
                                onClick = {
                                    viewModel.setNetWorthWidgetPeriod(period)
                                    showNetWorthDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(period)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNetWorthDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showIncomeExpenseDialog) {
        val periods = listOf("This Week", "This Month", "Last Month", "This Year")
        AlertDialog(
            onDismissRequest = { showIncomeExpenseDialog = false },
            title = { Text("Income / Expense Period") },
            text = {
                Column {
                    periods.forEach { period ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setIncomeExpenseWidgetPeriod(period)
                                    showIncomeExpenseDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = generalConfig.incomeExpenseWidgetPeriod == period,
                                onClick = {
                                    viewModel.setIncomeExpenseWidgetPeriod(period)
                                    showIncomeExpenseDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(period)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIncomeExpenseDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showBudgetWidgetDialog) {
        val categories = listOf("Food", "Groceries", "Transport", "Shopping", "Entertainment", "Utilities", "All Categories")
        AlertDialog(
            onDismissRequest = { showBudgetWidgetDialog = false },
            title = { Text("Select Budget Category") },
            text = {
                Column {
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setBudgetWidgetCategory(cat)
                                    showBudgetWidgetDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = generalConfig.budgetWidgetCategory == cat,
                                onClick = {
                                    viewModel.setBudgetWidgetCategory(cat)
                                    showBudgetWidgetDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBudgetWidgetDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun WidgetSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp)
        )

        Spacer(modifier = Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
