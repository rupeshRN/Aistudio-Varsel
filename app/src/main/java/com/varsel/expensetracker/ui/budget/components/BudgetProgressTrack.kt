package com.varsel.expensetracker.ui.budget.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BudgetProgressTrack(
    spentRatio: Float,
    todayRatio: Float,
    percentSpent: Int,
    periodStartLabel: String,
    periodEndLabel: String,
    dailyAllowanceText: String,
    modifier: Modifier = Modifier,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    isOverBudget: Boolean = false
) {
    val animatedSpent by animateFloatAsState(
        targetValue = spentRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "spentRatio"
    )

    val actualFillColor = if (isOverBudget) {
        MaterialTheme.colorScheme.error
    } else if (spentRatio > todayRatio && spentRatio > 0.8f) {
        Color(0xFFFF9800) // Warning Amber
    } else {
        fillColor
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Date range labels above track
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = periodStartLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = periodEndLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress track with floating "Today" indicator
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp) // Leave room for floating "Today" pin
        ) {
            val trackWidth = maxWidth
            val todayOffset = (trackWidth * todayRatio.coerceIn(0.04f, 0.96f)) - 18.dp

            // Track Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(trackColor)
                    .testTag("budget_progress_bar")
            ) {
                // Filled progress
                if (animatedSpent > 0.005f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedSpent)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(13.dp))
                            .background(actualFillColor),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (animatedSpent > 0.12f) {
                            Text(
                                text = "$percentSpent%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                // If spent is small, show percentage outside or inside
                if (animatedSpent <= 0.12f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "$percentSpent%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Floating "Today" Pin/Marker above track (Cashew style)
            Column(
                modifier = Modifier
                    .offset(x = todayOffset, y = (-18).dp)
                    .width(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                // Small downward pointing indicator tick
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(5.dp)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Daily allowance subtitle
        Text(
            text = dailyAllowanceText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
