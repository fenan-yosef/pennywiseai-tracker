package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for Commercial Bank of Ethiopia (CBE) - handles ETB currency transactions
 */
class CBEBankParser : BankParser() {

    override fun getBankName() = "Commercial Bank of Ethiopia"

    override fun getCurrency() = "ETB"  // Ethiopian Birr

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        return upperSender == "CBE" ||
                upperSender.contains("COMMERCIALBANK") ||
                upperSender.contains("CBEBANK") ||
                // DLT patterns for Ethiopia might be different
                upperSender.matches(Regex("""^[A-Z]{2}-CBE-[A-Z]$"""))
    }

    override fun extractAmount(message: String): BigDecimal? {
        // Prefer the amount tied to the action so we don't pick fees / VAT / balance.
        val actionAmountPatterns = listOf(
            Regex(
                """(?:credited with|debited with|transfer(?:r)?ed)\s+ETB\s*([0-9,]+(?:\.[0-9]+)?)""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """ETB\s*([0-9,]+(?:\.[0-9]+)?)\s+(?:has been|from|to)""",
                RegexOption.IGNORE_CASE
            )
        )
        for (pattern in actionAmountPatterns) {
            pattern.find(message)?.let { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                return try {
                    BigDecimal(amountStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }

        val pattern = Regex(
            """ETB\s*([0-9,]+(?:\.[0-9]+)?)""",
            RegexOption.IGNORE_CASE
        )

        pattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        }

        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            // Received money is income
            lowerMessage.contains("you have received") -> TransactionType.INCOME

            // Credit transactions are income
            lowerMessage.contains("has been credited") -> TransactionType.INCOME
            lowerMessage.contains("credited with") -> TransactionType.INCOME

            // Debit transactions are expenses
            lowerMessage.contains("has been debited") -> TransactionType.EXPENSE
            lowerMessage.contains("debited with") -> TransactionType.EXPENSE

            // Transfer transactions are expenses (money going out)
            lowerMessage.contains("you have transfered") -> TransactionType.EXPENSE
            lowerMessage.contains("transferred") -> TransactionType.EXPENSE

            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Pattern 1: Name in parentheses after "from account" or "to account"
        // e.g. "from account 1**0681 (Yishak Abrham Nibretu)"
        // e.g. "to account 1**7818 (Yohannes Musse Yilma)"
        val nameInParens = Regex(
            """(?:from|to)\s+account\s+\S+\s+\(([^)]+)\)""",
            RegexOption.IGNORE_CASE
        )
        nameInParens.find(message)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotEmpty()) {
                return cleanMerchantName(name)
            }
        }

        // Pattern 2: "from Be***" (credit transaction)
        val fromPattern = Regex("""from\s+([^,\s]+\*{0,3}[^,\s]*)""", RegexOption.IGNORE_CASE)
        fromPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotEmpty()) {
                return cleanMerchantName(merchant.replace("*", ""))
            }
        }

        // Pattern 3: "to Se*****" (transfer transaction)
        val toPattern = Regex("""to\s+([^,\s]+\*{0,5}[^,\s]*)""", RegexOption.IGNORE_CASE)
        toPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            if (merchant.isNotEmpty()) {
                return cleanMerchantName(merchant.replace("*", ""))
            }
        }

        // Pattern 4: Service charge or general debit
        if (message.contains("s.charge", ignoreCase = true) ||
            message.contains("service charge", ignoreCase = true)
        ) {
            return "Service Charge"
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        val isCredit = extractTransactionType(message) == TransactionType.INCOME

        if (isCredit) {
            // For credit/income transactions:
            // 1. "your Account 1********0122" or "your A/c 1********0122"
            val yourAccountPattern = Regex(
                """your\s+(?:Account|A/c|Acct)\s+\d?\*+(\d{4})""",
                RegexOption.IGNORE_CASE
            )
            yourAccountPattern.find(message)?.let { match ->
                return match.groupValues[1]
            }

            // 2. "to your account 1********0122" or "to account 1********0122"
            val toAccountPattern = Regex(
                """to\s+(?:your\s+)?(?:account|a/c|acct)\s+\d?\*+(\d{4})""",
                RegexOption.IGNORE_CASE
            )
            toAccountPattern.find(message)?.let { match ->
                return match.groupValues[1]
            }

            // Do not look for general Account/A/c patterns or fall back to super
            // because they could match the sender's account ("from account ...").
            return null
        } else {
            // For debit/transfer transactions:
            // 1. "from account 1********0122" or "from your account 1********0122"
            val fromAccountPattern = Regex(
                """from\s+(?:your\s+)?(?:account|a/c|acct)\s+\d?\*+(\d{4})""",
                RegexOption.IGNORE_CASE
            )
            fromAccountPattern.find(message)?.let { match ->
                return match.groupValues[1]
            }

            // 2. "your Account 1********0122"
            val yourAccountPattern = Regex(
                """your\s+(?:Account|A/c|Acct)\s+\d?\*+(\d{4})""",
                RegexOption.IGNORE_CASE
            )
            yourAccountPattern.find(message)?.let { match ->
                return match.groupValues[1]
            }

            // 3. General "Account 1********0122"
            val generalAccountPattern = Regex(
                """(?:Account|A/c|Acct)\s+\d?\*+(\d{4})""",
                RegexOption.IGNORE_CASE
            )
            generalAccountPattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }

        return super.extractAccountLast4(message)
    }

    override fun extractBalance(message: String): BigDecimal? {
        // Allow 1+ decimal places (CBE sometimes sends ETB9.61 or ETB 85803.6)
        val balancePattern =
            Regex("""current balance is ETB\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
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
        // Look for reference numbers: "with Ref No *********"
        val refPattern = Regex("""Ref No\s+(\*{0,9}[A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        refPattern.find(message)?.let { match ->
            val ref = match.groupValues[1].replace("*", "")
            if (ref.isNotEmpty()) {
                return ref
            }
        }

        // Look for transaction ID in URL: "id=FT25256RP1FK27799388"
        val urlIdPattern = Regex("""id=([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        urlIdPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        // Look for date and time: "on 13/09/2025 at 12:37:24"
        val dateTimePattern =
            Regex("""on\s+(\d{2}/\d{2}/\d{4}\s+at\s+\d{2}:\d{2}:\d{2})""", RegexOption.IGNORE_CASE)
        dateTimePattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        // Receipt URL from mbreciept: "https://mbreciept.cbe.com.et/v2-hfHCxzWXP84K6nu3tO04"
        val mbrecieptPattern = Regex("""mbreciept\.cbe\.com\.et/v2-(\w+)""", RegexOption.IGNORE_CASE)
        mbrecieptPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        // BranchReceipt URL: "BranchReceipt/FT26184G2GKQ&53250122"
        val branchReceiptPattern = Regex("""BranchReceipt/(\w+&\w+)""", RegexOption.IGNORE_CASE)
        branchReceiptPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return super.extractReference(message)
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        // CBE specific transaction keywords
        val cbeTransactionKeywords = listOf(
            "dear",
            "your account",
            "has been credited",
            "has been debited",
            "you have transfered",
            "you have received",
            "transferred",
            "current balance",
            "thank you for banking with cbe",
            "etb"
        )

        if (cbeTransactionKeywords.any { lowerMessage.contains(it) }) {
            return true
        }

        return super.isTransactionMessage(message)
    }
}