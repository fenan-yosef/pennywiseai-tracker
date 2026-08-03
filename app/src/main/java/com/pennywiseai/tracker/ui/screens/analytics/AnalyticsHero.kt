package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import kotlin.math.abs

@Composable
fun AnalyticsHero(
    state: AnalyticsUiState,
    selectedPeriod: TimePeriod,
    onPeriodTotalClick: () -> Unit = {},
    onTodayClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currency = state.currency
    val changePercent = state.spendingChangePercent
    val showProjection = selectedPeriod == TimePeriod.THIS_MONTH &&
        state.projectedSpending > BigDecimal.ZERO

    PennyWiseCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                HeroMetric(
                    label = "Today",
                    value = CurrencyFormatter.formatCurrency(state.todayAmount, currency),
                    subtitle = dailyAvgSubtitle(state),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onTodayClick)
                )
                HeroMetric(
                    label = periodTotalLabel(selectedPeriod),
                    value = CurrencyFormatter.formatCurrency(state.periodTotal, currency),
                    subtitle = changeSubtitle(changePercent),
                    subtitlePositiveIsGood = false,
                    changePercent = changePercent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onPeriodTotalClick)
                )
            }

            if (showProjection || state.pacePercent != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    if (showProjection) {
                        HeroMetric(
                            label = "Projected",
                            value = CurrencyFormatter.formatCurrency(state.projectedSpending, currency),
                            subtitle = "Month-end estimate",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.pacePercent?.let { pace ->
                        HeroMetric(
                            label = "Pace",
                            value = paceValueLabel(pace),
                            subtitle = paceSubtitle(pace),
                            subtitlePositiveIsGood = false,
                            changePercent = pace,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    subtitlePositiveIsGood: Boolean = true,
    changePercent: Float? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            val tint = when {
                changePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                abs(changePercent) < 1f -> MaterialTheme.colorScheme.onSurfaceVariant
                (changePercent > 0) == subtitlePositiveIsGood -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun periodTotalLabel(period: TimePeriod): String = when (period) {
    TimePeriod.TODAY -> "Today total"
    TimePeriod.YESTERDAY -> "Yesterday"
    TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK -> "Week total"
    TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH -> "Month total"
    else -> "Period total"
}

private fun dailyAvgSubtitle(state: AnalyticsUiState): String {
    val avg = state.insights.dailyAvg
    return if (avg > BigDecimal.ZERO) {
        "Avg ${CurrencyFormatter.formatCurrency(avg, state.currency)}/day"
    } else {
        "No spend yet"
    }
}

private fun changeSubtitle(changePercent: Float?): String? {
    if (changePercent == null) return null
    val abs = abs(changePercent)
    val arrow = when {
        changePercent > 0.5f -> "↑"
        changePercent < -0.5f -> "↓"
        else -> "→"
    }
    return "$arrow ${"%.0f".format(abs)}% vs prior"
}

private fun paceValueLabel(pacePercent: Float): String {
    val abs = abs(pacePercent)
    return when {
        abs < 5f -> "On track"
        pacePercent > 0 -> "+${"%.0f".format(abs)}%"
        else -> "-${"%.0f".format(abs)}%"
    }
}

private fun paceSubtitle(pacePercent: Float): String = when {
    abs(pacePercent) < 5f -> "Matching expected pace"
    pacePercent > 0 -> "Ahead of pace"
    else -> "Behind pace"
}
