package com.pennywiseai.tracker.ui.components.insights

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.screens.analytics.WeekdayDistribution
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun WeekdayPatternCard(
    weekdayDistribution: List<WeekdayDistribution>,
    weekdayTotal: BigDecimal,
    weekendTotal: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (weekdayDistribution.isEmpty()) return

    val total = weekdayTotal + weekendTotal
    val weekdayPct = if (total > BigDecimal.ZERO)
        (weekdayTotal / total * BigDecimal(100)).toFloat() else 0f

    PennyWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Weekday vs Weekend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.sm))

            // Weekday vs Weekend summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Weekdays",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(weekdayTotal, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Nightlight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Weekends",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(weekendTotal, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Day-by-day bars
            val maxAmount = weekdayDistribution.maxOf { it.amount.toFloat() }.coerceAtLeast(1f)

            weekdayDistribution.forEach { dist ->
                val fraction = (dist.amount.toFloat() / maxAmount).coerceIn(0f, 1f)
                WeekdayBar(
                    day = dist.dayOfWeek.name.take(3),
                    amount = dist.amount,
                    percentage = dist.percentage,
                    fraction = fraction,
                    currency = currency
                )
                Spacer(Modifier.height(2.dp))
            }

            // Insight text
            Spacer(Modifier.height(Spacing.sm))
            val higherPeriod = if (weekdayPct > 55f) "weekdays" else "weekends"
            Text(
                text = "You spend ${"%.0f".format(if (weekdayPct > 55f) weekdayPct else 100f - weekdayPct)}% of your money on $higherPeriod",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeekdayBar(
    day: String,
    amount: BigDecimal,
    percentage: Float,
    fraction: Float,
    currency: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(28.dp)
        )
        androidx.compose.material3.LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(30.dp)
        )
        Text(
            text = CurrencyFormatter.formatCurrency(amount, currency),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(70.dp)
        )
    }
}
