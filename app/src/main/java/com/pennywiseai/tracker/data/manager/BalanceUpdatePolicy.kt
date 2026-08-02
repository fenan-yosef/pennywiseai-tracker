package com.pennywiseai.tracker.data.manager

import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Guards account balance writes so older SMS and wild calculated values
 * cannot corrupt the displayed "latest" balance.
 */
object BalanceUpdatePolicy {

    /**
     * @return true if this balance update should be persisted.
     */
    fun shouldPersist(
        existing: AccountBalanceEntity?,
        smsTimestamp: LocalDateTime,
        newBalance: BigDecimal,
        fromSms: Boolean,
        transactionAmount: BigDecimal
    ): Boolean {
        if (existing == null) return true

        // Never let an older SMS overwrite after a newer balance.
        if (smsTimestamp.isBefore(existing.timestamp)) {
            return false
        }

        // Explicit balance from SMS is trusted when not stale.
        if (fromSms) return true

        return isPlausibleCalculatedUpdate(
            previous = existing.balance,
            proposed = newBalance,
            transactionAmount = transactionAmount
        )
    }

    /**
     * Accept calculated balances only when the delta is close to the txn amount
     * (or the account had no prior balance).
     */
    fun isPlausibleCalculatedUpdate(
        previous: BigDecimal,
        proposed: BigDecimal,
        transactionAmount: BigDecimal
    ): Boolean {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return true

        val delta = proposed.subtract(previous).abs()
        val amount = transactionAmount.abs()
        val maxDelta = amount.multiply(BigDecimal("1.05"))
            .max(amount.add(BigDecimal.ONE))
            .max(BigDecimal("1"))

        return delta <= maxDelta
    }
}
