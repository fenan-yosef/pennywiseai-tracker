package com.pennywiseai.tracker.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.presentation.common.getDateRangeForPeriod
import com.pennywiseai.tracker.utils.CurrencyUtils
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
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
    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedPeriod,
        customDateRange,
        _transactionTypeFilter,
        _selectedCurrency,
        _selectedBank
    ) { period, customRange, typeFilter, currency, bank ->
        FilterState(period, customRange, typeFilter, currency, bank)
    }.flatMapLatest { filterState ->
        val dateRange = if (filterState.period == TimePeriod.CUSTOM) {
            val customRange = filterState.customRange
            if (customRange == null) {
                android.util.Log.e("AnalyticsViewModel",
                    "CUSTOM period selected but no date range set - falling back to THIS_MONTH")
                _selectedPeriod.value = TimePeriod.THIS_MONTH
                getDateRangeForPeriod(TimePeriod.THIS_MONTH)
            } else {
                customRange
            }
        } else {
            getDateRangeForPeriod(filterState.period)
        }

        if (dateRange == null) {
            flowOf(AnalyticsUiState(isLoading = false))
        } else {
            // First load all transactions for the date range to get available currencies
            transactionRepository.getTransactionsBetweenDates(
                startDate = dateRange.first,
                endDate = dateRange.second
            ).flatMapLatest { allTransactions ->
                val allCurrencies = CurrencyUtils.sortCurrencies(
                    allTransactions.map { it.currency }.distinct()
                )
                _availableCurrencies.value = allCurrencies

                val currentSelectedCurrency = filterState.currency
                if (!allCurrencies.contains(currentSelectedCurrency) && allCurrencies.isNotEmpty()) {
                    _selectedCurrency.value = if (allCurrencies.contains("INR")) "INR" else allCurrencies.first()
                }

                // Query all transactions of selected currency (filter type in memory to keep context for charts)
                transactionRepository.getTransactionsFiltered(
                    startDate = dateRange.first,
                    endDate = dateRange.second,
                    currency = filterState.currency,
                    transactionType = null
                )
            }.map { currencyTransactions ->
                val bankCounts = currencyTransactions
                    .groupBy { it.bankName ?: "Unknown Bank" }
                    .mapValues { it.value.size }

                val availableBanks = bankCounts.keys.sorted()

                val effectiveSelectedBank = filterState.bank?.takeIf { availableBanks.contains(it) }
                if (effectiveSelectedBank != filterState.bank) {
                    _selectedBank.value = effectiveSelectedBank
                }

                val bankFilteredTransactions = effectiveSelectedBank?.let { bank ->
                    currencyTransactions.filter { (it.bankName ?: "Unknown Bank") == bank }
                } ?: currencyTransactions

                // Calculate Net Totals
                val totalIncome = bankFilteredTransactions
                    .filter { it.transactionType == TransactionType.INCOME }
                    .sumOf { it.amount.toDouble() }
                    .toBigDecimal()

                val totalExpenses = bankFilteredTransactions
                    .filter { it.transactionType == TransactionType.EXPENSE || it.transactionType == TransactionType.CREDIT }
                    .sumOf { it.amount.toDouble() }
                    .toBigDecimal()

                val netSavingsRate = if (totalIncome > BigDecimal.ZERO) {
                    ((totalIncome - totalExpenses).divide(totalIncome, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
                } else {
                    0f
                }

                // Apply active UI transaction type filter for lists/categories
                val uiFilteredTransactions = when (filterState.typeFilter) {
                    TransactionTypeFilter.ALL -> bankFilteredTransactions
                    TransactionTypeFilter.INCOME -> bankFilteredTransactions.filter { it.transactionType == TransactionType.INCOME }
                    TransactionTypeFilter.EXPENSE -> bankFilteredTransactions.filter { it.transactionType == TransactionType.EXPENSE }
                    TransactionTypeFilter.CREDIT -> bankFilteredTransactions.filter { it.transactionType == TransactionType.CREDIT }
                    TransactionTypeFilter.TRANSFER -> bankFilteredTransactions.filter { it.transactionType == TransactionType.TRANSFER }
                    TransactionTypeFilter.INVESTMENT -> bankFilteredTransactions.filter { it.transactionType == TransactionType.INVESTMENT }
                }

                val totalSpending = uiFilteredTransactions.sumOf { it.amount.toDouble() }.toBigDecimal()

                // Group by category
                val categoryBreakdown = uiFilteredTransactions
                    .groupBy { it.category ?: "Others" }
                    .map { (categoryName, txns) ->
                        val categoryTotal = txns.sumOf { it.amount.toDouble() }.toBigDecimal()
                        CategoryData(
                            name = categoryName,
                            amount = categoryTotal,
                            percentage = if (totalSpending > BigDecimal.ZERO) {
                                (categoryTotal.divide(totalSpending, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
                            } else 0f,
                            transactionCount = txns.size
                        )
                    }
                    .sortedByDescending { it.amount }

                // Group by merchant
                val merchantBreakdown = uiFilteredTransactions
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
                    .take(10)

                val averageAmount = if (uiFilteredTransactions.isNotEmpty()) {
                    totalSpending.divide(BigDecimal(uiFilteredTransactions.size), 2, RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }

                val topCategory = categoryBreakdown.firstOrNull()
                val insights = calculateInsights(uiFilteredTransactions)

                // Weekday vs Weekend Spending
                val spendingTxns = bankFilteredTransactions.filter { it.transactionType == TransactionType.EXPENSE || it.transactionType == TransactionType.CREDIT }
                val weekdaySpending = spendingTxns.filter { it.dateTime.dayOfWeek.value in 1..5 }.sumOf { it.amount.toDouble() }.toBigDecimal()
                val weekendSpending = spendingTxns.filter { it.dateTime.dayOfWeek.value in 6..7 }.sumOf { it.amount.toDouble() }.toBigDecimal()

                // Day of week distribution
                val dayOfWeekMap = spendingTxns.groupBy { it.dateTime.dayOfWeek }
                    .mapValues { (_, txs) -> txs.sumOf { it.amount.toDouble() }.toBigDecimal() }

                // Transaction scale count
                val transactionScaleMap = spendingTxns.groupBy {
                    getTransactionScale(it.amount, filterState.currency)
                }.mapValues { it.value.size }

                // Projections
                val earliest = bankFilteredTransactions.minOfOrNull { it.dateTime.toLocalDate() }
                val latest = bankFilteredTransactions.maxOfOrNull { it.dateTime.toLocalDate() }
                val days = if (earliest != null && latest != null) {
                    ChronoUnit.DAYS.between(earliest, latest).toInt().coerceAtLeast(0) + 1
                } else 1

                val dailyAvg = if (days > 0) {
                    totalExpenses.divide(BigDecimal(days), 2, RoundingMode.HALF_UP)
                } else BigDecimal.ZERO

                val today = LocalDate.now()
                val projectedSpending = if (filterState.period == TimePeriod.THIS_MONTH) {
                    val daysInMonth = YearMonth.now().lengthOfMonth()
                    val elapsedDays = today.dayOfMonth
                    val remainingDays = (daysInMonth - elapsedDays).coerceAtLeast(0)
                    totalExpenses + (dailyAvg * BigDecimal(remainingDays))
                } else {
                    BigDecimal.ZERO
                }

                // Trend series based on selected period
                val durationDays = ChronoUnit.DAYS.between(dateRange.first, dateRange.second).toInt() + 1
                val (weeklyTrend, monthlyTrend) = when {
                    durationDays <= 14 -> {
                        buildTrendSeries(uiFilteredTransactions, dateRange.first, dateRange.second) to emptyList()
                    }
                    else -> {
                        val midDate = dateRange.second.minusDays(14)
                        val startForShort = if (midDate.isAfter(dateRange.first)) midDate else dateRange.first
                        buildTrendSeries(uiFilteredTransactions, startForShort, dateRange.second) to 
                            buildWeeklyTrendSeries(uiFilteredTransactions, dateRange.first, dateRange.second)
                    }
                }

                AnalyticsUiState(
                    totalSpending = totalSpending,
                    totalIncome = totalIncome,
                    netSavingsRate = netSavingsRate,
                    categoryBreakdown = categoryBreakdown,
                    topMerchants = merchantBreakdown,
                    transactionCount = uiFilteredTransactions.size,
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
                    bankTransactionCounts = bankCounts,
                    weekdaySpending = weekdaySpending,
                    weekendSpending = weekendSpending,
                    dayOfWeekDistribution = dayOfWeekMap,
                    transactionScaleCount = transactionScaleMap,
                    projectedSpending = projectedSpending
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

    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        require(startDate <= endDate) {
            "Start date ($startDate) must be before or equal to end date ($endDate)"
        }
        savedStateHandle["customDateRange"] = startDate.toEpochDay() to endDate.toEpochDay()
        _selectedPeriod.value = TimePeriod.CUSTOM
    }

    fun clearCustomDateRange() {
        savedStateHandle["customDateRange"] = null
        if (_selectedPeriod.value == TimePeriod.CUSTOM) {
            _selectedPeriod.value = TimePeriod.THIS_MONTH
        }
    }

    private fun getTransactionScale(amount: BigDecimal, currency: String): String {
        val amountVal = amount.toDouble()
        return if (currency == "INR") {
            when {
                amountVal <= 200.0 -> "Small"
                amountVal <= 2000.0 -> "Medium"
                else -> "Large"
            }
        } else {
            when {
                amountVal <= 10.0 -> "Small"
                amountVal <= 100.0 -> "Medium"
                else -> "Large"
            }
        }
    }

    private fun calculateInsights(transactions: List<TransactionEntity>): AnalyticsInsights {
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

    private fun buildTrendSeries(
        transactions: List<TransactionEntity>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TrendPoint> {
        val durationDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val totalsByDate = transactions
            .groupBy { it.dateTime.toLocalDate() }
            .mapValues { (_, txns) -> txns.sumOf { it.amount.toDouble() }.toBigDecimal() }

        if (durationDays <= 2) {
            // Group by hour intervals (12 intervals of 2 hours each)
            val hourlyTotals = transactions.groupBy { it.dateTime.hour / 2 }
            return (0..11).map { interval ->
                val startHour = interval * 2
                val ampm = if (startHour >= 12) "PM" else "AM"
                val displayHour = when {
                    startHour == 0 -> 12
                    startHour > 12 -> startHour - 12
                    else -> startHour
                }
                val label = "$displayHour$ampm"
                val amount = hourlyTotals[interval]?.sumOf { it.amount.toDouble() }?.toBigDecimal() ?: BigDecimal.ZERO
                TrendPoint(label, amount)
            }
        }

        return (0 until durationDays).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val label = date.format(DateTimeFormatter.ofPattern("d MMM"))
            TrendPoint(
                label = label,
                amount = totalsByDate[date] ?: BigDecimal.ZERO
            )
        }
    }

    private fun buildWeeklyTrendSeries(
        transactions: List<TransactionEntity>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TrendPoint> {
        val durationDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val weekCount = ((durationDays + 6) / 7).coerceAtLeast(1)
        val weeklyTotals = mutableMapOf<Int, BigDecimal>()

        transactions.forEach { tx ->
            val daysFromStart = ChronoUnit.DAYS.between(startDate, tx.dateTime.toLocalDate()).toInt()
            val weekIdx = (daysFromStart / 7).coerceIn(0, weekCount - 1)
            val currentSum = weeklyTotals[weekIdx] ?: BigDecimal.ZERO
            weeklyTotals[weekIdx] = currentSum + tx.amount
        }

        return (0 until weekCount).map { weekIdx ->
            val weekStart = startDate.plusDays(weekIdx * 7L)
            val label = "W${weekIdx + 1} (${weekStart.format(DateTimeFormatter.ofPattern("d MMM"))})"
            weeklyTotals[weekIdx]?.let {
                TrendPoint(label, it)
            } ?: TrendPoint(label, BigDecimal.ZERO)
        }
    }
}

private data class FilterState(
    val period: TimePeriod,
    val customRange: Pair<LocalDate, LocalDate>?,
    val typeFilter: TransactionTypeFilter,
    val currency: String,
    val bank: String?
)

data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val netSavingsRate: Float = 0f,
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
    val weekdaySpending: BigDecimal = BigDecimal.ZERO,
    val weekendSpending: BigDecimal = BigDecimal.ZERO,
    val dayOfWeekDistribution: Map<DayOfWeek, BigDecimal> = emptyMap(),
    val transactionScaleCount: Map<String, Int> = emptyMap(),
    val projectedSpending: BigDecimal = BigDecimal.ZERO
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
