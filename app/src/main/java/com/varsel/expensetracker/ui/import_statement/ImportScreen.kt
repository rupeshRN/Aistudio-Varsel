package com.varsel.expensetracker.ui.import_statement

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.varsel.expensetracker.ui.import_statement.components.DeveloperDiagnosticsCard
import com.varsel.expensetracker.ui.import_statement.components.StatementSummaryCard
import com.varsel.expensetracker.ui.import_statement.components.TransactionReviewRow
import com.varsel.expensetracker.developer.ParserDiagnostics
import androidx.compose.foundation.layout.statusBarsPadding

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBackClick: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsState()

    val diagnostics by
        viewModel.diagnostics.collectAsState()

    val parserDiagnosticsEnabled by
        viewModel.parserDiagnosticsEnabled.collectAsState()

    var showTransactionReview by
        remember {
            mutableStateOf(false)
        }

    val launcher =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts.OpenDocument()

        ) { uri: Uri? ->

            uri?.let {

                showTransactionReview = false

                viewModel.processSelectedFile(
                    it,
                    null
                )
            }
        }

Scaffold(
    topBar = {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBackClick
            ) {
                Text("Back")
            }

            Spacer(
                modifier = Modifier.width(4.dp)
            )

            Text(
                text = "Import Statement",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
) { paddingValues ->

        Box(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)

        ) {

            when (val state = uiState) {

                //--------------------------------------------------
                // IDLE
                //--------------------------------------------------

                is ImportUiState.Idle -> {

                    Column(

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),

                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.Center

                    ) {

                        Button(

                            onClick = {

                                launcher.launch(

                                    arrayOf(
                                        "application/pdf",
                                        "image/*"
                                    )

                                )

                            }

                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Upload,

                                contentDescription =
                                    "Upload Statement"

                            )

                            Spacer(
                                Modifier.width(8.dp)
                            )

                            Text(
                                "Select Statement"
                            )
                        }
                    }
                }

                //--------------------------------------------------
                // LOADING / PROCESSING
                //--------------------------------------------------

                is ImportUiState.Loading,
                is ImportUiState.Processing -> {

                    Box(

                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        CircularProgressIndicator()

                    }
                }

                //--------------------------------------------------
                // PARSED
                //--------------------------------------------------

                is ImportUiState.ParsedTransactions -> {

                    if (!showTransactionReview) {

                 Column(
                    
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                )
                    
                    ) {

                            StatementSummaryCard(

                                summary =
                                    state.summary,

                                onContinue = {

                                    showTransactionReview =
                                        true

                                }

                            )
                        }

                    } else {

                        TransactionReviewContent(

                            state = state,

                            parserDiagnosticsEnabled =
                                parserDiagnosticsEnabled,

                            diagnostics =
                                diagnostics,

                            viewModel =
                                viewModel
                        )
                    }
                }

                //--------------------------------------------------
                // SAVED
                //--------------------------------------------------

                is ImportUiState.Saved -> {

                    Column(

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),

                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.Center

                    ) {

                        Text(

                            text =
                                "Successfully saved ${state.count} transactions!",

                            style =
                                MaterialTheme.typography.titleMedium

                        )

                        Spacer(
                            Modifier.height(16.dp)
                        )

                        Button(

                            onClick = {

                                showTransactionReview =
                                    false

                                viewModel.resetState()

                            }

                        ) {

                            Text(
                                "Import Another Statement"
                            )

                        }
                    }
                }

                //--------------------------------------------------
                // ERROR
                //--------------------------------------------------

                is ImportUiState.Error -> {

                    Column(

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),

                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.Center

                    ) {

                        Text(

                            text =
                                state.message,

                            color =
                                MaterialTheme.colorScheme.error,

                            style =
                                MaterialTheme.typography.bodyLarge

                        )

                        Spacer(
                            Modifier.height(16.dp)
                        )

                        Button(

                            onClick = {

                                showTransactionReview =
                                    false

                                viewModel.resetState()

                            }

                        ) {

                            Text("Try Again")

                        }
                    }
                }

                //--------------------------------------------------
                // PASSWORD
                //--------------------------------------------------

                is ImportUiState.PasswordRequired -> {

                    Box(

                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Text(
                            "Password protected documents are not supported."
                        )

                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionReviewContent(

    state: ImportUiState.ParsedTransactions,

    parserDiagnosticsEnabled: Boolean,

    diagnostics: ParserDiagnostics,

    viewModel: ImportViewModel

) {

    val totalCount =
        state.parsedTransactions.size

    val selectedCount =
        state.parsedTransactions.count {
            it.selected
        }

    val allSelected =
        totalCount > 0 &&
        selectedCount == totalCount

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        //--------------------------------------------------
        // Select / Deselect All
        //--------------------------------------------------

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 4.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Checkbox(
    checked = allSelected,
    onCheckedChange = { checked ->

        state.parsedTransactions.forEach { selectable ->

            selectable.selected = checked

        }
    }
)

            Spacer(
                Modifier.width(8.dp)
            )

            Text(

                text =
                    if (allSelected)
                        "Deselect All"
                    else
                        "Select All",

                style =
                    MaterialTheme.typography.labelLarge
            )

        }

        HorizontalDivider()

        //--------------------------------------------------
        // Transaction list
        //--------------------------------------------------

        LazyColumn(

            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()

        ) {

            items(

                items =
                    state.parsedTransactions

            ) { selectable ->

                TransactionReviewRow(

                    selectable =
                        selectable,

                    onCheckedChange = {
                        selectable.selected = it
                    }

                )

                HorizontalDivider(
                    thickness = 0.5.dp
                )
            }
        }

        //--------------------------------------------------
        // Developer diagnostics
        //--------------------------------------------------

        if (parserDiagnosticsEnabled) {

            DeveloperDiagnosticsCard(

                enabled =
                    parserDiagnosticsEnabled,

                diagnostics =
                    diagnostics

            )
        }

        //--------------------------------------------------
        // Bottom action area
        //--------------------------------------------------

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp)

        ) {

            Text(

                text =
                    "Selected $selectedCount / $totalCount",

                style =
                    MaterialTheme.typography.labelLarge,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant

            )

            Spacer(
                Modifier.height(8.dp)
            )

            Button(

                modifier =
                    Modifier.fillMaxWidth(),

                enabled =
                    selectedCount > 0,

                onClick = {

                    viewModel.confirmAndSaveTransactions(

                        state.parsedTransactions

                    )
                }

            ) {

                Text(

                    "Import $selectedCount Transactions"

                )
            }
        }
    }
}
