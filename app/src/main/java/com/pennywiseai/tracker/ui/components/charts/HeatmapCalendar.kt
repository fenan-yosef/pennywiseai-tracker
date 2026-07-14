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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * A calendar heatmap showing daily spending intensity.
 * Each cell = one day, colored from green (low spending) through yellow to red (high spending).
 */
@Composable
fun SpendingHeatmap(
    dailyData: Map<LocalDate, BigDecimal>,
    yearMonth: YearMonth,
    currency: String,
    modifier: Modifier = Modifier
) {
    val maxAmount = remember(dailyData) {
        dailyData.values.maxOrNull() ?: BigDecimal.ZERO
    }
    val daysInMonth = remember(yearMonth) { yearMonth.lengthOfMonth() }
    val firstDayOfMonth = remember(yearMonth) { yearMonth.atDay(1).dayOfWeek.value % 7 } // 0=Sun..6=Sat

    val cellSize = 48f
    val cellPadding = 4f
    val labelWidth = 32f

    val dayNames = remember {
        DayOfWeek.entries.map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }

    val totalSpending = remember(dailyData) {
        dailyData.values.fold(BigDecimal.ZERO) { acc, v -> acc + v }
    }

    val topDay = remember(dailyData) {
        dailyData.maxByOrNull { it.value }
    }

    val today = LocalDate.now()

    PennyWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + yearMonth.year,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (totalSpending > BigDecimal.ZERO) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Total: ${CurrencyFormatter.formatCurrency(totalSpending, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                topDay?.let { (date, amount) ->
                    Text(
                        text = "Highest: ${CurrencyFormatter.formatCurrency(amount, currency)} on ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Heatmap grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(cellPadding.dp)
            ) {
                // Day headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Spacer(Modifier.width(labelWidth.dp))
                    dayNames.forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(cellSize.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Week rows (up to 6)
                for (week in 0..5) {
                    val startCol = if (week == 0) firstDayOfMonth else 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Week label
                        Text(
                            text = "W${week + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(labelWidth.dp)
                        )

                        // Day cells
                        for (col in 0..6) {
                            val dayOfMonth = week * 7 + col - firstDayOfMonth + 1
                            if (dayOfMonth in 1..daysInMonth) {
                                val date = yearMonth.atDay(dayOfMonth)
                                val amount = dailyData[date] ?: BigDecimal.ZERO
                                val isToday = date == today

                                HeatmapCell(
                                    amount = amount,
                                    maxAmount = maxAmount,
                                    isToday = isToday,
                                    modifier = Modifier.size(cellSize.dp)
                                )
                            } else {
                                Spacer(Modifier.size(cellSize.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = Color(0xFF22C55E))
                }
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = Color(0xFFEAB308))
                }
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = Color(0xFFEF4444))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    amount: BigDecimal,
    maxAmount: BigDecimal,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val intensity = if (maxAmount > BigDecimal.ZERO)
        (amount / maxAmount).toFloat().coerceIn(0f, 1f)
    else 0f

    val bgColor = if (amount == BigDecimal.ZERO) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        // Green → Yellow → Red gradient based on intensity
        when {
            intensity < 0.33f -> lerpColor(Color(0xFF22C55E), Color(0xFFEAB308), intensity / 0.33f)
            intensity < 0.66f -> lerpColor(Color(0xFFEAB308), Color(0xFFF97316), (intensity - 0.33f) / 0.33f)
            else -> lerpColor(Color(0xFFF97316), Color(0xFFEF4444), (intensity - 0.66f) / 0.34f)
        }
    }

    val borderColor = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderAlpha = if (isToday) 1f else 0f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellRect = size.minDimension
            val padding = 1.dp.toPx()

            // Cell background
            drawRoundRect(
                color = bgColor,
                topLeft = Offset(padding, padding),
                size = Size(cellRect - padding * 2, cellRect - padding * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )

            // Today border
            if (isToday) {
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(padding, padding),
                    size = Size(cellRect - padding * 2, cellRect - padding * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                )
            }
        }

        if (amount > BigDecimal.ZERO) {
            Text(
                text = amount.toInt().toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * f,
        green = from.green + (to.green - from.green) * f,
        blue = from.blue + (to.blue - from.blue) * f,
        alpha = 0.85f
    )
}
