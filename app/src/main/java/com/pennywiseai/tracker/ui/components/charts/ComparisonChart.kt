package com.pennywiseai.tracker.ui.components.charts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

data class ComparisonItem(
    val label: String,
    val current: BigDecimal,
    val previous: BigDecimal,
    val currentLabel: String = "Current",
    val previousLabel: String = "Previous"
)

@Composable
fun PeriodComparisonCard(
    comparisons: List<ComparisonItem>,
    currency: String,
    modifier: Modifier = Modifier,
    title: String = "Period Comparison"
) {
    if (comparisons.isEmpty()) return

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
                LegendDot(color = MaterialTheme.colorScheme.primary, label = comparisons.firstOrNull()?.currentLabel ?: "Current")
                LegendDot(color = MaterialTheme.colorScheme.surfaceVariant, label = comparisons.firstOrNull()?.previousLabel ?: "Previous")
            }

            Spacer(Modifier.height(Spacing.sm))

            comparisons.forEach { item ->
                val currentVal = item.current.toFloat().coerceAtLeast(0f)
                val previousVal = item.previous.toFloat().coerceAtLeast(0f)
                val maxVal = currentVal.coerceAtLeast(previousVal).coerceAtLeast(1f)

                ComparisonRow(
                    label = item.label,
                    current = item.current,
                    previous = item.previous,
                    currentFraction = currentVal / maxVal,
                    previousFraction = previousVal / maxVal,
                    currency = currency
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(0.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = color)
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    current: BigDecimal,
    previous: BigDecimal,
    currentFraction: Float,
    previousFraction: Float,
    currency: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))

        // Current period bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { currentFraction.coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f).height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = CurrencyFormatter.formatCurrency(current, currency),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Previous period bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { previousFraction.coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f).height(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            )
            Text(
                text = CurrencyFormatter.formatCurrency(previous, currency),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Change indicator
        val change = current - previous
        val changePercent = if (previous > BigDecimal.ZERO)
            (change / previous * BigDecimal(100)).toFloat()
        else 0f
        val isPositive = change >= BigDecimal.ZERO

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isPositive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${if (isPositive) "+" else ""}${CurrencyFormatter.formatCurrency(change, currency)} (${
                    if (isPositive) "+" else ""
                }${"%.1f".format(changePercent)}%)",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPositive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
