package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal


/**
 * Parser for Bank of Abyssinia (BOA) - handles ETB currency transactions
 */
class BankOfAbyssiniaParser : BankParser() {

    override fun getBankName() = "Bank of Abyssinia"

    override fun getCurrency() = "ETB"

    override fun canHandle(sender: String): Boolean {
        val s = sender.uppercase()
        return s == "BOA" ||
                s.contains("BANKOFABYSSINIA") ||
                s.contains("BANK OF ABYSSINIA") ||
                s.contains("BOABANK") ||
                s.contains("BANK OF ABY")
    }

    override fun canHandleBody(message: String): Boolean {
        val lower = message.lowercase()
        // BOA often includes CS links or the toll-free number 8397
        return lower.contains("bankofabyssinia.com") ||
                lower.contains("cs.bankofabyssinia.com") ||
                lower.contains("receipt:") && lower.contains("trx=") ||
                lower.contains("call 8397") ||
                (
                    (lower.contains("your account") && lower.contains("etb")) &&
                            (lower.contains("credited") || lower.contains("debited"))
                )
    }

    override fun extractAmount(message: String): BigDecimal? {
        val patterns = listOf(
            Regex("""ETB\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""([0-9,]+(?:\.[0-9]{2})?)\s*ETB""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                return try {
                    BigDecimal(amountStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }

        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("debited") -> TransactionType.EXPENSE
            lower.contains("withdrawn") -> TransactionType.EXPENSE
            lower.contains("credited") -> TransactionType.INCOME
            lower.contains("deposited") -> TransactionType.INCOME
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // credit messages often have "credited with ETB 3,000.00 by <NAME>"
        val creditBy = Regex("""credited(?: with)?[\s\S]*?by\s+([^.,\n]+)""", RegexOption.IGNORE_CASE)
        creditBy.find(message)?.let { m ->
            val name = m.groupValues[1].trim().replace("*", "")
            if (name.isNotEmpty()) return cleanMerchantName(name)
        }

        // fallback to base extractor
        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        val accountPattern = Regex("""account\s+\d?\*+(\d{2,4})""", RegexOption.IGNORE_CASE)
        accountPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return super.extractAccountLast4(message)
    }

    override fun extractBalance(message: String): BigDecimal? {
        val balancePattern = Regex("""Available\s+Balance[:\s]*ETB\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE)
        balancePattern.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }

        return super.extractBalance(message)
    }

    override fun extractReference(message: String): String? {
        // Try trx parameter in receipt URL
        val trxPattern = Regex("""trx=([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        trxPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        // Fallback to general reference patterns
        return super.extractReference(message)
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()

        val keywords = listOf("your account", "has been credited", "has been debited", "available balance", "etb")
        if (keywords.any { lower.contains(it) }) return true

        return super.isTransactionMessage(message)
    }
}