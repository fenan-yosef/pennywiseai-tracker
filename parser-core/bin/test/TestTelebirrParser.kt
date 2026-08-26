package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import com.pennywiseai.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TelebirrParserTest {

    private val transferSms = """
Dear Fenan 
You have transferred ETB 160.00 to Nuru Abdo (2519****3931) on 12/08/2026 15:23:55. Your transaction number is DHC7Q85BHP. The service fee is  ETB 1.74 and  15% VAT on the service fee is ETB 0.26. Your current E-Money Account  balance is ETB 15,531.77. To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DHC7Q85BHP.

Thank you for using telebirr
Ethio telecom
""".trimIndent()

    private val packageSms = """
Dear Fenan
You have paid ETB 34.00 for package Monthly student pack 234 Min + 120SMS plus 234 Min night bonus purchase made for 905344533 on 12/08/2026 13:53:50. Your transaction number is  DHC3Q553TZ. Your current balance is ETB 15,693.77.To download your payment information please click this link: https://transactioninfo.ethiotelecom.et/receipt/DHC3Q553TZ
Thank you for using telebirr
Ethio telecom
""".trimIndent()

    private val receiveSms = """
Dear Fenan,
You have received  ETB 16,000.00 by transaction number DGP08R3174 on 2026-07-25 21:11:06 from Commercial Bank of Ethiopia to your telebirr Account 251905344533 - Fenan Yosef Waysa. Your current balance is ETB 16,343.77.
Thank you for using telebirr
Ethio telecom
""".trimIndent()

    @Test
    fun `telebirr parser handles transfer package and receive`() {
        val parser = TelebirrParser()

        ParserTestUtils.printTestHeader(
            parserName = "Telebirr",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Transfer to person with fee+VAT",
                message = transferSms,
                sender = "127",
                expected = ExpectedTransaction(
                    amount = BigDecimal("160.00"),
                    currency = "ETB",
                    type = TransactionType.EXPENSE,
                    merchant = "Nuru Abdo",
                    reference = "DHC7Q85BHP",
                    balance = BigDecimal("15531.77"),
                    accountLast4 = "TELE",
                    feeAmount = BigDecimal("2.00")
                )
            ),
            ParserTestCase(
                name = "Package purchase",
                message = packageSms,
                sender = "127",
                expected = ExpectedTransaction(
                    amount = BigDecimal("34.00"),
                    currency = "ETB",
                    type = TransactionType.EXPENSE,
                    merchant = "Monthly student pack 234 Min + 120SMS plus 234 Min night bonus",
                    reference = "DHC3Q553TZ",
                    balance = BigDecimal("15693.77"),
                    accountLast4 = "TELE"
                )
            ),
            ParserTestCase(
                name = "Receive from bank",
                message = receiveSms,
                sender = "127",
                expected = ExpectedTransaction(
                    amount = BigDecimal("16000.00"),
                    currency = "ETB",
                    type = TransactionType.INCOME,
                    merchant = "Commercial Bank of Ethiopia",
                    reference = "DGP08R3174",
                    balance = BigDecimal("16343.77"),
                    accountLast4 = "TELE"
                )
            )
        )

        val handleCases = listOf(
            "127" to true,
            "TELEBIRR" to true,
            "telebirr" to true,
            "ETHIOTELECOM" to true,
            "CBE" to false,
            "MPESA" to false,
            "" to false
        )

        ParserTestUtils.runTestSuite(parser, testCases, handleCases, "Telebirr Parser Tests")
    }

    @Test
    fun `factory resolves telebirr from sender 127`() {
        ParserTestUtils.runFactoryTestSuite(
            listOf(
                SimpleTestCase(
                    bankName = "Telebirr",
                    sender = "127",
                    currency = "ETB",
                    message = transferSms,
                    expected = ExpectedTransaction(
                        amount = BigDecimal("160.00"),
                        currency = "ETB",
                        type = TransactionType.EXPENSE,
                        merchant = "Nuru Abdo",
                        reference = "DHC7Q85BHP",
                        balance = BigDecimal("15531.77"),
                        feeAmount = BigDecimal("2.00")
                    ),
                    shouldHandle = true,
                    description = "Telebirr sender 127"
                )
            ),
            "Telebirr factory smoke"
        )
    }

    @Test
    fun `body fallback prefers telebirr over cbe bank name in body`() {
        val parser = BankParserFactory.getParser("UNKNOWN", receiveSms)
        assertNotNull(parser)
        assertEquals("Telebirr", parser!!.getBankName())
        val parsed = parser.parse(receiveSms, "UNKNOWN", System.currentTimeMillis())
        assertNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed!!.type)
        assertEquals(BigDecimal("16000.00"), parsed.amount)
    }
}
