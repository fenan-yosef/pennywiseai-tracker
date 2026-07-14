package com.pennywiseai.tracker.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.screens.analytics.CategoryData
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter

private val donutColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFFEC4899), // Pink
    Color(0xFF14B8A6), // Teal
    Color(0xFFF97316), // Orange
    Color(0xFF8B5CF6), // Violet
    Color(0xFF06B6D4), // Cyan
    Color(0xFFEF4444), // Red
    Color(0xFF22C55E), // Green
    Color(0xFFEAB308), // Yellow
    Color(0xFFD946EF), // Fuchsia
    Color(0xFF64748B), // Slate
    Color(0xFF84CC16), // Lime
)

@Composable
fun SpendingDonutChart(
    categories: List<CategoryData>,
    currency: String,
    modifier: Modifier = Modifier,
    onCategoryClick: ((CategoryData) -> Unit)? = null
) {
    if (categories.isEmpty()) return

    PennyWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut chart
                DonutCanvas(
                    categories = categories,
                    modifier = Modifier.size(160.dp)
                )

                Spacer(Modifier.width(Spacing.md))

                // Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.take(8).forEachIndexed { index, cat ->
                        DonutLegendItem(
                            color = donutColors[index % donutColors.size],
                            name = cat.name,
                            percentage = cat.percentage,
                            amount = CurrencyFormatter.formatCurrency(cat.amount, currency)
                        )
                    }
                    if (categories.size > 8) {
                        Text(
                            text = "+${categories.size - 8} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutCanvas(
    categories: List<CategoryData>,
    modifier: Modifier = Modifier
) {
    val total = remember(categories) {
        categories.sumOf { it.amount.toDouble() }.toFloat()
    }
    if (total == 0f) return

    val sorted = remember(categories) { categories.sortedByDescending { it.amount } }

    Canvas(modifier = modifier) {
        val thickness = size.minDimension * 0.22f
        val outerRadius = (size.minDimension - thickness) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        var startAngle = -90f

        sorted.forEachIndexed { index, cat ->
            val sweepAngle = (cat.amount.toFloat() / total) * 360f
            val color = donutColors[index % donutColors.size]

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = thickness, cap = StrokeCap.Butt)
            )

            startAngle += sweepAngle
        }
    }
}

@Composable
private fun DonutLegendItem(
    color: Color,
    name: String,
    percentage: Float,
    amount: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
