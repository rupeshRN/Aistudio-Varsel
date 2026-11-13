package com.varsel.expensetracker.ui.quality

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.domain.model.quality.DataQualityReport
import com.varsel.expensetracker.domain.model.quality.QualityActionType
import com.varsel.expensetracker.domain.model.quality.QualityCategory
import com.varsel.expensetracker.domain.model.quality.QualityIssue
import com.varsel.expensetracker.domain.model.quality.QualitySeverity
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit = {},
    viewModel: DataQualityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Data Quality & Health",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Health Score Hero
                    item(key = "hero_card") {
                        uiState.report?.let { report ->
                            QualityScoreHeroCard(report = report)
                        }
                    }

                    // 2. Category Filter Chips
                    item(key = "category_filters") {
                        CategoryFilterRow(
                            selected = uiState.selectedCategory,
                            onSelect = { viewModel.selectCategory(it) }
                        )
                    }

                    // 3. Severity Filter Row
                    item(key = "severity_filters") {
                        SeverityFilterRow(
                            selected = uiState.selectedSeverityFilter,
                            errorCount = uiState.report?.errorCount ?: 0,
                            warningCount = uiState.report?.warningCount ?: 0,
                            infoCount = uiState.report?.infoCount ?: 0,
                            onSelect = { viewModel.selectSeverity(it) }
                        )
                    }

                    // 4. Issues List
                    val issues = uiState.filteredIssues
                    if (issues.isEmpty()) {
                        item(key = "empty_state") {
                            AllCleanCard(selectedCategory = uiState.selectedCategory)
                        }
                    } else {
                        items(issues, key = { it.id }) { issue ->
                            QualityIssueCard(
                                issue = issue,
                                onQuickFix = { viewModel.openQuickFix(issue) },
                                onDismiss = { viewModel.dismissIssue(issue) }
                            )
                        }
                    }

                    item(key = "footer_spacer") {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // Quick Fix Sheets & Dialogs
            when (val sheet = uiState.quickFixSheetState) {
                is QuickFixSheetState.Categorize -> {
                    QuickCategorizeSheet(
                        issue = sheet.issue,
                        categories = uiState.categories,
                        onDismiss = { viewModel.closeQuickFix() },
                        onSelectCategory = { catName ->
                            sheet.issue.entityId?.let { txId ->
                                viewModel.applyCategorize(txId, catName)
                            }
                        }
                    )
                }

                is QuickFixSheetState.DuplicateMerge -> {
                    ConfirmDeleteDuplicateDialog(
                        issue = sheet.issue,
                        onDismiss = { viewModel.closeQuickFix() },
                        onConfirm = {
                            sheet.issue.entityId?.let { txId ->
                                viewModel.applyDeleteDuplicate(txId)
                            }
                        }
                    )
                }

                is QuickFixSheetState.AdjustDate -> {
                    AdjustDateDialog(
                        issue = sheet.issue,
                        onDismiss = { viewModel.closeQuickFix() },
                        onConfirmDate = { newTimestamp ->
                            sheet.issue.entityId?.let { txId ->
                                viewModel.applyAdjustDate(txId, newTimestamp)
                            }
                        }
                    )
                }

                QuickFixSheetState.None -> Unit
            }
        }
    }
}

@Composable
fun QualityScoreHeroCard(report: DataQualityReport) {
    val score = report.healthScore
    val scoreColor = when {
        score >= 90 -> MaterialTheme.colorScheme.primary
        score >= 70 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    val headline = when {
        report.isHealthy -> "Ledger In Pristine Shape"
        report.errorCount > 0 -> "${report.errorCount} Critical Issues Require Review"
        else -> "${report.totalIssues} Anomalies Observed"
    }

    val subtitle = when {
        report.isHealthy -> "All 15 financial data quality and integrity rules passed with zero discrepancies."
        else -> "Resolve or dismiss highlighted transactions to maintain pristine ledger accuracy."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Gauge
                Surface(
                    shape = CircleShape,
                    color = scoreColor.copy(alpha = 0.15f),
                    border = BorderStroke(2.dp, scoreColor),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$score%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = scoreColor.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricCounter(label = "Critical", count = report.errorCount, color = MaterialTheme.colorScheme.error)
                MetricCounter(label = "Warnings", count = report.warningCount, color = MaterialTheme.colorScheme.tertiary)
                MetricCounter(label = "Notices", count = report.infoCount, color = MaterialTheme.colorScheme.primary)
                MetricCounter(label = "Passed Checks", count = report.passedChecksCount, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun MetricCounter(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryFilterRow(
    selected: QualityCategory,
    onSelect: (QualityCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QualityCategory.values().forEach { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun SeverityFilterRow(
    selected: QualitySeverity?,
    errorCount: Int,
    warningCount: Int,
    infoCount: Int,
    onSelect: (QualitySeverity?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All Active") }
        )
        if (errorCount > 0) {
            FilterChip(
                selected = selected == QualitySeverity.ERROR,
                onClick = { onSelect(if (selected == QualitySeverity.ERROR) null else QualitySeverity.ERROR) },
                label = { Text("Errors ($errorCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
        if (warningCount > 0) {
            FilterChip(
                selected = selected == QualitySeverity.WARNING,
                onClick = { onSelect(if (selected == QualitySeverity.WARNING) null else QualitySeverity.WARNING) },
                label = { Text("Warnings ($warningCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
        }
        if (infoCount > 0) {
            FilterChip(
                selected = selected == QualitySeverity.INFO,
                onClick = { onSelect(if (selected == QualitySeverity.INFO) null else QualitySeverity.INFO) },
                label = { Text("Info ($infoCount)") }
            )
        }
    }
}

@Composable
fun QualityIssueCard(
    issue: QualityIssue,
    onQuickFix: () -> Unit,
    onDismiss: () -> Unit
) {
    val severityColor = when (issue.severity) {
        QualitySeverity.ERROR -> MaterialTheme.colorScheme.error
        QualitySeverity.WARNING -> MaterialTheme.colorScheme.tertiary
        QualitySeverity.INFO -> MaterialTheme.colorScheme.primary
    }

    val actionButtonText = when (issue.actionType) {
        QualityActionType.CATEGORIZE -> "Quick Categorise"
        QualityActionType.DELETE_DUPLICATE -> "Resolve Duplicate"
        QualityActionType.ADJUST_DATE -> "Correct Date"
        QualityActionType.CLEAN_ORPHAN -> "Clean Orphan"
        QualityActionType.LINK_TRANSFER -> "Review Transfer"
        QualityActionType.RECORD_RECURRING -> "Record Payment"
        QualityActionType.VIEW_LOAN -> "View Loan"
        QualityActionType.VIEW_BUDGET -> "View Budget"
        QualityActionType.DISMISS -> "Acknowledge"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quality_issue_${issue.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Severity Tag + Category
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = severityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = issue.type.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = severityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (issue.accountName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = issue.accountName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Dismiss icon button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Issue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Description
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (issue.details != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = issue.details,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onQuickFix,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = severityColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(actionButtonText, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun AllCleanCard(selectedCategory: QualityCategory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No Issues Found!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (selectedCategory == QualityCategory.ALL) {
                    "Your offline ledger is in excellent health. All 15 audit checks passed."
                } else {
                    "No anomalies detected in ${selectedCategory.displayName}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCategorizeSheet(
    issue: QualityIssue,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSelectCategory: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Choose Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a category for '${issue.title}'",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val commonCategories = if (categories.isNotEmpty()) {
                categories.map { it.name }
            } else {
                listOf("Groceries", "Food & Dining", "Shopping", "Transport", "Bills & Utilities", "Entertainment", "Health", "Salary", "Investment")
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                commonCategories.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { catName ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectCategory(catName) },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDuplicateDialog(
    issue: QualityIssue,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Resolve Duplicate",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Would you like to delete the duplicate transaction #${issue.entityId}?\n\nOriginal entry #${issue.secondaryEntityId ?: "primary"} will be preserved.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Duplicate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdjustDateDialog(
    issue: QualityIssue,
    onDismiss: () -> Unit,
    onConfirmDate: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Correct Future Date",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This transaction is dated in the future (${issue.dateTimestamp?.let { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(it)) } ?: "unknown"}).\n\nWould you like to set the date to today?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = { onConfirmDate(System.currentTimeMillis()) }) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Set to Today")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
