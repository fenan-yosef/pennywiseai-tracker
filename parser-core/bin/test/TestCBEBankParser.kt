import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.CBEBankParser
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CBEBankParserTest {

    @Test
    fun `cbe parser handles credit debit and transfer`() {
        val parser = CBEBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "Commercial Bank of Ethiopia",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Credit transaction",
                message = "Dear [Name] your Account 1*********9388 has been Credited with ETB 3,000.00 from Be***, on 13/09/2025 at 12:37:24 with Ref No ********* Your Current Balance is ETB 3,104.87. Thank you for Banking with CBE!",
                sender = "CBE",
                expected = ExpectedTransaction(
                    amount = BigDecimal("3000.00"),
                    currency = "ETB",
                    type = TransactionType.INCOME,
                    merchant = "Be",
                    accountLast4 = "9388",
                    balance = BigDecimal("3104.87")
                )
            ),
            ParserTestCase(
                name = "Debit transaction",
                message = "Dear [Name] your Account 1*********9388 has been debited with ETB 25.00. Your Current Balance is ETB 3,079.87 Thank you for Banking with CBE! https://apps.cbe.com.et:100/?id=FT25256RP1FK27799388",
                sender = "CBE",
                expected = ExpectedTransaction(
                    amount = BigDecimal("25.00"),
                    currency = "ETB",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "9388",
                    balance = BigDecimal("3079.87"),
                    reference = "FT25256RP1FK27799388"
                )
            ),
            ParserTestCase(
                name = "Transfer transaction",
                message = "Dear [Name], You have transfered ETB 250.00 to Se***** on 14/09/2025 at 12:28:56 from your account 1*********9388. Your account has been debited with a S.charge of ETB 0 and  15% VAT of ETB0.00, with a total of ETB250. Your Current Balance is ETB 2,829.87. Thank you for Banking with CBE!",
                sender = "CBE",
                expected = ExpectedTransaction(
                    amount = BigDecimal("250.00"),
                    currency = "ETB",
                    type = TransactionType.EXPENSE,
                    merchant = "your",
                    accountLast4 = "9388",
                    balance = BigDecimal("2829.87")
                )
            ),
            ParserTestCase(
                name = "Alternative sender credit",
                message = "Dear Customer your Account 1*********1234 has been Credited with ETB 5,000.00 from Salary Payment, on 15/09/2025 at 09:00:00 with Ref No ABC123456 Your Current Balance is ETB 8,000.00. Thank you for Banking with CBE!",
                sender = "CBEBANK",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5000.00"),
                    currency = "ETB",
                    type = TransactionType.INCOME,
                    merchant = "Salary",
                    accountLast4 = "1234",
                    balance = BigDecimal("8000.00"),
                    reference = "ABC123456"
                )
            ),
            ParserTestCase(
                name = "Large amount credit",
                message = "Dear [Name] your Account 1*********5678 has been Credited with ETB 125,500.50 from Business Payment, on 16/09/2025 at 14:30:00 Your Current Balance is ETB 130,000.75. Thank you for Banking with CBE!",
                sender = "CBE",
                expected = ExpectedTransaction(
                    amount = BigDecimal("125500.50"),
                    currency = "ETB",
                    type = TransactionType.INCOME,
                    merchant = "Business",
                    accountLast4 = "5678",
                    balance = BigDecimal("130000.75")
                )
            ),
            ParserTestCase(
                name = "Credit with one-decimal balance",
                message = "Dear Customer your Account 1****0122 has been credited with ETB 50003.00. Your Current Balance is ETB 85803.6. Thank you for Banking with CBE! for Reciept https://apps.cbe.com.et:100/BranchReceipt/FT261969PBWN&53250122",
                sender = "CBE",
                expected = ExpectedTransaction(
                    amount = BigDecimal("50003.00"),
                    currency = "ETB",
                    type = TransactionType.INCOME,
                    accountLast4 = "0122",
                    balance = BigDecimal("85803.6")
                )
            ),
            ParserTestCase(
                name = "Transfer from account with compact balance",
                message = "Dear Customer You have successfully transferred ETB 40.00 from account 1**7157 to account 1**1386 (Recipient Name). Service charge of ETB 0.00 and VAT(15%) of 0.0 and Disaster Recovery(5%) of 0.00 with total of ETB40.00 .Your current balance is ETB9.61. Thanks for Banking with CBE. https://Mbreciept.cbe.com.et/v2-hfHCxzjoH1RYEgtbs2le",
                sender = "CBE",
                expected = ExpectedTransaction(
                    amount = BigDecimal("40.00"),
                    currency = "ETB",
                    type = TransactionType.EXPENSE,
                    merchant = "Recipient Name",
                    accountLast4 = "7157",
                    balance = BigDecimal("9.61")
                )
            ),
            ParserTestCase(
                name = "User reported transferred message (July)",
                message = "Dear  Fenan Yoseph Weyesa You have successfully transferred ETB260.00 from account 1********0122 to account 1********2906 (Mesfin Yigzaye Tarekegn). Service charge of ETB 0.50 and VAT(15%) of ETB0.08 and Disaster Recovery(5%) of 0.03 with total of ETB260.61 .Your current balance is ETB38,900.27. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/v2-hfHCxG5KI9PApvU8JBkn  for feedback: https://forms.gle/kGNGQpG3mQCCk3iD6",
                sender = "CBE",
                expected = ExpectedTransaction(
                    amount = BigDecimal("260.00"),
                    currency = "ETB",
                    type = TransactionType.EXPENSE,
                    merchant = "Mesfin Yigzaye Tarekegn",
                    accountLast4 = "0122",
                    balance = BigDecimal("38900.27"),
                    reference = "hfHCxG5KI9PApvU8JBkn"
                )
            ),
            ParserTestCase(
                name = "Credit message with both own and sender account (Verify fix)",
                message = "Dear Fenan Yoseph Weyesa your Account 1********0122 has been Credited with ETB 5,000.00 from account 1********2906 (John Doe). Your Current Balance is ETB 43,900.27. Thank you for Banking with CBE!",
                sender = "CBE",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5000.00"),
                    currency = "ETB",
                    type = TransactionType.INCOME,
                    merchant = "John Doe",
                    accountLast4 = "0122",
                    balance = BigDecimal("43900.27")
                )
            ),
            ParserTestCase(
                name = "Received credit message where sender's account comes first",
                message = "Dear Fenan Yoseph Weyesa You have received ETB 5,000.00 from account 1********0681 (Yishak Abrham Nibretu) to your account 1********0122. Your current balance is ETB41,688.54. Thanks for Banking with CBE. https://mbreciept.cbe.com.et/v2-hfHCxFUSU5qK3GboiWOA  for feedback: https://forms.gle/kGNGQpG3mQCCk3iD6",
                sender = "CBE",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5000.00"),
                    currency = "ETB",
                    type = TransactionType.INCOME,
                    merchant = "Yishak Abrham Nibretu",
                    accountLast4 = "0122",
                    balance = BigDecimal("41688.54"),
                    reference = "hfHCxFUSU5qK3GboiWOA"
                )
            )
        )

        val handleChecks = listOf(
            "CBE" to true,
            "CBEBANK" to true,
            "AD-CBE-A" to true,
            "UNKNOWN" to false
        )

        val result = ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
            handleCases = handleChecks,
            suiteName = "CBE Parser"
        )


    }
}
