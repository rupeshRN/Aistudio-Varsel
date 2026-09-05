package com.varsel.expensetracker.ui.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.budget.BudgetCalculator
import com.varsel.expensetracker.ui.budget.model.BudgetUiModel

@Composable
fun BudgetCard(
    budgetUiModel: BudgetUiModel,
    onClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(budgetUiModel.categoryColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.22f),
            accentColor.copy(alpha = 0.08f)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag("budget_card_${budgetUiModel.budget.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Section with subtle gradient/frosted tint (Cashew style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = budgetUiModel.budget.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        val leftOrOverText = if (budgetUiModel.isOverBudget) {
                            "${BudgetCalculator.formatCurrency(budgetUiModel.overBudgetAmount, round = true)} over of ${BudgetCalculator.formatCurrency(budgetUiModel.budget.amount, round = true)}"
                        } else {
                            "${BudgetCalculator.formatCurrency(budgetUiModel.amountLeft, round = true)} left of ${BudgetCalculator.formatCurrency(budgetUiModel.budget.amount, round = true)}"
                        }

                        Text(
                            text = leftOrOverText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (budgetUiModel.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // History Icon Button (Cashew style circular button)
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onHistoryClick)
                            .testTag("budget_history_button_${budgetUiModel.budget.id}"),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        tonalElevation = 1.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Budget History",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Lower Section: Progress track with Today pin and daily allowance
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                BudgetProgressTrack(
                    spentRatio = budgetUiModel.spentRatio,
                    todayRatio = budgetUiModel.todayRatio,
                    percentSpent = budgetUiModel.percentSpent,
                    periodStartLabel = budgetUiModel.periodStartFormatted,
                    periodEndLabel = budgetUiModel.periodEndFormatted,
                    dailyAllowanceText = budgetUiModel.dailyAllowanceText,
                    fillColor = accentColor,
                    isOverBudget = budgetUiModel.isOverBudget
                )
            }
        }
    }
}
