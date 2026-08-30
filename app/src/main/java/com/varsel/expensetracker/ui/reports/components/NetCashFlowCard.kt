package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun NetCashFlowCard(
    actualIncome: Double,
    effectiveExpense: Double,
    netCashFlow: Double,
    modifier: Modifier = Modifier
) {
    val currencyFormatter =
        NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )

    val netColor =
        if (netCashFlow >= 0.0) {
            AppColors.Income
        } else {
            AppColors.Expense
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Net Cash Flow",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    currencyFormatter.format(
                        abs(netCashFlow)
                    ),
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.Bold,
                color = netColor
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                CashFlowValue(
                    label = "Income",
                    value = actualIncome,
                    color = AppColors.Income,
                    modifier =
                        Modifier.weight(1f)
                )

                Spacer(
                    modifier =
                        Modifier.width(16.dp)
                )

                CashFlowValue(
                    label = "Expenses",
                    value = effectiveExpense,
                    color = AppColors.Expense,
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CashFlowValue(
    label: String,
    value: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val currencyFormatter =
        NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )

    Column(
        modifier = modifier
    ) {

        Text(
            text = label,
            style =
                MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text =
                currencyFormatter.format(
                    abs(value)
                ),
            style =
                MaterialTheme.typography.titleMedium,
            fontWeight =
                FontWeight.SemiBold,
            color = color
        )
    }
}
