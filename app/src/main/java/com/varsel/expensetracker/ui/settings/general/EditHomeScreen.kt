package com.varsel.expensetracker.ui.settings.general

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.data.preference.HomeSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHomeScreen(
    onBackClick: () -> Unit,
    viewModel: GeneralSettingsViewModel = hiltViewModel()
) {
    val generalConfig by viewModel.generalConfig.collectAsStateWithLifecycle()
    val activeSections = generalConfig.activeHomeSections

    val allSections = HomeSection.entries
    val inactiveSections = allSections.map { it.id }.filterNot { it in activeSections }

    var selectedSectionForCustomization by remember { mutableStateOf<HomeSection?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("edit_home_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header Info Card (matching Screenshot 2)
            item(key = "header_info_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Edit Home",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reorder and enable homepage sections. Tap each section for extra customization.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Upper Card: Active Sections
            item(key = "active_sections_card") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .padding(vertical = 8.dp)
                    ) {
                        if (activeSections.isEmpty()) {
                            Text(
                                text = "No active sections. Add some from below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        } else {
                            activeSections.forEachIndexed { index, sectionId ->
                                val section = HomeSection.findById(sectionId)
                                if (section != null) {
                                    ActiveSectionRow(
                                        section = section,
                                        isFirst = index == 0,
                                        isLast = index == activeSections.lastIndex,
                                        showDivider = index < activeSections.lastIndex,
                                        onRemove = {
                                            viewModel.removeHomeSection(sectionId)
                                        },
                                        onMoveUp = {
                                            viewModel.moveHomeSectionUp(index)
                                        },
                                        onMoveDown = {
                                            viewModel.moveHomeSectionDown(index)
                                        },
                                        onCustomizationClick = {
                                            selectedSectionForCustomization = section
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lower Card: Disabled / Available Sections
            item(key = "available_sections_card") {
                if (inactiveSections.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .padding(vertical = 8.dp)
                        ) {
                            inactiveSections.forEachIndexed { index, sectionId ->
                                val section = HomeSection.findById(sectionId)
                                if (section != null) {
                                    AvailableSectionRow(
                                        section = section,
                                        showDivider = index < inactiveSections.lastIndex,
                                        onAdd = {
                                            viewModel.addHomeSection(sectionId)
                                        },
                                        onCustomizationClick = {
                                            selectedSectionForCustomization = section
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reset Button
            item(key = "reset_button") {
                OutlinedButton(
                    onClick = { viewModel.resetHomeSections() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reset_home_sections_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Default Layout")
                }
            }
        }
    }

    // Detail customization dialog
    selectedSectionForCustomization?.let { section ->
        AlertDialog(
            onDismissRequest = { selectedSectionForCustomization = null },
            icon = {
                Icon(
                    imageVector = getIconForSection(section),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = section.displayName,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = section.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Status: ${if (section.id in activeSections) "Visible on Homepage" else "Hidden"}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (section.id in activeSections) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSectionForCustomization = null }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun ActiveSectionRow(
    section: HomeSection,
    isFirst: Boolean,
    isLast: Boolean,
    showDivider: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onCustomizationClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCustomizationClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconForSection(section),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = section.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // More / Customize options
            IconButton(
                onClick = onCustomizationClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Red Minus Button (matching Screenshot 2)
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE57373).copy(alpha = 0.85f))
                    .testTag("remove_section_${section.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Reorder Up & Down handles
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        tint = if (!isFirst) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        tint = if (!isLast) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
private fun AvailableSectionRow(
    section: HomeSection,
    showDivider: Boolean,
    onAdd: () -> Unit,
    onCustomizationClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCustomizationClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconForSection(section),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = section.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // More / Info
            IconButton(
                onClick = onCustomizationClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Green Plus Button (matching Screenshot 2)
            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.85f))
                    .testTag("add_section_${section.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Drag indicator handle
            Icon(
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = "Handle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )
        }
    }
}

fun getIconForSection(section: HomeSection): ImageVector {
    return when (section) {
        HomeSection.BANNER -> Icons.Outlined.ViewAgenda
        HomeSection.NET_WORTH -> Icons.Outlined.TrendingUp
        HomeSection.QUICK_ACTIONS -> Icons.Outlined.FlashOn
        HomeSection.TRANSACTIONS -> Icons.Outlined.ReceiptLong
        HomeSection.INSIGHTS -> Icons.Outlined.Lightbulb
        HomeSection.LOANS -> Icons.Outlined.AccountBalance
        HomeSection.BUDGETS -> Icons.Outlined.PieChart
        HomeSection.INCOME_EXPENSE -> Icons.Outlined.SwapVert
        HomeSection.TRENDS_GRAPH -> Icons.Outlined.ShowChart
        HomeSection.ACCOUNTS_LIST -> Icons.Outlined.AccountBalanceWallet
        HomeSection.GOALS -> Icons.Outlined.Savings
    }
}
