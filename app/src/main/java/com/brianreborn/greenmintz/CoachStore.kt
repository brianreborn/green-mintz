package com.brianreborn.greenmintz

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mintz.domain.ActionKind
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

enum class Tab { POOLS, VENUES, RETRIEVE, ART }

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
) {
    val sats: Long get() = usdToSats(pileUsd, btcUsd)
    val hopPath: HopPath get() = hopPolicy(sats, lightningOk, usdcOk)
    val canMint: Boolean get() = mayMint(true, clientExclusive, rightsFlag)
}

object CoachStore {
    private val _state = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

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

    fun dismissPending() = _state.update { it.copy(pending = null) }

    fun confirmPending() {
        val p = _state.value.pending ?: return
        p.apply()
        _state.update { it.copy(pending = null) }
    }

    fun request(pending: Pending) {
        if (!needsUserConfirm(_state.value.confirmMode, pending.kind)) {
            pending.apply()
            return
        }
        _state.update { it.copy(pending = pending) }
    }

    fun kill() = _state.update {
        it.copy(
            stopped = true,
            hopPhase = HopPhase.IDLE,
            lastLine = "Stopped. Cancel open Coinbase orders. Revoke the view+trade key. Nothing keeps buying.",
            pending = null,
        )
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
            is Utterance.Holdings -> _state.update {
                it.copy(
                    lastLine = "Liquid ${it.split.liquidCrypto}% · NFT ${it.split.nft}%. Cash App pile ~$${it.pileUsd}.",
                )
            }
            is Utterance.Unknown -> _state.update {
                it.copy(lastLine = "Try: set split 70/30 · more NFTs · zero Blur · stop · what do we hold")
            }
            else -> Unit
        }
        _state.update { it.copy(speak = "") }
    }

    fun ingestIntent(context: Context, intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val stream: Uri? = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (stream != null) {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            stream,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    val mime = intent.type
                    val name = stream.lastPathSegment ?: "shared"
                    val kind = classifyShare(mime, name, null)
                    setShare(ShareItem(kind = kind, name = name, uri = stream))
                    return
                }
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!text.isNullOrBlank()) {
                    val kind = classifyShare(intent.type, null, text)
                    setShare(ShareItem(kind = kind, name = text.take(48)))
                }
            }
            Intent.ACTION_PROCESS_TEXT -> {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                if (!text.isNullOrBlank()) {
                    val kind = classifyShare("text/plain", null, text)
                    setShare(ShareItem(kind = kind, name = text.take(48)))
                }
            }
        }
    }
}
