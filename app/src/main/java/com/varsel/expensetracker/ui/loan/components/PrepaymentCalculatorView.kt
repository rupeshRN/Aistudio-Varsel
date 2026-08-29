package com.varsel.expensetracker.ui.loan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.domain.model.loan.LoanSummary
import com.varsel.expensetracker.domain.model.loan.PrepaymentReductionType
import com.varsel.expensetracker.domain.model.loan.PrepaymentSimulationResult
import java.text.NumberFormat
import java.util.*

@Composable
fun PrepaymentCalculatorView(
    loanSummary: LoanSummary,
    simulationResult: PrepaymentSimulationResult?,
    onSimulate: (extraLumpSum: Double, extraMonthly: Double, reductionType: PrepaymentReductionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var extraLumpSumString by remember { mutableStateOf("") }
    var extraMonthlyString by remember { mutableStateOf("") }
    var reductionType by remember { mutableStateOf(PrepaymentReductionType.REDUCE_TENURE) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    LaunchedEffect(extraLumpSumString, extraMonthlyString, reductionType) {
        val lumpSum = extraLumpSumString.toDoubleOrNull() ?: 0.0
        val monthly = extraMonthlyString.toDoubleOrNull() ?: 0.0
        onSimulate(lumpSum, monthly, reductionType)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header explanation card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Prepayment Savings Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "See how making extra lump-sum or monthly payments dramatically cuts your interest and tenure.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Inputs Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Prepayment Options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = extraLumpSumString,
                    onValueChange = { extraLumpSumString = it },
                    label = { Text("One-Time Lump Sum Prepayment (₹)") },
                    placeholder = { Text("e.g. 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Outlined.CurrencyRupee, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick preset buttons for lump sum
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(25000L, 50000L, 100000L).forEach { preset ->
                        OutlinedButton(
                            onClick = { extraLumpSumString = preset.toString() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("₹${preset / 1000}k", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = extraMonthlyString,
                    onValueChange = { extraMonthlyString = it },
                    label = { Text("Extra Monthly EMI Contribution (₹/mo)") },
                    placeholder = { Text("e.g. 2000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Outlined.CurrencyRupee, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Strategy selector
                Text(
                    text = "Prepayment Strategy",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = reductionType == PrepaymentReductionType.REDUCE_TENURE,
                        onClick = { reductionType = PrepaymentReductionType.REDUCE_TENURE },
                        label = { Text("Reduce Tenure (Recommended)", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = reductionType == PrepaymentReductionType.REDUCE_EMI,
                        onClick = { reductionType = PrepaymentReductionType.REDUCE_EMI },
                        label = { Text("Reduce EMI", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Live Simulation Results
        if (simulationResult != null && (simulationResult.extraLumpSum > 0 || simulationResult.extraMonthly > 0)) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Simulation Impact",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Interest Saved",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = currencyFormatter.format(simulationResult.interestSaved),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (reductionType == PrepaymentReductionType.REDUCE_TENURE) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Tenure Saved",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${simulationResult.monthsSaved} Months",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "New Reduced EMI",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = currencyFormatter.format(simulationResult.newEmiAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "New Remaining Tenure",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${simulationResult.newTenureMonths} months (was ${simulationResult.originalTenureMonths})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
