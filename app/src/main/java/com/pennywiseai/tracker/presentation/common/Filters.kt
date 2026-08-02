package com.pennywiseai.tracker.presentation.common

import java.time.LocalDate
import java.time.YearMonth

enum class TimePeriod(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    LAST_WEEK("Last Week"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    THIS_QUARTER("This Quarter"),
    LAST_QUARTER("Last Quarter"),
    CURRENT_FY("Current FY"),
    LAST_YEAR("Last Year"),
    ALL("All Time"),
    CUSTOM("Custom Range")
}

enum class TransactionTypeFilter(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense"),
    CREDIT("Credit"),
    TRANSFER("Transfer"),
    INVESTMENT("Investment")
}

fun getDateRangeForPeriod(period: TimePeriod): Pair<LocalDate, LocalDate>? {
    val today = LocalDate.now()
    return when (period) {
        TimePeriod.TODAY -> today to today
        TimePeriod.YESTERDAY -> today.minusDays(1) to today.minusDays(1)
        TimePeriod.THIS_WEEK -> {
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            monday to today
        }
        TimePeriod.LAST_WEEK -> {
            val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
            monday.minusDays(7) to monday.minusDays(1)
        }
        TimePeriod.LAST_7_DAYS -> today.minusDays(6) to today
        TimePeriod.LAST_30_DAYS -> today.minusDays(29) to today
        TimePeriod.THIS_MONTH -> {
            val start = YearMonth.now().atDay(1)
            start to today
        }
        TimePeriod.LAST_MONTH -> {
            val lastMonth = YearMonth.now().minusMonths(1)
            val start = lastMonth.atDay(1)
            val end = lastMonth.atEndOfMonth()
            start to end
        }
        TimePeriod.LAST_3_MONTHS -> {
            val start = today.minusMonths(3)
            start to today
        }
        TimePeriod.LAST_6_MONTHS -> {
            val start = today.minusMonths(6)
            start to today
        }
        TimePeriod.THIS_QUARTER -> {
            val currentQuarterStartMonth = ((today.monthValue - 1) / 3) * 3 + 1
            val start = LocalDate.of(today.year, currentQuarterStartMonth, 1)
            start to today
        }
        TimePeriod.LAST_QUARTER -> {
            val currentQuarterStartMonth = ((today.monthValue - 1) / 3) * 3 + 1
            val currentQuarterStart = LocalDate.of(today.year, currentQuarterStartMonth, 1)
            val end = currentQuarterStart.minusDays(1)
            val start = end.minusMonths(2).withDayOfMonth(1)
            start to end
        }
        TimePeriod.CURRENT_FY -> {
            // Indian Financial Year: April 1 to March 31
            val currentYear = today.year
            val currentMonth = today.monthValue
            val fyStart = if (currentMonth >= 4) {
                LocalDate.of(currentYear, 4, 1)  // Apr 1 of current year
            } else {
                LocalDate.of(currentYear - 1, 4, 1)  // Apr 1 of previous year
            }
            fyStart to today
        }
        TimePeriod.LAST_YEAR -> {
            val start = LocalDate.of(today.year - 1, 1, 1)
            val end = LocalDate.of(today.year - 1, 12, 31)
            start to end
        }
        TimePeriod.ALL -> {
            // Use a reasonable date range for "All Time" - 10 years back to today
            val start = today.minusYears(10)
            start to today
        }
        TimePeriod.CUSTOM -> {
            // Custom range is handled separately in ViewModel
            null
        }
    }
}