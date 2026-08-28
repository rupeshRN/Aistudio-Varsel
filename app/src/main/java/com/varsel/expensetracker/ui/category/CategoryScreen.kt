package com.varsel.expensetracker.ui.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel,
    onBackClick: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()

    var newCategoryName by remember { mutableStateOf("") }
    var newBudgetLimit by remember { mutableStateOf("") }

    var newMerchantPattern by remember { mutableStateOf("") }
    var selectedCategoryForRule by remember { mutableStateOf("") }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text("Manage Categories & Rules")

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
                .padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {

            Text(
                "Add New Category",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = newCategoryName,
                onValueChange = {
                    newCategoryName = it
                },
                label = {
                    Text("Category Name")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = newBudgetLimit,
                onValueChange = {
                    newBudgetLimit = it
                },
                label = {
                    Text("Monthly Budget Limit")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(

                modifier = Modifier.fillMaxWidth(),

                onClick = {

                    if (newCategoryName.isNotBlank()) {

                        viewModel.addCategory(

                            name = newCategoryName,

                            colorHex = "#FF6200EE",

                            iconName = "default_icon",

                            budgetLimit =
                                newBudgetLimit.toDoubleOrNull() ?: 0.0

                        )

                        newCategoryName = ""
                        newBudgetLimit = ""

                    }

                }

            ) {

                Text("Save Category")

            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Existing Categories",
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(

                modifier = Modifier.weight(1f),

                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                items(uiState.categories) { category ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Row(

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),

                            horizontalArrangement =
                                Arrangement.SpaceBetween,

                            verticalAlignment =
                                Alignment.CenterVertically

                        ) {

                            Column {

                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Text(
                                    "Budget: $${category.budgetLimit}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                            }

                            TextButton(

                                onClick = {

                                    viewModel.deleteCategory(category)

                                }

                            ) {

                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )

                            }

                        }

                    }

                }

            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Custom Parsing Rules",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(

                value = newMerchantPattern,

                onValueChange = {

                    newMerchantPattern = it

                },

                label = {

                    Text("Merchant Pattern")

                },

                modifier = Modifier.fillMaxWidth()

            )

            OutlinedTextField(

                value = selectedCategoryForRule,

                onValueChange = {

                    selectedCategoryForRule = it

                },

                label = {

                    Text("Target Category")

                },

                modifier = Modifier.fillMaxWidth()

            )

            Button(

                modifier = Modifier.fillMaxWidth(),

                onClick = {

                    if (

                        newMerchantPattern.isNotBlank() &&

                        selectedCategoryForRule.isNotBlank()

                    ) {

                        viewModel.addCustomRule(

                            merchantPattern =
                                newMerchantPattern,

                            displayDescription =
                                newMerchantPattern,

                            categoryName =
                                selectedCategoryForRule

                        )

                        newMerchantPattern = ""

                        selectedCategoryForRule = ""

                    }

                }

            ) {

                Text("Add Rule")

            }

            LazyColumn(

                modifier = Modifier.weight(1f),

                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                items(uiState.customRules) { rule ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Row(

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),

                            horizontalArrangement =
                                Arrangement.SpaceBetween,

                            verticalAlignment =
                                Alignment.CenterVertically

                        ) {

                            Column {

                                Text(
                                    "Pattern: ${rule.pattern}",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Text(
                                    "Display: ${rule.displayDescription}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Text(
                                    "Category: ${rule.categoryName}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                            }

                            TextButton(

                                onClick = {

                                    viewModel.deleteCustomRule(rule)

                                }

                            ) {

                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )

                            }

                        }

                    }

                }

            }

        }

    }

}
