package com.varsel.expensetracker.ui.settings.general

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.varsel.expensetracker.data.preference.GeneralConfig
import com.varsel.expensetracker.data.preference.HomeSection
import com.varsel.expensetracker.ui.dashboard.HomeWidgetItemsSelectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHomeScreen(
    onBackClick: () -> Unit,
    viewModel: GeneralSettingsViewModel = hiltViewModel()
) {
    val generalConfig by viewModel.generalConfig.collectAsStateWithLifecycle()
    val allBudgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val availableAccounts by viewModel.availableAccounts.collectAsStateWithLifecycle()
    val activeSections = generalConfig.activeHomeSections

    val allSections = HomeSection.entries
    val inactiveSections = allSections.map { it.id }.filterNot { it in activeSections }

    var selectedSectionForCustomization by remember { mutableStateOf<HomeSection?>(null) }

    val lazyListState = rememberLazyListState()
    val showCollapsedTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 30
        }
    }

    var draggingSectionId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val stepThresholdPx = with(density) { 56.dp.toPx() }
    val currentActiveSections by rememberUpdatedState(activeSections)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showCollapsedTitle,
                        enter = fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            initialOffsetX = { -20 }
                        ),
                        exit = fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                            animationSpec = tween(180, easing = FastOutLinearInEasing),
                            targetOffsetX = { -20 }
                        )
                    ) {
                        Text(
                            text = "Edit Home",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
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
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            state = lazyListState,
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
                                    val isDraggingThis = draggingSectionId == section.id
                                    val subtitle = getSectionSubtitle(section, generalConfig)
                                    ActiveSectionRow(
                                        section = section,
                                        subtitle = subtitle,
                                        isDragging = isDraggingThis,
                                        dragOffsetY = if (isDraggingThis) dragOffsetY else 0f,
                                        showDivider = index < activeSections.lastIndex,
                                        onRemove = {
                                            viewModel.removeHomeSection(sectionId)
                                        },
                                        onDragStart = {
                                            draggingSectionId = section.id
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            draggingSectionId = null
                                            dragOffsetY = 0f
                                        },
                                        onVerticalDrag = { dragAmount ->
                                            dragOffsetY += dragAmount
                                            val currentList = currentActiveSections
                                            val currentIndex = currentList.indexOf(section.id)
                                            if (currentIndex != -1) {
                                                if (currentIndex == 0 && dragOffsetY < 0f) {
                                                    dragOffsetY = 0f
                                                } else if (currentIndex == currentList.lastIndex && dragOffsetY > 0f) {
                                                    dragOffsetY = 0f
                                                } else if (dragOffsetY > stepThresholdPx && currentIndex < currentList.lastIndex) {
                                                    viewModel.moveHomeSectionDown(currentIndex)
                                                    dragOffsetY -= stepThresholdPx
                                                } else if (dragOffsetY < -stepThresholdPx && currentIndex > 0) {
                                                    viewModel.moveHomeSectionUp(currentIndex)
                                                    dragOffsetY += stepThresholdPx
                                                }
                                            }
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
                                    val subtitle = getSectionSubtitle(section, generalConfig)
                                    AvailableSectionRow(
                                        section = section,
                                        subtitle = subtitle,
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

    // Section-specific customization dialogs/sheets
    when (selectedSectionForCustomization) {
        HomeSection.ACCOUNTS_LIST -> {
            SelectAccountsSheet(
                pinnedAccounts = generalConfig.pinnedAccounts,
                availableAccounts = availableAccounts,
                primaryAccount = generalConfig.primaryAccount,
                onPrimaryAccountChange = { viewModel.setPrimaryAccount(it) },
                onTogglePin = { viewModel.togglePinAccount(it) },
                onAddAccount = { viewModel.addCustomAccount(it) },
                onRemoveAccount = { viewModel.removePinnedAccount(it) },
                onMoveUp = { viewModel.movePinnedAccountUp(it) },
                onMoveDown = { viewModel.movePinnedAccountDown(it) },
                onDismiss = { selectedSectionForCustomization = null }
            )
        }
        HomeSection.BUDGETS -> {
            val expenseBudgets = allBudgets.filter { !it.budget.budgetType.equals("SAVINGS", ignoreCase = true) }
            HomeWidgetItemsSelectionDialog(
                title = "Select Budgets to Display",
                items = expenseBudgets,
                currentSelection = generalConfig.homeBudgetsSelection,
                onSave = {
                    viewModel.setHomeBudgetsSelection(it)
                    selectedSectionForCustomization = null
                },
                onDismiss = { selectedSectionForCustomization = null }
            )
        }
        HomeSection.GOALS -> {
            val savingsGoals = allBudgets.filter { it.budget.budgetType.equals("SAVINGS", ignoreCase = true) }
            HomeWidgetItemsSelectionDialog(
                title = "Select Goals to Display",
                items = savingsGoals,
                currentSelection = generalConfig.homeGoalsSelection,
                onSave = {
                    viewModel.setHomeGoalsSelection(it)
                    selectedSectionForCustomization = null
                },
                onDismiss = { selectedSectionForCustomization = null }
            )
        }
        HomeSection.NET_WORTH -> {
            NetWorthCustomizationDialog(
                currentPeriod = generalConfig.netWorthWidgetPeriod,
                showBreakdown = generalConfig.showNetWorthBreakdown,
                onPeriodSelected = { viewModel.setNetWorthWidgetPeriod(it) },
                onShowBreakdownChange = { viewModel.setShowNetWorthBreakdown(it) },
                onDismiss = { selectedSectionForCustomization = null }
            )
        }
        HomeSection.TRANSACTIONS -> {
            TransactionsCustomizationDialog(
                currentCount = generalConfig.homeTransactionsCount,
                currentFilter = generalConfig.homeTransactionsFilter,
                onCountSelected = { viewModel.setHomeTransactionsCount(it) },
                onFilterSelected = { viewModel.setHomeTransactionsFilter(it) },
                onDismiss = { selectedSectionForCustomization = null }
            )
        }
        HomeSection.BANNER -> {
            BannerCustomizationDialog(
                showGreeting = generalConfig.homeBannerShowGreeting,
                showStatus = generalConfig.homeBannerShowStatus,
                onShowGreetingChange = { viewModel.setHomeBannerShowGreeting(it) },
                onShowStatusChange = { viewModel.setHomeBannerShowStatus(it) },
                onDismiss = { selectedSectionForCustomization = null }
            )
        }
        HomeSection.LOANS -> {
            LoansCustomizationDialog(
                currentFilter = generalConfig.homeLoansFilter,
                onFilterSelected = { viewModel.setHomeLoansFilter(it) },
                onDismiss = { selectedSectionForCustomization = null }
            )
        }
        HomeSection.QUICK_ACTIONS -> {
            QuickActionsCustomizationDialog(
                onDismiss = { selectedSectionForCustomization = null }
            )
        }
        HomeSection.INSIGHTS -> {
            InsightsCustomizationDialog(
                onDismiss = { selectedSectionForCustomization = null }
            )
        }
        null -> { /* No customization open */ }
    }
}

private fun getSectionSubtitle(
    section: HomeSection,
    generalConfig: GeneralConfig
): String? {
    return when (section) {
        HomeSection.NET_WORTH -> {
            val breakdown = if (generalConfig.showNetWorthBreakdown) " • Breakdown on" else ""
            "${generalConfig.netWorthWidgetPeriod}$breakdown"
        }
        HomeSection.TRANSACTIONS -> {
            val filterLabel = when (generalConfig.homeTransactionsFilter) {
                "INCOME" -> "Income"
                "EXPENSE" -> "Expense"
                "ALL_EXCEPT_TRANSFERS" -> "Excl. Transfers"
                else -> "All"
            }
            "${generalConfig.homeTransactionsCount} items • $filterLabel"
        }
        HomeSection.BUDGETS -> {
            if (generalConfig.homeBudgetsSelection == "ALL" || generalConfig.homeBudgetsSelection.isBlank()) "All Budgets"
            else "${generalConfig.homeBudgetsSelection.split(",").size} selected"
        }
        HomeSection.GOALS -> {
            if (generalConfig.homeGoalsSelection == "ALL" || generalConfig.homeGoalsSelection.isBlank()) "All Goals"
            else "${generalConfig.homeGoalsSelection.split(",").size} selected"
        }
        HomeSection.ACCOUNTS_LIST -> {
            if (generalConfig.pinnedAccounts.isEmpty()) "All Accounts"
            else "${generalConfig.pinnedAccounts.size} pinned"
        }
        HomeSection.BANNER -> {
            "Greeting: ${if (generalConfig.homeBannerShowGreeting) "On" else "Off"}"
        }
        HomeSection.LOANS -> {
            if (generalConfig.homeLoansFilter == "ACTIVE") "Active Only" else "All Loans"
        }
        else -> null
    }
}

@Composable
private fun ActiveSectionRow(
    section: HomeSection,
    subtitle: String? = null,
    isDragging: Boolean,
    dragOffsetY: Float,
    showDivider: Boolean,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onCustomizationClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .zIndex(if (isDragging) 10f else 1f)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY.coerceIn(-60f, 60f) else 0f
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
                shadowElevation = if (isDragging) 12f else 0f
            }
            .background(
                if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
            )
    ) {
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // More / Customize options
            IconButton(
                onClick = onCustomizationClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("customize_section_${section.id}")
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

            Spacer(modifier = Modifier.width(8.dp))

            // Draggable handle button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(section.id) {
                        detectVerticalDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                onVerticalDrag(dragAmount)
                            }
                        )
                    }
                    .testTag("drag_handle_${section.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (showDivider && !isDragging) {
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
    subtitle: String? = null,
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // More / Info
            IconButton(
                onClick = onCustomizationClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("customize_available_section_${section.id}")
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
        HomeSection.ACCOUNTS_LIST -> Icons.Outlined.AccountBalanceWallet
        HomeSection.QUICK_ACTIONS -> Icons.Outlined.FlashOn
        HomeSection.TRANSACTIONS -> Icons.Outlined.ReceiptLong
        HomeSection.INSIGHTS -> Icons.Outlined.Lightbulb
        HomeSection.LOANS -> Icons.Outlined.AccountBalance
        HomeSection.BUDGETS -> Icons.Outlined.PieChart
        HomeSection.GOALS -> Icons.Outlined.Savings
    }
}
