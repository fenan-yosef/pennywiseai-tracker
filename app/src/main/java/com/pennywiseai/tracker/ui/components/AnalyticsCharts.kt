package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.DayOfWeek
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun DonutChart(
    categories: List<com.pennywiseai.tracker.ui.screens.analytics.CategoryData>,
    currency: String,
    modifier: Modifier = Modifier,
    onCategoryClick: (String) -> Unit = {}
) {
    if (categories.isEmpty()) return

    val total = remember(categories) {
        categories.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
    }

    var activeIndex by remember { mutableStateOf(-1) }
    
    // Scale and angle ranges
    val slices = remember(categories) {
        var startAngle = 0f
        categories.map { category ->
            val sweep = if (total > BigDecimal.ZERO) {
                (category.amount.toDouble() / total.toDouble() * 360.0).toFloat()
            } else 0f
            val slice = Slice(
                categoryName = category.name,
                amount = category.amount,
                startAngle = startAngle,
                sweepAngle = sweep,
                color = CategoryMapping.categories[category.name]?.color ?: Color.Gray
            )
            startAngle += sweep
            slice
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(slices) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val xDiff = offset.x - centerX
                        val yDiff = offset.y - centerY
                        val distance = sqrt(xDiff * xDiff + yDiff * yDiff)
                        
                        // Check if tap falls in the active arc range (outer radius ~100dp, inner ~70dp)
                        val outerRadius = size.width / 2f
                        val innerRadius = outerRadius * 0.7f
                        
                        if (distance in innerRadius..outerRadius) {
                            var angle = Math.toDegrees(atan2(yDiff.toDouble(), xDiff.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            
                            val clickedSliceIdx = slices.indexOfFirst { slice ->
                                val endAngle = slice.startAngle + slice.sweepAngle
                                if (slice.startAngle <= angle && angle <= endAngle) {
                                    true
                                } else if (endAngle > 360f && angle <= (endAngle % 360f)) {
                                    true
                                } else {
                                    false
                                }
                            }
                            
                            if (clickedSliceIdx != -1) {
                                activeIndex = if (activeIndex == clickedSliceIdx) -1 else clickedSliceIdx
                                if (activeIndex != -1) {
                                    onCategoryClick(slices[activeIndex].categoryName)
                                }
                            }
                        } else {
                            activeIndex = -1
                        }
                    }
                }
        ) {
            val strokeWidth = 24.dp.toPx()
            val canvasSize = size.width
            val outerRadius = canvasSize / 2f
            val arcSize = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)
            val arcOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

            slices.forEachIndexed { index, slice ->
                val isSelected = index == activeIndex
                val extraStroke = if (isSelected) 8.dp.toPx() else 0f
                drawArc(
                    color = slice.color,
                    startAngle = slice.startAngle,
                    sweepAngle = slice.sweepAngle,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(
                        width = strokeWidth + extraStroke,
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // Center Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            if (activeIndex != -1 && activeIndex < slices.size) {
                val slice = slices[activeIndex]
                Text(
                    text = slice.categoryName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(slice.amount, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Total Spending",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(total, currency),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

data class Slice(
    val categoryName: String,
    val amount: BigDecimal,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color
)

@Composable
fun CashFlowBarChart(
    income: BigDecimal,
    expense: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier
) {
    val total = income + expense
    val incomePercent = if (total > BigDecimal.ZERO) (income.toDouble() / total.toDouble()).toFloat() else 0.5f
    val expensePercent = if (total > BigDecimal.ZERO) (expense.toDouble() / total.toDouble()).toFloat() else 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Cash Flow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Income vs spending comparison",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Net savings indicator
            val netSavings = income - expense
            val color = if (netSavings >= BigDecimal.ZERO) Color(0xFF2ECC71) else Color(0xFFE74C3C)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "${if (netSavings >= BigDecimal.ZERO) "+" else ""}${CurrencyFormatter.formatCurrency(netSavings, currency)}",
                    color = color,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            if (incomePercent > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(incomePercent.coerceAtLeast(0.01f))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2ECC71), Color(0xFF27AE60))
                            ),
                            shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                        )
                )
            }
            if (expensePercent > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(expensePercent.coerceAtLeast(0.01f))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFE74C3C), Color(0xFFC0392B))
                            ),
                            shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legends
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF2ECC71), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = CurrencyFormatter.formatCurrency(income, currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFE74C3C), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Spending", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = CurrencyFormatter.formatCurrency(expense, currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun DayOfWeekHeatmap(
    distribution: Map<DayOfWeek, BigDecimal>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(distribution) {
        distribution.values.maxOfOrNull { it.toDouble() } ?: 1.0
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.sm)
    ) {
        Text(
            text = "Weekly Activity Pattern",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Intensity of spending by day",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DayOfWeek.values().forEach { day ->
                val amount = distribution[day] ?: BigDecimal.ZERO
                val ratio = (amount.toDouble() / maxVal).toFloat()
                
                val baseColor = MaterialTheme.colorScheme.primary
                val intensityColor = baseColor.copy(alpha = ratio.coerceIn(0.1f, 1f))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = intensityColor,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.name.take(1),
                            color = if (ratio > 0.5f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (amount > BigDecimal.ZERO) {
                            CurrencyFormatter.formatCurrency(amount, "").substringBefore(".")
                        } else {
                            "-"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionScaleBar(
    scaleCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val total = remember(scaleCounts) {
        scaleCounts.values.sum().toFloat().coerceAtLeast(1f)
    }

    val smallPercent = (scaleCounts["Small"] ?: 0) / total
    val mediumPercent = (scaleCounts["Medium"] ?: 0) / total
    val largePercent = (scaleCounts["Large"] ?: 0) / total

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.sm)
    ) {
        Text(
            text = "Spending scale distribution",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))
        ) {
            if (smallPercent > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(smallPercent)
                        .background(Color(0xFF3498DB), RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp))
                )
            }
            if (mediumPercent > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(mediumPercent)
                        .background(Color(0xFFF1C40F))
                )
            }
            if (largePercent > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(largePercent)
                        .background(Color(0xFFE74C3C), RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF3498DB), RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Small txns (${(smallPercent * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFF1C40F), RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Medium (${(mediumPercent * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFE74C3C), RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Large (${(largePercent * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
