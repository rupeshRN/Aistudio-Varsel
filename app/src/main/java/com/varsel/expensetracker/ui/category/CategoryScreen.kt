package com.varsel.expensetracker.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.data.local.entity.CustomRuleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialog States
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var defaultCategoryType by remember { mutableStateOf("EXPENSE") }

    var showRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CustomRuleEntity?>(null) }

    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var ruleToDelete by remember { mutableStateOf<CustomRuleEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Categories & Rules",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Manage separate income, expense & smart auto-rules",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("category_screen_back_button")
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (uiState.selectedTab) {
                        CategoryTab.EXPENSES -> {
                            editingCategory = null
                            defaultCategoryType = "EXPENSE"
                            showCategoryDialog = true
                        }
                        CategoryTab.INCOME -> {
                            editingCategory = null
                            defaultCategoryType = "INCOME"
                            showCategoryDialog = true
                        }
                        CategoryTab.RULES -> {
                            editingRule = null
                            showRuleDialog = true
                        }
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        when (uiState.selectedTab) {
                            CategoryTab.EXPENSES -> "Add Expense Category"
                            CategoryTab.INCOME -> "Add Income Category"
                            CategoryTab.RULES -> "Add Smart Rule"
                        }
                    )
                },
                modifier = Modifier.testTag("add_category_or_rule_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stat Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetricChip(
                    title = "Expense",
                    count = uiState.expenseCategories.size,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                    selected = uiState.selectedTab == CategoryTab.EXPENSES,
                    onClick = { viewModel.selectTab(CategoryTab.EXPENSES) }
                )
                SummaryMetricChip(
                    title = "Income",
                    count = uiState.incomeCategories.size,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f),
                    selected = uiState.selectedTab == CategoryTab.INCOME,
                    onClick = { viewModel.selectTab(CategoryTab.INCOME) }
                )
                SummaryMetricChip(
                    title = "Rules",
                    count = uiState.customRules.size,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    selected = uiState.selectedTab == CategoryTab.RULES,
                    onClick = { viewModel.selectTab(CategoryTab.RULES) }
                )
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = {
                    Text("Search categories or keywords...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("category_search_input")
            )

            // Primary Tab Row
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.selectedTab == CategoryTab.EXPENSES,
                    onClick = { viewModel.selectTab(CategoryTab.EXPENSES) },
                    text = { Text("Expenses (${uiState.expenseCategories.size})") },
                    icon = { Icon(Icons.Default.NorthEast, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == CategoryTab.INCOME,
                    onClick = { viewModel.selectTab(CategoryTab.INCOME) },
                    text = { Text("Income (${uiState.incomeCategories.size})") },
                    icon = { Icon(Icons.Default.SouthWest, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == CategoryTab.RULES,
                    onClick = { viewModel.selectTab(CategoryTab.RULES) },
                    text = { Text("Smart Rules (${uiState.customRules.size})") },
                    icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) }
                )
            }

            // Tab Content
            when (uiState.selectedTab) {
                CategoryTab.EXPENSES -> {
                    CategoryListView(
                        categories = uiState.expenseCategories,
                        emptyMessage = if (uiState.searchQuery.isBlank()) "No expense categories found." else "No matching expense categories.",
                        onEdit = {
                            editingCategory = it
                            defaultCategoryType = "EXPENSE"
                            showCategoryDialog = true
                        },
                        onDelete = { categoryToDelete = it }
                    )
                }
                CategoryTab.INCOME -> {
                    CategoryListView(
                        categories = uiState.incomeCategories,
                        emptyMessage = if (uiState.searchQuery.isBlank()) "No income categories found." else "No matching income categories.",
                        onEdit = {
                            editingCategory = it
                            defaultCategoryType = "INCOME"
                            showCategoryDialog = true
                        },
                        onDelete = { categoryToDelete = it }
                    )
                }
                CategoryTab.RULES -> {
                    CustomRulesListView(
                        rules = uiState.customRules,
                        categories = uiState.allCategories,
                        emptyMessage = if (uiState.searchQuery.isBlank()) "No smart rules created yet." else "No matching smart rules.",
                        onEdit = {
                            editingRule = it
                            showRuleDialog = true
                        },
                        onDelete = { ruleToDelete = it }
                    )
                }
            }
        }
    }

    // Add / Edit Category Dialog
    if (showCategoryDialog) {
        AddEditCategoryDialog(
            initialCategory = editingCategory,
            defaultType = defaultCategoryType,
            onDismiss = {
                showCategoryDialog = false
                editingCategory = null
            },
            onSave = { id, name, type, colorHex, iconName, budget, keywords ->
                viewModel.saveCategory(
                    id = id,
                    name = name,
                    type = type,
                    colorHex = colorHex,
                    iconName = iconName,
                    budgetLimit = budget,
                    keywords = keywords
                )
                showCategoryDialog = false
                editingCategory = null
            }
        )
    }

    // Add / Edit Custom Rule Dialog
    if (showRuleDialog) {
        AddEditRuleDialog(
            initialRule = editingRule,
            availableCategories = uiState.allCategories,
            onDismiss = {
                showRuleDialog = false
                editingRule = null
            },
            onSave = { id, pattern, desc, categoryName ->
                viewModel.saveCustomRule(
                    id = id,
                    merchantPattern = pattern,
                    displayDescription = desc,
                    categoryName = categoryName
                )
                showRuleDialog = false
                editingRule = null
            }
        )
    }

    // Delete Category Confirmation Dialog
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = {
                Text("Are you sure you want to delete category \"${category.name}\"? Existing transactions assigned to this category will not be lost.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(category)
                        categoryToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Rule Confirmation Dialog
    ruleToDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("Delete Smart Rule") },
            text = {
                Text("Are you sure you want to delete the rule for pattern \"${rule.pattern}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomRule(rule)
                        ruleToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SummaryMetricChip(
    title: String,
    count: Int,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (selected) color else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (selected) color else MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryListView(
    categories: List<CategoryEntity>,
    emptyMessage: String,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit
) {
    if (categories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                val categoryColor = remember(category.colorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(category.colorHex))
                    } catch (e: Exception) {
                        Color(0xFF673AB7)
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Icon Avatar
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(categoryColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconCatalog.iconFor(category.iconName.ifBlank { category.name }),
                                contentDescription = category.name,
                                tint = categoryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Category Details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Category Type Badge
                                val typeBadgeText = when (category.type.uppercase()) {
                                    "INCOME" -> "Income"
                                    "BOTH" -> "Universal"
                                    else -> "Expense"
                                }
                                val typeBadgeColor = when (category.type.uppercase()) {
                                    "INCOME" -> Color(0xFF2E7D32)
                                    "BOTH" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.error
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = typeBadgeColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = typeBadgeText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = typeBadgeColor
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Budget Info
                            if (category.budgetLimit > 0.0) {
                                Text(
                                    text = "Monthly Budget: $${"%.2f".format(category.budgetLimit)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Keywords chips
                            if (category.keywords.isNotBlank()) {
                                val keywordList = category.keywords.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .take(4)

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    keywordList.forEach { kw ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = kw,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onEdit(category) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit category",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDelete(category) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete category",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomRulesListView(
    rules: List<CustomRuleEntity>,
    categories: List<CategoryEntity>,
    emptyMessage: String,
    onEdit: (CustomRuleEntity) -> Unit,
    onDelete: (CustomRuleEntity) -> Unit
) {
    if (rules.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(rules, key = { it.id }) { rule ->
                val matchedCategory = categories.firstOrNull { it.name.equals(rule.categoryName, ignoreCase = true) }
                val categoryColor = matchedCategory?.colorHex?.let {
                    try {
                        Color(android.graphics.Color.parseColor(it))
                    } catch (e: Exception) {
                        null
                    }
                } ?: MaterialTheme.colorScheme.primary

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = rule.pattern,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "→",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = categoryColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = rule.categoryName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = categoryColor
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (rule.displayDescription.isNotBlank() && rule.displayDescription != rule.pattern) {
                                Text(
                                    text = "Display Name: ${rule.displayDescription}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onEdit(rule) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit rule",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDelete(rule) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete rule",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCategoryDialog(
    initialCategory: CategoryEntity?,
    defaultType: String,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, type: String, colorHex: String, iconName: String, budget: Double, keywords: String) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var selectedType by remember {
        mutableStateOf(initialCategory?.type?.uppercase() ?: defaultType)
    }
    var selectedIconKey by remember {
        mutableStateOf(initialCategory?.iconName?.ifBlank { "ic_restaurant" } ?: "ic_restaurant")
    }
    var selectedColorHex by remember {
        mutableStateOf(initialCategory?.colorHex?.ifBlank { "#4CAF50" } ?: "#4CAF50")
    }
    var budgetText by remember {
        mutableStateOf(if (initialCategory != null && initialCategory.budgetLimit > 0) initialCategory.budgetLimit.toString() else "")
    }
    var keywords by remember { mutableStateOf(initialCategory?.keywords ?: "") }

    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = if (initialCategory == null) "Create Category" else "Edit Category",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Category Type Selector
                    Text(
                        text = "Category Type",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("EXPENSE" to "Expense", "INCOME" to "Income", "BOTH" to "Both").forEach { (typeKey, typeLabel) ->
                            FilterChip(
                                selected = selectedType == typeKey,
                                onClick = { selectedType = typeKey },
                                label = { Text(typeLabel) },
                                leadingIcon = if (selectedType == typeKey) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Category Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            isError = it.isBlank()
                        },
                        label = { Text("Category Name *") },
                        placeholder = { Text("e.g. Dining, Freelance, Groceries") },
                        isError = isError,
                        supportingText = if (isError) {
                            { Text("Category name cannot be empty") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Icon Selector
                    Text(
                        text = "Choose Icon",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(CategoryIconCatalog.availableIcons) { iconOption ->
                            val isSelected = selectedIconKey.equals(iconOption.key, ignoreCase = true)
                            val parsedColor = try {
                                Color(android.graphics.Color.parseColor(selectedColorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) parsedColor.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) parsedColor else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedIconKey = iconOption.key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconOption.icon,
                                    contentDescription = iconOption.label,
                                    tint = if (isSelected) parsedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Color Selector
                    Text(
                        text = "Choose Color",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIconCatalog.availableColorHexes.forEach { colorHex ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(colorHex))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                            val isSelected = selectedColorHex.equals(colorHex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorHex = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Budget Limit (applicable mainly to expenses)
                    if (selectedType != "INCOME") {
                        OutlinedTextField(
                            value = budgetText,
                            onValueChange = { budgetText = it },
                            label = { Text("Monthly Budget Limit (Optional)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Auto-categorization Keywords
                    OutlinedTextField(
                        value = keywords,
                        onValueChange = { keywords = it },
                        label = { Text("Auto-Match Keywords (Optional)") },
                        placeholder = { Text("e.g. SWIGGY, ZOMATO, MCDONALD") },
                        supportingText = {
                            Text("Separate multiple keywords with commas. SMS and statement transactions matching these keywords will be auto-assigned.")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                isError = true
                                return@Button
                            }
                            val budget = budgetText.toDoubleOrNull() ?: 0.0
                            onSave(
                                initialCategory?.id ?: 0L,
                                name.trim(),
                                selectedType,
                                selectedColorHex,
                                selectedIconKey,
                                budget,
                                keywords.trim()
                            )
                        }
                    ) {
                        Text(if (initialCategory == null) "Create" else "Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditRuleDialog(
    initialRule: CustomRuleEntity?,
    availableCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (id: Long, pattern: String, desc: String, categoryName: String) -> Unit
) {
    var pattern by remember { mutableStateOf(initialRule?.pattern ?: "") }
    var description by remember { mutableStateOf(initialRule?.displayDescription ?: "") }
    var selectedCategoryName by remember {
        mutableStateOf(initialRule?.categoryName ?: availableCategories.firstOrNull()?.name ?: "")
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var isPatternError by remember { mutableStateOf(false) }
    var isCategoryError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (initialRule == null) "Add Smart Auto-Rule" else "Edit Smart Auto-Rule",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = {
                            pattern = it
                            isPatternError = it.isBlank()
                        },
                        label = { Text("Merchant / Description Pattern *") },
                        placeholder = { Text("e.g. NETFLIX, UBER, STARBUCKS") },
                        isError = isPatternError,
                        supportingText = {
                            Text("Transactions containing this text will be auto-categorized.")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Clean Display Title (Optional)") },
                        placeholder = { Text("e.g. Netflix Subscription") },
                        supportingText = {
                            Text("Replaces ugly bank statement text with this clean readable name.")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Target Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategoryName.ifBlank { "Select Target Category" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Category *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val color = try {
                                                Color(android.graphics.Color.parseColor(cat.colorHex))
                                            } catch (e: Exception) {
                                                MaterialTheme.colorScheme.primary
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Text(cat.name)
                                            Text(
                                                "(${cat.type})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCategoryName = cat.name
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (pattern.isBlank()) {
                                isPatternError = true
                                return@Button
                            }
                            if (selectedCategoryName.isBlank()) {
                                isCategoryError = true
                                return@Button
                            }
                            onSave(
                                initialRule?.id ?: 0L,
                                pattern.trim(),
                                description.trim(),
                                selectedCategoryName.trim()
                            )
                        }
                    ) {
                        Text(if (initialRule == null) "Create Rule" else "Save Rule")
                    }
                }
            }
        }
    }
}
