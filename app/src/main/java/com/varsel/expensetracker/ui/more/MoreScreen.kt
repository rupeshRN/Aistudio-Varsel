package com.varsel.expensetracker.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.ui.settings.general.GeneralSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onLoansClick: () -> Unit,
    onImportClick: () -> Unit,
    onBudgetsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onTransactionsClick: () -> Unit = {},
    onHeatmapClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onGeneralSettingsClick: () -> Unit = {},
    viewModel: GeneralSettingsViewModel = hiltViewModel()
) {
    val generalConfig by viewModel.generalConfig.collectAsStateWithLifecycle()
    val pinnedTabs = generalConfig.navigationTabs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Financial Tools",
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Budgets, loans, statements & account tools",
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic Unpinned Primary Sections (Tools that are NOT currently in the bottom bar)
        val notPinned = remember(pinnedTabs) {
            val list = mutableListOf<Triple<String, String, ImageVector>>()
            if (!pinnedTabs.contains("budgets")) {
                list.add(Triple("budgets", "Budgets & Spending Limits", Icons.Outlined.PieChart))
            }
            if (!pinnedTabs.contains("loans")) {
                list.add(Triple("loans", "Loans & Liabilities", Icons.Outlined.AccountBalance))
            }
            if (!pinnedTabs.contains("reports")) {
                list.add(Triple("reports", "Reports & Analytics", Icons.Outlined.Assessment))
            }
            if (!pinnedTabs.contains("transactions")) {
                list.add(Triple("transactions", "Transactions Ledger", Icons.Outlined.ListAlt))
            }
            if (!pinnedTabs.contains("calendar_heatmap")) {
                list.add(Triple("calendar_heatmap", "Calendar Heatmap", Icons.Outlined.CalendarMonth))
            }
            list
        }

        if (notPinned.isNotEmpty()) {
            Text(
                text = "Unpinned Features",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            notPinned.forEach { (route, title, icon) ->
                ToolHubCard(
                    title = title,
                    subtitle = when (route) {
                        "budgets" -> "Set daily & monthly spending caps, track progress with Today indicators"
                        "loans" -> "Track loans, liabilities, EMI schedules and prepayment savings"
                        "reports" -> "Cash flow analytics, category distribution charts and trend graphs"
                        "transactions" -> "Complete log of all income, expense and account transfers"
                        "calendar_heatmap" -> "Daily spending intensity, calendar patterns & zero-spend streaks"
                        else -> "Tap to open"
                    },
                    icon = icon,
                    isPinned = false,
                    containerColor = when (route) {
                        "budgets" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        "loans" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        "reports" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        "calendar_heatmap" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    iconTint = when (route) {
                        "budgets" -> MaterialTheme.colorScheme.tertiary
                        "loans" -> MaterialTheme.colorScheme.primary
                        "reports" -> MaterialTheme.colorScheme.secondary
                        "calendar_heatmap" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    onClick = {
                        when (route) {
                            "budgets" -> onBudgetsClick()
                            "loans" -> onLoansClick()
                            "reports" -> onReportsClick()
                            "transactions" -> onTransactionsClick()
                            "calendar_heatmap" -> onHeatmapClick()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Statement Import Card
        ToolHubCard(
            title = "Import Statement",
            subtitle = "Import bank statement PDFs to auto-categorize and sync offline accounts",
            icon = Icons.Outlined.UploadFile,
            isPinned = false,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconTint = MaterialTheme.colorScheme.secondary,
            onClick = onImportClick
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ToolHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isPinned: Boolean = false,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("tool_card_${title.lowercase().replace(" ", "_")}"),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isPinned) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Pinned",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
