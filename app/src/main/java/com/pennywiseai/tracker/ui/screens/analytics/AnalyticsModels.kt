package com.pennywiseai.tracker.ui.screens.analytics

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

// ==================== Tab & Filter State ====================

enum class AnalyticsTab(val label: String) {
    OVERVIEW("Overview"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly"),
    CUSTOM("Custom")
}

// ==================== Core UI State ====================

data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
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
    // Existing extra fields
    val netSavingsRate: Float = 0f,
    val weekdaySpending: BigDecimal = BigDecimal.ZERO,
    val weekendSpending: BigDecimal = BigDecimal.ZERO,
    val dayOfWeekDistribution: Map<DayOfWeek, BigDecimal> = emptyMap(),
    val transactionScaleCount: Map<String, Int> = emptyMap(),
    val projectedSpending: BigDecimal = BigDecimal.ZERO,
    // New fields
    val activeTab: AnalyticsTab = AnalyticsTab.OVERVIEW,
    val overviewData: OverviewData = OverviewData(),
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
    val heatmapDays: Map<LocalDate, BigDecimal> = emptyMap()
)

// ==================== Base Models (existing, preserved) ====================

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

// ==================== New Insight Models ====================

/** Overview tab header data: income vs expense + net */
data class OverviewData(
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val netSavings: BigDecimal = BigDecimal.ZERO,
    val savingsRate: Float = 0f,
    val transactionCount: Int = 0,
    val avgPerTransaction: BigDecimal = BigDecimal.ZERO,
    val topCategory: String = "",
    val topCategoryAmount: BigDecimal = BigDecimal.ZERO,
    val topCategoryPercent: Float = 0f,
    val topMerchant: String = "",
    val topMerchantAmount: BigDecimal = BigDecimal.ZERO
)

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

/** Day cell for calendar heatmap */
data class HeatmapDay(
    val date: LocalDate,
    val amount: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0,
    val intensity: Float = 0f // 0f..1f normalized within the visible range
)

/** Full heatmap data for a year/month range */
data class HeatmapData(
    val year: Int,
    val months: List<HeatmapMonth>,
    val maxDailyAmount: BigDecimal = BigDecimal.ZERO
)

data class HeatmapMonth(
    val month: java.time.Month,
    val days: List<HeatmapDay>
)
