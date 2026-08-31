package com.varsel.expensetracker.ui.settings

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("settings_back_button")
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            // Group 1: Preferences & Appearance
            Text(
                text = "Preferences & Rules",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column {
                    MoreMenuItem(
                        icon = Icons.Outlined.Category,
                        title = "Categories",
                        subtitle = "Manage spending, income & loan category tags",
                        showDivider = true,
                        onClick = onCategoriesClick
                    )

                    MoreMenuItem(
                        icon = Icons.Outlined.Palette,
                        title = "Appearance",
                        subtitle = "Dark mode, Material You, accents & AMOLED display",
                        showDivider = true,
                        onClick = onAppearanceClick
                    )

                    MoreMenuItem(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Learning Rules",
                        subtitle = "Manage smart rule auto-categorization",
                        showDivider = false,
                        onClick = onLearningRulesClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 2: System & Info
            Text(
                text = "System & About",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column {
                    MoreMenuItem(
                        icon = Icons.Outlined.Code,
                        title = "Developer Tools",
                        subtitle = "Statement parser logs & diagnostics",
                        showDivider = true,
                        onClick = onDeveloperClick
                    )

                    MoreMenuItem(
                        icon = Icons.Outlined.Info,
                        title = "About Varsel",
                        subtitle = "GitHub repo, version info & privacy guarantee",
                        showDivider = false,
                        badgeText = "v1.0.0",
                        onClick = onAboutClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Tagline Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Varsel • 100% Offline & Private",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version 1.0.0 (Release Build)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
