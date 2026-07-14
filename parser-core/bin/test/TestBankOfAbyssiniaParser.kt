package com.pennywiseai.parser.core.bank

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class TestBankOfAbyssiniaParser {

    private val parser = BankOfAbyssiniaParser()

    @Test
    fun testDebitMessage() {
        val sms = "Dear FENANE, your account 1*49 was debited with ETB 555.00. Available Balance:  ETB 2,953.88. Receipt: https://cs.bankofabyssinia.com/slip/?trx=FT25266VHSWF99449 Feedback: https://cs.bankofabyssinia.com/cs/?trx=DFT25266VHSWF For help, call 8397 (24/7 Toll-Free). Bank of Abyssinia."
        val parsed = parser.parse(sms, "BOA", System.currentTimeMillis())
        assertNotNull(parsed, "Parser should return a ParsedTransaction for debit message")
        assertEquals("ETB", parsed?.currency)
        assertEquals("Bank of Abyssinia", parsed?.bankName)
        assertEquals("FT25266VHSWF99449", parsed?.reference)
        assertEquals("2953.88", parsed?.balance?.toPlainString())
        assertEquals("555.00", parsed?.amount?.toPlainString())
    }

    @Test
    fun testCreditMessage() {
        val sms = "Dear FENANE, your account 1*49 was credited with ETB 3,000.00 by CHIRSTIAN TESFAYE KASSAHUN. Available Balance:  ETB 5,027.46. Receipt: https://cs.bankofabyssinia.com/slip/?trx=FT25253SS1KP36984 Feedback: https://cs.bankofabyssinia.com/cs/?trx=CFT25253SS1KP For help, call 8397 (24/7 Toll-Free). Bank of Abyssinia."
        val parsed = parser.parse(sms, "BOA", System.currentTimeMillis())
        assertNotNull(parsed, "Parser should return a ParsedTransaction for credit message")
        assertEquals("ETB", parsed?.currency)
        assertEquals("Bank of Abyssinia", parsed?.bankName)
        assertEquals("FT25253SS1KP36984", parsed?.reference)
        assertEquals("5027.46", parsed?.balance?.toPlainString())
        assertEquals("3000.00", parsed?.amount?.toPlainString())
        assertEquals("CHIRSTIAN TESFAYE KASSAHUN", parsed?.merchant)
    }
}
