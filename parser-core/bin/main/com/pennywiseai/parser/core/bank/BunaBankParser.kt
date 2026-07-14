package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal


/**
*Parser of Buna Bank   Currency: ETB
*/


class BunaBankParser : BankParser() {

    override fun getBankName() = "Buna Bank"
    override fun getCurrency() = "ETB"

    override fun canHandle(sender: String): Boolean {
        val s = sender.uppercase()
        return s == "BUNA" ||
                s.contains("BUNA") ||
                s.contains("BUNABANK") ||
                s.contains("BUNA BANK")
    }

    override fun canHandleBody(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("buna bank") ||
                lower.contains("bunabank") ||
                (lower.contains("etb") && (lower.contains("credited") || lower.contains("debited") || lower.contains("withdrawn") || lower.contains("deposited")))
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
                } catch (_: NumberFormatException) {
                    null
                }
            }
        }

        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("debited") || lower.contains("withdrawn") || lower.contains("paid") -> TransactionType.EXPENSE
            lower.contains("credited") || lower.contains("deposited") || lower.contains("received") -> TransactionType.INCOME
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val byPattern = Regex("""by\s+([^.,\n]+)""", RegexOption.IGNORE_CASE)
        byPattern.find(message)?.let { m ->
            val name = m.groupValues[1].trim().replace("*", "")
            if (name.isNotEmpty()) return cleanMerchantName(name)
        }

        val toPattern = Regex("""to\s+([^.,\n]+)""", RegexOption.IGNORE_CASE)
        toPattern.find(message)?.let { m ->
            val name = m.groupValues[1].trim().replace("*", "")
            if (name.isNotEmpty()) return cleanMerchantName(name)
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        val accountPattern = Regex("""account\s+\d?\*+(\n?\d{2,4})""", RegexOption.IGNORE_CASE)
        accountPattern.find(message)?.let { m ->
            val last = m.groupValues[1].filter { it.isDigit() }
            if (last.length in 2..4) return last
        }
        return super.extractAccountLast4(message)
    }

    override fun extractBalance(message: String): BigDecimal? {
        val balancePattern = Regex("""Available\s+Balance[:\s]*ETB\s*([0-9,]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE)
        balancePattern.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (_: NumberFormatException) {
                null
            }
        }
        return super.extractBalance(message)
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        val keywords = listOf("your account", "has been credited", "has been debited", "available balance", "etb")
        if (keywords.any { lower.contains(it) }) return true
        return super.isTransactionMessage(message)
    }














    

}