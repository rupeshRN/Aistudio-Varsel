package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
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
import com.varsel.expensetracker.ui.theme.isDark

@Composable
fun InsightsCard(
    insights: List<FinancialInsight>,
    modifier: Modifier = Modifier,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {}
) {
    if (insights.isEmpty()) return

    val isDark = MaterialTheme.colorScheme.isDark
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { insights.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header with Insight Counter & Smart Badge
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (insights.size > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(insights.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(if (isSelected) 16.dp else 5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                                )
                        )
                    }
                }
            }
        }

        // Swipeable Actionable Bento Card Deck
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 10.dp
        ) { page ->
            val insight = insights[page]
            ActionableInsightTile(
                insight = insight,
                onActionClick = {
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

@Composable
private fun ActionableInsightTile(
    insight: FinancialInsight,
    onActionClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val (accentColor, bgColor, borderColor, actionTag) = when (insight.type) {
        InsightType.POSITIVE -> {
            val primary = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
            val bg = if (isDark) Color(0xFF0F291E).copy(alpha = 0.75f) else Color(0xFFF0FDF4)
            val border = if (isDark) Color(0xFF166534).copy(alpha = 0.60f) else Color(0xFFBBF7D0)
            Quad(primary, bg, border, "Momentum")
        }
        InsightType.ATTENTION -> {
            val primary = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
            val bg = if (isDark) Color(0xFF2C1518).copy(alpha = 0.75f) else Color(0xFFFEF2F2)
            val border = if (isDark) Color(0xFF991B1B).copy(alpha = 0.60f) else Color(0xFFFECACA)
            Quad(primary, bg, border, "Action Needed")
        }
        InsightType.NEUTRAL -> {
            val primary = MaterialTheme.colorScheme.primary
            val bg = if (isDark) Color(0xFF0F172A).copy(alpha = 0.75f) else Color(0xFFF8FAFC)
            val border = if (isDark) Color(0xFF334155).copy(alpha = 0.60f) else Color(0xFFE2E8F0)
            Quad(primary, bg, border, "Overview")
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onActionClick),
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDark) 0.dp else 1.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Emoji + Title + Metric Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.14f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = insight.emoji,
                                fontSize = 17.sp
                            )
                        }
                    }

                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Highlight Metric Tag or Status Tag
                val tagText = insight.metricHighlight ?: actionTag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = tagText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            // Middle Description
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                maxLines = 2
            )

            // Bottom 1-Tap Action Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val buttonText = insight.actionLabel ?: "View Details"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onActionClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = accentColor
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
