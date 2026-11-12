package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.domain.model.quality.QualitySeverity
import com.varsel.expensetracker.domain.model.quality.QualitySummaryUiModel

@Composable
fun DataQualityPill(
    summary: QualitySummaryUiModel?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (summary == null) return

    val isHealthy = !summary.hasIssues
    val containerColor = if (isHealthy) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    } else when (summary.severity) {
        QualitySeverity.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        QualitySeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        QualitySeverity.INFO -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    val contentColor = if (isHealthy) {
        MaterialTheme.colorScheme.secondary
    } else when (summary.severity) {
        QualitySeverity.ERROR -> MaterialTheme.colorScheme.error
        QualitySeverity.WARNING -> MaterialTheme.colorScheme.tertiary
        QualitySeverity.INFO -> MaterialTheme.colorScheme.primary
    }

    val borderColor = contentColor.copy(alpha = 0.25f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("dashboard_quality_pill"),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Score badge / Icon
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isHealthy) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Score pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = contentColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "${summary.healthScore}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = summary.headline,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = "Inspect Data Quality",
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
