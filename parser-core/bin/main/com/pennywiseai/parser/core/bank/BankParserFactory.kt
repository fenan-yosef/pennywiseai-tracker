package com.pennywiseai.parser.core.bank

/**
 * Factory for creating bank-specific parsers based on SMS sender.
 */
object BankParserFactory {

    private val parsers = listOf(
        HDFCBankParser(),
        SBIBankParser(),
        SaraswatBankParser(),
        DBSBankParser(),
        IndianBankParser(),
        FederalBankParser(),
        JuspayParser(),
        SliceParser(),
        LazyPayParser(),
        UtkarshBankParser(),
        ICICIBankParser(),
        KarnatakaBankParser(),
        KeralaGraminBankParser(),
        IDBIBankParser(),
        JupiterBankParser(),
        AxisBankParser(),
        PNBBankParser(),
        CanaraBankParser(),
        BankOfBarodaParser(),
        BankOfIndiaParser(),
        JioPaymentsBankParser(),
        KotakBankParser(),
        IDFCFirstBankParser(),
        UnionBankParser(),
        HSBCBankParser(),
        CentralBankOfIndiaParser(),
        SouthIndianBankParser(),
        JKBankParser(),
        JioPayParser(),
        IPPBParser(),
        CityUnionBankParser(),
        IndianOverseasBankParser(),
        AirtelPaymentsBankParser(),
        IndusIndBankParser(),
        AMEXBankParser(),
        OneCardParser(),
        UCOBankParser(),
        AUBankParser(),
        YesBankParser(),
        BandhanBankParser(),
        ADCBParser(),  // Abu Dhabi Commercial Bank (UAE)
        FABParser(),  // First Abu Dhabi Bank (UAE)
        CitiBankParser(),  // Citi Bank (USA)
        DiscoverCardParser(),  // Discover Card (USA)
        OldHickoryParser(),  // Old Hickory Credit Union (USA)
        LaxmiBankParser(),  // Laxmi Sunrise Bank (Nepal)
        CBEBankParser(),  // Commercial Bank of Ethiopia
        BunaBankParser(), // Buna Bank (Ethiopia)
        BankOfAbyssiniaParser(), // Bank of Abyssinia (Ethiopia)
        TelebirrParser(), // Telebirr (Ethio telecom wallet, Ethiopia)
        EverestBankParser(),  // Everest Bank (Nepal)
        BancolombiaParser(),  // Bancolombia (Colombia)
        MashreqBankParser(),  // Mashreq Bank (UAE)
        CharlesSchwabParser(),  // Charles Schwab (USA)
        NavyFederalParser(),  // Navy Federal Credit Union (USA)
        PriorbankParser(),  // Priorbank (Belarus)
        AlinmaBankParser(),  // Alinma Bank (Saudi Arabia)
        NMBBankParser(),  // NMB Bank / Nabil Bank (Nepal)
        CIBEgyptParser(),  // CIB - Commercial International Bank (Egypt)
        DhanlaxmiBankParser(),  // Dhanlaxmi Bank (India)
        HuntingtonBankParser(),  // Huntington Bank (USA)
        StandardCharteredBankParser()  // Standard Chartered Bank (India)
        // Add more bank parsers here as we implement them
    )

    /**
     * Returns the appropriate bank parser for the given sender.
     * Returns null if no specific parser is found.
     */
    fun getParser(sender: String): BankParser? {
        return parsers.firstOrNull { it.canHandle(sender) }
    }

    /**
     * Try to get a parser using the sender first; if none found and a message body
     * is provided, attempt a fallback by matching the bank name in the message body.
     */
    fun getParser(sender: String, messageBody: String?): BankParser? {
        // Prefer matching by sender
        val bySender = getParser(sender)
        if (bySender != null) return bySender

        // If sender didn't match, try matching by message body content
        if (!messageBody.isNullOrBlank()) {
            // 1) Parser-specific body detection
            parsers.forEach { parser ->
                try {
                    if (parser.canHandleBody(messageBody)) return parser
                } catch (_: Exception) {
                }
            }
            // 2) Fallback: naive match of bank name within body
            val upperBody = messageBody.uppercase()
            parsers.forEach { parser ->
                val bankNameUpper = parser.getBankName().uppercase()
                if (upperBody.contains(bankNameUpper)) return parser
            }
        }

        return null
    }

    /**
     * Returns the bank parser for the given bank name.
     * Returns null if no specific parser is found.
     */
    fun getParserByName(bankName: String): BankParser? {
        return parsers.firstOrNull { it.getBankName() == bankName }
    }

    /**
     * Returns all available bank parsers.
     */
    fun getAllParsers(): List<BankParser> = parsers

    /**
     * Checks if the sender belongs to any known bank.
     */
    fun isKnownBankSender(sender: String): Boolean {
        return parsers.any { it.canHandle(sender) }
    }
}
