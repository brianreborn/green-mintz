package mintz.domain

const val RAPID_BAND_PCT = 3
const val RAPID_TICK_MS = 5_000L
const val RAPID_MAX_QUOTE_USDC = 50.0

data class RapidPolicy(
    val alts: Boolean = false,
    val nfts: Boolean = false,
    val paper: Boolean = true,
    val bandPct: Int = RAPID_BAND_PCT,
    val maxQuoteUsdc: Double = RAPID_MAX_QUOTE_USDC,
    val maxDayUsdc: Double = 500.0,
) {
    val armed: Boolean get() = alts || nfts
}

data class FloorMark(
    val venueId: String,
    val collection: String,
    val floorUsdc: Double,
    val openUsdc: Double,
)

enum class NftAction { HOLD, BUY_FLOOR, SELL_NOW, SKIP }

data class NftIntent(
    val venueId: String,
    val action: NftAction,
    val reason: String,
    val maxUsdc: Double = 0.0,
)

fun defaultAltBook(): List<LiquidPair> = listOf(
    LiquidPair("BTC-USDC", "BTC", weight = 30),
    LiquidPair("ETH-USDC", "ETH", weight = 18),
    LiquidPair("SOL-USDC", "SOL", weight = 14),
    LiquidPair("LINK-USDC", "LINK", weight = 8),
    LiquidPair("AVAX-USDC", "AVAX", weight = 8),
    LiquidPair("SUI-USDC", "SUI", weight = 6),
    LiquidPair("DOGE-USDC", "DOGE", weight = 6),
    LiquidPair("XRP-USDC", "XRP", weight = 5),
    LiquidPair("ADA-USDC", "ADA", weight = 5),
)

fun allowedProducts(): Set<String> =
    (defaultLiquidBook() + defaultAltBook()).map { it.productId.uppercase() }.toSet()

fun capIntent(intent: OrderIntent, maxQuote: Double): OrderIntent {
    val q = intent.quoteSize
    if (q == null || q <= maxQuote) return intent
    return intent.copy(quoteSize = maxQuote, reason = intent.reason + " · capped ${maxQuote.toInt()} USDC")
}

fun planNftSleeve(
    venues: List<Venue>,
    nftBudgetUsdc: Double,
    floors: List<FloorMark>,
    policy: RapidPolicy,
    stopped: Boolean,
): List<NftIntent> {
    if (stopped || !policy.nfts) {
        return listOf(NftIntent("none", NftAction.HOLD, "NFT rapid off. Sleeve stays USDC dry powder."))
    }
    if (nftBudgetUsdc < MIN_QUOTE_USDC) {
        return listOf(NftIntent("none", NftAction.HOLD, "NFT sleeve too small to fill."))
    }
    val fillable = venues.filter { it.weight > 0 && !it.discoveryOnly && !it.fungible }
    if (fillable.isEmpty()) {
        return listOf(NftIntent("none", NftAction.SKIP, "No fillable NFT venue (discovery-only skipped)."))
    }
    val weightSum = fillable.sumOf { it.weight }.coerceAtLeast(1)
    return fillable.map { v ->
        val slice = nftBudgetUsdc * (v.weight / weightSum.toDouble())
        val floor = floors.firstOrNull { it.venueId == v.id }
        when {
            floor == null -> NftIntent(v.id, NftAction.HOLD, "${v.label}: no floor print yet. Do not guess.")
            floor.openUsdc > 0 && floor.floorUsdc <= floor.openUsdc * (1.0 - DUMP_VS_OPEN) ->
                NftIntent(v.id, NftAction.SELL_NOW, "${v.label}: floor dumped vs open. Exit now.", slice.coerceAtMost(policy.maxQuoteUsdc))
            slice >= floor.floorUsdc && floor.floorUsdc < floor.openUsdc * (1.0 - BOUNCE_OFF_DIP) ->
                NftIntent(v.id, NftAction.BUY_FLOOR, "${v.label}: floor cheap vs open. Buy once.", slice.coerceAtMost(policy.maxQuoteUsdc))
            else -> NftIntent(v.id, NftAction.HOLD, "${v.label}: no necessary trade.")
        }
    }
}
