package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for Telebirr (Ethio telecom) mobile money wallet — ETB.
 *
 * Sample shapes:
 * - "You have transferred ETB 160.00 to Nuru Abdo (2519****3931)..."
 * - "You have paid ETB 34.00 for package Monthly student pack ... purchase made for..."
 * - "You have received ETB 16,000.00 ... from Commercial Bank of Ethiopia to your telebirr Account..."
 *
 * Sender is often numeric ("127"); body markers identify Telebirr reliably.
 */
class TelebirrParser : BankParser() {

    override fun getBankName() = "Telebirr"

    override fun getCurrency() = "ETB"

    override fun canHandle(sender: String): Boolean {
        val s = sender.uppercase().trim()
        return s == "127" ||
                s == "TELEBIRR" ||
                s == "ETHIOTELECOM" ||
                s.contains("TELEBIRR") ||
                s.contains("ETHIOTELECOM") ||
                s.contains("ETHIO TELECOM")
    }

    override fun canHandleBody(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("telebirr") ||
                lower.contains("ethiotelecom.et") ||
                lower.contains("thank you for using telebirr") ||
                lower.contains("e-money account")
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()

        if (lower.contains("otp") ||
            lower.contains("one time password") ||
            lower.contains("verification code")
        ) {
            return false
        }

        val hasTelebirrMarker = lower.contains("telebirr") ||
                lower.contains("e-money") ||
                lower.contains("ethiotelecom")

        if (!hasTelebirrMarker) return false

        return lower.contains("you have transferred") ||
                lower.contains("you have transfered") ||
                lower.contains("you have paid") ||
                lower.contains("you have received")
    }

    override fun extractAmount(message: String): BigDecimal? {
        val actionPatterns = listOf(
            Regex(
                """you have (?:transferred|transfered|paid|received)\s+ETB\s*([0-9,]+(?:\.[0-9]+)?)""",
                RegexOption.IGNORE_CASE
            )
        )
        for (pattern in actionPatterns) {
            pattern.find(message)?.let { match ->
                return parseAmount(match.groupValues[1])
            }
        }
        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("you have received") -> TransactionType.INCOME
            lower.contains("you have transferred") ||
                    lower.contains("you have transfered") ||
                    lower.contains("you have paid") -> TransactionType.EXPENSE
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Transfer: "to Nuru Abdo (2519****3931)"
        val transferTo = Regex(
            """(?:transferred|transfered)\s+ETB\s*[0-9,]+(?:\.[0-9]+)?\s+to\s+(.+?)\s*\(""",
            RegexOption.IGNORE_CASE
        )
        transferTo.find(message)?.let { match ->
            val name = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(name)) return name
        }

        // Package payment: "for package <desc> purchase made for"
        val packagePay = Regex(
            """for\s+package\s+(.+?)\s+purchase\s+made\s+for""",
            RegexOption.IGNORE_CASE
        )
        packagePay.find(message)?.let { match ->
            val name = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(name)) return name
        }

        // Paid without "package": "paid ETB X for <merchant>"
        val paidFor = Regex(
            """paid\s+ETB\s*[0-9,]+(?:\.[0-9]+)?\s+for\s+(.+?)(?:\s+purchase|\s+on\s+\d|\s+made\s+for|\.)""",
            RegexOption.IGNORE_CASE
        )
        paidFor.find(message)?.let { match ->
            var name = match.groupValues[1].trim()
            if (name.startsWith("package ", ignoreCase = true)) {
                name = name.removePrefix("package ").removePrefix("Package ")
            }
            name = cleanMerchantName(name)
            if (isValidMerchantName(name)) return name
        }

        // Received: "from Commercial Bank of Ethiopia to your telebirr"
        val receivedFrom = Regex(
            """from\s+(.+?)\s+to\s+your\s+telebirr""",
            RegexOption.IGNORE_CASE
        )
        receivedFrom.find(message)?.let { match ->
            val name = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(name)) return name
        }

        // Received from another telebirr / generic "from Name"
        val fromGeneric = Regex(
            """received\s+ETB\s*[0-9,]+(?:\.[0-9]+)?.*?from\s+([A-Za-z][A-Za-z0-9 .'*]+?)(?:\s+to\s+your|\s+on\s|\.|$)""",
            RegexOption.IGNORE_CASE
        )
        fromGeneric.find(message)?.let { match ->
            val name = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(name)) return name
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractBalance(message: String): BigDecimal? {
        val patterns = listOf(
            Regex(
                """(?:E-Money\s+Account\s+)?balance\s+is\s+ETB\s*([0-9,]+(?:\.[0-9]+)?)""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """current\s+balance\s+is\s+ETB\s*([0-9,]+(?:\.[0-9]+)?)""",
                RegexOption.IGNORE_CASE
            )
        )
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return parseAmount(match.groupValues[1])
            }
        }
        return null
    }

    override fun extractReference(message: String): String? {
        val patterns = listOf(
            Regex(
                """transaction\s+number\s+is\s+([A-Z0-9]+)""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """by\s+transaction\s+number\s+([A-Z0-9]+)""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """receipt/([A-Z0-9]+)""",
                RegexOption.IGNORE_CASE
            )
        )
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    override fun extractAccountLast4(message: String): String? {
        // Telebirr is a single e-money wallet. Always use one stable account id so
        // every SMS (transfer/package/receive) updates the same Home balance/tab,
        // matching how CBE/BOA accounts behave.
        return WALLET_ACCOUNT_LAST4
    }

    companion object {
        const val WALLET_ACCOUNT_LAST4 = "TELE"
    }

    override fun extractFeeAmount(message: String): BigDecimal? {
        val serviceFee = Regex(
            """service\s+fee\s+is\s+ETB\s*([0-9,]+(?:\.[0-9]+)?)""",
            RegexOption.IGNORE_CASE
        ).find(message)?.groupValues?.get(1)?.let { parseAmount(it) }

        val vat = Regex(
            """VAT[^\d]*ETB\s*([0-9,]+(?:\.[0-9]+)?)""",
            RegexOption.IGNORE_CASE
        ).find(message)?.groupValues?.get(1)?.let { parseAmount(it) }

        val total = listOfNotNull(serviceFee, vat).fold(BigDecimal.ZERO) { acc, v -> acc + v }
        return total.takeIf { it > BigDecimal.ZERO }
    }

    private fun parseAmount(raw: String): BigDecimal? {
        return try {
            BigDecimal(raw.replace(",", ""))
        } catch (_: NumberFormatException) {
            null
        }
    }
}
