package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.theme.isDark

@Composable
fun QuickActionBar(
    onImportClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onTransferClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FintechDockButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Add,
            label = "Add Entry",
            onClick = onAddTransactionClick
        )

        FintechDockButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.SwapHoriz,
            label = "Transfer",
            onClick = onTransferClick
        )

        FintechDockButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.FileUpload,
            label = "Import",
            onClick = onImportClick
        )

        FintechDockButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.BarChart,
            label = "Reports",
            onClick = onAnalyticsClick
        )
    }
}

@Composable
private fun FintechDockButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val containerColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.65f) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
    val iconTint = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDark) 0.dp else 1.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = if (isDark) 0.14f else 0.09f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = labelColor,
                maxLines = 1
            )
        }
    }
}

