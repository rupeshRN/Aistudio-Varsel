package com.varsel.expensetracker.ui.loan.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.varsel.expensetracker.domain.model.loan.AmortizationScheduleItem
import com.varsel.expensetracker.domain.model.loan.InterestRateType
import com.varsel.expensetracker.domain.model.loan.LoanPayment
import com.varsel.expensetracker.domain.model.loan.LoanPaymentType
import com.varsel.expensetracker.domain.model.loan.LoanStatus
import com.varsel.expensetracker.domain.model.loan.LoanSummary
import com.varsel.expensetracker.ui.loan.components.PrepaymentCalculatorView
import com.varsel.expensetracker.ui.loan.components.RecordPaymentDialog
import com.varsel.expensetracker.ui.loan.components.UpdateFloatingRateDialog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: Long,
    onBackClick: () -> Unit,
    onEditLoanClick: (Long) -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showFloatingRateDialog by remember { mutableStateOf(false) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(loanId) {
        viewModel.setLoanId(loanId)
    }

    val loanSummary = uiState.loanSummary

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Loan Account?") },
            text = { Text("Are you sure you want to delete this loan account and all its recorded payment history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteLoan(onDeleted = onBackClick)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRecordPaymentDialog && loanSummary != null) {
        RecordPaymentDialog(
            loanSummary = loanSummary,
            onDismiss = { showRecordPaymentDialog = false },
            onConfirm = { payment, createBankTx, accountId, accountLast4 ->
                viewModel.recordPayment(
                    payment = payment,
                    createBankTransaction = createBankTx,
                    bankAccountId = accountId,
                    bankAccountLast4 = accountLast4,
                    onSuccess = { showRecordPaymentDialog = false }
                )
            }
        )
    }

    if (showFloatingRateDialog && loanSummary != null) {
        UpdateFloatingRateDialog(
            loanSummary = loanSummary,
            amortizationEngine = viewModel.amortizationEngine,
            currencyFormatter = currencyFormatter,
            onDismiss = { showFloatingRateDialog = false },
            onConfirm = { newAnnualRate, newBenchmarkRate, newSpreadRate, recalculateEmi ->
                viewModel.updateFloatingRate(
                    newAnnualRate = newAnnualRate,
                    newBenchmarkRate = newBenchmarkRate,
                    newSpreadRate = newSpreadRate,
                    recalculateEmi = recalculateEmi,
                    onSuccess = {
                        showFloatingRateDialog = false
                    }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = loanSummary?.loan?.name ?: "Loan Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (loanSummary != null) {
                        IconButton(onClick = { onEditLoanClick(loanSummary.loan.id) }) {
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit Loan")
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete Loan", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (loanSummary != null && loanSummary.loan.status == LoanStatus.ACTIVE) {
                ExtendedFloatingActionButton(
                    onClick = { showRecordPaymentDialog = true },
                    icon = { Icon(Icons.Outlined.Payment, contentDescription = null) },
                    text = { Text("Record Payment") }
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading || loanSummary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    edgePadding = 16.dp,
                    divider = { HorizontalDivider() }
                ) {
                    LoanDetailTab.values().forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = { Text(tab.title) }
                        )
                    }
                }

                // Tab Content
                when (uiState.selectedTab) {
                    LoanDetailTab.OVERVIEW -> {
                        LoanOverviewTab(
                            loanSummary = loanSummary,
                            currencyFormatter = currencyFormatter,
                            dateFormat = dateFormat,
                            onUpdateFloatingRateClick = { showFloatingRateDialog = true }
                        )
                    }
                    LoanDetailTab.SCHEDULE -> {
                        LoanScheduleTab(
                            schedule = uiState.amortizationSchedule,
                            currencyFormatter = currencyFormatter,
                            dateFormat = dateFormat
                        )
                    }
                    LoanDetailTab.PAYMENTS -> {
                        LoanPaymentsTab(
                            payments = uiState.payments,
                            currencyFormatter = currencyFormatter,
                            dateFormat = dateFormat,
                            onDeletePayment = { viewModel.deletePayment(it) }
                        )
                    }
                    LoanDetailTab.PREPAY_CALC -> {
                        PrepaymentCalculatorView(
                            loanSummary = loanSummary,
                            simulationResult = uiState.simulationResult,
                            onSimulate = { lumpSum, monthly, type ->
                                viewModel.runPrepaymentSimulation(lumpSum, monthly, type)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanOverviewTab(
    loanSummary: LoanSummary,
    currencyFormatter: NumberFormat,
    dateFormat: SimpleDateFormat,
    onUpdateFloatingRateClick: () -> Unit
) {
    val loan = loanSummary.loan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Balance Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Outstanding Principal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currencyFormatter.format(loanSummary.currentOutstandingBalance),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (loan.status == LoanStatus.ACTIVE) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = loan.status.name,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (loan.status == LoanStatus.ACTIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Repaid: ${currencyFormatter.format(loanSummary.totalPrincipalPaid)} (${String.format(Locale.getDefault(), "%.1f", loanSummary.progressPercentage)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Total: ${currencyFormatter.format(loan.principal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (loanSummary.progressPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Monthly EMI and Balance Tenure Info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Monthly EMI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormatter.format(loanSummary.nextEmiAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Balance Tenure",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${loanSummary.remainingTenureMonths} mo left",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (loanSummary.nextEmiDueDateTimestamp != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Next Due Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormat.format(Date(loanSummary.nextEmiDueDateTimestamp)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Interest Rate Revision Card (if Floating)
        if (loan.interestType == InterestRateType.FLOATING) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoGraph,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Repo-Linked Floating Loan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${loan.annualInterestRate}% p.a.",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    val rateInfo = if (loan.benchmarkRate != null && loan.spreadRate != null) {
                        "Repo Rate: ${loan.benchmarkRate}% • Spread: ${loan.spreadRate}%"
                    } else {
                        "Tied to central bank / RBI repo rate revisions"
                    }

                    Text(
                        text = "$rateInfo. Has RBI revised the repo rate? Update your rate to automatically recalculate future EMI & amortization schedule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                    )

                    Button(
                        onClick = onUpdateFloatingRateClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(imageVector = Icons.Outlined.ChangeCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Floating Rate / Repo Rate Change")
                    }
                }
            }
        }

        // Details Grid
        Text(
            text = "Loan Specifications",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailMetricCard(
                title = "Balance Tenure",
                value = "${loanSummary.remainingTenureMonths} of ${loan.totalTenureMonths} M",
                icon = Icons.Outlined.HourglassTop,
                modifier = Modifier.weight(1f)
            )
            DetailMetricCard(
                title = "Interest Rate",
                value = "${loan.annualInterestRate}% (${loan.interestType.shortName})",
                icon = Icons.Outlined.Percent,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailMetricCard(
                title = "Interest Paid",
                value = currencyFormatter.format(loanSummary.totalInterestPaid),
                icon = Icons.Outlined.Paid,
                modifier = Modifier.weight(1f)
            )
            DetailMetricCard(
                title = "Est. Total Interest",
                value = currencyFormatter.format(loanSummary.totalProjectedInterest),
                icon = Icons.Outlined.Savings,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailMetricCard(
                title = "Loan Type",
                value = loan.loanType.displayName,
                icon = Icons.Outlined.Category,
                modifier = Modifier.weight(1f)
            )
            DetailMetricCard(
                title = "Start Date",
                value = dateFormat.format(Date(loan.startDateTimestamp)),
                icon = Icons.Outlined.Event,
                modifier = Modifier.weight(1f)
            )
        }

        if (!loan.lenderName.isNullOrBlank() || !loan.loanAccountNumber.isNullOrBlank() || loan.bankAccountLast4 != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Account & Lender Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!loan.lenderName.isNullOrBlank()) {
                        InfoRow(label = "Lender / Bank", value = loan.lenderName)
                    }
                    if (!loan.loanAccountNumber.isNullOrBlank()) {
                        InfoRow(label = "Account / Reference #", value = loan.loanAccountNumber)
                    }
                    if (loan.bankAccountLast4 != null) {
                        InfoRow(label = "Auto-Debit Bank Account", value = "•••• ${loan.bankAccountLast4}")
                    }
                    if (!loan.collateralOrNotes.isNullOrBlank()) {
                        InfoRow(label = "Collateral / Notes", value = loan.collateralOrNotes)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun DetailMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoanScheduleTab(
    schedule: List<AmortizationScheduleItem>,
    currencyFormatter: NumberFormat,
    dateFormat: SimpleDateFormat
) {
    if (schedule.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No schedule available")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Month / Date", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Principal + Int = EMI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Balance", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(schedule) { item ->
                ScheduleRowItem(item = item, currencyFormatter = currencyFormatter, dateFormat = dateFormat)
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun ScheduleRowItem(
    item: AmortizationScheduleItem,
    currencyFormatter: NumberFormat,
    dateFormat: SimpleDateFormat
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPaid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isPaid) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Paid",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Column {
                    Text(
                        text = "M${item.monthIndex}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(Date(item.dueDateTimestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currencyFormatter.format(item.emiAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "P: ${currencyFormatter.format(item.principalComponent)} | I: ${currencyFormatter.format(item.interestComponent)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Bal: ${currencyFormatter.format(item.closingBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LoanPaymentsTab(
    payments: List<LoanPayment>,
    currencyFormatter: NumberFormat,
    dateFormat: SimpleDateFormat,
    onDeletePayment: (Long) -> Unit
) {
    if (payments.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No payments recorded yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tap 'Record Payment' below to log an EMI or lump-sum payment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(payments) { payment ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (payment.paymentType == LoanPaymentType.PRE_PAYMENT) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = payment.paymentType.displayName,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dateFormat.format(Date(payment.paymentDateTimestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Principal: ${currencyFormatter.format(payment.principalComponent)} • Interest: ${currencyFormatter.format(payment.interestComponent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!payment.notes.isNullOrBlank()) {
                                Text(
                                    text = payment.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = currencyFormatter.format(payment.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { onDeletePayment(payment.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete payment",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}
