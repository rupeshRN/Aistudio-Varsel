package com.varsel.expensetracker.ui.settings.general

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PIN_COLORS = listOf(
    Color(0xFF5C6BC0), // Indigo / Soft Blue
    Color(0xFFFFA726), // Warm Amber / Orange
    Color(0xFFEF5350), // Coral / Pink
    Color(0xFF78909C), // Slate Blue Grey
    Color(0xFF26A69A), // Teal
    Color(0xFFAB47BC), // Purple
    Color(0xFF42A5F5), // Sky Blue
    Color(0xFF66BB6A)  // Emerald
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAccountsSheet(
    pinnedAccounts: List<String>,
    availableAccounts: List<String>,
    primaryAccount: String,
    onPrimaryAccountChange: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onAddAccount: (String) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var showPrimaryMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row: "Select Accounts" + Pencil icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Select Accounts",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { isEditMode = !isEditMode },
                    modifier = Modifier.testTag("edit_accounts_pencil")
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Outlined.Check else Icons.Outlined.Edit,
                        contentDescription = "Edit Accounts",
                        tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-row: Set Primary Account
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Set Primary Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Dropdown trigger chip
                Box {
                    Surface(
                        onClick = { showPrimaryMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD6E2FB),
                        modifier = Modifier.testTag("set_primary_account_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = primaryAccount,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E3A8A)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF1E3A8A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showPrimaryMenu,
                        onDismissRequest = { showPrimaryMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "First Select",
                                    fontWeight = if (primaryAccount == "First Select") FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onPrimaryAccountChange("First Select")
                                showPrimaryMenu = false
                            }
                        )
                        pinnedAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = account,
                                        fontWeight = if (primaryAccount == account) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onPrimaryAccountChange(account)
                                    showPrimaryMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Pinned Accounts List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (pinnedAccounts.isEmpty()) {
                    item {
                        Text(
                            text = "No accounts pinned. Tap '+' below to add accounts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    itemsIndexed(
                        items = pinnedAccounts,
                        key = { _, account -> account }
                    ) { index, account ->
                        val pinColor = PIN_COLORS[index % PIN_COLORS.size]
                        val isPrimary = primaryAccount == account || (primaryAccount == "First Select" && index == 0)

                        AccountRowItem(
                            accountName = account,
                            pinColor = pinColor,
                            isPrimary = isPrimary,
                            isEditMode = isEditMode,
                            isFirst = index == 0,
                            isLast = index == pinnedAccounts.lastIndex,
                            onMoveUp = { onMoveUp(index) },
                            onMoveDown = { onMoveDown(index) },
                            onRemove = { onRemoveAccount(account) },
                            onTogglePin = { onTogglePin(account) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Account Button Box (matching screenshot '+' box)
            Surface(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_account_button")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Account",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        var newAccountText by remember { mutableStateOf("") }
        val unpinned = availableAccounts.filterNot { it in pinnedAccounts }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Add Account",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (unpinned.isNotEmpty()) {
                        Text(
                            text = "Detected Accounts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        unpinned.forEach { acc ->
                            Surface(
                                onClick = {
                                    onAddAccount(acc)
                                    showAddDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = acc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = "Or enter custom name:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newAccountText,
                        onValueChange = { newAccountText = it },
                        placeholder = { Text("e.g. Axis, SBI, Cash") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newAccountText.isNotBlank()) {
                            onAddAccount(newAccountText.trim())
                            showAddDialog = false
                        }
                    },
                    enabled = newAccountText.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AccountRowItem(
    accountName: String,
    pinColor: Color,
    isPrimary: Boolean,
    isEditMode: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onTogglePin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!isEditMode) onTogglePin()
            }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pin icon in colored container or tinted directly
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(pinColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pinned Account",
                tint = pinColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = accountName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (isPrimary && !isEditMode) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "Primary",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        AnimatedVisibility(visible = isEditMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isFirst) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (!isLast) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove Account",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
