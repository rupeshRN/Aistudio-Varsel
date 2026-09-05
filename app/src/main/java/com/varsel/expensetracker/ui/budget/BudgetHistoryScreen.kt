package com.varsel.expensetracker.ui.budget

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.budget.components.BudgetSplineChart
import com.varsel.expensetracker.ui.budget.model.BudgetPastPeriodUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetHistoryScreen(
    budgetId: Long,
    viewModel: BudgetViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val historyFlow = remember(budgetId) { viewModel.getBudgetHistory(budgetId) }
    val historyModel by historyFlow.collectAsState()

    val currentModel = historyModel

    if (currentModel == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val accentColor = try {
        Color(android.graphics.Color.parseColor(currentModel.categoryColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("budget_history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header (Cashew Screenshot 3 style)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentModel.budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // History Spline Curve Chart (Cashew Screenshot 3 style)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        BudgetSplineChart(
                            trendPoints = currentModel.trendPoints,
                            lineColor = accentColor
                        )
                    }
                }
            }

            // Category Summary Row with Average & Total (Cashew Screenshot 3 style)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = currentModel.categoryIcon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentModel.budget.categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${BudgetCalculator.formatCurrency(currentModel.categoryAverageSpent, round = true)} average spent",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = BudgetCalculator.formatCurrency(currentModel.totalAllTimeSpent, round = true),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Past Periods Header
            item {
                Text(
                    text = "Past Periods",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Past Periods List (Cashew Screenshot 3: Current Period, August 1, July 1, etc.)
            items(currentModel.pastPeriods, key = { it.periodTitle }) { pastPeriod ->
                PastPeriodCard(
                    period = pastPeriod,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
fun PastPeriodCard(
    period: BudgetPastPeriodUiModel,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = period.periodTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                val subtitle = if (period.isOverBudget) {
                    "${BudgetCalculator.formatCurrency(period.amountSpent - period.budgetLimit, round = true)} over of ${BudgetCalculator.formatCurrency(period.budgetLimit, round = true)}"
                } else {
                    "${BudgetCalculator.formatCurrency(period.amountLeft, round = true)} left of ${BudgetCalculator.formatCurrency(period.budgetLimit, round = true)}"
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (period.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Circular Mini Progress Ring with percentage in center (Cashew Screenshot 3)
            MiniCircularProgressRing(
                percent = period.percentSpent,
                ratio = period.spentRatio,
                color = if (period.isOverBudget) MaterialTheme.colorScheme.error else accentColor
            )
        }
    }
}

@Composable
fun MiniCircularProgressRing(
    percent: Int,
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        started = true
    }

    val animatedRatio by animateFloatAsState(
        targetValue = if (started) ratio.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "miniRing"
    )

    Box(
        modifier = modifier.size(46.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            // Background ring track
            drawArc(
                color = Color.LightGray.copy(alpha = 0.35f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            if (animatedRatio > 0.005f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = animatedRatio * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
