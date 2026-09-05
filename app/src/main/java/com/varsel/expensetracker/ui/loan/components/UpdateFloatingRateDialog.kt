package com.varsel.expensetracker.ui.loan.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.domain.engine.LoanAmortizationEngine
import com.varsel.expensetracker.domain.model.loan.LoanSummary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

@Composable
fun UpdateFloatingRateDialog(
    loanSummary: LoanSummary,
    amortizationEngine: LoanAmortizationEngine,
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (newAnnualRate: Double, newBenchmarkRate: Double?, newSpreadRate: Double?, recalculateEmi: Boolean) -> Unit
) {
    val loan = loanSummary.loan
    val outstanding = loanSummary.currentOutstandingBalance
    val remainingMonths = max(1, loanSummary.remainingTenureMonths)
    val currentEmi = loan.emiAmount
    val currentRate = loan.annualInterestRate

    val defaultBenchmark = loan.benchmarkRate ?: 6.50
    val defaultSpread = loan.spreadRate ?: max(0.0, currentRate - defaultBenchmark)

    var benchmarkString by remember { mutableStateOf(defaultBenchmark.toString()) }
    var spreadString by remember { mutableStateOf(String.format(Locale.US, "%.2f", defaultSpread)) }
    var totalRateString by remember { mutableStateOf(currentRate.toString()) }

    var recalculateEmi by remember { mutableStateOf(true) }

    fun updateCalculatedTotalRate() {
        val b = benchmarkString.toDoubleOrNull()
        val s = spreadString.toDoubleOrNull()
        if (b != null && s != null) {
            val total = round((b + s) * 100.0) / 100.0
            totalRateString = total.toString()
        }
    }

    val newRate = totalRateString.toDoubleOrNull() ?: currentRate

    // Dynamic calculations
    val calculatedNewEmi = if (outstanding > 0.0 && remainingMonths > 0) {
        amortizationEngine.calculateEmi(outstanding, newRate, remainingMonths)
    } else 0.0
    val emiDiff = calculatedNewEmi - currentEmi

    val calculatedNewRemainingTenure = if (outstanding > 0.0 && currentEmi > 0.0) {
        amortizationEngine.calculateTenureMonths(outstanding, newRate, currentEmi)
    } else remainingMonths
    val tenureDiff = calculatedNewRemainingTenure - remainingMonths

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.AutoGraph,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Update Floating Rate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Repo rate change revision",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Current Loan Status Snapshot Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current Rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currentRate% p.a.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Outstanding Balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormatter.format(outstanding),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Remaining Tenure",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$remainingMonths months",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current Monthly EMI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormatter.format(currentEmi),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // New Rate Parameters
                Text(
                    text = "New Benchmark & Spread",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = benchmarkString,
                        onValueChange = {
                            benchmarkString = it
                            updateCalculatedTotalRate()
                        },
                        label = { Text("Repo Rate %") },
                        placeholder = { Text("6.50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = spreadString,
                        onValueChange = {
                            spreadString = it
                            updateCalculatedTotalRate()
                        },
                        label = { Text("Bank Spread %") },
                        placeholder = { Text("2.25") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = totalRateString,
                    onValueChange = { totalRateString = it },
                    label = { Text("Effective Annual Interest Rate (% p.a.) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Outlined.Percent, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Recalculation Mode Selection
                Text(
                    text = "Recalculation Method",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Option 1: Recalculate EMI
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { recalculateEmi = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (recalculateEmi) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (recalculateEmi) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recalculate EMI (Keep Tenure)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            RadioButton(
                                selected = recalculateEmi,
                                onClick = { recalculateEmi = true }
                            )
                        }
                        Text(
                            text = "Maintains remaining tenure at $remainingMonths months. Updates your monthly EMI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "New EMI: ${currencyFormatter.format(calculatedNewEmi)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (emiDiff != 0.0) {
                                val isSavings = emiDiff < 0
                                Icon(
                                    imageVector = if (isSavings) Icons.Outlined.TrendingDown else Icons.Outlined.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isSavings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isSavings) "(-${currencyFormatter.format(abs(emiDiff))}/mo)"
                                    else "(+${currencyFormatter.format(abs(emiDiff))}/mo)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSavings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Option 2: Adjust Tenure
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { recalculateEmi = false },
                    shape = RoundedCornerShape(12.dp),
                    color = if (!recalculateEmi) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (!recalculateEmi) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                    else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Adjust Tenure (Keep EMI)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            RadioButton(
                                selected = !recalculateEmi,
                                onClick = { recalculateEmi = false }
                            )
                        }
                        Text(
                            text = "Maintains monthly EMI at ${currencyFormatter.format(currentEmi)}. Adjusts remaining tenure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "New Tenure: $calculatedNewRemainingTenure months",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (tenureDiff != 0) {
                                val isSavings = tenureDiff < 0
                                Text(
                                    text = if (isSavings) "(-${abs(tenureDiff)} months)"
                                    else "(+${abs(tenureDiff)} months)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSavings) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = totalRateString.toDoubleOrNull() ?: currentRate
                    val b = benchmarkString.toDoubleOrNull()
                    val s = spreadString.toDoubleOrNull()
                    onConfirm(rate, b, s, recalculateEmi)
                }
            ) {
                Text("Apply & Recalculate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
