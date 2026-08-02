package com.pennywiseai.tracker.ui.components.charts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.screens.analytics.MonthlySpending
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter

@Composable
fun SixMonthTrendCard(
    monthlyData: List<MonthlySpending>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (monthlyData.isEmpty()) return

    val maxAmount = monthlyData.maxOf { it.amount.toFloat() }.coerceAtLeast(1f)

    PennyWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Last 6 Months",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Monthly spending overview",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.md))

            monthlyData.forEach { month ->
                val fraction = (month.amount.toFloat() / maxAmount).coerceIn(0f, 1f)
                SixMonthBar(
                    label = month.yearMonth.format(DateTimeFormatter.ofPattern("MMM yy")),
                    amount = month.amount,
                    income = month.income,
                    expenses = month.expenses,
                    fraction = fraction,
                    transactionCount = month.transactionCount,
                    currency = currency
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }
    }
}

@Composable
private fun SixMonthBar(
    label: String,
    amount: java.math.BigDecimal,
    income: java.math.BigDecimal,
    expenses: java.math.BigDecimal,
    fraction: Float,
    transactionCount: Int,
    currency: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(60.dp)
                )
                Spacer(Modifier.width(8.dp))
                // Net indicator
                val net = income - expenses
                val isPositive = net >= java.math.BigDecimal.ZERO
                Text(
                    text = if (isPositive) "↑" else "↓",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${transactionCount}tx",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyFormatter.formatCurrency(amount, currency),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(2.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        // Income/Expense mini bars
        if (income > java.math.BigDecimal.ZERO || expenses > java.math.BigDecimal.ZERO) {
            Spacer(Modifier.height(2.dp))
            val total = income + expenses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (expenses > java.math.BigDecimal.ZERO) {
                    val expFrac = (expenses / total).toFloat().coerceIn(0f, 1f)
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { expFrac },
                        modifier = Modifier.weight(1f).height(4.dp),
                        color = MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                    )
                }
                if (income > java.math.BigDecimal.ZERO) {
                    val incFrac = (income / total).toFloat().coerceIn(0f, 1f)
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { incFrac },
                        modifier = Modifier.weight(1f).height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                    )
                }
            }
        }
    }
}
