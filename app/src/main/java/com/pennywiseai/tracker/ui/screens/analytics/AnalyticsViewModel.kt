package com.pennywiseai.tracker.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.presentation.common.getDateRangeForPeriod
import com.pennywiseai.tracker.utils.CurrencyUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.max
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {
    
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _transactionTypeFilter = MutableStateFlow(TransactionTypeFilter.EXPENSE)
    val transactionTypeFilter: StateFlow<TransactionTypeFilter> = _transactionTypeFilter.asStateFlow()

    private val _selectedBank = MutableStateFlow<String?>(null)
    val selectedBank: StateFlow<String?> = _selectedBank.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("INR") // Default to INR
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    // Store custom date range as epoch days to survive process death
    // Stored as Pair<Long, Long> (startEpochDay, endEpochDay) in SavedStateHandle
    private val _customDateRangeEpochDays = savedStateHandle.getStateFlow<Pair<Long, Long>?>("customDateRange", null)

    // Expose as LocalDate pair for convenience
    val customDateRange: StateFlow<Pair<LocalDate, LocalDate>?> = _customDateRangeEpochDays
        .map { epochDays ->
            epochDays?.let { (startEpochDay, endEpochDay) ->
                LocalDate.ofEpochDay(startEpochDay) to LocalDate.ofEpochDay(endEpochDay)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private val _availableCurrencies = MutableStateFlow<List<String>>(emptyList())
    val availableCurrencies: StateFlow<List<String>> = _availableCurrencies.asStateFlow()

    // Reactive UI state that automatically updates when any filter changes
    // Uses flatMapLatest to cancel previous data loads when filters change (prevents race conditions)
    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedPeriod,
        customDateRange,
        _transactionTypeFilter,
        _selectedCurrency,
        _selectedBank
    ) { period, customRange, typeFilter, currency, bank ->
        // Combine all filter states
        FilterState(period, customRange, typeFilter, currency, bank)
    }.flatMapLatest { filterState ->
        // Determine date range based on selected period
        val dateRange = if (filterState.period == TimePeriod.CUSTOM) {
            val customRange = filterState.customRange
            // Guard against invalid state: CUSTOM period must have a date range
            if (customRange == null) {
                android.util.Log.e("AnalyticsViewModel",
                    "CUSTOM period selected but no date range set - falling back to THIS_MONTH")
                // Auto-correct the invalid state
                _selectedPeriod.value = TimePeriod.THIS_MONTH
                getDateRangeForPeriod(TimePeriod.THIS_MONTH)
            } else {
                customRange
            }
        } else {
            getDateRangeForPeriod(filterState.period)
        }

        if (dateRange == null) {
            // No valid date range, return empty state
            flowOf(AnalyticsUiState(isLoading = false))
        } else {
            // First load all transactions for the date range to get available currencies
            transactionRepository.getTransactionsBetweenDates(
                startDate = dateRange.first,
                endDate = dateRange.second
            ).flatMapLatest { allTransactions ->
                // Update available currencies using standard sorting (INR first, then alphabetical)
                val allCurrencies = CurrencyUtils.sortCurrencies(
                    allTransactions.map { it.currency }.distinct()
                )
                _availableCurrencies.value = allCurrencies

                // Auto-select primary currency if not already selected or if current currency no longer exists
                val currentSelectedCurrency = filterState.currency
                if (!allCurrencies.contains(currentSelectedCurrency) && allCurrencies.isNotEmpty()) {
                    _selectedCurrency.value = if (allCurrencies.contains("INR")) "INR" else allCurrencies.first()
                }

                // Use database-level filtering for better performance
                // Convert TransactionTypeFilter to TransactionType for database query
                val dbTransactionType = when (filterState.typeFilter) {
                    TransactionTypeFilter.ALL -> null // null means no type filter at DB level
                    TransactionTypeFilter.INCOME -> com.pennywiseai.tracker.data.database.entity.TransactionType.INCOME
                    TransactionTypeFilter.EXPENSE -> com.pennywiseai.tracker.data.database.entity.TransactionType.EXPENSE
                    TransactionTypeFilter.CREDIT -> com.pennywiseai.tracker.data.database.entity.TransactionType.CREDIT
                    TransactionTypeFilter.TRANSFER -> com.pennywiseai.tracker.data.database.entity.TransactionType.TRANSFER
                    TransactionTypeFilter.INVESTMENT -> com.pennywiseai.tracker.data.database.entity.TransactionType.INVESTMENT
                }

                // Load filtered transactions from database (filtered at DB level for performance)
                transactionRepository.getTransactionsFiltered(
                    startDate = dateRange.first,
                    endDate = dateRange.second,
                    currency = filterState.currency,
                    transactionType = dbTransactionType
                )
            }.map { filteredTransactions ->

                val bankCounts = filteredTransactions
                    .groupBy { it.bankName ?: "Unknown Bank" }
                    .mapValues { it.value.size }

                val availableBanks = bankCounts.keys.sorted()

                // Ensure selected bank is valid; if not, reset to null (All)
                val effectiveSelectedBank = filterState.bank?.takeIf { availableBanks.contains(it) }
                if (effectiveSelectedBank != filterState.bank) {
                    _selectedBank.value = effectiveSelectedBank
                }

                val bankFilteredTransactions = effectiveSelectedBank?.let { bank ->
                    filteredTransactions.filter { (it.bankName ?: "Unknown Bank") == bank }
                } ?: filteredTransactions

                // Calculate total
                val totalSpending = bankFilteredTransactions.sumOf { it.amount.toDouble() }.toBigDecimal()

                // Group by category
                val categoryBreakdown = bankFilteredTransactions
                    .groupBy { it.category ?: "Others" }
                    .map { (categoryName, txns) ->
                        val categoryTotal = txns.sumOf { it.amount.toDouble() }.toBigDecimal()
                        CategoryData(
                            name = categoryName,
                            amount = categoryTotal,
                            percentage = if (totalSpending > BigDecimal.ZERO) {
                                (categoryTotal.divide(totalSpending, 4, java.math.RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
                            } else 0f,
                            transactionCount = txns.size
                        )
                    }
                    .sortedByDescending { it.amount }

                // Group by merchant
                val merchantBreakdown = bankFilteredTransactions
                    .groupBy { it.merchantName }
                    .mapValues { (merchant, txns) ->
                        MerchantData(
                            name = merchant,
                            amount = txns.sumOf { it.amount.toDouble() }.toBigDecimal(),
                            transactionCount = txns.size,
                            isSubscription = txns.any { it.isRecurring }
                        )
                    }
                    .values
                    .sortedByDescending { it.amount }
                    .take(10) // Top 10 merchants

                // Calculate average amount
                val averageAmount = if (bankFilteredTransactions.isNotEmpty()) {
                    totalSpending.divide(BigDecimal(bankFilteredTransactions.size), 2, java.math.RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }

                // Get top category info
                val topCategory = categoryBreakdown.firstOrNull()

                val insights = calculateInsights(bankFilteredTransactions)

                val weeklyTrend = buildLast7DailyTrendSeries(
                    transactions = bankFilteredTransactions,
                    endInclusive = dateRange.second
                )

                val monthlyStart = run {
                    val daysInRange = ChronoUnit.DAYS.between(dateRange.first, dateRange.second).toInt().coerceAtLeast(0) + 1
                    if (daysInRange <= 31) {
                        dateRange.first
                    } else {
                        dateRange.second.minusDays(29)
                    }
                }
                val monthlyTrend = buildLast4WeeklyTrendSeries(
                    transactions = bankFilteredTransactions,
                    endInclusive = dateRange.second
                )

                AnalyticsUiState(
                    totalSpending = totalSpending,
                    categoryBreakdown = categoryBreakdown,
                    topMerchants = merchantBreakdown,
                    transactionCount = bankFilteredTransactions.size,
                    averageAmount = averageAmount,
                    topCategory = topCategory?.name,
                    topCategoryPercentage = topCategory?.percentage ?: 0f,
                    currency = filterState.currency,
                    isLoading = false,
                    insights = insights,
                    weeklyTrend = weeklyTrend,
                    monthlyTrend = monthlyTrend,
                    banks = availableBanks,
                    selectedBank = effectiveSelectedBank,
                    bankTransactionCounts = bankCounts
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState(isLoading = true)
    )

    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    fun setTransactionTypeFilter(filter: TransactionTypeFilter) {
        _transactionTypeFilter.value = filter
    }

    fun selectCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    fun selectBank(bank: String?) {
        _selectedBank.value = bank
    }

    /**
     * Sets a custom date range filter and switches the period to CUSTOM.
     * Date range is persisted in SavedStateHandle to survive process death.
     *
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @throws IllegalArgumentException if startDate > endDate
     */
    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        require(startDate <= endDate) {
            "Start date ($startDate) must be before or equal to end date ($endDate)"
        }
        // Store as epoch days for process death survival
        savedStateHandle["customDateRange"] = startDate.toEpochDay() to endDate.toEpochDay()
        _selectedPeriod.value = TimePeriod.CUSTOM
    }

    /**
     * Clears the custom date range and resets to THIS_MONTH period.
     * Always safe to call - ensures we never have CUSTOM period with null dates.
     */
    fun clearCustomDateRange() {
        savedStateHandle["customDateRange"] = null
        // Always reset to a valid period to prevent CUSTOM with null dates
        if (_selectedPeriod.value == TimePeriod.CUSTOM) {
            _selectedPeriod.value = TimePeriod.THIS_MONTH
        }
    }

    private fun calculateInsights(transactions: List<com.pennywiseai.tracker.data.database.entity.TransactionEntity>): AnalyticsInsights {
        if (transactions.isEmpty()) return AnalyticsInsights()

        val totalCount = transactions.size
        val totalAmount = transactions.sumOf { it.amount.toDouble() }.toBigDecimal()
        val avgAmount = if (totalCount > 0) totalAmount.divide(BigDecimal(totalCount), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

        val largest = transactions.maxByOrNull { it.amount }

        val topCategory = transactions
            .groupBy { it.category ?: "Others" }
            .mapValues { entry ->
                val total = entry.value.sumOf { it.amount.toDouble() }.toBigDecimal()
                val count = entry.value.size
                total to count
            }
            .maxByOrNull { it.value.first }
            ?.let { (name, value) ->
                HighlightedGroup(name = name, amount = value.first, count = value.second)
            }

        val topMerchant = transactions
            .groupBy { it.merchantName }
            .mapValues { entry ->
                val total = entry.value.sumOf { it.amount.toDouble() }.toBigDecimal()
                val count = entry.value.size
                total to count
            }
            .maxByOrNull { it.value.first }
            ?.let { (name, value) ->
                HighlightedGroup(name = name, amount = value.first, count = value.second)
            }

        val earliest = transactions.minOfOrNull { it.dateTime.toLocalDate() }
        val latest = transactions.maxOfOrNull { it.dateTime.toLocalDate() }
        val days = if (earliest != null && latest != null) {
            ChronoUnit.DAYS.between(earliest, latest).toInt().coerceAtLeast(0) + 1
        } else 1

        val dailyAvg = if (days > 0) {
            totalAmount.divide(BigDecimal(days), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val today = LocalDate.now()
        val todayAmount = transactions
            .filter { it.dateTime.toLocalDate() == today }
            .sumOf { it.amount.toDouble() }.toBigDecimal()

        val recurringCount = transactions.count { it.isRecurring }

        return AnalyticsInsights(
            totalCount = totalCount,
            avgAmount = avgAmount,
            largest = largest?.let {
                HighlightedTransaction(
                    amount = it.amount,
                    label = it.merchantName,
                    category = it.category
                )
            },
            topCategory = topCategory,
            topMerchant = topMerchant,
            dailyAvg = dailyAvg,
            todayAmount = todayAmount,
            recurringCount = recurringCount
        )
    }

    private fun buildLast7DailyTrendSeries(
        transactions: List<com.pennywiseai.tracker.data.database.entity.TransactionEntity>,
        endInclusive: LocalDate
    ): List<TrendPoint> {
        val start = endInclusive.minusDays(6)
        val totalsByDate = transactions
            .groupBy { it.dateTime.toLocalDate() }
            .mapValues { (_, txns) -> txns.sumOf { it.amount.toDouble() }.toBigDecimal() }

        return (0..6).map { offset ->
            val date = start.plusDays(offset.toLong())
            TrendPoint(
                label = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                amount = totalsByDate[date] ?: BigDecimal.ZERO
            )
        }
    }

    private fun buildLast4WeeklyTrendSeries(
        transactions: List<com.pennywiseai.tracker.data.database.entity.TransactionEntity>,
        endInclusive: LocalDate
    ): List<TrendPoint> {
        // Build 4 weeks buckets ending on endInclusive. Week 4 is most recent week (7 days), week1 is oldest.
        val weeks = (0..3).map { i ->
            val weekEnd = endInclusive.minusDays((3 - i) * 7L)
            val weekStart = weekEnd.minusDays(6)
            weekStart to weekEnd
        }

        val totalsByDate = transactions
            .groupBy { it.dateTime.toLocalDate() }
            .mapValues { (_, txns) -> txns.sumOf { it.amount.toDouble() }.toBigDecimal() }

        return weeks.mapIndexed { idx, (start, end) ->
            val total = (0..ChronoUnit.DAYS.between(start, end).toInt()).fold(BigDecimal.ZERO) { acc, off ->
                val d = start.plusDays(off.toLong())
                acc + (totalsByDate[d] ?: BigDecimal.ZERO)
            }
            TrendPoint(label = "Week ${idx + 1}", amount = total)
        }
    }
}

/**
 * Internal state for combining all filter parameters.
 * Used in reactive Flow to trigger data reload when any filter changes.
 */
private data class FilterState(
    val period: TimePeriod,
    val customRange: Pair<LocalDate, LocalDate>?,
    val typeFilter: TransactionTypeFilter,
    val currency: String,
    val bank: String?
)

data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
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
    val bankTransactionCounts: Map<String, Int> = emptyMap()
)

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

