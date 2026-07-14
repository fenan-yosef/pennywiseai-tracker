package com.pennywiseai.tracker.ui.components.insights

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.screens.analytics.CategoryTrend
import com.pennywiseai.tracker.ui.screens.analytics.TrendDirection
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun CategoryTrendsCard(
    trends: List<CategoryTrend>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) return

    PennyWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Category Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.sm))

            trends.forEach { trend ->
                val lastAmount = trend.monthlyAmounts.values.lastOrNull() ?: BigDecimal.ZERO
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trend.category,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Trend indicator
                    when (trend.trend) {
                        TrendDirection.RISING -> Icon(
                            Icons.Default.TrendingUp, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        TrendDirection.FALLING -> Icon(
                            Icons.Default.TrendingDown, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        TrendDirection.STABLE -> Icon(
                            Icons.Default.TrendingFlat, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = CurrencyFormatter.formatCurrency(lastAmount, currency),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (trend.trend != TrendDirection.STABLE) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${if (trend.changePercent > 0) "+" else ""}${"%.1f".format(trend.changePercent)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (trend.trend == TrendDirection.RISING) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (trend != trends.last()) {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
