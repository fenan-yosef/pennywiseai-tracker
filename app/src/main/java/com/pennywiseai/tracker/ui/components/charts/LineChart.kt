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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.screens.analytics.TrendPoint
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

/**
 * A multi-series line chart showing cumulative spending trend over time.
 * Uses custom Canvas for full control over multi-series rendering.
 */
@Composable
fun TrendLineChart(
    series: List<TrendSeries>,
    currency: String,
    modifier: Modifier = Modifier,
    title: String = "Spending Trend",
    height: Int = 180
) {
    if (series.isEmpty() || series.first().points.size < 2) return

    val xLabels = remember(series) { series.first().points.map { it.label } }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    PennyWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.sm))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                series.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = s.color)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = s.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))

            // Y-axis labels
            val allValues = series.flatMap { s -> s.points.map { it.amount.toFloat() } }
            val minVal = allValues.minOrNull() ?: 0f
            val maxVal = allValues.maxOrNull() ?: 1f
            val range = (maxVal - minVal).coerceAtLeast(1f)
            val padding = range * 0.1f
            val paddedMin = minVal - padding
            val paddedRange = range + padding * 2

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Y-axis
                Column(
                    modifier = Modifier.width(48.dp).padding(end = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = CurrencyFormatter.formatCurrency(paddedMin.toBigDecimal(), currency),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency((paddedMin + paddedRange).toBigDecimal(), currency),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Chart area
                Canvas(
                    modifier = Modifier.weight(1f).height(height.dp)
                ) {
                    val canvasW = size.width
                    val canvasH = size.height
                    val pointCount = xLabels.size.coerceAtLeast(2)

                    series.forEach { s ->
                        val points = s.points
                        if (points.size < 2) return@forEach

                        val path = Path()
                        val pts = mutableListOf<Offset>()

                        points.forEachIndexed { idx, pt ->
                            val x = canvasW * (idx.toFloat() / (pointCount - 1).coerceAtLeast(1))
                            val y = canvasH * (1f - ((pt.amount.toFloat() - paddedMin) / paddedRange))
                            val offset = Offset(x, y.coerceIn(0f, canvasH))
                            pts.add(offset)

                            if (idx == 0) path.moveTo(x, y.coerceIn(0f, canvasH))
                            else path.lineTo(x, y.coerceIn(0f, canvasH))
                        }

                        // Gradient fill
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(canvasW, canvasH)
                            lineTo(0f, canvasH)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(s.color.copy(alpha = 0.2f), s.color.copy(alpha = 0.02f)),
                                startY = 0f, endY = canvasH
                            )
                        )

                        // Line
                        drawPath(
                            path = path,
                            color = s.color,
                            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Points
                        pts.forEach { pt ->
                            drawCircle(color = surfaceColor, radius = 3.dp.toPx(), center = pt)
                            drawCircle(color = s.color, radius = 3.dp.toPx(), center = pt, style = Stroke(1.5.dp.toPx()))
                        }
                    }
                }
            }

            // X-axis labels
            val labelStep = (xLabels.size / 4).coerceAtLeast(1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                xLabels.filterIndexed { idx, _ -> idx % labelStep == 0 || idx == xLabels.lastIndex }
                    .forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
            }
        }
    }
}

data class TrendSeries(
    val name: String,
    val color: androidx.compose.ui.graphics.Color,
    val points: List<TrendPoint>
)
