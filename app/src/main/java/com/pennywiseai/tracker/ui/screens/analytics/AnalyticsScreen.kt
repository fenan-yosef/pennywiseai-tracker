package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.ui.screens.analytics.AnalyticsInsights
import com.pennywiseai.tracker.ui.components.*
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.DateRangeUtils
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: (category: String?, merchant: String?, period: String?, currency: String?) -> Unit = { _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactionTypeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val selectedBank by viewModel.selectedBank.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Calculate active filter count
    val activeFilterCount = if (transactionTypeFilter != TransactionTypeFilter.EXPENSE) 1 else 0

    // Cache expensive operations
    val timePeriods = remember { TimePeriod.values().toList() }
    val customRangeLabel = remember(customDateRange) {
        DateRangeUtils.formatDateRange(customDateRange)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = Dimensions.Padding.content,
            end = Dimensions.Padding.content,
            top = Spacing.md,
            bottom = Dimensions.Component.bottomBarHeight + Spacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Period Selector - Always visible
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(timePeriods) { period ->
                    FilterChip(
                        // Only show CUSTOM as selected if both period is CUSTOM AND dates are set
                        selected = if (period == TimePeriod.CUSTOM) {
                            selectedPeriod == period && customDateRange != null
                        } else {
                            selectedPeriod == period
                        },
                        onClick = {
                            if (period == TimePeriod.CUSTOM) {
                                showDateRangePicker = true
                                // Don't change selectedPeriod until user confirms dates
                            } else {
                                viewModel.selectPeriod(period)
                            }
                        },
                        label = {
                            Text(
                                if (period == TimePeriod.CUSTOM && customRangeLabel != null) {
                                    customRangeLabel
                                } else {
                                    period.label
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Bank Tabs (All + per-bank) similar to Home
        if (uiState.banks.isNotEmpty()) {
            item {
                AnalyticsBankTabs(
                    banks = uiState.banks,
                    selectedBank = selectedBank,
                    counts = uiState.bankTransactionCounts,
                    onSelect = { viewModel.selectBank(it) }
                )
            }
        }

        // Currency Selector (if multiple currencies available)
        if (availableCurrencies.size > 1) {
            item {
                CurrencyFilterRow(
                    selectedCurrency = selectedCurrency,
                    availableCurrencies = availableCurrencies,
                    onCurrencySelected = { viewModel.selectCurrency(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Collapsible Transaction Type Filter
        item {
            CollapsibleFilterRow(
                isExpanded = showAdvancedFilters,
                activeFilterCount = activeFilterCount,
                onToggle = { showAdvancedFilters = !showAdvancedFilters },
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(TransactionTypeFilter.values().toList()) { typeFilter ->
                        FilterChip(
                            selected = transactionTypeFilter == typeFilter,
                            onClick = { viewModel.setTransactionTypeFilter(typeFilter) },
                            label = { Text(typeFilter.label) },
                            leadingIcon = if (transactionTypeFilter == typeFilter) {
                                {
                                    when (typeFilter) {
                                        TransactionTypeFilter.INCOME -> Icon(
                                            Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.EXPENSE -> Icon(
                                            Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.CREDIT -> Icon(
                                            Icons.Default.CreditCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.TRANSFER -> Icon(
                                            Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.INVESTMENT -> Icon(
                                            Icons.AutoMirrored.Filled.ShowChart,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        else -> null
                                    }
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
            }
        }
        
        // Analytics Summary Card
        if (uiState.totalSpending > BigDecimal.ZERO || uiState.transactionCount > 0) {
            item {
                AnalyticsSummaryCard(
                    totalAmount = uiState.totalSpending,
                    transactionCount = uiState.transactionCount,
                    averageAmount = uiState.averageAmount,
                    topCategory = uiState.topCategory,
                    topCategoryPercentage = uiState.topCategoryPercentage,
                    currency = uiState.currency,
                    isLoading = uiState.isLoading
                )
            }
        }

        // Insights Card
        if (uiState.transactionCount > 0) {
            item {
                AnalyticsInsightsCard(
                    insights = uiState.insights,
                    currency = uiState.currency,
                    isLoading = uiState.isLoading
                )
            }
        }

        // Trends (weekly + monthly)
        if (uiState.transactionCount > 0) {
            item {
                AnalyticsTrendsCard(
                    weeklyTrend = uiState.weeklyTrend,
                    monthlyTrend = uiState.monthlyTrend,
                    currency = uiState.currency,
                    isLoading = uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Category Breakdown Section
        if (uiState.categoryBreakdown.isNotEmpty()) {
            item {
                CategoryBreakdownCard(
                    categories = uiState.categoryBreakdown,
                    currency = selectedCurrency,
                    onCategoryClick = { category ->
                        onNavigateToTransactions(category.name, null, selectedPeriod.name, selectedCurrency)
                    }
                )
            }
        }
        
        // Top Merchants Section
        if (uiState.topMerchants.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Top Merchants"
                )
            }
            
            // All Merchants with expandable list
            item {
                ExpandableList(
                    items = uiState.topMerchants,
                    visibleItemCount = 3,
                    modifier = Modifier.fillMaxWidth()
                ) { merchant ->
                    MerchantListItem(
                        merchant = merchant,
                        currency = selectedCurrency,
                        onClick = {
                            onNavigateToTransactions(null, merchant.name, selectedPeriod.name, selectedCurrency)
                        }
                    )
                }
            }
        }
        
        
        // Empty state
        if (uiState.topMerchants.isEmpty() && uiState.categoryBreakdown.isEmpty() && !uiState.isLoading) {
            item {
                EmptyAnalyticsState()
            }
        }
    }
    
//    // Chat FAB
//    SmallFloatingActionButton(
//        onClick = onNavigateToChat,
//        modifier = Modifier
//            .align(Alignment.BottomEnd)
//            .padding(Dimensions.Padding.content),
//        containerColor = MaterialTheme.colorScheme.secondaryContainer,
//        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
//    ) {
//        Icon(
//            imageVector = Icons.AutoMirrored.Filled.Chat,
//            contentDescription = "Open AI Assistant"
//        )
//    }
    }

    if (showDateRangePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startDate, endDate ->
                viewModel.setCustomDateRange(startDate, endDate)
                showDateRangePicker = false
            },
            initialStartDate = customDateRange?.first,
            initialEndDate = customDateRange?.second
        )
    }
}

@Composable
private fun CategoryListItem(
    category: CategoryData,
    currency: String
) {
    val categoryInfo = CategoryMapping.categories[category.name]
        ?: CategoryMapping.categories["Others"]!!
    
    ListItemCard(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryInfo.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    category = category.name,
                    size = 24.dp,
                    tint = categoryInfo.color
                )
            }
        },
        title = category.name,
        subtitle = "${category.transactionCount} transactions",
        amount = CurrencyFormatter.formatCurrency(category.amount, currency),
        trailingContent = {
            Text(
                text = "${category.percentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun MerchantListItem(
    merchant: MerchantData,
    currency: String,
    onClick: () -> Unit = {}
) {
    val subtitle = buildString {
        append("${merchant.transactionCount} ")
        append(if (merchant.transactionCount == 1) "transaction" else "transactions")
        if (merchant.isSubscription) {
            append(" • Subscription")
        }
    }
    
    ListItemCard(
        leadingContent = {
            BrandIcon(
                merchantName = merchant.name,
                size = 40.dp,
                showBackground = true
            )
        },
        title = merchant.name,
        subtitle = subtitle,
        amount = CurrencyFormatter.formatCurrency(merchant.amount, currency),
        onClick = onClick
    )
}

@Composable
private fun EmptyAnalyticsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.Padding.content),
        contentAlignment = Alignment.Center
    ) {
        PennyWiseCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.empty),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Start tracking expenses to see analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CurrencyFilterRow(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Text(
                text = "Currency:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    vertical = Spacing.sm,
                    horizontal = Spacing.xs
                )
            )
        }
        items(availableCurrencies) { currency ->
            FilterChip(
                selected = selectedCurrency == currency,
                onClick = { onCurrencySelected(currency) },
                label = { Text(currency) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun AnalyticsInsightsCard(
    insights: AnalyticsInsights,
    currency: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading && insights.totalCount == 0) return

    PennyWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content)) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            InsightRow(
                title = "Transactions",
                value = insights.totalCount.toString(),
                subtitle = if (insights.recurringCount > 0) "${insights.recurringCount} recurring" else null
            )

            InsightRow(
                title = "Average",
                value = CurrencyFormatter.formatCurrency(insights.avgAmount, currency)
            )

            insights.largest?.let {
                InsightRow(
                    title = "Largest",
                    value = CurrencyFormatter.formatCurrency(it.amount, currency),
                    subtitle = listOfNotNull(it.label, it.category).joinToString(" · ")
                )
            }

            insights.topCategory?.let {
                InsightRow(
                    title = "Top category",
                    value = CurrencyFormatter.formatCurrency(it.amount, currency),
                    subtitle = "${it.name} • ${it.count} tx"
                )
            }

            insights.topMerchant?.let {
                InsightRow(
                    title = "Top merchant",
                    value = CurrencyFormatter.formatCurrency(it.amount, currency),
                    subtitle = "${it.name} • ${it.count} tx"
                )
            }

            InsightRow(
                title = "Today vs daily avg",
                value = CurrencyFormatter.formatCurrency(insights.todayAmount, currency),
                subtitle = "Daily avg ${CurrencyFormatter.formatCurrency(insights.dailyAvg, currency)}"
            )
        }
    }
}

@Composable
private fun InsightRow(
    title: String,
    value: String,
    subtitle: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AnalyticsBankTabs(
    banks: List<String>,
    selectedBank: String?,
    counts: Map<String, Int>,
    onSelect: (String?) -> Unit
) {
    val tabs = listOf("All") + banks
    val selectedIndex = (selectedBank?.let { tabs.indexOf(it) } ?: 0).coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 8.dp
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Tab(
                selected = isSelected,
                onClick = { onSelect(if (index == 0) null else title) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (index == 0) "*" else title.take(2).uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val count = if (index == 0) counts.values.sum() else counts[title] ?: 0
                    if (count > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(20.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTrendsCard(
    weeklyTrend: List<TrendPoint>,
    monthlyTrend: List<TrendPoint>,
    currency: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading && weeklyTrend.isEmpty() && monthlyTrend.isEmpty()) return
    if (weeklyTrend.size < 2 && monthlyTrend.size < 2) return

    PennyWiseCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content)) {
            Text(
                text = "Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            if (weeklyTrend.size >= 2) {
                TrendSection(
                    title = "Weekly (last 7 days)",
                    points = weeklyTrend,
                    xLabels = weeklyTrend.map { it.label },
                    currency = currency
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            if (monthlyTrend.size >= 2) {
                val weeks = remember(monthlyTrend) { monthlyTrend.map { it.label } }
                TrendSection(
                    title = "Monthly",
                    points = monthlyTrend,
                    xLabels = weeks,
                    currency = currency
                )
            }
        }
    }
}

@Composable
private fun TrendSection(
    title: String,
    points: List<TrendPoint>,
    currency: String,
    xLabels: List<String>? = null
) {
    val total = remember(points) {
        points.fold(BigDecimal.ZERO) { acc, p -> acc + p.amount }
    }
    val last = points.lastOrNull()?.amount ?: BigDecimal.ZERO
    val minPoint = remember(points) { points.minByOrNull { it.amount } }
    val maxPoint = remember(points) { points.maxByOrNull { it.amount } }

    val resolvedLabels = remember(points, xLabels) {
        xLabels ?: run {
            if (points.size <= 8) points.map { it.label }
            else listOf(points.first().label, points[points.size / 2].label, points.last().label)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "Total ${CurrencyFormatter.formatCurrency(total, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = CurrencyFormatter.formatCurrency(last, currency),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(modifier = Modifier.height(Spacing.xs))

    Sparkline(
        points = points.map { it.amount },
        xLabels = resolvedLabels,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
    )

    Spacer(modifier = Modifier.height(Spacing.xs))

    // Min / Max benchmarks
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        minPoint?.let {
            Text(
                text = "Min ${CurrencyFormatter.formatCurrency(it.amount, currency)} (${it.label})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        maxPoint?.let {
            Text(
                text = "Max ${CurrencyFormatter.formatCurrency(it.amount, currency)} (${it.label})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Sparkline(
    points: List<BigDecimal>,
    xLabels: List<String>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val values = remember(points) { points.map { it.toFloat() } }
    val min = remember(values) { values.minOrNull() ?: 0f }
    val max = remember(values) { values.maxOrNull() ?: 0f }
    val range = (max - min).takeIf { it > 0f } ?: 1f

    val lineColor = MaterialTheme.colorScheme.primary
    val areaBrush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.00f)
        )
    )
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val labelColumnWidth = 64.dp

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Y-axis labels column
            Column(
                modifier = Modifier.width(labelColumnWidth).padding(end = Spacing.xs),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = CurrencyFormatter.formatCurrency(max.toBigDecimal(), ""), style = MaterialTheme.typography.labelSmall)
                Text(text = CurrencyFormatter.formatCurrency(((max + min) / 2f).toBigDecimal(), ""), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = CurrencyFormatter.formatCurrency(min.toBigDecimal(), ""), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Canvas area
            Box(modifier = Modifier.weight(1f)) {
                Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
                    val w = size.width
                    val h = size.height
                    val leftPad = 4f
                    val rightPad = 4f
                    val topPad = 6f
                    val bottomPad = 10f

                    val usableW = (w - leftPad - rightPad).coerceAtLeast(1f)
                    val usableH = (h - topPad - bottomPad).coerceAtLeast(1f)

                    // three horizontal gridlines: top, mid, bottom
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPad, topPad),
                        end = Offset(w - rightPad, topPad),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPad, topPad + usableH / 2f),
                        end = Offset(w - rightPad, topPad + usableH / 2f),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPad, topPad + usableH),
                        end = Offset(w - rightPad, topPad + usableH),
                        strokeWidth = 1f
                    )

                    fun xFor(index: Int): Float {
                        val t = index.toFloat() / (values.size - 1).toFloat()
                        return leftPad + usableW * t
                    }

                    fun yFor(value: Float): Float {
                        val norm = (value - min) / range
                        return topPad + usableH * (1f - norm)
                    }

                    val linePath = Path()
                    values.forEachIndexed { index, v ->
                        val x = xFor(index)
                        val y = yFor(v)
                        if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                    }

                    val areaPath = Path().apply {
                        addPath(linePath)
                        lineTo(xFor(values.lastIndex), topPad + usableH)
                        lineTo(xFor(0), topPad + usableH)
                        close()
                    }

                    drawPath(path = areaPath, brush = areaBrush)
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // X-axis labels row aligned with canvas (start with spacer equal to label column)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Spacer(modifier = Modifier.width(labelColumnWidth))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                xLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
