package com.varsel.expensetracker.ui.developer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.varsel.expensetracker.developer.DeveloperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: DeveloperViewModel = hiltViewModel()
) {
    val parserDiagnosticsEnabled by viewModel.parserDiagnosticsEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Developer Tools",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("developer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            // Header Info Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Advanced diagnostic toggles to inspect statement parsing, regex extraction and categorization internals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Parser & Diagnostics",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            DeveloperSettingItem(
                title = "Parser Diagnostics",
                description = "View parser statistics, transaction block analysis and import processing details after importing a statement.",
                checked = parserDiagnosticsEnabled,
                enabled = true,
                icon = Icons.Outlined.Analytics,
                badgeText = "Active",
                onCheckedChange = {
                    viewModel.setParserDiagnostics(it)
                }
            )

            DeveloperSettingItem(
                title = "OCR Diagnostics",
                description = "Inspect extracted OCR text, normalization results and recognition quality.",
                checked = false,
                enabled = false,
                icon = Icons.Outlined.DocumentScanner,
                badgeText = "Soon",
                onCheckedChange = {}
            )

            DeveloperSettingItem(
                title = "Import Discovery",
                description = "Review statement structure detection and multi-column transaction discovery.",
                checked = false,
                enabled = false,
                icon = Icons.Outlined.FileDownload,
                badgeText = "Soon",
                onCheckedChange = {}
            )

            DeveloperSettingItem(
                title = "Debug Logging",
                description = "Generate detailed parser logs and regex capture groups for troubleshooting.",
                checked = false,
                enabled = false,
                icon = Icons.Outlined.BugReport,
                badgeText = "Soon",
                onCheckedChange = {}
            )

            DeveloperSettingItem(
                title = "Experimental Engine",
                description = "Enable experimental rule synthesis and statement layout learning features.",
                checked = false,
                enabled = false,
                icon = Icons.Outlined.Science,
                badgeText = "Soon",
                onCheckedChange = {}
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
