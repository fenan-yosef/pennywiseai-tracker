package com.pennywiseai.tracker.ui.components.charts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DailyBarChart(
    dailyData: List<DailyBarItem>,
    currency: String,
    modifier: Modifier = Modifier,
    title: String = "Daily Spending"
) {
    if (dailyData.size < 2) return

    PennyWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.sm))

            val maxAmount = dailyData.maxOf { it.amount.toFloat() }.coerceAtLeast(1f)

            // Bar chart
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                dailyData.forEach { item ->
                    val fraction = (item.amount.toFloat() / maxAmount).coerceIn(0f, 1f)
                    DailyBarRow(
                        label = item.label,
                        amount = item.amount,
                        fraction = fraction,
                        currency = currency
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyBarRow(
    label: String,
    amount: BigDecimal,
    fraction: Float,
    currency: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyFormatter.formatCurrency(amount, currency),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(1.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

data class DailyBarItem(
    val label: String,
    val amount: BigDecimal,
    val date: LocalDate? = null
)

// ==================== Monthly Bar Chart ====================

@Composable
fun MonthlyBarChart(
    monthlyData: List<MonthlyBarItem>,
    currency: String,
    modifier: Modifier = Modifier,
    title: String = "Monthly Spending"
) {
    if (monthlyData.size < 2) return

    PennyWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(Spacing.sm))

            val maxAmount = monthlyData.maxOf { it.amount.toFloat() }.coerceAtLeast(1f)

            monthlyData.forEach { item ->
                val fraction = (item.amount.toFloat() / maxAmount).coerceIn(0f, 1f)
                MonthlyBarRow(
                    label = item.label,
                    amount = item.amount,
                    fraction = fraction,
                    currency = currency,
                    income = item.income,
                    expenses = item.expenses
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }
    }
}

@Composable
private fun MonthlyBarRow(
    label: String,
    amount: BigDecimal,
    fraction: Float,
    currency: String,
    income: BigDecimal = BigDecimal.ZERO,
    expenses: BigDecimal = BigDecimal.ZERO
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = CurrencyFormatter.formatCurrency(amount, currency),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Expense bar
        val expFraction = if (expenses > BigDecimal.ZERO && (expenses + income) > BigDecimal.ZERO)
            (expenses / (expenses + income)).toFloat() else 0f

        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expense
            androidx.compose.material3.LinearProgressIndicator(
                progress = { expFraction.coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f).height(4.dp),
                color = MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            if (expenses > BigDecimal.ZERO) {
                Text(
                    text = CurrencyFormatter.formatCurrency(expenses, currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (income > BigDecimal.ZERO) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val incFraction = if (income > BigDecimal.ZERO && (expenses + income) > BigDecimal.ZERO)
                    (income / (expenses + income)).toFloat() else 0f
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { incFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(income, currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

data class MonthlyBarItem(
    val label: String,
    val amount: BigDecimal,
    val income: BigDecimal = BigDecimal.ZERO,
    val expenses: BigDecimal = BigDecimal.ZERO,
    val yearMonth: YearMonth? = null
)
