package com.varsel.expensetracker.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MoreScreen(
    onLoansClick: () -> Unit,
    onImportClick: () -> Unit,
    onBudgetsClick: () -> Unit = {}
) {
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

        // Budgets Section
        ToolHubCard(
            title = "Budgets & Spending Limits",
            subtitle = "Set daily & monthly spending caps, track progress with Today indicators and historical trends",
            icon = Icons.Outlined.PieChart,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            iconTint = MaterialTheme.colorScheme.tertiary,
            onClick = onBudgetsClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Financial Hub Section
        ToolHubCard(
            title = "Loans & Liabilities",
            subtitle = "Track Home, Car, Personal & Gold loans, EMI schedules and prepayment savings",
            icon = Icons.Outlined.AccountBalance,
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = onLoansClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        ToolHubCard(
            title = "Import Statement",
            subtitle = "Import bank statement PDFs to auto-categorize and sync offline accounts",
            icon = Icons.Outlined.UploadFile,
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            iconTint = MaterialTheme.colorScheme.secondary,
            onClick = onImportClick
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ToolHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
