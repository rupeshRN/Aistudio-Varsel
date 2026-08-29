package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.design.AppColors
import com.varsel.expensetracker.ui.reports.ReportsFlow

@Composable
fun MoneyFlowCard(
    selectedFlow: ReportsFlow,
    onFlowSelected: (ReportsFlow) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier =
            modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(20.dp),
        color =
            MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier =
                Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedFlow == ReportsFlow.EXPENSES) "Expense Breakdown" else "Income Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            content()
        }
    }
}

@Composable
private fun FlowSelector(
    selectedFlow: ReportsFlow,
    onFlowSelected: (ReportsFlow) -> Unit
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        FlowTab(
            label = "Expenses",
            selected =
                selectedFlow ==
                    ReportsFlow.EXPENSES,
            selectedColor =
                AppColors.Expense,
            onClick = {
                onFlowSelected(
                    ReportsFlow.EXPENSES
                )
            },
            modifier =
                Modifier.weight(1f)
        )

        Spacer(
            modifier =
                Modifier.width(8.dp)
        )

        FlowTab(
            label = "Income",
            selected =
                selectedFlow ==
                    ReportsFlow.INCOME,
            selectedColor =
                AppColors.Income,
            onClick = {
                onFlowSelected(
                    ReportsFlow.INCOME
                )
            },
            modifier =
                Modifier.weight(1f)
        )
    }
}

@Composable
private fun FlowTab(
    label: String,
    selected: Boolean,
    selectedColor:
        androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape =
            RoundedCornerShape(14.dp),
        color =
            if (selected) {
                selectedColor.copy(
                    alpha = 0.14f
                )
            } else {
                MaterialTheme.colorScheme
                    .surfaceVariant
            },
        onClick = onClick
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 12.dp
                    ),
            horizontalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = label,
                style =
                    MaterialTheme.typography
                        .labelLarge,
                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                color =
                    if (selected) {
                        selectedColor
                    } else {
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                    }
            )
        }
    }
}
