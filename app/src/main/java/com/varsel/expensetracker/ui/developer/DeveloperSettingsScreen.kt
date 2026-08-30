package com.varsel.expensetracker.ui.developer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    val parserDiagnosticsEnabled by
        viewModel.parserDiagnosticsEnabled.collectAsState()

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Developer Settings")
                },

                navigationIcon = {

                    TextButton(
                        onClick = onBackClick
                    ) {
                        Text("Back")
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())

        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Developer Tools",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            DeveloperSettingItem(

                title = "Enable Parser Diagnostics",

                description =
                    "View parser statistics, transaction block analysis and import processing details after importing a statement.",

                checked = parserDiagnosticsEnabled,

                enabled = true,

                onCheckedChange = {
                    viewModel.setParserDiagnostics(it)
                }
            )

            DeveloperSettingItem(

                title = "OCR Diagnostics (Coming Soon)",

                description =
                    "Inspect extracted OCR text, normalization results and recognition quality.",

                checked = false,

                enabled = false,

                onCheckedChange = {}
            )

            DeveloperSettingItem(

                title = "Import Diagnostics (Coming Soon)",

                description =
                    "Review statement detection and transaction discovery.",

                checked = false,

                enabled = false,

                onCheckedChange = {}
            )

            DeveloperSettingItem(

                title = "Debug Logging (Coming Soon)",

                description =
                    "Generate detailed parser logs for troubleshooting.",

                checked = false,

                enabled = false,

                onCheckedChange = {}
            )

            DeveloperSettingItem(

                title = "Experimental Features (Coming Soon)",

                description =
                    "Enable unfinished developer features.",

                checked = false,

                enabled = false,

                onCheckedChange = {}
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
