package com.brianreborn.greenmintz

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mintz.broker.CoinbaseClient
import mintz.domain.ActionKind
import mintz.domain.Balance
import mintz.domain.BookPlan
import mintz.domain.ConfirmMode
import mintz.domain.HopPath
import mintz.domain.PoolSplit
import mintz.domain.ShareKind
import mintz.domain.Utterance
import mintz.domain.Venue
import mintz.domain.classifyShare
import mintz.domain.defaultNftVenues
import mintz.domain.defaultSplit
import mintz.domain.hopPolicy
import mintz.domain.mayMint
import mintz.domain.needsUserConfirm
import mintz.domain.parseUtterance
import mintz.domain.resolveVenueToken
import mintz.domain.setVenueWeight
import mintz.domain.usdToSats

enum class Tab { POOLS, VENUES, RETRIEVE, ART, BOOK }

enum class HopPhase { IDLE, CHECKLIST, WAITING, LANDED }

enum class ArtIntent { NONE, MINT, COMMISSION }

data class Pending(
    val kind: ActionKind,
    val title: String,
    val body: String,
    val apply: () -> Unit,
)

data class ShareItem(
    val kind: ShareKind,
    val name: String,
    val uri: Uri? = null,
)

data class CoachUiState(
    val tab: Tab = Tab.POOLS,
    val confirmMode: ConfirmMode = ConfirmMode.DEVELOPMENT,
    val stopped: Boolean = false,
    val split: PoolSplit = defaultSplit(),
    val venues: List<Venue> = defaultNftVenues(),
    val pileUsd: Double = 8.0,
    val lightningOk: Boolean = true,
    val usdcOk: Boolean = false,
    val btcUsd: Double = 77_000.0,
    val hopPhase: HopPhase = HopPhase.IDLE,
    val share: ShareItem? = null,
    val artIntent: ArtIntent = ArtIntent.NONE,
    val artTitle: String = "",
    val chain: String = "solana",
    val clientExclusive: Boolean = false,
    val rightsFlag: Boolean = false,
    val mintVenues: Set<String> = setOf("magicEden", "tensor"),
    val lastLine: String = "Ramp pile is too small for on-chain. Use Lightning or leave it. Sliders are armed.",
    val pending: Pending? = null,
    val speak: String = "",
    val bookArmed: Boolean = false,
    val convertArmed: Boolean = false,
    val rapidAlts: Boolean = false,
    val rapidNfts: Boolean = false,
    val paperBook: Boolean = true,
    val keyPresent: Boolean = false,
    val keyLabel: String = "no key on this phone",
    val balances: List<Balance> = emptyList(),
    val planLine: String = "",
) {
    val sats: Long get() = usdToSats(pileUsd, btcUsd)
    val hopPath: HopPath get() = hopPolicy(sats, lightningOk, usdcOk)
    val canMint: Boolean get() = mayMint(true, clientExclusive, rightsFlag)
}

object CoachStore {
    private val _state = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = _state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var app: Context? = null
    private var gate: CompletableDeferred<Boolean>? = null

    fun attach(ctx: Context) {
        app = ctx.applicationContext
        KeyVault.init(ctx)
        _state.update { it.copy(keyPresent = KeyVault.present(), keyLabel = KeyVault.label()) }
    }

    fun setTab(tab: Tab) = _state.update { it.copy(tab = tab) }
    fun setSpeak(speak: String) = _state.update { it.copy(speak = speak) }
    fun setPileUsd(pileUsd: Double) = _state.update { it.copy(pileUsd = pileUsd.coerceAtLeast(0.0)) }
    fun setLightningOk(v: Boolean) = _state.update { it.copy(lightningOk = v) }
    fun setUsdcOk(v: Boolean) = _state.update { it.copy(usdcOk = v) }
    fun setConfirmMode(m: ConfirmMode) = _state.update { it.copy(confirmMode = m) }
    fun setArtTitle(s: String) = _state.update { it.copy(artTitle = s) }
    fun setChain(c: String) = _state.update { it.copy(chain = c) }
    fun setClientExclusive(v: Boolean) = _state.update { it.copy(clientExclusive = v) }
    fun setRightsFlag(v: Boolean) = _state.update { it.copy(rightsFlag = v) }
    fun toggleMintVenue(id: String) = _state.update {
        val next = if (id in it.mintVenues) it.mintVenues - id else it.mintVenues + id
        it.copy(mintVenues = next)
    }

    fun setArtIntent(intent: ArtIntent) = _state.update { s ->
        val share = s.share
        val kind = if (intent == ArtIntent.MINT || intent == ArtIntent.COMMISSION) ShareKind.ART else share?.kind
        s.copy(
            artIntent = intent,
            share = if (share != null && kind != null) share.copy(kind = kind) else share,
            lastLine = when (intent) {
                ArtIntent.COMMISSION -> "Commission path. VGen and Fantia are tap-lists. No scrape, no auto-post."
                ArtIntent.MINT -> "Mint to YOUR wallet. Grok never receives this file as custody."
                ArtIntent.NONE -> s.lastLine
            },
        )
    }

    fun dismissPending() {
        _state.update { it.copy(pending = null) }
        gate?.complete(false)
        gate = null
    }

    fun confirmPending() {
        val p = _state.value.pending ?: return
        p.apply()
        _state.update { it.copy(pending = null) }
        gate?.complete(true)
        gate = null
    }

    fun request(pending: Pending) {
        if (!needsUserConfirm(_state.value.confirmMode, pending.kind)) {
            pending.apply()
            return
        }
        _state.update { it.copy(pending = pending) }
    }

    suspend fun requestAndAwait(pending: Pending): Boolean {
        if (!needsUserConfirm(_state.value.confirmMode, pending.kind)) {
            pending.apply()
            return true
        }
        val d = CompletableDeferred<Boolean>()
        gate = d
        _state.update { it.copy(pending = pending, tab = Tab.BOOK) }
        val ok = withTimeoutOrNull(90_000) { d.await() }
        if (ok == null) {
            _state.update { it.copy(pending = null, lastLine = "Confirm timed out. Book still armed.") }
            gate = null
            return false
        }
        return ok
    }

    fun kill() {
        gate?.complete(false)
        gate = null
        _state.update {
            it.copy(
                stopped = true,
                bookArmed = false,
                convertArmed = false,
                rapidAlts = false,
                rapidNfts = false,
                hopPhase = HopPhase.IDLE,
                lastLine = "Stopped. Cancel open Coinbase orders. Revoke the view+trade key. Nothing keeps buying.",
                pending = null,
            )
        }
        app?.let { WatchService.stop(it) }
        scope.launch {
            val key = KeyVault.load() ?: return@launch
            val n = runCatching { CoinbaseClient(key.name, key.privateKeyPem).cancelAllOpen() }.getOrDefault(0)
            bookNote("Stopped. Cancelled $n open orders.")
        }
    }

    fun proposeSplit(liquid: Int) {
        val next = _state.value.split.withLiquid(liquid)
        request(
            Pending(
                kind = ActionKind.POOL_MATH,
                title = "Apply pool split",
                body = "Liquid crypto ${next.liquidCrypto}% · NFT ${next.nft}%. Development confirms every coach step.",
                apply = {
                    _state.update { it.copy(split = next, lastLine = "Split ${next.liquidCrypto} / ${next.nft}") }
                },
            ),
        )
    }

    fun proposeVenue(id: String, weight: Int) {
        val next = setVenueWeight(_state.value.venues, id, weight)
        val row = next.firstOrNull { it.id == id }
        request(
            Pending(
                kind = ActionKind.POOL_MATH,
                title = "Change venue weight",
                body = "${row?.label ?: id} → ${row?.weight ?: 0}%. Zero means no new capital. Weights renormalize.",
                apply = {
                    _state.update {
                        it.copy(
                            venues = next,
                            lastLine = "${row?.label} at ${row?.weight}%. " +
                                if (row?.weight == 0) "No new capital there." else "",
                        )
                    }
                },
            ),
        )
    }

    fun beginHop() {
        if (_state.value.stopped) return
        request(
            Pending(
                kind = ActionKind.HOP,
                title = "Show hop taps",
                body = "Coach lists taps. You copy addresses in Coinbase or Cash App. green-mintz never supplies an invoice.",
                apply = {
                    _state.update { it.copy(hopPhase = HopPhase.CHECKLIST, tab = Tab.RETRIEVE) }
                },
            ),
        )
    }

    fun markPaid() = _state.update {
        it.copy(hopPhase = HopPhase.WAITING, lastLine = "Watch Coinbase. Do not send again.")
    }

    fun setShare(share: ShareItem?) = _state.update { s ->
        s.copy(
            share = share,
            artIntent = ArtIntent.NONE,
            tab = if (share != null) Tab.ART else s.tab,
            lastLine = when (share?.kind) {
                ShareKind.SCREENSHOT -> "Cash App screenshot is hop insight only. It will not mint."
                ShareKind.ART -> "Image landed in Art. Mint, commission, or both."
                ShareKind.NOTE -> "Shared a note. It is not an NFT unless you say so with a different file."
                null -> s.lastLine
            },
            artTitle = if (share?.kind == ShareKind.ART && s.artTitle.isEmpty()) {
                share.name.substringBeforeLast('.')
            } else {
                s.artTitle
            },
        )
    }

    fun markScreenshot() = _state.update { s ->
        val share = s.share ?: return@update s
        s.copy(
            share = share.copy(kind = ShareKind.SCREENSHOT),
            artIntent = ArtIntent.NONE,
            lastLine = "Cash App screenshot is hop insight only. It will not mint.",
        )
    }

    fun requestMint() {
        val s = _state.value
        if (s.stopped || !s.canMint) return
        val title = s.artTitle.ifBlank { "untitled" }
        request(
            Pending(
                kind = ActionKind.MINT,
                title = "Open wallet to sign",
                body = "Mint “$title” on ${s.chain} to your wallet. No Grok destination.",
                apply = {
                    _state.update { it.copy(lastLine = "Open your wallet and sign. Coach stops before Confirm.") }
                },
            ),
        )
    }

    fun saveKey(raw: String) {
        try {
            KeyVault.savePaste(raw)
            _state.update {
                it.copy(
                    keyPresent = true,
                    keyLabel = KeyVault.label(),
                    lastLine = "Key saved on this phone only. Transfer stays off.",
                    tab = Tab.BOOK,
                )
            }
        } catch (e: Exception) {
            bookError(e.message ?: "Could not save key")
        }
    }

    fun clearKey() {
        KeyVault.clear()
        disarmBook()
        _state.update { it.copy(keyPresent = false, keyLabel = KeyVault.label(), lastLine = "Key removed from this phone.") }
    }

    fun testKey() {
        scope.launch {
            val key = KeyVault.load() ?: return@launch bookError("No key")
            runCatching {
                val bals = CoinbaseClient(key.name, key.privateKeyPem).listBalances()
                setBalances(bals)
                bookNote("View works. ${bals.size} positive balances. Trade will fire when armed.")
            }.onFailure { bookError(it.message ?: "key test failed") }
        }
    }

    fun armBook() {
        if (_state.value.stopped) return
        if (!KeyVault.present()) {
            bookError("Paste a View+Trade CDP key first. Transfer off.")
            return
        }
        request(
            Pending(
                kind = ActionKind.BOOK_ORDER,
                title = "Arm Coinbase book",
                body = "While this app runs it will place *-USDC orders on YOUR Coinbase. Watch them on another console. Transfer is off. STOP cancels opens.",
                apply = {
                    _state.update {
                        it.copy(bookArmed = true, stopped = false, tab = Tab.BOOK, lastLine = "Watching Coinbase *-USDC")
                    }
                    app?.let { WatchService.start(it) }
                },
            ),
        )
    }

    fun armRapid() {
        propose(
            Pending(
                kind = ActionKind.BOOK_ORDER,
                title = "Arm rapid alts + NFTs",
                body = "5s ticks. Coinbase *-USDC alts auto. NFT sleeve only trades when floor dumps or is cheap. Caps 50 USDC/order. Transfer off. STOP cancels.",
                apply = {
                    _state.update {
                        it.copy(
                            bookArmed = true,
                            rapidAlts = true,
                            rapidNfts = true,
                            paperBook = false,
                            confirmMode = ConfirmMode.PRODUCTION,
                            stopped = false,
                            tab = Tab.BOOK,
                            lastLine = "Rapid on. Alts 5s. NFT when necessary.",
                        )
                    }
                    app?.let { WatchService.start(it) }
                },
            ),
        )
    }

    fun setPaper(v: Boolean) = _state.update {
        it.copy(paperBook = v, lastLine = if (v) "Paper book. Plans only." else "Live book.")
    }

    fun disarmBook() {
        _state.update {
            it.copy(
                bookArmed = false,
                convertArmed = false,
                rapidAlts = false,
                rapidNfts = false,
                lastLine = "Book disarmed.",
            )
        }
        app?.let { WatchService.stop(it) }
    }

    fun armConvert() {
        _state.update { it.copy(convertArmed = true, lastLine = "Next tick will sell BTC to USDC.") }
        if (_state.value.bookArmed) app?.let { WatchService.start(it) }
    }

    fun clearConvert() = _state.update { it.copy(convertArmed = false) }

    fun setBalances(balances: List<Balance>) = _state.update { it.copy(balances = balances) }

    fun setPlanNotes(plan: BookPlan) = _state.update {
        it.copy(planLine = plan.notes.joinToString(" "))
    }

    fun bookNote(msg: String) = _state.update { it.copy(lastLine = msg, planLine = msg) }

    fun bookError(msg: String) = _state.update { it.copy(lastLine = msg, planLine = msg) }

    fun runUtterance(raw: String) {
        val u = parseUtterance(raw)
        if (u is Utterance.Stop) {
            kill()
            return
        }
        if (_state.value.stopped && u !is Utterance.Unknown) {
            _state.update { it.copy(lastLine = "Stopped. Start a new session to trade again.", speak = "") }
            return
        }
        when (u) {
            is Utterance.SetSplit -> proposeSplit(u.split.liquidCrypto)
            is Utterance.NudgeNft -> proposeSplit(_state.value.split.liquidCrypto - u.delta)
            is Utterance.ZeroVenue -> {
                val v = resolveVenueToken(u.token, _state.value.venues)
                if (v != null) proposeVenue(v.id, 0)
                else _state.update { it.copy(lastLine = "No venue matches “${u.token}”.") }
            }
            is Utterance.ListArt -> _state.update { it.copy(tab = Tab.ART) }
            is Utterance.ArmBook -> armBook()
            is Utterance.ArmRapid -> armRapid()
            is Utterance.PaperBook -> setPaper(true)
            is Utterance.DisarmBook -> disarmBook()
            is Utterance.ConvertBtc -> armConvert()
            is Utterance.Holdings -> _state.update {
                it.copy(
                    lastLine = "Liquid ${it.split.liquidCrypto}% · NFT ${it.split.nft}%. Cash App pile ~$${it.pileUsd}.",
                )
            }
            is Utterance.Unknown -> _state.update {
                it.copy(lastLine = "Try: arm rapid · paper · arm book · convert btc · stop")
            }
            else -> Unit
        }
        _state.update { it.copy(speak = "") }
    }

    fun ingestPicked(context: Context, uri: Uri) {
        ingestUri(context, uri, context.contentResolver.getType(uri), null)
    }

    fun ingestIntent(context: Context, intent: Intent?) {
        if (intent == null) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        val first = Inbox.streamUris(intent).firstOrNull()
        if (first != null) {
            ingestUri(context, first, context.contentResolver.getType(first) ?: intent.type, text)
            return
        }
        if (!text.isNullOrBlank()) {
            setShare(ShareItem(kind = classifyShare(intent.type, null, text), name = text.take(80)))
        }
    }

    private fun ingestUri(context: Context, uri: Uri, mime: String?, text: String?) {
        val copied = Inbox.copy(context, uri)
        val name = copied?.first ?: uri.lastPathSegment ?: "shared"
        val kind = classifyShare(mime, name, text)
        setShare(ShareItem(kind = kind, name = name, uri = copied?.second ?: uri))
    }
}
