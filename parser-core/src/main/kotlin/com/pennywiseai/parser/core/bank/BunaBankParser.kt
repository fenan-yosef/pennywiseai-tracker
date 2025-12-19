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














    

}