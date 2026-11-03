package com.merry8989uk.universalcalculator.calc

/**
 * Every country and the currency it spends.
 *
 * The rate service deals in currency codes, but a person thinks in countries:
 * "how much is my rupee worth in Nepal" is a question about Nepal, not about
 * NPR. This maps one to the other so the converter can be asked in the words the
 * question is actually in.
 *
 * Several countries share a currency — the euro across twenty of them, the West
 * African franc across eight — so the mapping goes both ways: a code can belong
 * to more than one country, and a country has exactly one code.
 */
data class Country(val name: String, val code: String)

object Countries {

    val all: List<Country> = listOf(
        // ---- Asia ----
        Country("India", "INR"),
        Country("Nepal", "NPR"),
        Country("Bhutan", "BTN"),
        Country("Bangladesh", "BDT"),
        Country("Pakistan", "PKR"),
        Country("Sri Lanka", "LKR"),
        Country("Maldives", "MVR"),
        Country("Afghanistan", "AFN"),
        Country("China", "CNY"),
        Country("Japan", "JPY"),
        Country("South Korea", "KRW"),
        Country("North Korea", "KPW"),
        Country("Taiwan", "TWD"),
        Country("Hong Kong", "HKD"),
        Country("Macau", "MOP"),
        Country("Mongolia", "MNT"),
        Country("Singapore", "SGD"),
        Country("Malaysia", "MYR"),
        Country("Thailand", "THB"),
        Country("Vietnam", "VND"),
        Country("Cambodia", "KHR"),
        Country("Laos", "LAK"),
        Country("Myanmar", "MMK"),
        Country("Indonesia", "IDR"),
        Country("Philippines", "PHP"),
        Country("Brunei", "BND"),
        Country("Timor-Leste", "USD"),
        Country("Kazakhstan", "KZT"),
        Country("Kyrgyzstan", "KGS"),
        Country("Tajikistan", "TJS"),
        Country("Turkmenistan", "TMT"),
        Country("Uzbekistan", "UZS"),

        // ---- Middle East ----
        Country("United Arab Emirates", "AED"),
        Country("Saudi Arabia", "SAR"),
        Country("Qatar", "QAR"),
        Country("Kuwait", "KWD"),
        Country("Bahrain", "BHD"),
        Country("Oman", "OMR"),
        Country("Yemen", "YER"),
        Country("Jordan", "JOD"),
        Country("Lebanon", "LBP"),
        Country("Syria", "SYP"),
        Country("Iraq", "IQD"),
        Country("Iran", "IRR"),
        Country("Israel", "ILS"),
        Country("Palestine", "ILS"),
        Country("Turkey", "TRY"),
        Country("Georgia", "GEL"),
        Country("Armenia", "AMD"),
        Country("Azerbaijan", "AZN"),

        // ---- Europe ----
        Country("United Kingdom", "GBP"),
        Country("Ireland", "EUR"),
        Country("France", "EUR"),
        Country("Germany", "EUR"),
        Country("Italy", "EUR"),
        Country("Spain", "EUR"),
        Country("Portugal", "EUR"),
        Country("Netherlands", "EUR"),
        Country("Belgium", "EUR"),
        Country("Luxembourg", "EUR"),
        Country("Austria", "EUR"),
        Country("Switzerland", "CHF"),
        Country("Liechtenstein", "CHF"),
        Country("Sweden", "SEK"),
        Country("Norway", "NOK"),
        Country("Denmark", "DKK"),
        Country("Finland", "EUR"),
        Country("Iceland", "ISK"),
        Country("Poland", "PLN"),
        Country("Czechia", "CZK"),
        Country("Slovakia", "EUR"),
        Country("Hungary", "HUF"),
        Country("Slovenia", "EUR"),
        Country("Croatia", "EUR"),
        Country("Bosnia and Herzegovina", "BAM"),
        Country("Serbia", "RSD"),
        Country("Montenegro", "EUR"),
        Country("North Macedonia", "MKD"),
        Country("Albania", "ALL"),
        Country("Greece", "EUR"),
        Country("Bulgaria", "BGN"),
        Country("Romania", "RON"),
        Country("Moldova", "MDL"),
        Country("Ukraine", "UAH"),
        Country("Belarus", "BYN"),
        Country("Russia", "RUB"),
        Country("Lithuania", "EUR"),
        Country("Latvia", "EUR"),
        Country("Estonia", "EUR"),
        Country("Malta", "EUR"),
        Country("Cyprus", "EUR"),
        Country("Monaco", "EUR"),
        Country("Andorra", "EUR"),
        Country("San Marino", "EUR"),
        Country("Vatican City", "EUR"),
        Country("Gibraltar", "GIP"),

        // ---- Africa ----
        Country("Nigeria", "NGN"),
        Country("Ghana", "GHS"),
        Country("Kenya", "KES"),
        Country("Tanzania", "TZS"),
        Country("Uganda", "UGX"),
        Country("Rwanda", "RWF"),
        Country("Burundi", "BIF"),
        Country("Ethiopia", "ETB"),
        Country("Eritrea", "ERN"),
        Country("Djibouti", "DJF"),
        Country("Somalia", "SOS"),
        Country("Sudan", "SDG"),
        Country("South Sudan", "SSP"),
        Country("Egypt", "EGP"),
        Country("Libya", "LYD"),
        Country("Tunisia", "TND"),
        Country("Algeria", "DZD"),
        Country("Morocco", "MAD"),
        Country("Mauritania", "MRU"),
        Country("Senegal", "XOF"),
        Country("Mali", "XOF"),
        Country("Burkina Faso", "XOF"),
        Country("Niger", "XOF"),
        Country("Ivory Coast", "XOF"),
        Country("Togo", "XOF"),
        Country("Benin", "XOF"),
        Country("Guinea-Bissau", "XOF"),
        Country("Cameroon", "XAF"),
        Country("Chad", "XAF"),
        Country("Central African Republic", "XAF"),
        Country("Gabon", "XAF"),
        Country("Republic of the Congo", "XAF"),
        Country("DR Congo", "CDF"),
        Country("Equatorial Guinea", "XAF"),
        Country("Angola", "AOA"),
        Country("Zambia", "ZMW"),
        Country("Zimbabwe", "ZWL"),
        Country("Malawi", "MWK"),
        Country("Mozambique", "MZN"),
        Country("Madagascar", "MGA"),
        Country("Mauritius", "MUR"),
        Country("Seychelles", "SCR"),
        Country("Comoros", "KMF"),
        Country("South Africa", "ZAR"),
        Country("Namibia", "NAD"),
        Country("Botswana", "BWP"),
        Country("Lesotho", "LSL"),
        Country("Eswatini", "SZL"),
        Country("Gambia", "GMD"),
        Country("Guinea", "GNF"),
        Country("Sierra Leone", "SLE"),
        Country("Liberia", "LRD"),
        Country("Cape Verde", "CVE"),
        Country("Sao Tome and Principe", "STN"),

        // ---- North America and the Caribbean ----
        Country("United States", "USD"),
        Country("Canada", "CAD"),
        Country("Mexico", "MXN"),
        Country("Guatemala", "GTQ"),
        Country("Belize", "BZD"),
        Country("Honduras", "HNL"),
        Country("El Salvador", "USD"),
        Country("Nicaragua", "NIO"),
        Country("Costa Rica", "CRC"),
        Country("Panama", "PAB"),
        Country("Cuba", "CUP"),
        Country("Jamaica", "JMD"),
        Country("Haiti", "HTG"),
        Country("Dominican Republic", "DOP"),
        Country("Bahamas", "BSD"),
        Country("Barbados", "BBD"),
        Country("Trinidad and Tobago", "TTD"),
        Country("Guyana", "GYD"),
        Country("Suriname", "SRD"),
        Country("Puerto Rico", "USD"),
        Country("Bermuda", "BMD"),
        Country("Cayman Islands", "KYD"),

        // ---- South America ----
        Country("Brazil", "BRL"),
        Country("Argentina", "ARS"),
        Country("Chile", "CLP"),
        Country("Colombia", "COP"),
        Country("Peru", "PEN"),
        Country("Venezuela", "VES"),
        Country("Ecuador", "USD"),
        Country("Bolivia", "BOB"),
        Country("Paraguay", "PYG"),
        Country("Uruguay", "UYU"),

        // ---- Oceania ----
        Country("Australia", "AUD"),
        Country("New Zealand", "NZD"),
        Country("Fiji", "FJD"),
        Country("Papua New Guinea", "PGK"),
        Country("Solomon Islands", "SBD"),
        Country("Vanuatu", "VUV"),
        Country("Samoa", "WST"),
        Country("Tonga", "TOP"),
        Country("New Caledonia", "XPF"),
        Country("French Polynesia", "XPF"),
    )

    /** A name a person would say for a currency, for the ones the app lists. */
    private val CURRENCY_NAMES = mapOf(
        "INR" to "Indian Rupee", "NPR" to "Nepalese Rupee", "BTN" to "Bhutanese Ngultrum",
        "BDT" to "Bangladeshi Taka", "PKR" to "Pakistani Rupee", "LKR" to "Sri Lankan Rupee",
        "MVR" to "Maldivian Rufiyaa", "AFN" to "Afghan Afghani", "USD" to "US Dollar",
        "EUR" to "Euro", "GBP" to "Pound Sterling", "JPY" to "Japanese Yen",
        "CNY" to "Chinese Yuan", "KRW" to "South Korean Won", "HKD" to "Hong Kong Dollar",
        "SGD" to "Singapore Dollar", "MYR" to "Malaysian Ringgit", "THB" to "Thai Baht",
        "VND" to "Vietnamese Dong", "IDR" to "Indonesian Rupiah", "PHP" to "Philippine Peso",
        "AED" to "UAE Dirham", "SAR" to "Saudi Riyal", "QAR" to "Qatari Riyal",
        "KWD" to "Kuwaiti Dinar", "BHD" to "Bahraini Dinar", "OMR" to "Omani Rial",
        "ILS" to "Israeli Shekel", "TRY" to "Turkish Lira", "RUB" to "Russian Ruble",
        "CHF" to "Swiss Franc", "SEK" to "Swedish Krona", "NOK" to "Norwegian Krone",
        "DKK" to "Danish Krone", "PLN" to "Polish Zloty", "CZK" to "Czech Koruna",
        "HUF" to "Hungarian Forint", "RON" to "Romanian Leu", "UAH" to "Ukrainian Hryvnia",
        "AUD" to "Australian Dollar", "NZD" to "New Zealand Dollar", "CAD" to "Canadian Dollar",
        "MXN" to "Mexican Peso", "BRL" to "Brazilian Real", "ARS" to "Argentine Peso",
        "CLP" to "Chilean Peso", "COP" to "Colombian Peso", "PEN" to "Peruvian Sol",
        "ZAR" to "South African Rand", "NGN" to "Nigerian Naira", "KES" to "Kenyan Shilling",
        "EGP" to "Egyptian Pound", "MAD" to "Moroccan Dirham", "GHS" to "Ghanaian Cedi",
        "TZS" to "Tanzanian Shilling", "UGX" to "Ugandan Shilling", "ETB" to "Ethiopian Birr",
        "XOF" to "West African CFA Franc", "XAF" to "Central African CFA Franc",
        "XPF" to "CFP Franc", "KZT" to "Kazakhstani Tenge", "TWD" to "New Taiwan Dollar",
        "MOP" to "Macanese Pataca", "BND" to "Brunei Dollar", "KHR" to "Cambodian Riel",
        "LAK" to "Lao Kip", "MMK" to "Myanmar Kyat", "MNT" to "Mongolian Tugrik",
        "KGS" to "Kyrgyzstani Som", "UZS" to "Uzbekistani Som", "TJS" to "Tajikistani Somoni",
        "TMT" to "Turkmenistani Manat", "GEL" to "Georgian Lari", "AMD" to "Armenian Dram",
        "AZN" to "Azerbaijani Manat", "IQD" to "Iraqi Dinar", "IRR" to "Iranian Rial",
        "JOD" to "Jordanian Dinar", "LBP" to "Lebanese Pound", "SYP" to "Syrian Pound",
        "YER" to "Yemeni Rial", "BGN" to "Bulgarian Lev", "RSD" to "Serbian Dinar",
        "MKD" to "Macedonian Denar", "ALL" to "Albanian Lek", "BAM" to "Bosnian Mark",
        "MDL" to "Moldovan Leu", "BYN" to "Belarusian Ruble", "ISK" to "Icelandic Krona",
        "GIP" to "Gibraltar Pound", "MUR" to "Mauritian Rupee", "SCR" to "Seychellois Rupee",
        "MGA" to "Malagasy Ariary", "MZN" to "Mozambican Metical", "ZMW" to "Zambian Kwacha",
        "MWK" to "Malawian Kwacha", "BWP" to "Botswana Pula", "NAD" to "Namibian Dollar",
        "LSL" to "Lesotho Loti", "SZL" to "Swazi Lilangeni", "AOA" to "Angolan Kwanza",
        "CDF" to "Congolese Franc", "RWF" to "Rwandan Franc", "BIF" to "Burundian Franc",
        "DJF" to "Djiboutian Franc", "SOS" to "Somali Shilling", "SDG" to "Sudanese Pound",
        "SSP" to "South Sudanese Pound", "LYD" to "Libyan Dinar", "TND" to "Tunisian Dinar",
        "DZD" to "Algerian Dinar", "MRU" to "Mauritanian Ouguiya", "GMD" to "Gambian Dalasi",
        "GNF" to "Guinean Franc", "SLE" to "Sierra Leonean Leone", "LRD" to "Liberian Dollar",
        "CVE" to "Cape Verdean Escudo", "STN" to "Sao Tome Dobra", "KMF" to "Comorian Franc",
        "ERN" to "Eritrean Nakfa", "KPW" to "North Korean Won", "MVR" to "Maldivian Rufiyaa",
        "GTQ" to "Guatemalan Quetzal", "BZD" to "Belize Dollar", "HNL" to "Honduran Lempira",
        "NIO" to "Nicaraguan Cordoba", "CRC" to "Costa Rican Colon", "PAB" to "Panamanian Balboa",
        "CUP" to "Cuban Peso", "JMD" to "Jamaican Dollar", "HTG" to "Haitian Gourde",
        "DOP" to "Dominican Peso", "BSD" to "Bahamian Dollar", "BBD" to "Barbadian Dollar",
        "TTD" to "Trinidad and Tobago Dollar", "GYD" to "Guyanese Dollar",
        "SRD" to "Surinamese Dollar", "BMD" to "Bermudian Dollar", "KYD" to "Cayman Islands Dollar",
        "VES" to "Venezuelan Bolivar", "BOB" to "Bolivian Boliviano", "PYG" to "Paraguayan Guarani",
        "UYU" to "Uruguayan Peso", "FJD" to "Fijian Dollar", "PGK" to "Papua New Guinean Kina",
        "SBD" to "Solomon Islands Dollar", "VUV" to "Vanuatu Vatu", "WST" to "Samoan Tala",
        "TOP" to "Tongan Pa'anga", "ZWL" to "Zimbabwean Dollar",
    )

    /** The countries that use a currency, in the order they appear above. */
    fun byCode(code: String): List<Country> = all.filter { it.code == code.uppercase() }

    /** The currency a country uses, or null if the country is not in the list. */
    fun codeFor(countryName: String): String? =
        all.firstOrNull { it.name.equals(countryName.trim(), ignoreCase = true) }?.code

    /** "Indian Rupee" for INR, falling back to the code itself. */
    fun currencyName(code: String): String = CURRENCY_NAMES[code.uppercase()] ?: code.uppercase()

    /**
     * What to show in a picker: the country, its currency and what that currency
     * is called. "India · INR · Indian Rupee" answers the question the user is
     * actually asking, which is usually about the country.
     */
    fun label(country: Country): String =
        "${country.name} · ${country.code} · ${currencyName(country.code)}"

    /**
     * Countries whose name, currency code or currency name matches [query].
     *
     * A blank query returns everything. Matching a code also matches every
     * country that uses it, so typing EUR finds all twenty.
     */
    fun search(query: String): List<Country> {
        val q = query.trim()
        if (q.isEmpty()) return all
        val lower = q.lowercase()
        return all.filter { c ->
            c.name.lowercase().contains(lower) ||
                c.code.lowercase().contains(lower) ||
                currencyName(c.code).lowercase().contains(lower)
        }
    }

    /** Every currency code the country list mentions, for checking coverage. */
    fun codes(): List<String> = all.map { it.code }.distinct().sorted()
}
