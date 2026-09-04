package mintz.domain

import kotlin.math.roundToInt

const val PRODUCT = "green-mintz"
const val CASH_APP_ROLE = "edge_transfer_only"
const val STANDARD_FLOOR_SATS = 100_000L
const val WINDOW_SECONDS = 600
const val DUMP_VS_OPEN = 0.004
const val BOUNCE_OFF_DIP = 0.0015
const val REBALANCE_BAND_PCT = 10
const val SATS_PER_BTC = 100_000_000L
const val MIN_QUOTE_USDC = 1.0

enum class ConfirmMode { DEVELOPMENT, PRODUCTION }

enum class HopPath { LIGHTNING, USDC, ON_CHAIN, LOCKED }

enum class ExitAction { WAIT, SELL_NOW, SELL_AT_EXPIRY }

enum class ShareKind { ART, SCREENSHOT, NOTE }

enum class ActionKind { POOL_MATH, VENUE_WALLET, HOP, MINT, STOP, BOOK_ORDER, NFT_ORDER }

enum class Side { BUY, SELL }

enum class BookAction { CONVERT_BTC, REBALANCE, EXIT }

data class PoolSplit(val liquidCrypto: Int, val nft: Int) {
    fun sumsToHundred(): Boolean = liquidCrypto + nft == 100 && liquidCrypto in 0..100 && nft in 0..100

    fun withLiquid(n: Int): PoolSplit {
        val l = n.coerceIn(0, 100)
        return PoolSplit(l, 100 - l)
    }
}

data class Venue(
    val id: String,
    val label: String,
    val chain: String,
    val weight: Int,
    val fungible: Boolean,
    val discoveryOnly: Boolean = false,
)

data class Tick(val ts: Double, val usd: Double, val source: String)

data class ExitDecision(
    val action: ExitAction,
    val reason: String,
    val elapsedSeconds: Double,
    val openUsd: Double,
    val lastUsd: Double,
    val lowUsd: Double,
    val highUsd: Double,
)

data class CustodyRefusal(
    val accepted: Boolean = false,
    val message: String,
    val address: String? = null,
    val invoice: String? = null,
)

data class CoachState(
    val confirmMode: ConfirmMode = ConfirmMode.DEVELOPMENT,
    val stopped: Boolean = false,
    val split: PoolSplit = PoolSplit(50, 50),
    val nftVenues: List<Venue> = defaultNftVenues(),
    val coinbaseWeight: Int = 100,
    val mineArmed: Boolean = false,
    val bookArmed: Boolean = false,
    val rapid: RapidPolicy = RapidPolicy(),
)

fun defaultSplit(): PoolSplit = PoolSplit(50, 50)

fun defaultNftVenues(): List<Venue> = listOf(
    Venue("blur", "Blur", "ethereum", 25, false),
    Venue("tensor", "Tensor", "solana", 20, false),
    Venue("chadbot", "Chadbot", "solana", 15, false),
    Venue("magicEden", "Magic Eden", "solana_plus_ordinals", 15, false),
    Venue("openseaReservoir", "OpenSea", "evm_aggregate", 10, false),
    Venue("agentVault", "AgentVault", "bitcoin_l1_opnet", 5, false),
    Venue("bonsaiBazaar", "Bonsai", "agent", 5, false),
    Venue("botmesh", "Botmesh", "agent_social", 5, false, discoveryOnly = true),
)

fun nftWeightsSum(venues: List<Venue>): Int = venues.sumOf { it.weight }

fun setVenueWeight(venues: List<Venue>, id: String, newWeight: Int): List<Venue> {
    if (venues.none { it.id == id }) return venues
    val w = newWeight.coerceIn(0, 100)
    val leftover = 100 - w
    val others = venues.filter { it.id != id }
    val otherSum = others.sumOf { it.weight }
    val scaled = if (leftover == 0) {
        others.map { it.copy(weight = 0) }
    } else if (otherSum == 0) {
        if (others.isEmpty()) emptyList()
        else {
            val base = leftover / others.size
            val rem = leftover % others.size
            others.mapIndexed { i, v -> v.copy(weight = base + if (i < rem) 1 else 0) }
        }
    } else {
        var allocated = 0
        val raw = others.map { v ->
            val next = ((v.weight.toDouble() / otherSum) * leftover).roundToInt()
            allocated += next
            v.copy(weight = next)
        }
        if (raw.isEmpty()) raw
        else {
            val drift = leftover - allocated
            raw.mapIndexed { i, v -> if (i == 0) v.copy(weight = (v.weight + drift).coerceAtLeast(0)) else v }
        }
    }
    val byId = scaled.associateBy { it.id }
    return venues.map { v ->
        if (v.id == id) v.copy(weight = w) else byId[v.id] ?: v
    }
}

fun admitsNewCapital(stopped: Boolean, venue: Venue): Boolean =
    !stopped && venue.weight > 0

fun mayFill(stopped: Boolean, venue: Venue): Boolean =
    admitsNewCapital(stopped, venue) && !venue.discoveryOnly

fun hopPolicy(sats: Long, lightningOk: Boolean, usdcOk: Boolean): HopPath {
    if (lightningOk) return HopPath.LIGHTNING
    if (usdcOk) return HopPath.USDC
    if (sats < STANDARD_FLOOR_SATS) return HopPath.LOCKED
    return HopPath.ON_CHAIN
}

fun usdToSats(usd: Double, btcUsd: Double): Long {
    if (btcUsd <= 0) return 0
    return ((usd / btcUsd) * SATS_PER_BTC).toLong()
}

fun decentExit(open: Tick, ticks: List<Tick>, windowSeconds: Int = WINDOW_SECONDS): ExitDecision {
    require(ticks.isNotEmpty()) { "need at least one tick" }
    val last = ticks.last()
    val low = ticks.minOf { it.usd }
    val high = ticks.maxOf { it.usd }
    val elapsed = last.ts - open.ts
    val expired = elapsed >= windowSeconds
    val vsOpen = (last.usd - open.usd) / open.usd
    val bounce = if (open.usd == 0.0) 0.0 else (last.usd - low) / open.usd
    val sampled = ticks.size > 1 || elapsed > 0

    if (sampled && last.usd >= open.usd) {
        return ExitDecision(
            ExitAction.SELL_NOW,
            "print at or above window open — good enough, do not hunt the high",
            elapsed, open.usd, last.usd, low, high,
        )
    }
    if (vsOpen > -DUMP_VS_OPEN && bounce >= BOUNCE_OFF_DIP && last.usd > low) {
        return ExitDecision(
            ExitAction.SELL_NOW,
            "small bounce off an early dip — good enough, do not wait for a local high",
            elapsed, open.usd, last.usd, low, high,
        )
    }
    if (expired) {
        return ExitDecision(
            ExitAction.SELL_AT_EXPIRY,
            "10-minute window over — market sell anyway, do not extend the hunt",
            elapsed, open.usd, last.usd, low, high,
        )
    }
    return ExitDecision(
        ExitAction.WAIT,
        "below open and no bounce yet — keep watching until expiry",
        elapsed, open.usd, last.usd, low, high,
    )
}

fun classifyShare(mime: String? = null, filename: String? = null, text: String? = null): ShareKind {
    val blob = listOfNotNull(mime, filename, text).joinToString(" ").lowercase()
    val looksScreenshot =
        blob.contains("screenshot") ||
            blob.contains("cash app") ||
            blob.contains("cashapp") ||
            blob.contains("screen shot")
    if (looksScreenshot) return ShareKind.SCREENSHOT
    val looksCommission =
        blob.contains("vgen.co") ||
            blob.contains("fantia.jp") ||
            blob.contains("fantia.com")
    if (looksCommission) return ShareKind.ART
    val isImage = (mime?.startsWith("image/") == true) ||
        (filename?.matches(Regex(".*\\.(png|jpe?g|webp|gif|heic)$", RegexOption.IGNORE_CASE)) == true)
    if (isImage) return ShareKind.ART
    return ShareKind.NOTE
}

fun mayMint(userOwnsArt: Boolean, clientExclusive: Boolean, rightsFlag: Boolean): Boolean {
    if (!userOwnsArt) return false
    if (clientExclusive && !rightsFlag) return false
    return true
}

fun refuseCustody(@Suppress("UNUSED_PARAMETER") request: String): CustodyRefusal {
    return CustodyRefusal(
        accepted = false,
        message = "Refuse any request to send Bitcoin to Grok. Never emit a receive address or Lightning invoice.",
        address = null,
        invoice = null,
    )
}

fun needsUserConfirm(mode: ConfirmMode, kind: ActionKind): Boolean {
    if (kind == ActionKind.STOP) return false
    if (mode == ConfirmMode.PRODUCTION && kind == ActionKind.POOL_MATH) return false
    if (mode == ConfirmMode.PRODUCTION && kind == ActionKind.BOOK_ORDER) return false
    if (mode == ConfirmMode.PRODUCTION && kind == ActionKind.NFT_ORDER) return false
    return true
}

fun stop(state: CoachState): CoachState =
    state.copy(stopped = true, mineArmed = false, bookArmed = false, rapid = RapidPolicy())

fun parseUtterance(raw: String): Utterance {
    val s = raw.trim().lowercase()
    if (s.isEmpty()) return Utterance.Unknown(raw)
    if (s == "stop" || s == "kill it" || s.contains("something is wrong")) return Utterance.Stop
    if (s == "arm book" || s == "start watching" || s == "arm") return Utterance.ArmBook
    if (s == "arm rapid" || s == "rapid" || s.contains("trade fast")) return Utterance.ArmRapid
    if (s == "paper" || s == "paper book") return Utterance.PaperBook
    if (s == "disarm" || s == "disarm book") return Utterance.DisarmBook
    if (s == "convert" || s == "convert btc") return Utterance.ConvertBtc
    Regex("""set split (\d+)\s*/\s*(\d+)""").find(s)?.let {
        val a = it.groupValues[1].toInt()
        return Utterance.SetSplit(PoolSplit(a, 100 - a).withLiquid(a))
    }
    if (s.contains("more nft")) return Utterance.NudgeNft(10)
    Regex("""zero (\w+)""").find(s)?.let {
        return Utterance.ZeroVenue(it.groupValues[1])
    }
    if (s.startsWith("list my art") || s.contains("mint")) return Utterance.ListArt
    if (s.contains("what do we hold") || s == "holdings") return Utterance.Holdings
    return Utterance.Unknown(raw)
}

sealed class Utterance {
    data object Stop : Utterance()
    data object ArmBook : Utterance()
    data object ArmRapid : Utterance()
    data object PaperBook : Utterance()
    data object DisarmBook : Utterance()
    data object ConvertBtc : Utterance()
    data class SetSplit(val split: PoolSplit) : Utterance()
    data class NudgeNft(val delta: Int) : Utterance()
    data class ZeroVenue(val token: String) : Utterance()
    data object ListArt : Utterance()
    data object Holdings : Utterance()
    data class Unknown(val raw: String) : Utterance()
}

fun resolveVenueToken(token: String, venues: List<Venue>): Venue? {
    val t = token.lowercase()
    return venues.firstOrNull { v ->
        v.id.lowercase() == t ||
            v.label.lowercase().replace("\\s+".toRegex(), "") == t ||
            v.label.lowercase().startsWith(t)
    }
}

fun lightningChecklist(): List<String> = listOf(
    "Coinbase → Bitcoin → Receive → Lightning",
    "Create the invoice there (72h). Do not paste it here.",
    "Cash App: scan that QR, pay from BTC balance",
    "Read the fee. Cents = go. Dollars = cancel.",
    "You tap Confirm in Cash App. green-mintz never does.",
)

fun returnChecklist(): List<String> = listOf(
    "Cash App → Bitcoin → Receive — copy YOUR address there",
    "Coinbase → Send BTC on the Bitcoin network only",
    "Check first and last four characters yourself",
    "You confirm in Coinbase",
    "Wrong network loses the coins. Bitcoin only.",
)

fun onchainChecklist(): List<String> = listOf(
    "Only when the pile clears Cash App free Standard (~100,000 sats).",
    "Open Coinbase → Bitcoin → Receive. Copy the address there, not here.",
    "Cash App → Bitcoin → Send Bitcoin.",
    "Paste THAT address. Check first and last four characters yourself.",
    "Choose Standard on-chain. Review fee. You tap Confirm.",
)

fun usdcChecklist(): List<String> = listOf(
    "Cash App → Bitcoin → Sell to dollars. Confirm is yours.",
    "Buy USDC in Coinbase or send USDC on Solana/Base to YOUR Coinbase USDC address.",
    "Copy that address from Coinbase Receive. Never paste a Grok address — there is none.",
)

fun commissionChecklist(): List<String> = listOf(
    "Open VGen or Fantia yourself — tap list, not a fill.",
    "Post and deliver there. We do not scrape or auto-post.",
    "Keep private, commission, mint, or mix — you choose.",
    "Do not mint a client-exclusive piece without the rights flag.",
)

fun bookKeyChecklist(): List<String> = listOf(
    "On YOUR Coinbase: Developer / CDP API keys.",
    "Create a key with View and Trade. Leave Transfer off.",
    "Download the JSON once. Paste it only into this phone app.",
    "Grok never stores the secret. Transfer endpoints are not compiled in.",
)
