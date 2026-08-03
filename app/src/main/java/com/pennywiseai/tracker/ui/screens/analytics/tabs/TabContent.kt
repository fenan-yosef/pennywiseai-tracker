package com.pennywiseai.tracker.ui.screens.analytics.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pennywiseai.tracker.ui.components.charts.*
import com.pennywiseai.tracker.ui.components.insights.*
import com.pennywiseai.tracker.ui.screens.analytics.*
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
fun SnapshotTab(
    state: AnalyticsUiState,
    onCategoryClick: (CategoryData) -> Unit = {}
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(
            horizontal = Spacing.md,
            vertical = Spacing.sm
        )
    ) {
        item {
            IncomeVsExpenseCard(
                totalIncome = state.totalIncome,
                totalExpense = state.totalExpense,
                netSavings = state.netSavings,
                savingsRate = state.netSavingsRate,
                currency = state.currency
            )
        }

        if (state.comparisonData != null) {
            item {
                val c = state.comparisonData
                val comparisons = listOf(
                    ComparisonItem(
                        label = "Spending",
                        current = c.currentPeriod.totalSpending.let {
                            if (it > BigDecimal.ZERO) it else state.totalExpense
                        },
                        previous = c.previousPeriod.totalSpending,
                        currentLabel = c.currentPeriod.label,
                        previousLabel = c.previousPeriod.label
                    ),
                    ComparisonItem(
                        label = "Income",
                        current = c.currentPeriod.totalIncome.let {
                            if (it > BigDecimal.ZERO) it else state.totalIncome
                        },
                        previous = c.previousPeriod.totalIncome
                    )
                )
                PeriodComparisonCard(
                    comparisons = comparisons,
                    currency = state.currency
                )
            }
        }

        item {
            SpendingVelocityCard(
                velocity = state.spendingVelocity,
                currency = state.currency
            )
        }

        if (state.categoryBreakdown.isNotEmpty()) {
            item {
                SpendingDonutChart(
                    categories = state.categoryBreakdown,
                    currency = state.currency,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        if (state.categoryTrends.isNotEmpty()) {
            item {
                CategoryTrendsCard(
                    trends = state.categoryTrends,
                    currency = state.currency
                )
            }
        }

        if (state.topMerchants.isNotEmpty()) {
            item {
                MerchantHorizontalBars(
                    merchants = state.topMerchants,
                    currency = state.currency
                )
            }
        }

        if (state.bankComparison.isNotEmpty()) {
            item {
                BankComparisonCard(
                    banks = state.bankComparison,
                    currency = state.currency
                )
            }
        }

        if (state.anomalousTransactions.isNotEmpty()) {
            item {
                AnomalyCard(
                    anomalies = state.anomalousTransactions,
                    currency = state.currency
                )
            }
        }
    }
}

@Composable
fun TrendsTab(state: AnalyticsUiState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(
            horizontal = Spacing.md,
            vertical = Spacing.sm
        )
    ) {
        if (state.dailyTrend.isNotEmpty()) {
            item {
                DailyBarChart(
                    dailyData = state.dailyTrend.map {
                        DailyBarItem(
                            label = it.date.format(DateTimeFormatter.ofPattern("d MMM")),
                            amount = it.amount,
                            date = it.date
                        )
                    },
                    currency = state.currency,
                    title = "Daily Spending"
                )
            }
        }

        if (state.weeklyBreakdown.isNotEmpty()) {
            item {
                DailyBarChart(
                    dailyData = state.weeklyBreakdown.map {
                        DailyBarItem(
                            label = it.weekStart.format(DateTimeFormatter.ofPattern("d MMM")),
                            amount = it.amount
                        )
                    },
                    currency = state.currency,
                    title = "Weekly Spending"
                )
            }
        }

        if (state.monthlyBreakdown.isNotEmpty()) {
            item {
                MonthlyBarChart(
                    monthlyData = state.monthlyBreakdown.map {
                        MonthlyBarItem(
                            label = it.yearMonth.format(DateTimeFormatter.ofPattern("MMM yy")),
                            amount = it.amount,
                            income = it.income,
                            expenses = it.expenses,
                            yearMonth = it.yearMonth
                        )
                    },
                    currency = state.currency,
                    title = "Monthly Breakdown"
                )
            }
        }

        if (state.sixMonthSpending.isNotEmpty()) {
            item {
                SixMonthTrendCard(
                    monthlyData = state.sixMonthSpending,
                    currency = state.currency
                )
            }
        }

        if (state.heatmapDays.isNotEmpty()) {
            item {
                ActivityHeatmap(
                    dailyData = state.heatmapDays,
                    currency = state.currency
                )
            }
        }

        if (state.weekdayPattern.isNotEmpty()) {
            item {
                WeekdayPatternCard(
                    weekdayDistribution = state.weekdayPattern,
                    weekdayTotal = state.weekdaySpending,
                    weekendTotal = state.weekendSpending,
                    currency = state.currency
                )
            }
        }

        if (state.peakDays.isNotEmpty()) {
            item {
                PeakDaysCard(
                    peakDays = state.peakDays,
                    currency = state.currency
                )
            }
        }
    }
}
