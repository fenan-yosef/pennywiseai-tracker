package com.pennywiseai.tracker.ui.screens.analytics.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pennywiseai.tracker.ui.components.charts.*
import com.pennywiseai.tracker.ui.components.insights.*
import com.pennywiseai.tracker.ui.screens.analytics.*
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun OverviewTab(
    state: AnalyticsUiState,
    onCategoryClick: (CategoryData) -> Unit = {},
    onMerchantClick: (MerchantData) -> Unit = {}
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.sm)
    ) {
        // Summary
        item {
            IncomeVsExpenseCard(
                totalIncome = state.totalIncome,
                totalExpense = state.totalSpending,
                netSavings = state.netSavings,
                savingsRate = state.netSavingsRate,
                currency = state.currency
            )
        }

        // Period comparison
        if (state.comparisonData != null) {
            item {
                val c = state.comparisonData
                val totalExpense = state.totalSpending
                val comparisons = listOf(
                    ComparisonItem(
                        label = "Spending",
                        current = c.currentPeriod.totalSpending.let { if (it > BigDecimal.ZERO) it else totalExpense },
                        previous = c.previousPeriod.totalSpending.let { if (it > BigDecimal.ZERO) it else BigDecimal.ZERO },
                        currentLabel = c.currentPeriod.label,
                        previousLabel = c.previousPeriod.label
                    ),
                    ComparisonItem(
                        label = "Income",
                        current = c.currentPeriod.totalIncome.let { if (it > BigDecimal.ZERO) it else state.totalIncome },
                        previous = c.previousPeriod.totalIncome.let { if (it > BigDecimal.ZERO) it else BigDecimal.ZERO }
                    )
                )
                PeriodComparisonCard(
                    comparisons = comparisons,
                    currency = state.currency
                )
            }
        }

        // Spending velocity
        item {
            SpendingVelocityCard(
                velocity = state.spendingVelocity,
                currency = state.currency
            )
        }

        // Category donut
        if (state.categoryBreakdown.isNotEmpty()) {
            item {
                SpendingDonutChart(
                    categories = state.categoryBreakdown,
                    currency = state.currency,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        // Weekday pattern
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

        // Category trends
        if (state.categoryTrends.isNotEmpty()) {
            item {
                CategoryTrendsCard(
                    trends = state.categoryTrends,
                    currency = state.currency
                )
            }
        }

        // Top merchants
        if (state.topMerchants.isNotEmpty()) {
            item {
                MerchantHorizontalBars(
                    merchants = state.topMerchants,
                    currency = state.currency
                )
            }
        }

        // Bank comparison
        if (state.bankComparison.isNotEmpty()) {
            item {
                BankComparisonCard(
                    banks = state.bankComparison,
                    currency = state.currency
                )
            }
        }

        // Anomaly detection
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
fun DailyTab(state: AnalyticsUiState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.sm)
    ) {
        // Daily bar chart
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

        // Weekday pattern
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

        // Peak days
        if (state.peakDays.isNotEmpty()) {
            item {
                PeakDaysCard(
                    peakDays = state.peakDays,
                    currency = state.currency
                )
            }
        }

        // Spending velocity
        item {
            SpendingVelocityCard(
                velocity = state.spendingVelocity,
                currency = state.currency
            )
        }
    }
}

@Composable
fun WeeklyTab(state: AnalyticsUiState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.sm)
    ) {
        // Weekly breakdown bars
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

        // Weekday pattern
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

        // Spending velocity
        item {
            SpendingVelocityCard(
                velocity = state.spendingVelocity,
                currency = state.currency
            )
        }
    }
}

@Composable
fun MonthlyTab(
    state: AnalyticsUiState,
    onCategoryClick: (CategoryData) -> Unit = {},
    onMerchantClick: (MerchantData) -> Unit = {}
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.sm)
    ) {
        // Monthly bar chart
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

        // Category donut
        if (state.categoryBreakdown.isNotEmpty()) {
            item {
                SpendingDonutChart(
                    categories = state.categoryBreakdown,
                    currency = state.currency,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        // Category trends
        if (state.categoryTrends.isNotEmpty()) {
            item {
                CategoryTrendsCard(
                    trends = state.categoryTrends,
                    currency = state.currency
                )
            }
        }

        // Top merchants
        if (state.topMerchants.isNotEmpty()) {
            item {
                MerchantHorizontalBars(
                    merchants = state.topMerchants,
                    currency = state.currency
                )
            }
        }

        // Income vs Expense
        item {
            IncomeVsExpenseCard(
                totalIncome = state.totalIncome,
                totalExpense = state.totalSpending,
                netSavings = state.netSavings,
                savingsRate = state.netSavingsRate,
                currency = state.currency
            )
        }
    }
}

@Composable
fun QuarterlyTab(state: AnalyticsUiState) {
    MonthlyTab(state) // Quarterly reuses monthly view with aggregated data
}

@Composable
fun YearlyTab(state: AnalyticsUiState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(vertical = Spacing.sm)
    ) {
        // Yearly breakdown (monthly chart)
        if (state.yearlyBreakdown.isNotEmpty()) {
            item {
                MonthlyBarChart(
                    monthlyData = state.yearlyBreakdown.map {
                        MonthlyBarItem(
                            label = it.yearMonth.format(DateTimeFormatter.ofPattern("MMM yy")),
                            amount = it.amount,
                            income = it.income,
                            expenses = it.expenses,
                            yearMonth = it.yearMonth
                        )
                    },
                    currency = state.currency,
                    title = "Year Overview"
                )
            }
        }

        // Income vs Expense summary
        item {
            IncomeVsExpenseCard(
                totalIncome = state.totalIncome,
                totalExpense = state.totalSpending,
                netSavings = state.netSavings,
                savingsRate = state.netSavingsRate,
                currency = state.currency
            )
        }

        // Heatmap calendar for current/selected month
        val ym = state.dailyTrend.firstOrNull()?.date?.let { YearMonth.from(it) }
            ?: YearMonth.now()
        if (state.heatmapDays.isNotEmpty()) {
            item {
                SpendingHeatmap(
                    dailyData = state.heatmapDays,
                    yearMonth = ym,
                    currency = state.currency
                )
            }
        }

        // Category donut
        if (state.categoryBreakdown.isNotEmpty()) {
            item {
                SpendingDonutChart(
                    categories = state.categoryBreakdown,
                    currency = state.currency
                )
            }
        }

        // Bank comparison
        if (state.bankComparison.isNotEmpty()) {
            item {
                BankComparisonCard(
                    banks = state.bankComparison,
                    currency = state.currency
                )
            }
        }
    }
}

@Composable
fun CustomTab(state: AnalyticsUiState) {
    OverviewTab(state)
}
