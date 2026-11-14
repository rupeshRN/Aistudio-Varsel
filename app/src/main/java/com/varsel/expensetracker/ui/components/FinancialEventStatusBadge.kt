package com.varsel.expensetracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.domain.model.FinancialEventStatus
import com.varsel.expensetracker.domain.model.FinancialEventStatusSummary
import com.varsel.expensetracker.ui.design.AppColors
import com.varsel.expensetracker.ui.theme.isDark

data class FinancialEventStatusPalette(
    val contentColor: Color,
    val containerColor: Color,
    val borderColor: Color,
    val icon: ImageVector
)

@Composable
fun rememberFinancialEventStatusPalette(
    status: FinancialEventStatus
): FinancialEventStatusPalette {
    val isDark = MaterialTheme.colorScheme.isDark
    return when (status) {
        FinancialEventStatus.PENDING -> {
            val accent = if (isDark) Color(0xFFFFB74D) else Color(0xFFD97706)
            FinancialEventStatusPalette(
                contentColor = accent,
                containerColor = accent.copy(alpha = if (isDark) 0.16f else 0.12f),
                borderColor = accent.copy(alpha = 0.35f),
                icon = Icons.Default.Schedule
            )
        }
        FinancialEventStatus.PARTIAL -> {
            val accent = if (isDark) Color(0xFF60A5FA) else AppColors.Savings
            FinancialEventStatusPalette(
                contentColor = accent,
                containerColor = accent.copy(alpha = if (isDark) 0.18f else 0.11f),
                borderColor = accent.copy(alpha = 0.35f),
                icon = Icons.Default.DonutLarge
            )
        }
        FinancialEventStatus.SETTLED -> {
            val accent = if (isDark) Color(0xFF4ADE80) else AppColors.Income
            FinancialEventStatusPalette(
                contentColor = accent,
                containerColor = accent.copy(alpha = if (isDark) 0.16f else 0.12f),
                borderColor = accent.copy(alpha = 0.35f),
                icon = Icons.Default.CheckCircle
            )
        }
        FinancialEventStatus.OVERPAID -> {
            val accent = if (isDark) Color(0xFF2DD4BF) else Color(0xFF00897B)
            FinancialEventStatusPalette(
                contentColor = accent,
                containerColor = accent.copy(alpha = if (isDark) 0.16f else 0.12f),
                borderColor = accent.copy(alpha = 0.35f),
                icon = Icons.Default.AddCircle
            )
        }
        FinancialEventStatus.ADVANCE_RECEIVED -> {
            val accent = if (isDark) Color(0xFF38BDF8) else Color(0xFF0288D1)
            FinancialEventStatusPalette(
                contentColor = accent,
                containerColor = accent.copy(alpha = if (isDark) 0.16f else 0.12f),
                borderColor = accent.copy(alpha = 0.35f),
                icon = Icons.Default.AccountBalanceWallet
            )
        }
        FinancialEventStatus.NO_ACTIVITY -> {
            val accent = MaterialTheme.colorScheme.onSurfaceVariant
            FinancialEventStatusPalette(
                contentColor = accent,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                icon = Icons.Default.EventNote
            )
        }
    }
}

@Composable
fun FinancialEventStatusBadge(
    summary: FinancialEventStatusSummary,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val palette = rememberFinancialEventStatusPalette(summary.status)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = palette.containerColor,
        border = BorderStroke(1.dp, palette.borderColor)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 7.dp else 9.dp,
                vertical = if (compact) 3.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = palette.icon,
                contentDescription = summary.badgeLabel,
                tint = palette.contentColor,
                modifier = Modifier.size(if (compact) 12.dp else 14.dp)
            )
            Text(
                text = summary.badgeLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = palette.contentColor,
                maxLines = 1
            )
        }
    }
}
