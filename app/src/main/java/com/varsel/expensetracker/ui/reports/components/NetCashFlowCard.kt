package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    val isDark = isSystemInDarkTheme()

    // High Contrast Semantic Green & Red Palettes matching Dashboard & Transactions
    val incomeColor = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val expenseColor = if (isDark) Color(0xFFFF5252) else Color(0xFFC62828)

    val incomePillBg = if (isDark) {
        Color(0xFF1B5E20).copy(alpha = 0.25f)
    } else {
        Color(0xFFE8F5E9)
    }

    val expensePillBg = if (isDark) {
        Color(0xFFB71C1C).copy(alpha = 0.25f)
    } else {
        Color(0xFFFFEBEE)
    }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val isPositive = netCashFlow >= 0.0
    val netColor = if (isPositive) incomeColor else expenseColor
    val netPillBg = if (isPositive) incomePillBg else expensePillBg

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net Cash Flow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = netPillBg,
                    border = BorderStroke(1.dp, netColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = netColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isPositive) "Surplus" else "Deficit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = netColor
                        )
                    }
                }
            }

            Text(
                text = (if (isPositive) "+ " else "- ") + currencyFormatter.format(abs(netCashFlow)),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = netColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CashFlowValue(
                    label = "Income",
                    value = actualIncome,
                    color = incomeColor,
                    backgroundColor = incomePillBg,
                    icon = Icons.Outlined.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )

                CashFlowValue(
                    label = "Expenses",
                    value = effectiveExpense,
                    color = expenseColor,
                    backgroundColor = expensePillBg,
                    icon = Icons.Outlined.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CashFlowValue(
    label: String,
    value: Double,
    color: Color,
    backgroundColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = currencyFormatter.format(abs(value)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}
