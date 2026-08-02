package com.pennywiseai.tracker.data.manager

import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class BalanceUpdatePolicyTest {

    private val baseTime = LocalDateTime.of(2026, 7, 16, 12, 0)

    private fun existing(
        balance: String,
        timestamp: LocalDateTime = baseTime
    ) = AccountBalanceEntity(
        bankName = "Commercial Bank of Ethiopia",
        accountLast4 = "0122",
        balance = BigDecimal(balance),
        timestamp = timestamp,
        currency = "ETB"
    )

    @Test
    fun `accepts first balance when no existing account`() {
        assertTrue(
            BalanceUpdatePolicy.shouldPersist(
                existing = null,
                smsTimestamp = baseTime,
                newBalance = BigDecimal("84643"),
                fromSms = true,
                transactionAmount = BigDecimal("1160")
            )
        )
    }

    @Test
    fun `rejects older SMS even with explicit balance`() {
        assertFalse(
            BalanceUpdatePolicy.shouldPersist(
                existing = existing("84643", baseTime),
                smsTimestamp = baseTime.minusDays(1),
                newBalance = BigDecimal("181000"),
                fromSms = true,
                transactionAmount = BigDecimal("1000")
            )
        )
    }

    @Test
    fun `accepts newer SMS balance`() {
        assertTrue(
            BalanceUpdatePolicy.shouldPersist(
                existing = existing("85803", baseTime.minusHours(1)),
                smsTimestamp = baseTime,
                newBalance = BigDecimal("84643"),
                fromSms = true,
                transactionAmount = BigDecimal("1160")
            )
        )
    }

    @Test
    fun `rejects wild calculated jump`() {
        assertFalse(
            BalanceUpdatePolicy.shouldPersist(
                existing = existing("84643", baseTime.minusMinutes(1)),
                smsTimestamp = baseTime,
                newBalance = BigDecimal("181000"),
                fromSms = false,
                transactionAmount = BigDecimal("1160")
            )
        )
    }

    @Test
    fun `accepts calculated update matching transaction delta`() {
        assertTrue(
            BalanceUpdatePolicy.shouldPersist(
                existing = existing("85803", baseTime.minusMinutes(1)),
                smsTimestamp = baseTime,
                newBalance = BigDecimal("84643"),
                fromSms = false,
                transactionAmount = BigDecimal("1160")
            )
        )
    }
}
