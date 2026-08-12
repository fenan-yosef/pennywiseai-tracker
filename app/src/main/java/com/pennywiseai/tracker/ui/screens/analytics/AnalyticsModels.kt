package com.pennywiseai.tracker.ui.screens.analytics

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

// ==================== Tab & Filter State ====================

enum class AnalyticsTab(val label: String) {
    SNAPSHOT("Snapshot"),
    TRENDS("Trends")
}

data class BudgetUiState(
    val limit: Float = 0f,
    val bank: String? = null,
    val currentSpent: Float = 0f,
    val dailyBurnRate: Float = 0f,
    val projectedSpending: Float = 0f,
    val isExceeded: Boolean = false,
    val willExceed: Boolean = false,
    val exceedDayOfMonth: Int = 0,
    val remainingDays: Int = 0
)

// ==================== Core UI State ====================

data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val netSavings: BigDecimal = BigDecimal.ZERO,
    val categoryBreakdown: List<CategoryData> = emptyList(),
    val topMerchants: List<MerchantData> = emptyList(),
    val transactionCount: Int = 0,
    val averageAmount: BigDecimal = BigDecimal.ZERO,
    val topCategory: String? = null,
    val topCategoryPercentage: Float = 0f,
    val currency: String = "INR",
    val isLoading: Boolean = true,
    val insights: AnalyticsInsights = AnalyticsInsights(),
    val weeklyTrend: List<TrendPoint> = emptyList(),
    val monthlyTrend: List<TrendPoint> = emptyList(),
    val banks: List<String> = emptyList(),
    val selectedBank: String? = null,
    val bankTransactionCounts: Map<String, Int> = emptyMap(),
    val netSavingsRate: Float = 0f,
    val weekdaySpending: BigDecimal = BigDecimal.ZERO,
    val weekendSpending: BigDecimal = BigDecimal.ZERO,
    val dayOfWeekDistribution: Map<DayOfWeek, BigDecimal> = emptyMap(),
    val transactionScaleCount: Map<String, Int> = emptyMap(),
    val projectedSpending: BigDecimal = BigDecimal.ZERO,
    val activeTab: AnalyticsTab = AnalyticsTab.SNAPSHOT,
    val dailyTrend: List<DailySpending> = emptyList(),
    val weeklyBreakdown: List<WeeklySpending> = emptyList(),
    val monthlyBreakdown: List<MonthlySpending> = emptyList(),
    val yearlyBreakdown: List<MonthlySpending> = emptyList(),
    val categoryTrends: List<CategoryTrend> = emptyList(),
    val weekdayPattern: List<WeekdayDistribution> = emptyList(),
    val peakDays: List<DayOfMonthDistribution> = emptyList(),
    val comparisonData: ComparisonData? = null,
    val bankComparison: List<BankSummary> = emptyList(),
    val anomalousTransactions: List<AnomalousTransaction> = emptyList(),
    val spendingVelocity: SpendingVelocity = SpendingVelocity(),
    val heatmapDays: Map<LocalDate, HeatmapDay> = emptyMap(),
    val sixMonthSpending: List<MonthlySpending> = emptyList(),
    /** Hero: today's amount for the active type filter */
    val todayAmount: BigDecimal = BigDecimal.ZERO,
    /** Hero: filtered period total (respects type filter) */
    val periodTotal: BigDecimal = BigDecimal.ZERO,
    /** Hero: % change vs previous equal-length period (null if unavailable) */
    val spendingChangePercent: Float? = null,
    /** Hero: ahead of pace (>0), behind (<0), or on pace (~0); only for This Month */
    val pacePercent: Float? = null,
    val budgetState: BudgetUiState = BudgetUiState()
)

// ==================== Base Models ====================

data class CategoryData(
    val name: String,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int
)

data class MerchantData(
    val name: String,
    val amount: BigDecimal,
    val transactionCount: Int,
    val isSubscription: Boolean
)

data class AnalyticsInsights(
    val totalCount: Int = 0,
    val avgAmount: BigDecimal = BigDecimal.ZERO,
    val largest: HighlightedTransaction? = null,
    val topCategory: HighlightedGroup? = null,
    val topMerchant: HighlightedGroup? = null,
    val dailyAvg: BigDecimal = BigDecimal.ZERO,
    val todayAmount: BigDecimal = BigDecimal.ZERO,
    val recurringCount: Int = 0
)

data class HighlightedTransaction(
    val amount: BigDecimal,
    val label: String,
    val category: String?
)

data class HighlightedGroup(
    val name: String,
    val amount: BigDecimal,
    val count: Int
)

data class TrendPoint(
    val label: String,
    val amount: BigDecimal
)

// ==================== Insight Models ====================

/** Single day's spending for bar/line charts */
data class DailySpending(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val amount: BigDecimal,
    val transactionCount: Int
)

/** Single week's aggregated spending */
data class WeeklySpending(
    val weekStart: LocalDate,
    val amount: BigDecimal,
    val transactionCount: Int
)

/** Single month's income/expenses/net */
data class MonthlySpending(
    val yearMonth: YearMonth,
    val amount: BigDecimal,
    val income: BigDecimal,
    val expenses: BigDecimal,
    val transactionCount: Int
)

/** How a category's spending changes month-to-month */
data class CategoryTrend(
    val category: String,
    val monthlyAmounts: Map<YearMonth, BigDecimal>,
    val trend: TrendDirection = TrendDirection.STABLE,
    val changePercent: Float = 0f
)

enum class TrendDirection { RISING, FALLING, STABLE }

/** Spending by day of week (Mon-Sun) */
data class WeekdayDistribution(
    val dayOfWeek: DayOfWeek,
    val amount: BigDecimal,
    val transactionCount: Int,
    val percentage: Float = 0f
)

/** Spending by day-of-month (1-31) */
data class DayOfMonthDistribution(
    val day: Int,
    val amount: BigDecimal,
    val transactionCount: Int
)

/** Period-over-period comparison */
data class ComparisonData(
    val currentPeriod: PeriodSummary,
    val previousPeriod: PeriodSummary,
    val spendingChange: BigDecimal,
    val spendingChangePercent: Float,
    val incomeChange: BigDecimal,
    val incomeChangePercent: Float
)

data class PeriodSummary(
    val label: String,
    val totalSpending: BigDecimal,
    val totalIncome: BigDecimal,
    val transactionCount: Int,
    val avgAmount: BigDecimal,
    val topCategory: String
)

/** Per-bank summary for bank comparison card */
data class BankSummary(
    val bankName: String,
    val income: BigDecimal,
    val expenses: BigDecimal,
    val net: BigDecimal,
    val transactionCount: Int
)

/** Flagged unusual transaction */
data class AnomalousTransaction(
    val id: Long,
    val merchantName: String,
    val amount: BigDecimal,
    val dateTime: LocalDateTime,
    val category: String,
    val zScore: Double,
    val isHigh: Boolean // true = unusually high, false = unusually low
)

/** Running spending rate tracking */
data class SpendingVelocity(
    val dailyAvg: BigDecimal = BigDecimal.ZERO,
    val weeklyAvg: BigDecimal = BigDecimal.ZERO,
    val monthlyAvg: BigDecimal = BigDecimal.ZERO,
    val trendDirection: TrendDirection = TrendDirection.STABLE
)

// ==================== Heatmap ====================

/** Day cell for activity heatmap (expense / income / both) */
data class HeatmapDay(
    val date: LocalDate,
    val expense: BigDecimal = BigDecimal.ZERO,
    val income: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0
) {
    val hasExpense: Boolean get() = expense > BigDecimal.ZERO
    val hasIncome: Boolean get() = income > BigDecimal.ZERO
    val isEmpty: Boolean get() = !hasExpense && !hasIncome
    val net: BigDecimal get() = income - expense
}
