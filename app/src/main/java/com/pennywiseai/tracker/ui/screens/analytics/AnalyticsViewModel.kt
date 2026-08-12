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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: com.pennywiseai.tracker.data.preferences.UserPreferencesRepository,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    private val _transactionTypeFilter = MutableStateFlow(TransactionTypeFilter.EXPENSE)
    val transactionTypeFilter: StateFlow<TransactionTypeFilter> = _transactionTypeFilter.asStateFlow()

    private val _selectedBank = MutableStateFlow<String?>(null)
    val selectedBank: StateFlow<String?> = _selectedBank.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("INR")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val allTx = transactionRepository.getAllTransactions().first()
                if (allTx.isNotEmpty()) {
                    val mostCommonCurrency = allTx.groupBy { it.currency }
                        .maxByOrNull { it.value.size }?.key
                    mostCommonCurrency?.let { _selectedCurrency.value = it }

                    val now = LocalDate.now()
                    val startOfMonth = now.withDayOfMonth(1)
                    val hasCurrentMonthTx = allTx.any {
                        !it.dateTime.toLocalDate().isBefore(startOfMonth)
                    }

                    if (!hasCurrentMonthTx) {
                        val startOfLastMonth = now.minusMonths(1).withDayOfMonth(1)
                        val hasLastMonthTx = allTx.any {
                            val date = it.dateTime.toLocalDate()
                            !date.isBefore(startOfLastMonth) && date.isBefore(startOfMonth)
                        }
                        _selectedPeriod.value = if (hasLastMonthTx) {
                            TimePeriod.LAST_MONTH
                        } else {
                            TimePeriod.ALL
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore initialization failures
            }
        }
    }

    private val _customDateRangeEpochDays =
        savedStateHandle.getStateFlow<Pair<Long, Long>?>("customDateRange", null)

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

    private val _activeTab = MutableStateFlow(AnalyticsTab.SNAPSHOT)
    val activeTab: StateFlow<AnalyticsTab> = _activeTab.asStateFlow()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedPeriod,
        customDateRange,
        _transactionTypeFilter,
        _selectedCurrency,
        _selectedBank,
        userPreferencesRepository.userPreferences
    ) { args: Array<Any?> ->
        val period = args[0] as TimePeriod
        val customRange = args[1] as? Pair<LocalDate, LocalDate>
        val typeFilter = args[2] as TransactionTypeFilter
        val currency = args[3] as String
        val bank = args[4] as? String
        val prefs = args[5] as com.pennywiseai.tracker.data.preferences.UserPreferences
        FilterState(
            period = period,
            customRange = customRange,
            typeFilter = typeFilter,
            currency = currency,
            bank = bank,
            budgetLimit = prefs.budgetLimit,
            budgetBank = prefs.budgetBank
        )
    }.flatMapLatest { filterState ->
        val dateRange = if (filterState.period == TimePeriod.CUSTOM) {
            val customRange = filterState.customRange
            if (customRange == null) {
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
            val previousRange = previousEqualLengthRange(dateRange.first, dateRange.second)
            val sixMonthStart = LocalDate.now().minusMonths(5).withDayOfMonth(1)
            val sixMonthEnd = LocalDate.now()

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
                    _selectedCurrency.value =
                        if (allCurrencies.contains("INR")) "INR" else allCurrencies.first()
                }

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
                    ),
                    transactionRepository.getTransactionsFiltered(
                        startDate = previousRange.first,
                        endDate = previousRange.second,
                        currency = filterState.currency,
                        transactionType = null
                    )
                ) { currencyTransactions, sixMonthTransactions, previousTransactions ->
                    Triple(currencyTransactions, sixMonthTransactions, previousTransactions)
                }
            }.map { (currencyTransactions, sixMonthTransactions, previousTransactions) ->
                buildUiState(
                    filterState = filterState,
                    dateRange = dateRange,
                    previousRange = previousRange,
                    currencyTransactions = currencyTransactions,
                    sixMonthTransactions = sixMonthTransactions,
                    previousTransactions = previousTransactions
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

    fun updateBudgetSettings(limit: Float, bank: String?) {
        viewModelScope.launch {
            userPreferencesRepository.updateBudgetSettings(limit, bank)
        }
    }

    private fun buildUiState(
        filterState: FilterState,
        dateRange: Pair<LocalDate, LocalDate>,
        previousRange: Pair<LocalDate, LocalDate>,
        currencyTransactions: List<TransactionEntity>,
        sixMonthTransactions: List<TransactionEntity>,
        previousTransactions: List<TransactionEntity>
    ): AnalyticsUiState {
        val budgetState = calculateBudgetState(
            limit = filterState.budgetLimit,
            bank = filterState.budgetBank,
            sixMonthTransactions = sixMonthTransactions,
            currency = filterState.currency
        )

        val bankCounts = currencyTransactions
            .groupBy { it.bankName ?: "Unknown Bank" }
            .mapValues { it.value.size }

        val availableBanks = bankCounts.keys.sorted()
        val effectiveSelectedBank = filterState.bank?.takeIf { availableBanks.contains(it) }
        if (effectiveSelectedBank != filterState.bank) {
            _selectedBank.value = effectiveSelectedBank
        }

        fun applyBank(txns: List<TransactionEntity>) =
            effectiveSelectedBank?.let { bank ->
                txns.filter { (it.bankName ?: "Unknown Bank") == bank }
            } ?: txns

        val bankFilteredTransactions = applyBank(currencyTransactions)
        val sixMonthBankFiltered = applyBank(sixMonthTransactions)
        val previousBankFiltered = applyBank(previousTransactions)

        fun applyTypeFilter(txns: List<TransactionEntity>) = when (filterState.typeFilter) {
            TransactionTypeFilter.ALL -> txns
            TransactionTypeFilter.INCOME -> txns.filter { it.transactionType == TransactionType.INCOME }
            TransactionTypeFilter.EXPENSE -> txns.filter { it.transactionType == TransactionType.EXPENSE }
            TransactionTypeFilter.CREDIT -> txns.filter { it.transactionType == TransactionType.CREDIT }
            TransactionTypeFilter.TRANSFER -> txns.filter { it.transactionType == TransactionType.TRANSFER }
            TransactionTypeFilter.INVESTMENT -> txns.filter { it.transactionType == TransactionType.INVESTMENT }
        }

        val uiFilteredTransactions = applyTypeFilter(bankFilteredTransactions)
        val previousUiFiltered = applyTypeFilter(previousBankFiltered)

        // Totals respect type filter for hero / period metrics
        val periodTotal = uiFilteredTransactions.sumOf { it.amount.toDouble() }.toBigDecimal()

        // Income vs expense always from bank-filtered P&L (true period picture)
        val totalIncome = bankFilteredTransactions
            .filter { it.transactionType == TransactionType.INCOME }
            .sumOf { it.amount.toDouble() }
            .toBigDecimal()

        val totalExpenses = bankFilteredTransactions
            .filter {
                it.transactionType == TransactionType.EXPENSE ||
                    it.transactionType == TransactionType.CREDIT
            }
            .sumOf { it.amount.toDouble() }
            .toBigDecimal()

        // When a specific type is selected, Snapshot expense/income cards use filtered totals
        val displayIncome = when (filterState.typeFilter) {
            TransactionTypeFilter.ALL, TransactionTypeFilter.INCOME ->
                if (filterState.typeFilter == TransactionTypeFilter.INCOME) periodTotal else totalIncome
            else -> totalIncome
        }
        val displayExpense = when (filterState.typeFilter) {
            TransactionTypeFilter.ALL -> totalExpenses
            TransactionTypeFilter.EXPENSE, TransactionTypeFilter.CREDIT -> periodTotal
            else -> totalExpenses
        }
        val netSavings = displayIncome - displayExpense
        val netSavingsRate = if (displayIncome > BigDecimal.ZERO) {
            ((displayIncome - displayExpense)
                .divide(displayIncome, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
        } else {
            0f
        }

        val categoryBreakdown = uiFilteredTransactions
            .groupBy { it.category ?: "Others" }
            .map { (categoryName, txns) ->
                val categoryTotal = txns.sumOf { it.amount.toDouble() }.toBigDecimal()
                CategoryData(
                    name = categoryName,
                    amount = categoryTotal,
                    percentage = if (periodTotal > BigDecimal.ZERO) {
                        (categoryTotal.divide(periodTotal, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
                    } else 0f,
                    transactionCount = txns.size
                )
            }
            .sortedByDescending { it.amount }

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
            periodTotal.divide(BigDecimal(uiFilteredTransactions.size), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val topCategory = categoryBreakdown.firstOrNull()
        val insights = calculateInsights(uiFilteredTransactions)

        val spendingForPatterns = when (filterState.typeFilter) {
            TransactionTypeFilter.ALL -> bankFilteredTransactions.filter {
                it.transactionType == TransactionType.EXPENSE ||
                    it.transactionType == TransactionType.CREDIT
            }
            else -> uiFilteredTransactions
        }

        val weekdaySpending = spendingForPatterns
            .filter { it.dateTime.dayOfWeek.value in 1..5 }
            .sumOf { it.amount.toDouble() }
            .toBigDecimal()
        val weekendSpending = spendingForPatterns
            .filter { it.dateTime.dayOfWeek.value in 6..7 }
            .sumOf { it.amount.toDouble() }
            .toBigDecimal()

        val dayOfWeekMap = spendingForPatterns.groupBy { it.dateTime.dayOfWeek }
            .mapValues { (_, txs) -> txs.sumOf { it.amount.toDouble() }.toBigDecimal() }

        val patternTotal = spendingForPatterns.sumOf { it.amount.toDouble() }.toBigDecimal()
        val weekdayPattern = DayOfWeek.entries.map { dow ->
            val amount = dayOfWeekMap[dow] ?: BigDecimal.ZERO
            val count = spendingForPatterns.count { it.dateTime.dayOfWeek == dow }
            val pct = if (patternTotal > BigDecimal.ZERO) {
                (amount.divide(patternTotal, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
            } else 0f
            WeekdayDistribution(dow, amount, count, pct)
        }

        val peakDays = spendingForPatterns
            .groupBy { it.dateTime.dayOfMonth }
            .map { (day, txns) ->
                DayOfMonthDistribution(
                    day = day,
                    amount = txns.sumOf { it.amount.toDouble() }.toBigDecimal(),
                    transactionCount = txns.size
                )
            }
            .sortedByDescending { it.amount }
            .take(8)

        val transactionScaleMap = spendingForPatterns.groupBy {
            getTransactionScale(it.amount, filterState.currency)
        }.mapValues { it.value.size }

        val earliest = uiFilteredTransactions.minOfOrNull { it.dateTime.toLocalDate() }
        val latest = uiFilteredTransactions.maxOfOrNull { it.dateTime.toLocalDate() }
        val days = if (earliest != null && latest != null) {
            ChronoUnit.DAYS.between(earliest, latest).toInt().coerceAtLeast(0) + 1
        } else 1

        val dailyAvg = if (days > 0 && periodTotal > BigDecimal.ZERO) {
            periodTotal.divide(BigDecimal(days), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val today = LocalDate.now()
        val projectedSpending = if (filterState.period == TimePeriod.THIS_MONTH && dailyAvg > BigDecimal.ZERO) {
            val daysInMonth = YearMonth.now().lengthOfMonth()
            val elapsedDays = today.dayOfMonth
            val remainingDays = (daysInMonth - elapsedDays).coerceAtLeast(0)
            periodTotal + (dailyAvg * BigDecimal(remainingDays))
        } else {
            BigDecimal.ZERO
        }

        val pacePercent = if (filterState.period == TimePeriod.THIS_MONTH) {
            computePacePercent(
                periodTotal = periodTotal,
                sixMonthTransactions = applyTypeFilter(sixMonthBankFiltered),
                today = today
            )
        } else null

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

        val comparisonData = if (filterState.period != TimePeriod.ALL) {
            buildComparisonData(
                current = uiFilteredTransactions,
                previous = previousUiFiltered,
                currentRange = dateRange,
                previousRange = previousRange,
                bankFilteredCurrent = bankFilteredTransactions,
                bankFilteredPrevious = previousBankFiltered
            )
        } else null

        val chartSource = when (filterState.typeFilter) {
            TransactionTypeFilter.ALL -> bankFilteredTransactions.filter {
                it.transactionType == TransactionType.EXPENSE ||
                    it.transactionType == TransactionType.CREDIT
            }
            else -> uiFilteredTransactions
        }

        return AnalyticsUiState(
            totalSpending = periodTotal,
            totalIncome = displayIncome,
            totalExpense = displayExpense,
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
            activeTab = _activeTab.value,
            dailyTrend = buildDailySpending(chartSource, dateRange.first, dateRange.second),
            weeklyBreakdown = buildWeeklyBreakdown(chartSource, dateRange.first, dateRange.second),
            monthlyBreakdown = buildMonthlyBreakdown(bankFilteredTransactions),
            yearlyBreakdown = buildMonthlyBreakdown(bankFilteredTransactions),
            categoryTrends = buildCategoryTrends(uiFilteredTransactions),
            weekdayPattern = weekdayPattern,
            peakDays = peakDays,
            comparisonData = comparisonData,
            bankComparison = buildBankComparison(bankFilteredTransactions),
            anomalousTransactions = buildAnomalousDetection(uiFilteredTransactions),
            spendingVelocity = buildSpendingVelocity(chartSource, dateRange.first, dateRange.second),
            heatmapDays = buildHeatmapDays(sixMonthBankFiltered),
            sixMonthSpending = buildSixMonthSpending(sixMonthBankFiltered),
            todayAmount = insights.todayAmount,
            periodTotal = periodTotal,
            spendingChangePercent = comparisonData?.spendingChangePercent,
            pacePercent = pacePercent,
            budgetState = budgetState
        )
    }

    private fun computePacePercent(
        periodTotal: BigDecimal,
        sixMonthTransactions: List<TransactionEntity>,
        today: LocalDate
    ): Float? {
        val lastMonth = YearMonth.now().minusMonths(1)
        val lastMonthTotal = sixMonthTransactions
            .filter { YearMonth.from(it.dateTime) == lastMonth }
            .sumOf { it.amount.toDouble() }
            .toBigDecimal()
        if (lastMonthTotal <= BigDecimal.ZERO) return null

        val elapsed = today.dayOfMonth.coerceAtLeast(1)
        val expectedToDate = lastMonthTotal
            .multiply(BigDecimal(elapsed))
            .divide(BigDecimal(lastMonth.lengthOfMonth()), 4, RoundingMode.HALF_UP)

        if (expectedToDate <= BigDecimal.ZERO) return null
        return ((periodTotal - expectedToDate)
            .divide(expectedToDate, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
    }

    private fun buildComparisonData(
        current: List<TransactionEntity>,
        previous: List<TransactionEntity>,
        currentRange: Pair<LocalDate, LocalDate>,
        previousRange: Pair<LocalDate, LocalDate>,
        bankFilteredCurrent: List<TransactionEntity>,
        bankFilteredPrevious: List<TransactionEntity>
    ): ComparisonData {
        fun summarize(
            filtered: List<TransactionEntity>,
            bankFiltered: List<TransactionEntity>,
            label: String
        ): PeriodSummary {
            val spending = filtered.sumOf { it.amount.toDouble() }.toBigDecimal()
            val income = bankFiltered
                .filter { it.transactionType == TransactionType.INCOME }
                .sumOf { it.amount.toDouble() }
                .toBigDecimal()
            val avg = if (filtered.isNotEmpty()) {
                spending.divide(BigDecimal(filtered.size), 2, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            val topCat = filtered
                .groupBy { it.category ?: "Others" }
                .maxByOrNull { it.value.sumOf { t -> t.amount.toDouble() } }
                ?.key ?: ""
            return PeriodSummary(label, spending, income, filtered.size, avg, topCat)
        }

        val formatter = DateTimeFormatter.ofPattern("d MMM")
        val currentLabel = "${currentRange.first.format(formatter)} – ${currentRange.second.format(formatter)}"
        val previousLabel = "${previousRange.first.format(formatter)} – ${previousRange.second.format(formatter)}"

        val currentSummary = summarize(current, bankFilteredCurrent, currentLabel)
        val previousSummary = summarize(previous, bankFilteredPrevious, previousLabel)

        val spendingChange = currentSummary.totalSpending - previousSummary.totalSpending
        val spendingChangePercent = if (previousSummary.totalSpending > BigDecimal.ZERO) {
            (spendingChange.divide(previousSummary.totalSpending, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
        } else 0f

        val incomeChange = currentSummary.totalIncome - previousSummary.totalIncome
        val incomeChangePercent = if (previousSummary.totalIncome > BigDecimal.ZERO) {
            (incomeChange.divide(previousSummary.totalIncome, 4, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
        } else 0f

        return ComparisonData(
            currentPeriod = currentSummary,
            previousPeriod = previousSummary,
            spendingChange = spendingChange,
            spendingChangePercent = spendingChangePercent,
            incomeChange = incomeChange,
            incomeChangePercent = incomeChangePercent
        )
    }

    private fun previousEqualLengthRange(
        start: LocalDate,
        end: LocalDate
    ): Pair<LocalDate, LocalDate> {
        val days = ChronoUnit.DAYS.between(start, end)
        val prevEnd = start.minusDays(1)
        val prevStart = prevEnd.minusDays(days)
        return prevStart to prevEnd
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
        val avgAmount = if (totalCount > 0) {
            totalAmount.divide(BigDecimal(totalCount), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val largest = transactions.maxByOrNull { it.amount }

        val topCategory = transactions
            .groupBy { it.category ?: "Others" }
            .mapValues { entry ->
                entry.value.sumOf { it.amount.toDouble() }.toBigDecimal() to entry.value.size
            }
            .maxByOrNull { it.value.first }
            ?.let { (name, value) ->
                HighlightedGroup(name = name, amount = value.first, count = value.second)
            }

        val topMerchant = transactions
            .groupBy { it.merchantName }
            .mapValues { entry ->
                entry.value.sumOf { it.amount.toDouble() }.toBigDecimal() to entry.value.size
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
            recurringCount = transactions.count { it.isRecurring }
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
            val hourlyTotals = transactions.groupBy { it.dateTime.hour / 2 }
            return (0..11).map { interval ->
                val startHour = interval * 2
                val ampm = if (startHour >= 12) "PM" else "AM"
                val displayHour = when {
                    startHour == 0 -> 12
                    startHour > 12 -> startHour - 12
                    else -> startHour
                }
                TrendPoint(
                    "$displayHour$ampm",
                    hourlyTotals[interval]?.sumOf { it.amount.toDouble() }?.toBigDecimal()
                        ?: BigDecimal.ZERO
                )
            }
        }

        return (0 until durationDays).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            TrendPoint(
                label = date.format(DateTimeFormatter.ofPattern("d MMM")),
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
            weeklyTotals[weekIdx] = (weeklyTotals[weekIdx] ?: BigDecimal.ZERO) + tx.amount
        }

        return (0 until weekCount).map { weekIdx ->
            val weekStart = startDate.plusDays(weekIdx * 7L)
            val label = "W${weekIdx + 1} (${weekStart.format(DateTimeFormatter.ofPattern("d MMM"))})"
            TrendPoint(label, weeklyTotals[weekIdx] ?: BigDecimal.ZERO)
        }
    }

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
            val txns = transactions.filter {
                val d = it.dateTime.toLocalDate()
                !d.isBefore(ws) && !d.isAfter(we)
            }
            result.add(
                WeeklySpending(
                    ws,
                    txns.sumOf { it.amount.toDouble() }.toBigDecimal(),
                    txns.size
                )
            )
            ws = ws.plusWeeks(1)
        }
        return result
    }

    private fun buildMonthlyBreakdown(
        transactions: List<TransactionEntity>
    ): List<MonthlySpending> = transactions.groupBy { YearMonth.from(it.dateTime) }
        .map { (ym, txns) ->
            MonthlySpending(
                ym,
                txns.sumOf { it.amount.toDouble() }.toBigDecimal(),
                txns.filter { it.transactionType == TransactionType.INCOME }
                    .sumOf { it.amount.toDouble() }.toBigDecimal(),
                txns.filter { it.transactionType == TransactionType.EXPENSE }
                    .sumOf { it.amount.toDouble() }.toBigDecimal(),
                txns.size
            )
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
            val changePct = if (sorted.size >= 2 &&
                (monthlyAmounts[sorted.first()] ?: BigDecimal.ZERO) > BigDecimal.ZERO
            ) {
                ((monthlyAmounts[sorted.last()]!! - monthlyAmounts[sorted.first()]!!)
                    .divide(monthlyAmounts[sorted.first()]!!, 4, RoundingMode.HALF_UP) * BigDecimal(100))
                    .toFloat()
            } else 0f
            CategoryTrend(cat, monthlyAmounts, direction, changePct)
        }
    }

    private fun buildBankComparison(
        transactions: List<TransactionEntity>
    ): List<BankSummary> = transactions.groupBy { it.bankName ?: "Unknown Bank" }
        .map { (bank, txns) ->
            val income = txns.filter { it.transactionType == TransactionType.INCOME }
                .sumOf { it.amount.toDouble() }.toBigDecimal()
            val expenses = txns.filter { it.transactionType == TransactionType.EXPENSE }
                .sumOf { it.amount.toDouble() }.toBigDecimal()
            BankSummary(bank, income, expenses, income - expenses, txns.size)
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
            if (abs(zScore) > 2.0) {
                AnomalousTransaction(
                    tx.id, tx.merchantName, tx.amount, tx.dateTime,
                    tx.category ?: "Others", zScore, zScore > 0
                )
            } else null
        }.sortedByDescending { abs(it.zScore) }.take(10)
    }

    private fun buildSpendingVelocity(
        transactions: List<TransactionEntity>, start: LocalDate, end: LocalDate
    ): SpendingVelocity {
        val days = ChronoUnit.DAYS.between(start, end).coerceAtLeast(0) + 1
        val total = transactions.sumOf { it.amount.toDouble() }.toBigDecimal()
        val dailyAvg = if (days > 0) {
            total.divide(BigDecimal(days), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        return SpendingVelocity(
            dailyAvg,
            dailyAvg * BigDecimal(7),
            dailyAvg * BigDecimal(30),
            TrendDirection.STABLE
        )
    }

    private fun buildHeatmapDays(
        transactions: List<TransactionEntity>
    ): Map<LocalDate, HeatmapDay> {
        val today = LocalDate.now()
        val sixMonthsAgo = today.minusMonths(5).withDayOfMonth(1)
        return transactions
            .filter { !it.dateTime.toLocalDate().isBefore(sixMonthsAgo) }
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
                HeatmapDay(date, expense, income, count)
            }
            .filterValues { !it.isEmpty }
    }

    private fun buildSixMonthSpending(
        transactions: List<TransactionEntity>
    ): List<MonthlySpending> {
        val today = LocalDate.now()
        val sixMonthsAgo = today.minusMonths(5).withDayOfMonth(1)
        val rangeTransactions = transactions.filter {
            !it.dateTime.toLocalDate().isBefore(sixMonthsAgo)
        }

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

    private fun calculateBudgetState(
        limit: Float,
        bank: String?,
        sixMonthTransactions: List<TransactionEntity>,
        currency: String
    ): BudgetUiState {
        if (limit <= 0f) return BudgetUiState()

        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1)

        val currentMonthExpenses = sixMonthTransactions.filter { txn ->
            val date = txn.dateTime.toLocalDate()
            !date.isBefore(startOfMonth) && !date.isAfter(today) &&
                    txn.transactionType == TransactionType.EXPENSE &&
                    txn.currency == currency &&
                    (bank == null || txn.bankName == bank)
        }

        val currentSpent = currentMonthExpenses.sumOf { it.amount.toDouble() }.toFloat()

        val currentDayOfMonth = today.dayOfMonth
        val totalDaysInMonth = today.lengthOfMonth()
        val remainingDays = totalDaysInMonth - currentDayOfMonth

        val dailyBurnRate = if (currentDayOfMonth > 0) {
            currentSpent / currentDayOfMonth
        } else {
            0f
        }

        val projectedSpending = dailyBurnRate * totalDaysInMonth

        val isExceeded = currentSpent > limit
        val willExceed = projectedSpending > limit

        val exceedDayOfMonth = if (dailyBurnRate > 0f) {
            val day = (limit / dailyBurnRate).toInt()
            day.coerceIn(1, totalDaysInMonth)
        } else {
            0
        }

        return BudgetUiState(
            limit = limit,
            bank = bank,
            currentSpent = currentSpent,
            dailyBurnRate = dailyBurnRate,
            projectedSpending = projectedSpending,
            isExceeded = isExceeded,
            willExceed = willExceed,
            exceedDayOfMonth = exceedDayOfMonth,
            remainingDays = remainingDays
        )
    }
}

private data class FilterState(
    val period: TimePeriod,
    val customRange: Pair<LocalDate, LocalDate>?,
    val typeFilter: TransactionTypeFilter,
    val currency: String,
    val bank: String?,
    val budgetLimit: Float,
    val budgetBank: String?
)
