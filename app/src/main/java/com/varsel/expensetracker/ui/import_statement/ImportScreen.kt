package com.varsel.expensetracker.ui.import_statement

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.developer.ParserDiagnostics
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.design.AppColors
import com.varsel.expensetracker.ui.import_statement.components.DeleteImportDialog
import com.varsel.expensetracker.ui.import_statement.components.DeveloperDiagnosticsCard
import com.varsel.expensetracker.ui.import_statement.components.ImportFeaturesBanner
import com.varsel.expensetracker.ui.import_statement.components.ImportHistoryCard
import com.varsel.expensetracker.ui.import_statement.components.StatementDetailDialog
import com.varsel.expensetracker.ui.import_statement.components.StatementSummaryCard
import com.varsel.expensetracker.ui.import_statement.components.StatementUploadHeroCard
import com.varsel.expensetracker.ui.import_statement.components.TransactionReviewRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBackClick: () -> Unit,
    onNavigateToTransactions: () -> Unit = onBackClick,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val importHistory by viewModel.importHistory.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val parserDiagnosticsEnabled by viewModel.parserDiagnosticsEnabled.collectAsState()

    var showTransactionReview by remember { mutableStateOf(false) }
    var selectedSnapshotForDetail by remember { mutableStateOf<StatementSnapshotEntity?>(null) }
    var selectedSnapshotForDelete by remember { mutableStateOf<StatementSnapshotEntity?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            showTransactionReview = false
            viewModel.processSelectedFile(it, null)
        }
    }

    // Detail Dialog
    selectedSnapshotForDetail?.let { snapshot ->
        StatementDetailDialog(
            snapshot = snapshot,
            onDismiss = { selectedSnapshotForDetail = null }
        )
    }

    // Delete Confirmation Dialog
    selectedSnapshotForDelete?.let { snapshot ->
        DeleteImportDialog(
            snapshot = snapshot,
            onDismiss = { selectedSnapshotForDelete = null },
            onConfirmDelete = { deleteTransactions ->
                if (deleteTransactions) {
                    viewModel.deleteSnapshotWithTransactions(snapshot)
                } else {
                    viewModel.deleteSnapshot(snapshot.id)
                }
                selectedSnapshotForDelete = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            uiState is ImportUiState.ParsedTransactions && showTransactionReview -> "Review Transactions"
                            uiState is ImportUiState.ParsedTransactions -> "Statement Summary"
                            else -> "Import Statement"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState is ImportUiState.ParsedTransactions && showTransactionReview) {
                                showTransactionReview = false
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.testTag("import_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                //--------------------------------------------------
                // IDLE: Hero Uploader, Smart Features, Import History
                //--------------------------------------------------
                is ImportUiState.Idle -> {
                    IdleImportContent(
                        history = importHistory,
                        onSelectFileClick = {
                            launcher.launch(arrayOf("application/pdf", "image/*"))
                        },
                        onSnapshotClick = { snapshot ->
                            selectedSnapshotForDetail = snapshot
                        },
                        onDeleteSnapshotClick = { snapshot ->
                            selectedSnapshotForDelete = snapshot
                        }
                    )
                }

                //--------------------------------------------------
                // LOADING / PROCESSING
                //--------------------------------------------------
                is ImportUiState.Loading,
                is ImportUiState.Processing -> {
                    LoadingImportContent()
                }

                //--------------------------------------------------
                // PARSED
                //--------------------------------------------------
                is ImportUiState.ParsedTransactions -> {
                    if (!showTransactionReview) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            item {
                                StatementSummaryCard(
                                    summary = state.summary,
                                    onContinue = {
                                        showTransactionReview = true
                                    }
                                )
                            }
                        }
                    } else {
                        TransactionReviewContent(
                            state = state,
                            parserDiagnosticsEnabled = parserDiagnosticsEnabled,
                            diagnostics = diagnostics,
                            viewModel = viewModel
                        )
                    }
                }

                //--------------------------------------------------
                // SAVED
                //--------------------------------------------------
                is ImportUiState.Saved -> {
                    SavedSuccessContent(
                        count = state.count,
                        onDone = {
                            showTransactionReview = false
                            viewModel.resetState()
                            onNavigateToTransactions()
                        },
                        onImportAnother = {
                            showTransactionReview = false
                            viewModel.resetState()
                        }
                    )
                }

                //--------------------------------------------------
                // ERROR
                //--------------------------------------------------
                is ImportUiState.Error -> {
                    ErrorImportContent(
                        message = state.message,
                        onRetry = {
                            showTransactionReview = false
                            viewModel.resetState()
                        }
                    )
                }

                //--------------------------------------------------
                // PASSWORD REQUIRED
                //--------------------------------------------------
                is ImportUiState.PasswordRequired -> {
                    PasswordRequiredContent(
                        onDismiss = {
                            viewModel.resetState()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleImportContent(
    history: List<StatementSnapshotEntity>,
    onSelectFileClick: () -> Unit,
    onSnapshotClick: (StatementSnapshotEntity) -> Unit,
    onDeleteSnapshotClick: (StatementSnapshotEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("idle_import_content"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Hero Upload Container
        item {
            StatementUploadHeroCard(
                onSelectFileClick = onSelectFileClick
            )
        }

        // 2. Smart Capabilities Banner
        item {
            ImportFeaturesBanner()
        }

        // 3. Import History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Import History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (history.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${history.size} statement${if (history.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 4. Import History Items or Empty State
        if (history.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "No Previous Imports",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Uploaded bank statements and their reconciliation audit snapshots will appear here for easy review and rollback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = history,
                key = { it.id }
            ) { snapshot ->
                ImportHistoryCard(
                    snapshot = snapshot,
                    onClick = { onSnapshotClick(snapshot) },
                    onDeleteClick = { onDeleteSnapshotClick(snapshot) }
                )
            }
        }

        // Bottom space for breathing room
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LoadingImportContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Processing Statement",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Extracting text, identifying transaction blocks, and validating mathematical balance reconciliation...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SavedSuccessContent(
    count: Int,
    onDone: () -> Unit,
    onImportAnother: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppColors.Success.copy(alpha = 0.15f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppColors.Success,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Import Successful!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Successfully parsed and saved $count transaction${if (count == 1) "" else "s"} into your ledger with category mappings and duplicate checks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go to Transactions")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onImportAnother,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Import Another Statement")
                }
            }
        }
    }
}

@Composable
private fun ErrorImportContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Import Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}

@Composable
private fun PasswordRequiredContent(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Password-Protected PDF",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Password protected documents are currently not supported for automated local parsing. Please export an unlocked statement copy and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionReviewContent(
    state: ImportUiState.ParsedTransactions,
    parserDiagnosticsEnabled: Boolean,
    diagnostics: ParserDiagnostics,
    viewModel: ImportViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(0) } // 0: All, 1: Expenses, 2: Income, 3: Duplicates

    val allTransactions = state.parsedTransactions

    val filteredTransactions = remember(allTransactions, searchQuery, selectedFilter) {
        allTransactions.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.transaction.description.contains(searchQuery, ignoreCase = true) ||
                item.transaction.category.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                1 -> item.transaction.type == TransactionType.EXPENSE
                2 -> item.transaction.type == TransactionType.INCOME
                3 -> item.isDuplicate
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val totalCount = allTransactions.size
    val selectedCount = allTransactions.count { it.selected }
    val allSelected = totalCount > 0 && selectedCount == totalCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("transaction_review_content")
    ) {
        // Search and Select All row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions or categories...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("All ($totalCount)") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text("Debits") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    label = { Text("Credits") },
                    shape = RoundedCornerShape(8.dp)
                )
                val dupCount = allTransactions.count { it.isDuplicate }
                if (dupCount > 0) {
                    FilterChip(
                        selected = selectedFilter == 3,
                        onClick = { selectedFilter = 3 },
                        label = { Text("Duplicates ($dupCount)") },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Select / Deselect All
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val targetState = !allSelected
                        allTransactions.forEach { it.selected = targetState }
                    }
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            allTransactions.forEach { it.selected = checked }
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (allSelected) "Deselect All" else "Select All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "$selectedCount of $totalCount selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp)

        // Transaction List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(
                items = filteredTransactions,
                key = { index, item -> "${index}_${item.transaction.id}_${item.transaction.dateTimestamp}_${item.transaction.amount}" }
            ) { _, selectable ->
                TransactionReviewRow(
                    selectable = selectable,
                    onCheckedChange = { selectable.selected = it }
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }

        // Developer Diagnostics
        if (parserDiagnosticsEnabled) {
            DeveloperDiagnosticsCard(
                enabled = parserDiagnosticsEnabled,
                diagnostics = diagnostics
            )
        }

        // Bottom Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_import_button"),
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        viewModel.confirmAndSaveTransactions(
                            allTransactions.filter { it.selected }
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Import $selectedCount Transaction${if (selectedCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
