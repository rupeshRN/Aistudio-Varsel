package com.varsel.expensetracker.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.more.MoreMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onLearningRulesClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onDeveloperClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "Preferences & Rules",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            MoreMenuItem(
                icon = Icons.Outlined.Category,
                title = "Categories",
                subtitle = "Manage spending and income categories",
                onClick = onCategoriesClick
            )

            MoreMenuItem(
                icon = Icons.Outlined.AutoAwesome,
                title = "Learning Rules",
                subtitle = "Manage offline AI categorization rules",
                onClick = onLearningRulesClick
            )

            MoreMenuItem(
                icon = Icons.Outlined.Palette,
                title = "Appearance",
                subtitle = "Theme & display preferences",
                onClick = onAppearanceClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Text(
                text = "System & Diagnostics",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            MoreMenuItem(
                icon = Icons.Outlined.Code,
                title = "Developer Tools",
                subtitle = "Statement parser tools & diagnostics",
                onClick = onDeveloperClick
            )

            MoreMenuItem(
                icon = Icons.Outlined.Info,
                title = "About",
                subtitle = "About Varsel & Version",
                onClick = onAboutClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Varsel • Version 1.0.0",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
