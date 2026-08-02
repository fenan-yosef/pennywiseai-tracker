package com.pennywiseai.tracker.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.screens.analytics.HeatmapDay
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.theme.expense_dark
import com.pennywiseai.tracker.ui.theme.expense_light
import com.pennywiseai.tracker.ui.theme.income_dark
import com.pennywiseai.tracker.ui.theme.income_light
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * GitHub-style activity heatmap for the last ~6 months.
 * Cells show expense / income / both (diagonal split). Tap opens a day breakdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHeatmap(
    dailyData: Map<LocalDate, HeatmapDay>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val rangeStart = remember(today) {
        today.minusMonths(5).withDayOfMonth(1)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val weeks = remember(rangeStart, today) {
        buildWeekColumns(rangeStart, today)
    }
    val monthLabels = remember(weeks) {
        buildMonthLabels(weeks)
    }

    val expenseColor = if (!isSystemInDarkTheme()) expense_light else expense_dark
    val incomeColor = if (!isSystemInDarkTheme()) income_light else income_dark
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val weekdayLabels = remember {
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.FRIDAY
        ).associateWith { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
    }

    PennyWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Last 6 months",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.sm))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Weekday gutter (Mon–Sun rows; labels on M/W/F)
                Column(
                    modifier = Modifier
                        .width(14.dp)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY,
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                    ).forEach { dow ->
                        Box(
                            modifier = Modifier.height(11.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            weekdayLabels[dow]?.let { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    // Month labels aligned to week columns
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .padding(bottom = 2.dp)
                    ) {
                        monthLabels.forEach { (weekIndex, label) ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.offset(x = (weekIndex * 13).dp)
                            )
                        }
                    }

                    // Grid: 7 rows (Mon–Sun), columns = weeks
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        weeks.forEach { weekStart ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                for (dayOffset in 0..6) {
                                    val date = weekStart.plusDays(dayOffset.toLong())
                                    val inRange = !date.isAfter(today) && !date.isBefore(rangeStart)
                                    val day = if (inRange) dailyData[date] else null
                                    ActivityHeatmapCell(
                                        day = day,
                                        isFuture = !inRange,
                                        expenseColor = expenseColor,
                                        incomeColor = incomeColor,
                                        emptyColor = emptyColor,
                                        onClick = {
                                            if (day != null && !day.isEmpty) {
                                                selectedDay = day
                                            }
                                        }
                                    )
                                }
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
                LegendSwatch(color = expenseColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Expense",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(Spacing.sm))
                LegendSwatch(color = incomeColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Income",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(Spacing.sm))
                BothLegendSwatch(expenseColor = expenseColor, incomeColor = incomeColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Both",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    selectedDay?.let { day ->
        ModalBottomSheet(
            onDismissRequest = { selectedDay = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            DayBreakdownSheetContent(day = day, currency = currency)
        }
    }
}

@Composable
private fun DayBreakdownSheetContent(
    day: HeatmapDay,
    currency: String
) {
    val dateLabel = remember(day.date) {
        day.date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
    }
    val netColor = when {
        day.net > BigDecimal.ZERO -> if (!isSystemInDarkTheme()) income_light else income_dark
        day.net < BigDecimal.ZERO -> if (!isSystemInDarkTheme()) expense_light else expense_dark
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${day.transactionCount} transaction${if (day.transactionCount == 1) "" else "s"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xs))
        BreakdownRow(
            label = "Expense",
            amount = day.expense,
            currency = currency,
            color = if (!isSystemInDarkTheme()) expense_light else expense_dark
        )
        BreakdownRow(
            label = "Income",
            amount = day.income,
            currency = currency,
            color = if (!isSystemInDarkTheme()) income_light else income_dark
        )
        BreakdownRow(
            label = "Net",
            amount = day.net,
            currency = currency,
            color = netColor,
            emphasize = true
        )
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: BigDecimal,
    currency: String,
    color: Color,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasize) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = CurrencyFormatter.formatCurrency(amount, currency),
            style = if (emphasize) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun ActivityHeatmapCell(
    day: HeatmapDay?,
    isFuture: Boolean,
    expenseColor: Color,
    incomeColor: Color,
    emptyColor: Color,
    onClick: () -> Unit
) {
    val hasActivity = day != null && !day.isEmpty
    Box(
        modifier = Modifier
            .size(11.dp)
            .then(
                if (hasActivity) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padding = 0.5.dp.toPx()
            val cellSize = Size(
                width = size.width - padding * 2,
                height = size.height - padding * 2
            )
            val topLeft = Offset(padding, padding)
            val radius = CornerRadius(2.dp.toPx())

            when {
                isFuture || day == null || day.isEmpty -> {
                    drawRoundRect(
                        color = emptyColor.copy(alpha = if (isFuture) 0.2f else emptyColor.alpha),
                        topLeft = topLeft,
                        size = cellSize,
                        cornerRadius = radius
                    )
                }
                day.hasExpense && day.hasIncome -> {
                    val clip = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                left = topLeft.x,
                                top = topLeft.y,
                                right = topLeft.x + cellSize.width,
                                bottom = topLeft.y + cellSize.height,
                                cornerRadius = radius
                            )
                        )
                    }
                    clipPath(clip) {
                        // Top-left triangle = expense
                        drawPath(
                            path = Path().apply {
                                moveTo(topLeft.x, topLeft.y)
                                lineTo(topLeft.x + cellSize.width, topLeft.y)
                                lineTo(topLeft.x, topLeft.y + cellSize.height)
                                close()
                            },
                            color = expenseColor
                        )
                        // Bottom-right triangle = income
                        drawPath(
                            path = Path().apply {
                                moveTo(topLeft.x + cellSize.width, topLeft.y)
                                lineTo(topLeft.x + cellSize.width, topLeft.y + cellSize.height)
                                lineTo(topLeft.x, topLeft.y + cellSize.height)
                                close()
                            },
                            color = incomeColor
                        )
                    }
                }
                day.hasExpense -> {
                    drawRoundRect(
                        color = expenseColor,
                        topLeft = topLeft,
                        size = cellSize,
                        cornerRadius = radius
                    )
                }
                else -> {
                    drawRoundRect(
                        color = incomeColor,
                        topLeft = topLeft,
                        size = cellSize,
                        cornerRadius = radius
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Canvas(modifier = Modifier.size(10.dp)) {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

@Composable
private fun BothLegendSwatch(expenseColor: Color, incomeColor: Color) {
    Canvas(modifier = Modifier.size(10.dp)) {
        val radius = CornerRadius(2.dp.toPx())
        val clip = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = radius
                )
            )
        }
        clipPath(clip) {
            drawPath(
                path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(0f, size.height)
                    close()
                },
                color = expenseColor
            )
            drawPath(
                path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                },
                color = incomeColor
            )
        }
    }
}

/** Each column starts on Monday. */
private fun buildWeekColumns(rangeStart: LocalDate, today: LocalDate): List<LocalDate> {
    val weeks = mutableListOf<LocalDate>()
    var cursor = rangeStart
    while (!cursor.isAfter(today)) {
        weeks.add(cursor)
        cursor = cursor.plusWeeks(1)
    }
    return weeks
}

/**
 * Month abbreviation at the first week column where that month begins
 * (or the first visible week if the month already started).
 */
private fun buildMonthLabels(weeks: List<LocalDate>): Map<Int, String> {
    val labels = mutableMapOf<Int, String>()
    var lastMonth: java.time.Month? = null
    weeks.forEachIndexed { index, weekStart ->
        // Prefer the month of the Thursday of the week (stable month membership)
        val midWeek = weekStart.plusDays(3)
        if (midWeek.month != lastMonth) {
            labels[index] = midWeek.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            lastMonth = midWeek.month
        }
    }
    return labels
}
