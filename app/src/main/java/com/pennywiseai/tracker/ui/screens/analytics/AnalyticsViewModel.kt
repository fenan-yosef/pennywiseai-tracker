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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
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

    init {
        viewModelScope.launch {
            try {
                // Get all transactions to find the primary currency and see if current month has data
                val allTx = transactionRepository.getAllTransactions().first()
                if (allTx.isNotEmpty()) {
                    // 1. Determine primary currency from all transactions
                    val mostCommonCurrency = allTx.groupBy { it.currency }
                        .maxByOrNull { it.value.size }?.key
                    
                    mostCommonCurrency?.let {
                        _selectedCurrency.value = it
                    }
                    
                    // 2. Check if current month has any transactions in the database
                    val now = LocalDate.now()
                    val startOfMonth = now.withDayOfMonth(1)
                    val hasCurrentMonthTx = allTx.any { 
                        val date = it.dateTime.toLocalDate()
                        !date.isBefore(startOfMonth) 
                    }
                    
                    if (!hasCurrentMonthTx) {
                        // Current month is empty, check if last month has transactions
                        val startOfLastMonth = now.minusMonths(1).withDayOfMonth(1)
                        val hasLastMonthTx = allTx.any { 
                            val date = it.dateTime.toLocalDate()
                            !date.isBefore(startOfLastMonth) && date.isBefore(startOfMonth)
                        }
                        if (hasLastMonthTx) {
                            _selectedPeriod.value = TimePeriod.LAST_MONTH
                        } else {
                            // If neither, fallback to ALL
                            _selectedPeriod.value = TimePeriod.ALL
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore initialization failures
            }
        }
    }

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

    private val _activeTab = MutableStateFlow(AnalyticsTab.OVERVIEW)
    val activeTab: StateFlow<AnalyticsTab> = _activeTab.asStateFlow()

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

                // Period-scoped txns for analytics filters; separate 6-month window for
                // activity heatmap / six-month trend (not limited by selected period).
                val sixMonthStart = LocalDate.now().minusMonths(5).withDayOfMonth(1)
                val sixMonthEnd = LocalDate.now()
                combine(
                    transactionRepository.getTransactionsFiltered(
                        startDate = dateRange.first,
                        endDate = dateRange.second,
                        currency = filterState.currency,
                        transactionType = null
                    ),
                    transactionRepository.getTransactionsFiltered(
                        startDate = sixMonthStart,
                        endDate = sixMonthEnd,
                        currency = filterState.currency,
                        transactionType = null
                    )
                ) { currencyTransactions, sixMonthTransactions ->
                    currencyTransactions to sixMonthTransactions
                }
            }.map { (currencyTransactions, sixMonthTransactions) ->
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

                val sixMonthBankFiltered = effectiveSelectedBank?.let { bank ->
                    sixMonthTransactions.filter { (it.bankName ?: "Unknown Bank") == bank }
                } ?: sixMonthTransactions

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

                // New computations
                val netSavings = totalIncome - totalExpenses
                val dailySpending = buildDailySpending(bankFilteredTransactions, dateRange.first, dateRange.second)
                val weeklyBreakdownList = buildWeeklyBreakdown(bankFilteredTransactions, dateRange.first, dateRange.second)
                val monthlyBreakdownList = buildMonthlyBreakdown(bankFilteredTransactions)
                val categoryTrendsList = buildCategoryTrends(bankFilteredTransactions)
                val bankComparisonList = buildBankComparison(bankFilteredTransactions)
                val anomalousList = buildAnomalousDetection(bankFilteredTransactions)
                val velocity = buildSpendingVelocity(bankFilteredTransactions, dateRange.first, dateRange.second)
                val overview = buildOverviewData(totalIncome, totalExpenses, netSavings, averageAmount,
                    bankFilteredTransactions, categoryBreakdown, merchantBreakdown)
                val heatmap = buildHeatmapDays(sixMonthBankFiltered)
                val sixMonthSpending = buildSixMonthSpending(sixMonthBankFiltered)

                AnalyticsUiState(
                    totalSpending = totalSpending,
                    totalIncome = totalIncome,
                    totalExpense = totalExpenses,
                    netSavings = netSavings,
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
                    projectedSpending = projectedSpending,
                    // New computed data:
                    activeTab = _activeTab.value,
                    overviewData = overview,
                    dailyTrend = dailySpending,
                    weeklyBreakdown = weeklyBreakdownList,
                    monthlyBreakdown = monthlyBreakdownList,
                    yearlyBreakdown = monthlyBreakdownList,
                    categoryTrends = categoryTrendsList,
                    bankComparison = bankComparisonList,
                    anomalousTransactions = anomalousList,
                    spendingVelocity = velocity,
                    heatmapDays = heatmap,
                    sixMonthSpending = sixMonthSpending
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

    fun selectTab(tab: AnalyticsTab) {
        _activeTab.value = tab
        val currentPeriod = _selectedPeriod.value
        when (tab) {
            AnalyticsTab.DAILY -> {
                val allowedDaily = listOf(
                    TimePeriod.TODAY,
                    TimePeriod.YESTERDAY,
                    TimePeriod.THIS_WEEK,
                    TimePeriod.LAST_WEEK,
                    TimePeriod.LAST_7_DAYS,
                    TimePeriod.LAST_30_DAYS,
                    TimePeriod.THIS_MONTH,
                    TimePeriod.LAST_MONTH,
                    TimePeriod.CUSTOM
                )
                if (currentPeriod !in allowedDaily) {
                    _selectedPeriod.value = TimePeriod.LAST_30_DAYS
                }
            }
            AnalyticsTab.TRENDS -> {
                val allowedTrends = listOf(
                    TimePeriod.LAST_MONTH,
                    TimePeriod.LAST_3_MONTHS,
                    TimePeriod.LAST_6_MONTHS,
                    TimePeriod.THIS_QUARTER,
                    TimePeriod.LAST_QUARTER,
                    TimePeriod.CURRENT_FY,
                    TimePeriod.LAST_YEAR,
                    TimePeriod.ALL,
                    TimePeriod.CUSTOM
                )
                if (currentPeriod !in allowedTrends) {
                    _selectedPeriod.value = TimePeriod.LAST_3_MONTHS
                }
            }
            else -> {}
        }
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

    // ==================== New Analytics Computations ====================

    private fun buildDailySpending(
        transactions: List<TransactionEntity>, start: LocalDate, end: LocalDate
    ): List<DailySpending> {
        val totals = transactions.groupBy { it.dateTime.toLocalDate() }
            .mapValues { (_, txns) -> txns.sumOf { it.amount.toDouble() }.toBigDecimal() to txns.size }
        return (0..ChronoUnit.DAYS.between(start, end)).map { offset ->
            val date = start.plusDays(offset)
            val (amount, count) = totals[date] ?: (BigDecimal.ZERO to 0)
            DailySpending(date, date.dayOfWeek, amount, count)
        }
    }

    private fun buildWeeklyBreakdown(
        transactions: List<TransactionEntity>, start: LocalDate, end: LocalDate
    ): List<WeeklySpending> {
        val result = mutableListOf<WeeklySpending>()
        var ws = start.minusDays((start.dayOfWeek.value - 1).toLong())
        while (ws <= end) {
            val we = ws.plusDays(6)
            val txns = transactions.filter { val d = it.dateTime.toLocalDate(); !d.isBefore(ws) && !d.isAfter(we) }
            result.add(WeeklySpending(ws, txns.sumOf { it.amount.toDouble() }.toBigDecimal(), txns.size))
            ws = ws.plusWeeks(1)
        }
        return result
    }

    private fun buildMonthlyBreakdown(
        transactions: List<TransactionEntity>
    ): List<MonthlySpending> = transactions.groupBy { YearMonth.from(it.dateTime) }
        .map { (ym, txns) ->
            MonthlySpending(ym,
                txns.sumOf { it.amount.toDouble() }.toBigDecimal(),
                txns.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount.toDouble() }.toBigDecimal(),
                txns.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount.toDouble() }.toBigDecimal(),
                txns.size)
        }.sortedBy { it.yearMonth }

    private fun buildCategoryTrends(
        transactions: List<TransactionEntity>
    ): List<CategoryTrend> {
        val topCategories = transactions.groupBy { it.category ?: "Others" }
            .mapValues { it.value.sumOf { t -> t.amount.toDouble() }.toBigDecimal() }
            .entries.sortedByDescending { it.value }.take(5).map { it.key }
        return topCategories.map { cat ->
            val monthlyAmounts = transactions.filter { (it.category ?: "Others") == cat }
                .groupBy { YearMonth.from(it.dateTime) }
                .mapValues { (_, txns) -> txns.sumOf { t -> t.amount.toDouble() }.toBigDecimal() }
            val sorted = monthlyAmounts.keys.sorted()
            val direction = if (sorted.size >= 2) {
                val first = monthlyAmounts[sorted.first()] ?: BigDecimal.ZERO
                val last = monthlyAmounts[sorted.last()] ?: BigDecimal.ZERO
                when {
                    last > first * BigDecimal("1.1") -> TrendDirection.RISING
                    last < first * BigDecimal("0.9") -> TrendDirection.FALLING
                    else -> TrendDirection.STABLE
                }
            } else TrendDirection.STABLE
            val changePct = if (sorted.size >= 2 && (monthlyAmounts[sorted.first()] ?: BigDecimal.ZERO) > BigDecimal.ZERO)
                ((monthlyAmounts[sorted.last()]!! - monthlyAmounts[sorted.first()]!!)
                    .divide(monthlyAmounts[sorted.first()]!!, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
            else 0f
            CategoryTrend(cat, monthlyAmounts, direction, changePct)
        }
    }

    private fun buildBankComparison(
        transactions: List<TransactionEntity>
    ): List<BankSummary> = transactions.groupBy { it.bankName ?: "Unknown Bank" }
        .map { (bank, txns) ->
            BankSummary(bank,
                txns.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount.toDouble() }.toBigDecimal(),
                txns.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount.toDouble() }.toBigDecimal(),
                BigDecimal.ZERO, txns.size)
        }.sortedByDescending { it.transactionCount }

    private fun buildAnomalousDetection(
        transactions: List<TransactionEntity>
    ): List<AnomalousTransaction> {
        if (transactions.size < 5) return emptyList()
        val amounts = transactions.map { it.amount.toDouble() }
        val mean = amounts.average()
        val stddev = sqrt(amounts.map { (it - mean) * (it - mean) }.average())
        if (stddev == 0.0) return emptyList()
        return transactions.mapNotNull { tx ->
            val zScore = (tx.amount.toDouble() - mean) / stddev
            if (abs(zScore) > 2.0)
                AnomalousTransaction(tx.id, tx.merchantName, tx.amount, tx.dateTime, tx.category ?: "Others", zScore, zScore > 0)
            else null
        }.sortedByDescending { abs(it.zScore) }.take(10)
    }

    private fun buildSpendingVelocity(
        transactions: List<TransactionEntity>, start: LocalDate, end: LocalDate
    ): SpendingVelocity {
        val days = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1) + 1
        val total = transactions.sumOf { it.amount.toDouble() }.toBigDecimal()
        val dailyAvg = if (days > 0) total.divide(BigDecimal(days), 2, RoundingMode.HALF_UP) else BigDecimal.ZERO
        return SpendingVelocity(dailyAvg, dailyAvg * BigDecimal(7), dailyAvg * BigDecimal(30), TrendDirection.STABLE)
    }

    private fun buildOverviewData(
        totalIncome: BigDecimal, totalExpense: BigDecimal, netSavings: BigDecimal,
        avgAmount: BigDecimal, bankFiltered: List<TransactionEntity>,
        categoryBreakdown: List<CategoryData>, topMerchants: List<MerchantData>
    ): OverviewData {
        val savingsRate = if (totalIncome > BigDecimal.ZERO)
            (netSavings.divide(totalIncome, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat() else 0f
        return OverviewData(totalIncome, totalExpense, netSavings, savingsRate, bankFiltered.size, avgAmount,
            categoryBreakdown.firstOrNull()?.name ?: "", categoryBreakdown.firstOrNull()?.amount ?: BigDecimal.ZERO,
            categoryBreakdown.firstOrNull()?.percentage ?: 0f,
            topMerchants.firstOrNull()?.name ?: "", topMerchants.firstOrNull()?.amount ?: BigDecimal.ZERO)
    }

    private fun buildHeatmapDays(
        transactions: List<TransactionEntity>
    ): Map<LocalDate, HeatmapDay> {
        val today = LocalDate.now()
        val sixMonthsAgo = today.minusMonths(5).withDayOfMonth(1)
        val rangeTransactions = transactions.filter {
            !it.dateTime.toLocalDate().isBefore(sixMonthsAgo)
        }
        return rangeTransactions
            .groupBy { it.dateTime.toLocalDate() }
            .mapValues { (date, txns) ->
                val expense = txns
                    .filter {
                        it.transactionType == TransactionType.EXPENSE ||
                            it.transactionType == TransactionType.CREDIT
                    }
                    .sumOf { it.amount.toDouble() }
                    .toBigDecimal()
                val income = txns
                    .filter { it.transactionType == TransactionType.INCOME }
                    .sumOf { it.amount.toDouble() }
                    .toBigDecimal()
                val count = txns.count {
                    it.transactionType == TransactionType.EXPENSE ||
                        it.transactionType == TransactionType.CREDIT ||
                        it.transactionType == TransactionType.INCOME
                }
                HeatmapDay(
                    date = date,
                    expense = expense,
                    income = income,
                    transactionCount = count
                )
            }
            .filterValues { !it.isEmpty }
    }

    private fun buildSixMonthSpending(
        transactions: List<TransactionEntity>
    ): List<MonthlySpending> {
        val today = LocalDate.now()
        val sixMonthsAgo = today.minusMonths(5).withDayOfMonth(1)
        val rangeTransactions = if (transactions.isNotEmpty()) {
            // Filter existing loaded transactions to last 6 months
            transactions.filter { !it.dateTime.toLocalDate().isBefore(sixMonthsAgo) }
        } else {
            emptyList()
        }

        // Build complete 6-month series including months with zero spending
        return (0 until 6).map { offset ->
            val ym = YearMonth.from(sixMonthsAgo.plusMonths(offset.toLong()))
            val monthTxns = rangeTransactions.filter { YearMonth.from(it.dateTime) == ym }
            MonthlySpending(
                yearMonth = ym,
                amount = monthTxns.sumOf { it.amount.toDouble() }.toBigDecimal(),
                income = monthTxns.filter { it.transactionType == TransactionType.INCOME }
                    .sumOf { it.amount.toDouble() }.toBigDecimal(),
                expenses = monthTxns.filter { it.transactionType == TransactionType.EXPENSE }
                    .sumOf { it.amount.toDouble() }.toBigDecimal(),
                transactionCount = monthTxns.size
            )
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
