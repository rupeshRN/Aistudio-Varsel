package com.varsel.expensetracker.ui.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.data.local.entity.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetBottomSheet(
    existingBudget: BudgetEntity? = null,
    initialBudgetType: String = "EXPENSE",
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        categoryName: String,
        amount: Double,
        period: String,
        startDayOfMonth: Int,
        limitTotalType: String,
        spendingLimitType: String,
        budgetType: String,
        colorHex: String?,
        iconName: String?
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val budgetType = existingBudget?.budgetType ?: initialBudgetType
    val isSavings = budgetType.equals("SAVINGS", ignoreCase = true)

    var name by remember { mutableStateOf(existingBudget?.name ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            existingBudget?.categoryName ?: if (isSavings) {
                categories.firstOrNull { it.type == "INCOME" }?.name ?: "All Savings"
            } else {
                categories.firstOrNull { it.name.contains("Food", ignoreCase = true) }?.name ?: "Dining & Food"
            }
        )
    }
    var amountText by remember {
        mutableStateOf(existingBudget?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: if (isSavings) "5000" else "3000")
    }
    var selectedPeriod by remember { mutableStateOf(existingBudget?.period ?: "MONTHLY") }
    var startDay by remember { mutableStateOf(existingBudget?.startDayOfMonth?.toString() ?: "1") }
    var limitTotalType by remember { mutableStateOf(existingBudget?.limitTotalType ?: "CONTRIBUTED") }
    var spendingLimitType by remember { mutableStateOf(existingBudget?.spendingLimitType ?: "FIXED") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = when {
                    existingBudget != null && isSavings -> "Edit Savings Budget"
                    existingBudget != null -> "Edit Expense Budget"
                    isSavings -> "New Savings Budget"
                    else -> "New Expense Budget"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category Selection Chips
            Text(
                text = if (isSavings) "Select Savings Category" else "Select Expense Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            val allCategoryOptions = if (isSavings) {
                listOf(
                    "All Savings" to "ic_wallet"
                ) + categories.filter { it.type == "INCOME" || it.type == "BOTH" }.map { it.name to it.iconName }
            } else {
                listOf(
                    "All Expenses" to "ic_wallet"
                ) + categories.filter { it.type == "EXPENSE" || it.type == "BOTH" }.map { it.name to it.iconName }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allCategoryOptions) { (catName, iconKey) ->
                    val isSelected = selectedCategory.equals(catName, ignoreCase = true)
                    val icon = CategoryIconCatalog.iconFor(iconKey)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = catName
                            if (name.isBlank() || allCategoryOptions.any { it.first.equals(name, ignoreCase = true) }) {
                                name = when (catName) {
                                    "All Expenses" -> "Overall Spending"
                                    "All Savings" -> "Total Savings"
                                    else -> catName
                                }
                            }
                        },
                        label = { Text(catName) },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Budget Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (isSavings) "Goal / Budget Name" else "Budget Name") },
                placeholder = { Text(if (isSavings) "e.g. Emergency Fund, Investments" else "e.g. Food, Entertainment") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Budget Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                        amountText = input
                    }
                },
                label = { Text(if (isSavings) "Target Savings Goal (₹)" else "Budget Limit (₹)") },
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_amount_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Period Selection
            Text(
                text = "Budget Period",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("MONTHLY" to "Monthly", "WEEKLY" to "Weekly", "YEARLY" to "Yearly").forEach { (periodKey, periodLabel) ->
                    val isSelected = selectedPeriod == periodKey
                    ElevatedFilterChip(
                        selected = isSelected,
                        onClick = { selectedPeriod = periodKey },
                        label = { Text(periodLabel) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (selectedPeriod == "MONTHLY") {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = startDay,
                    onValueChange = { input ->
                        if (input.isEmpty() || (input.all { it.isDigit() } && (input.toIntOrNull() ?: 0) in 1..28)) {
                            startDay = input
                        }
                    },
                    label = { Text("Month Start Day (1-28)") },
                    placeholder = { Text("1 (e.g. 1st of every month)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Limit Total Type Card (Cashew Screenshot 4 style!)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Limit total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress preview bar (25% preview as seen in Screenshot 4)
                    val previewSpent = 25
                    val previewTotal = 100
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.25f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "25%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(end = 6.dp),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Options: Contributed vs Remaining
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { limitTotalType = "CONTRIBUTED" }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = limitTotalType == "CONTRIBUTED",
                            onClick = { limitTotalType = "CONTRIBUTED" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isSavings) "• ₹$previewSpent saved / ₹$previewTotal" else "• ₹$previewSpent / ₹$previewTotal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isSavings) "Displays accumulated savings so far" else "Displays contributed amount spent",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { limitTotalType = "REMAINING" }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = limitTotalType == "REMAINING",
                            onClick = { limitTotalType = "REMAINING" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "• ₹${previewTotal - previewSpent} left / ₹$previewTotal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isSavings) "Displays amount needed to reach target goal" else "Displays remaining amount left to spend",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingBudget != null && onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: if (isSavings) 5000.0 else 3000.0
                        val startDayInt = startDay.toIntOrNull() ?: 1
                        val finalCategory = if (selectedCategory == "All Expenses" || selectedCategory == "All Savings") "ALL" else selectedCategory
                        val finalName = if (name.isBlank()) finalCategory else name
                        val catEntity = categories.find { it.name.equals(finalCategory, ignoreCase = true) }

                        onSave(
                            finalName,
                            finalCategory,
                            amount,
                            selectedPeriod,
                            startDayInt,
                            limitTotalType,
                            spendingLimitType,
                            budgetType,
                            catEntity?.colorHex,
                            catEntity?.iconName
                        )
                    },
                    modifier = Modifier
                        .weight(if (existingBudget != null && onDelete != null) 1.5f else 1f)
                        .height(52.dp)
                        .testTag("save_budget_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (existingBudget == null) "Create Budget" else "Save Changes")
                }
            }
        }
    }
}
