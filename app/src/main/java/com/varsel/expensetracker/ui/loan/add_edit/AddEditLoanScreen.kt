package com.varsel.expensetracker.ui.loan.add_edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.varsel.expensetracker.domain.model.loan.InterestRateType
import com.varsel.expensetracker.domain.model.loan.LoanType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanScreen(
    loanId: Long = 0L,
    onBackClick: () -> Unit,
    onLoanSaved: (Long) -> Unit,
    viewModel: AddEditLoanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.startDateTimestamp
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onStartDateChange(it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) "Edit Loan" else "Add New Loan",
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
                    TextButton(
                        onClick = { viewModel.saveLoan(onLoanSaved) },
                        enabled = !uiState.isSaving
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Loan Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text("Loan Name *") },
                placeholder = { Text("e.g. HDFC Home Loan, SBI Car Loan") },
                leadingIcon = { Icon(Icons.Outlined.AccountBalance, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Loan Type Selector
            ExposedDropdownMenuBox(
                expanded = showTypeDropdown,
                onExpandedChange = { showTypeDropdown = !showTypeDropdown }
            ) {
                OutlinedTextField(
                    value = uiState.loanType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Loan Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showTypeDropdown,
                    onDismissRequest = { showTypeDropdown = false }
                ) {
                    LoanType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                viewModel.onLoanTypeChange(type)
                                showTypeDropdown = false
                            }
                        )
                    }
                }
            }

            // Principal Amount
            OutlinedTextField(
                value = uiState.principalString,
                onValueChange = { viewModel.onPrincipalChange(it) },
                label = { Text("Principal Borrowed (₹) *") },
                placeholder = { Text("e.g. 2500000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Outlined.CurrencyRupee, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Interest Rate Type Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Interest Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InterestRateType.entries.forEach { type ->
                        val isSelected = uiState.interestType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onInterestTypeChange(type) },
                            label = {
                                Text(
                                    text = type.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Floating Rate parameters breakdown if selected
            if (uiState.interestType == InterestRateType.FLOATING) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Repo-Linked Floating Parameters",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.benchmarkRateString,
                                onValueChange = { viewModel.onBenchmarkRateChange(it) },
                                label = { Text("Repo Rate %") },
                                placeholder = { Text("e.g. 6.50") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.spreadRateString,
                                onValueChange = { viewModel.onSpreadRateChange(it) },
                                label = { Text("Bank Spread %") },
                                placeholder = { Text("e.g. 2.25") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Future repo rate changes can be updated anytime from the loan screen to automatically recalculate your EMI and schedule.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Interest Rate and Tenure Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.interestRateString,
                    onValueChange = { viewModel.onInterestRateChange(it) },
                    label = { Text(if (uiState.interestType == InterestRateType.FLOATING) "Total Rate (% p.a.) *" else "Interest Rate (% p.a.) *") },
                    placeholder = { Text("e.g. 8.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Outlined.Percent, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = uiState.tenureMonthsString,
                    onValueChange = { viewModel.onTenureMonthsChange(it) },
                    label = { Text("Tenure (Months) *") },
                    placeholder = { Text("e.g. 240") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // EMI Amount & Auto-calc switch
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-Calculate EMI",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = uiState.isAutoEmi,
                            onCheckedChange = { viewModel.onToggleAutoEmi(it) }
                        )
                    }

                    OutlinedTextField(
                        value = uiState.emiAmountString,
                        onValueChange = { viewModel.onEmiAmountChange(it) },
                        label = { Text("Monthly EMI (₹)") },
                        enabled = !uiState.isAutoEmi,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Outlined.Payment, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Start Date Picker
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Loan Start Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(Date(uiState.startDateTimestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Select Start Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Linked Bank Account
            if (uiState.bankAccounts.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = showAccountDropdown,
                    onExpandedChange = { showAccountDropdown = !showAccountDropdown }
                ) {
                    val currentSelection = uiState.bankAccounts.find { it.accountId == uiState.selectedBankAccountId }
                    OutlinedTextField(
                        value = currentSelection?.bankName ?: "None / Unlinked",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Linked Bank Account (Auto-Debit)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showAccountDropdown,
                        onDismissRequest = { showAccountDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None / Unlinked") },
                            onClick = {
                                viewModel.onBankAccountSelected(null)
                                showAccountDropdown = false
                            }
                        )
                        uiState.bankAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.bankName) },
                                onClick = {
                                    viewModel.onBankAccountSelected(acc)
                                    showAccountDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Optional Lender & Loan Account Number
            OutlinedTextField(
                value = uiState.lenderName,
                onValueChange = { viewModel.onLenderNameChange(it) },
                label = { Text("Lender / Financial Institution (Optional)") },
                placeholder = { Text("e.g. HDFC Bank, ICICI Bank") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.loanAccountNumber,
                onValueChange = { viewModel.onLoanAccountNumberChange(it) },
                label = { Text("Loan Account Number / Ref # (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Collateral / Notes
            OutlinedTextField(
                value = uiState.collateralOrNotes,
                onValueChange = { viewModel.onCollateralOrNotesChange(it) },
                label = { Text("Collateral / Notes (Optional)") },
                placeholder = { Text("e.g. Property papers at branch, gold weight 45g") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.saveLoan(onLoanSaved) },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (uiState.isEditing) "Update Loan" else "Create Loan Account",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
