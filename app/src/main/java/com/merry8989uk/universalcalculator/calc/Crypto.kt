package com.merry8989uk.universalcalculator.calc

/**
 * The coins worth listing, with the name people actually search for.
 *
 * A crypto has no country, so it cannot be found the way a currency is found —
 * "the money of Nepal" has one answer, "BTC" has none. It needs both its symbol
 * and its name in the list, because a beginner knows the name and a trader knows
 * the symbol, and the search has to serve both.
 */
data class Coin(val symbol: String, val name: String)

object Crypto {

    val all: List<Coin> = listOf(
        Coin("BTC", "Bitcoin"),
        Coin("ETH", "Ethereum"),
        Coin("USDT", "Tether"),
        Coin("BNB", "BNB"),
        Coin("SOL", "Solana"),
        Coin("XRP", "XRP"),
        Coin("USDC", "USD Coin"),
        Coin("ADA", "Cardano"),
        Coin("DOGE", "Dogecoin"),
        Coin("TRX", "TRON"),
        Coin("AVAX", "Avalanche"),
        Coin("LINK", "Chainlink"),
        Coin("TON", "Toncoin"),
        Coin("SHIB", "Shiba Inu"),
        Coin("DOT", "Polkadot"),
        Coin("LTC", "Litecoin"),
        Coin("BCH", "Bitcoin Cash"),
        Coin("MATIC", "Polygon"),
        Coin("POL", "Polygon Ecosystem Token"),
        Coin("XLM", "Stellar"),
        Coin("UNI", "Uniswap"),
        Coin("ATOM", "Cosmos"),
        Coin("ETC", "Ethereum Classic"),
        Coin("XMR", "Monero"),
        Coin("NEAR", "NEAR Protocol"),
        Coin("APT", "Aptos"),
        Coin("ARB", "Arbitrum"),
        Coin("OP", "Optimism"),
        Coin("FIL", "Filecoin"),
        Coin("ICP", "Internet Computer"),
        Coin("HBAR", "Hedera"),
        Coin("VET", "VeChain"),
        Coin("IMX", "Immutable"),
        Coin("INJ", "Injective"),
        Coin("SUI", "Sui"),
        Coin("SEI", "Sei"),
        Coin("GRT", "The Graph"),
        Coin("AAVE", "Aave"),
        Coin("MKR", "Maker"),
        Coin("ALGO", "Algorand"),
        Coin("FTM", "Fantom"),
        Coin("SAND", "The Sandbox"),
        Coin("MANA", "Decentraland"),
        Coin("AXS", "Axie Infinity"),
        Coin("EOS", "EOS"),
        Coin("XTZ", "Tezos"),
        Coin("FLOW", "Flow"),
        Coin("CHZ", "Chiliz"),
        Coin("CRO", "Cronos"),
        Coin("LDO", "Lido DAO"),
        Coin("RENDER", "Render"),
        Coin("TIA", "Celestia"),
        Coin("WIF", "dogwifhat"),
        Coin("PEPE", "Pepe"),
        Coin("BONK", "Bonk"),
        Coin("JUP", "Jupiter"),
        Coin("STX", "Stacks"),
        Coin("RUNE", "THORChain"),
        Coin("GALA", "Gala"),
        Coin("FET", "Artificial Superintelligence Alliance"),
        Coin("RNDR", "Render (legacy)"),
        Coin("KAS", "Kaspa"),
        Coin("TAO", "Bittensor"),
        Coin("AR", "Arweave"),
        Coin("EGLD", "MultiversX"),
        Coin("THETA", "Theta Network"),
        Coin("BSV", "Bitcoin SV"),
        Coin("DAI", "Dai"),
        Coin("WBT", "WhiteBIT Coin"),
        Coin("LEO", "UNUS SED LEO"),
        Coin("OKB", "OKB"),
        Coin("KCS", "KuCoin Token"),
    )

    private val bySymbol: Map<String, Coin> = all.associateBy { it.symbol }

    fun isCrypto(code: String): Boolean = bySymbol.containsKey(code.uppercase())

    fun name(code: String): String = bySymbol[code.uppercase()]?.name ?: code.uppercase()

    /** "Bitcoin · BTC", the way a coin is written in a list. */
    fun label(coin: Coin): String = "${coin.name} · ${coin.symbol}"

    /**
     * Coins whose symbol or name matches [query].
     *
     * A blank query returns everything. Both halves are searched because "bit"
     * and "BTC" are the same coin to two different people.
     */
    fun search(query: String): List<Coin> {
        val q = query.trim()
        if (q.isEmpty()) return all
        val lower = q.lowercase()
        return all.filter { it.symbol.lowercase().contains(lower) || it.name.lowercase().contains(lower) }
    }
}
