package mintz.domain

import kotlin.math.abs
import kotlin.math.max

data class LiquidPair(
    val productId: String,
    val base: String,
    val quote: String = "USDC",
    val weight: Int,
)

data class Balance(
    val currency: String,
    val available: Double,
)

data class PriceMark(
    val productId: String,
    val mid: Double,
)

data class OrderIntent(
    val productId: String,
    val side: Side,
    val quoteSize: Double? = null,
    val baseSize: Double? = null,
    val reason: String,
    val action: BookAction,
)

data class TargetSlice(
    val currency: String,
    val productId: String?,
    val targetUsdc: Double,
    val currentUsdc: Double,
)

data class BookPlan(
    val totalUsdc: Double,
    val targets: List<TargetSlice>,
    val intents: List<OrderIntent>,
    val notes: List<String>,
)

data class CdpKey(
    val name: String,
    val privateKeyPem: String,
)

fun defaultLiquidBook(): List<LiquidPair> = listOf(
    LiquidPair("BTC-USDC", "BTC", weight = 50),
    LiquidPair("ETH-USDC", "ETH", weight = 25),
    LiquidPair("SOL-USDC", "SOL", weight = 25),
)

fun isForbiddenBrokerPath(path: String): Boolean {
    val p = path.lowercase()
    if (p.contains("://") || p.contains("..") || p.contains("\\")) return true
    val banned = listOf(
        "withdraw",
        "transfer",
        "/addresses",
        "payment-method",
        "convert",
        "portfolios/move",
        "intx/",
        "send",
        "deposit",
    )
    return banned.any { p.contains(it) }
}

fun isAllowedBrokerPath(path: String): Boolean {
    if (isForbiddenBrokerPath(path)) return false
    val p = path.substringBefore("?").lowercase().trim()
    if (!p.startsWith("/api/v3/brokerage/")) return false
    return p == "/api/v3/brokerage/accounts" ||
        p == "/api/v3/brokerage/best_bid_ask" ||
        p == "/api/v3/brokerage/orders/historical/batch" ||
        p == "/api/v3/brokerage/orders" ||
        p == "/api/v3/brokerage/orders/batch_cancel"
}

fun isAllowedProduct(productId: String): Boolean {
    val id = productId.uppercase()
    if (!id.endsWith("-USDC")) return false
    return id in allowedProducts()
}

fun allowedBrokerPath(path: String): Boolean = isAllowedBrokerPath(path)

fun usdcValue(currency: String, amount: Double, marks: List<PriceMark>): Double {
    if (amount <= 0.0) return 0.0
    val c = currency.uppercase()
    if (c == "USDC" || c == "USD") return amount
    val mark = marks.firstOrNull { it.productId.equals("$c-USDC", ignoreCase = true) } ?: return 0.0
    if (mark.mid <= 0.0) return 0.0
    return amount * mark.mid
}

fun planBook(
    balances: List<Balance>,
    marks: List<PriceMark>,
    split: PoolSplit,
    book: List<LiquidPair> = defaultLiquidBook(),
    convertBtc: Boolean = false,
    stopped: Boolean = false,
    bookArmed: Boolean = true,
    hasOpenOrders: Boolean = false,
    bandPct: Int = REBALANCE_BAND_PCT,
    maxQuoteUsdc: Double = Double.MAX_VALUE,
): BookPlan {
    val notes = mutableListOf<String>()
    val byCcy = balances.groupingBy { it.currency.uppercase() }
        .fold(0.0) { acc, b -> acc + b.available }

    val current = linkedMapOf<String, Double>()
    current["USDC"] = (byCcy["USDC"] ?: 0.0) + (byCcy["USD"] ?: 0.0)
    for (pair in book) {
        current[pair.base] = usdcValue(pair.base, byCcy[pair.base] ?: 0.0, marks)
    }
    val total = current.values.sum()
    if (total <= 0.0) {
        return BookPlan(0.0, emptyList(), emptyList(), listOf("No Coinbase balances yet. Hop Lightning first."))
    }

    val liquidBudget = total * (split.liquidCrypto / 100.0)
    val nftBudget = total * (split.nft / 100.0)
    val bookSum = book.sumOf { it.weight }.coerceAtLeast(1)
    val targets = mutableListOf<TargetSlice>()
    for (pair in book) {
        val t = liquidBudget * (pair.weight / bookSum.toDouble())
        targets += TargetSlice(pair.base, pair.productId, t, current[pair.base] ?: 0.0)
    }
    targets += TargetSlice("USDC", null, nftBudget, current["USDC"] ?: 0.0)

    if (stopped || !bookArmed) {
        return BookPlan(total, targets, emptyList(), listOf("Book disarmed. No new Coinbase orders."))
    }
    if (hasOpenOrders) {
        return BookPlan(total, targets, emptyList(), listOf("Open orders on Coinbase — waiting, not stacking."))
    }

    val intents = mutableListOf<OrderIntent>()
    val btcAmt = byCcy["BTC"] ?: 0.0
    val btcVal = current["BTC"] ?: 0.0
    if (convertBtc && btcAmt > 0.0 && btcVal >= MIN_QUOTE_USDC) {
        intents += OrderIntent(
            productId = "BTC-USDC",
            side = Side.SELL,
            baseSize = btcAmt,
            reason = "Convert hopped BTC to USDC funding book",
            action = BookAction.CONVERT_BTC,
        )
        return BookPlan(total, targets, intents, listOf("Flatten BTC → USDC this tick. Rebalance next."))
    }

    val threshold = max(MIN_QUOTE_USDC, total * (bandPct / 100.0))
    val sells = mutableListOf<OrderIntent>()
    val buys = mutableListOf<OrderIntent>()
    for (slice in targets) {
        val pid = slice.productId ?: continue
        val delta = slice.currentUsdc - slice.targetUsdc
        if (abs(delta) < threshold) continue
        if (delta > 0) {
            sells += OrderIntent(
                productId = pid,
                side = Side.SELL,
                quoteSize = delta,
                reason = "Rebalance ${slice.currency} overweight by ${delta.toInt()} USDC",
                action = BookAction.REBALANCE,
            )
        } else {
            buys += OrderIntent(
                productId = pid,
                side = Side.BUY,
                quoteSize = -delta,
                reason = "Rebalance ${slice.currency} underweight by ${(-delta).toInt()} USDC",
                action = BookAction.REBALANCE,
            )
        }
    }
    intents += sells.map { capIntent(it, maxQuoteUsdc) }
    intents += buys.map { capIntent(it, maxQuoteUsdc) }
    if (intents.isEmpty()) notes += "Drift inside ${bandPct}% band. Watching."
    else notes += "Sells first, then buys. NFT sleeve stays USDC."
    return BookPlan(total, targets, intents, notes)
}

fun parseCdpPaste(raw: String): CdpKey? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    fun unescape(s: String): String = s.replace("\\n", "\n").replace("\\r", "").trim()
    fun grab(key: String): String? {
        val re = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
        return re.find(t)?.groupValues?.get(1)
    }
    val pem = grab("privateKey") ?: grab("private_key")
        ?: Regex("-----BEGIN [^-]+-----.*?-----END [^-]+-----", setOf(RegexOption.DOT_MATCHES_ALL))
            .find(t)?.value
    val name = grab("name") ?: grab("id")
    if (!pem.isNullOrBlank() && !name.isNullOrBlank()) {
        val normalized = unescape(pem)
        if (!normalized.contains("BEGIN")) return null
        if (!name.contains("organizations/") && !name.contains("apiKeys")) {
            // still accept a UUID-looking name; Coinbase also uses the organizations/... form
        }
        return CdpKey(name = unescape(name), privateKeyPem = normalized)
    }
    return null
}
