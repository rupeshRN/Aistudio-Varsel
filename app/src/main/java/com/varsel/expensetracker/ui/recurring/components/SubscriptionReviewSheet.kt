package com.varsel.expensetracker.ui.recurring.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCardOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.ui.recurring.model.SubscriptionReviewItem
import com.varsel.expensetracker.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionReviewSheet(
    reviewItems: List<SubscriptionReviewItem>,
    onDismiss: () -> Unit,
    onCatchUpItem: (RecurringItem) -> Unit,
    onEditItem: (RecurringItem) -> Unit,
    onConfirmPriceHike: (RecurringItem, Double) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Review Subscriptions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smart audit from schedules & imported statement history",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (reviewItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "All Subscriptions Look Healthy!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No overdue cycles, unexpected price changes, or unlinked accounts found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "${reviewItems.size} item(s) need attention",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(reviewItems) { item ->
                        when (item) {
                            is SubscriptionReviewItem.OverdueMissedCycles -> {
                                ReviewCard(
                                    title = item.item.title,
                                    badgeText = "${item.missedCount} MISSED CYCLES",
                                    badgeColor = MaterialTheme.colorScheme.errorContainer,
                                    badgeTextColor = MaterialTheme.colorScheme.onErrorContainer,
                                    icon = Icons.Default.WarningAmber,
                                    description = "Oldest missed since ${item.oldestMissedDateFormatted}. Catch up or fast forward.",
                                    actionText = "Review Missed",
                                    onAction = { onCatchUpItem(item.item) }
                                )
                            }
                            is SubscriptionReviewItem.PriceHike -> {
                                ReviewCard(
                                    title = item.item.title,
                                    badgeText = "+${CurrencyFormatter.format(item.diff)} PRICE HIKE",
                                    badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    badgeTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                                    description = "Latest statement charge was ${CurrencyFormatter.format(item.newAmount)} (configured was ${CurrencyFormatter.format(item.oldAmount)}).",
                                    actionText = "Accept New Price",
                                    onAction = { onConfirmPriceHike(item.item, item.newAmount) }
                                )
                            }
                            is SubscriptionReviewItem.NoRecentConfirmation -> {
                                ReviewCard(
                                    title = item.item.title,
                                    badgeText = "NO RECENT CONFIRMATION",
                                    badgeColor = MaterialTheme.colorScheme.surfaceVariant,
                                    badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    icon = Icons.Outlined.HourglassEmpty,
                                    description = item.message,
                                    actionText = "Inspect",
                                    onAction = { onEditItem(item.item) }
                                )
                            }
                            is SubscriptionReviewItem.MissingAccountLink -> {
                                ReviewCard(
                                    title = item.item.title,
                                    badgeText = "NO LINKED ACCOUNT",
                                    badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                                    badgeTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    icon = Icons.Outlined.CreditCardOff,
                                    description = item.message,
                                    actionText = "Link Account",
                                    onAction = { onEditItem(item.item) }
                                )
                            }
                            is SubscriptionReviewItem.InactiveItem -> {
                                ReviewCard(
                                    title = item.item.title,
                                    badgeText = "INACTIVE",
                                    badgeColor = MaterialTheme.colorScheme.surfaceVariant,
                                    badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    icon = Icons.Outlined.History,
                                    description = "Subscription is paused or has reached its end date.",
                                    actionText = "Resume / Edit",
                                    onAction = { onEditItem(item.item) }
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
private fun ReviewCard(
    title: String,
    badgeText: String,
    badgeColor: Color,
    badgeTextColor: Color,
    icon: ImageVector,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    SubscriptionBrandBadge(
                        title = title,
                        category = "Entertainment",
                        categoryColorHex = null,
                        size = 28.dp,
                        shapeRadius = 6.dp
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
