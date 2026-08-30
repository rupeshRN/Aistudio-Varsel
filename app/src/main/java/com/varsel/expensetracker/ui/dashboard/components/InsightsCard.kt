package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.model.FinancialInsight
import com.varsel.expensetracker.ui.model.InsightType

@Composable
fun InsightsCard(
    insights: List<FinancialInsight>,
    modifier: Modifier = Modifier,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {}
) {
    if (insights.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
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
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = "Actionable Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "${insights.size} updates",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Actionable Insight Cards
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            insights.forEach { insight ->
                ActionableInsightTile(
                    insight = insight,
                    onClick = {
                        when (insight.type) {
                            InsightType.POSITIVE -> onNavigateToAnalytics()
                            InsightType.ATTENTION -> onNavigateToTransactions()
                            InsightType.NEUTRAL -> onNavigateToAnalytics()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionableInsightTile(
    insight: FinancialInsight,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val (accentColor, bgColor, borderColor, actionTag) = when (insight.type) {
        InsightType.POSITIVE -> {
            val primary = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
            val bg = if (isDark) Color(0xFF1B5E20).copy(alpha = 0.18f) else Color(0xFFE8F5E9).copy(alpha = 0.65f)
            val border = if (isDark) Color(0xFF66BB6A).copy(alpha = 0.25f) else Color(0xFF2E7D32).copy(alpha = 0.20f)
            Quad(primary, bg, border, "Good Momentum")
        }
        InsightType.ATTENTION -> {
            val primary = if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
            val bg = if (isDark) Color(0xFFB71C1C).copy(alpha = 0.16f) else Color(0xFFFFEBEE).copy(alpha = 0.65f)
            val border = if (isDark) Color(0xFFEF5350).copy(alpha = 0.25f) else Color(0xFFC62828).copy(alpha = 0.20f)
            Quad(primary, bg, border, "Needs Attention")
        }
        InsightType.NEUTRAL -> {
            val primary = MaterialTheme.colorScheme.primary
            val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            val border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
            Quad(primary, bg, border, "Category Analysis")
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Distinct rounded Emoji Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = insight.emoji,
                        fontSize = 18.sp
                    )
                }
            }

            // Insight Narrative Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = actionTag,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            // Action Affordance Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "View details",
                tint = accentColor.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
