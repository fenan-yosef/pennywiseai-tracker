package com.pennywiseai.tracker.data.mapper

import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType as ParserTransactionType
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Maps ParsedTransaction from parser-core to TransactionEntity
 */
fun ParsedTransaction.toEntity(): TransactionEntity {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault()
    )

    // Normalize merchant name to proper case
    val normalizedMerchant = merchant?.let { normalizeMerchantName(it) }

    // Map TransactionType from parser-core to database entity
    val entityType = when (type) {
        ParserTransactionType.INCOME -> TransactionType.INCOME
        ParserTransactionType.EXPENSE -> TransactionType.EXPENSE
        ParserTransactionType.CREDIT -> TransactionType.CREDIT
        ParserTransactionType.TRANSFER -> TransactionType.TRANSFER
        ParserTransactionType.INVESTMENT -> TransactionType.INVESTMENT
    }

    return TransactionEntity(
        id = 0, // Auto-generated
        amount = amount,
        merchantName = normalizedMerchant ?: "Unknown Merchant",
        category = determineCategory(merchant, entityType),
        transactionType = entityType,
        dateTime = dateTime,
        description = null,
        smsBody = smsBody,
        bankName = bankName,
        smsSender = sender,
        accountNumber = accountLast4,
        balanceAfter = balance,
        transactionHash = transactionHash?.takeIf { it.isNotBlank() } ?: generateTransactionId(),
        isRecurring = false, // Will be determined later
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        currency = currency,
        fromAccount = fromAccount,
        toAccount = toAccount
    )
}

/**
 * When [ParsedTransaction.feeAmount] is present and > 0, builds a companion expense
 * (e.g. "Telebirr Service Fee") so fees appear under Bank Charges without a schema change.
 * Hash differs via amount; balanceAfter is null so balance is not double-written.
 */
fun ParsedTransaction.toFeeCompanionEntity(): TransactionEntity? {
    val fee = feeAmount?.takeIf { it > BigDecimal.ZERO } ?: return null
    val feeMerchant = "$bankName Service Fee"
    return copy(
        amount = fee,
        type = ParserTransactionType.EXPENSE,
        merchant = feeMerchant,
        balance = null,
        accountLast4 = null,
        feeAmount = null,
        fromAccount = null,
        toAccount = null,
        transactionHash = null
    ).toEntity().copy(
        category = "Bank Charges",
        balanceAfter = null,
        accountNumber = null
    )
}

/**
 * Normalizes merchant name to consistent format.
 * Converts all-caps to proper case, preserves already mixed case.
 */
private fun normalizeMerchantName(name: String): String {
    val trimmed = name.trim()

    // If it's all uppercase, convert to proper case
    return if (trimmed == trimmed.uppercase()) {
        trimmed.lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    } else {
        // Already has mixed case, keep as is
        trimmed
    }
}

/**
 * Determines the category based on merchant name and transaction type.
 */
private fun determineCategory(merchant: String?, type: TransactionType): String {
    val merchantName = merchant ?: return "Others"

    // Special handling for income transactions
    if (type == TransactionType.INCOME) {
        val merchantLower = merchantName.lowercase()
        return when {
            merchantLower.contains("salary") -> "Salary"
            merchantLower.contains("refund") -> "Refunds"
            merchantLower.contains("cashback") -> "Cashback"
            merchantLower.contains("interest") -> "Interest"
            merchantLower.contains("dividend") -> "Dividends"
            else -> "Income"
        }
    }

    // Use unified category mapping for expenses
    return CategoryMapping.getCategory(merchantName)
}

/**
 * Extension to map parser-core TransactionType to database entity TransactionType
 */
fun com.pennywiseai.parser.core.TransactionType.toEntityType(): TransactionType {
    return when (this) {
        com.pennywiseai.parser.core.TransactionType.INCOME -> TransactionType.INCOME
        com.pennywiseai.parser.core.TransactionType.EXPENSE -> TransactionType.EXPENSE
        com.pennywiseai.parser.core.TransactionType.CREDIT -> TransactionType.CREDIT
        com.pennywiseai.parser.core.TransactionType.TRANSFER -> TransactionType.TRANSFER
        com.pennywiseai.parser.core.TransactionType.INVESTMENT -> TransactionType.INVESTMENT
    }
}
