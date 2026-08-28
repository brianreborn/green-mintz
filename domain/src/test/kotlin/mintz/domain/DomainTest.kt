package mintz.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainTest {
    @Test
    fun resolveVenueTokenMatchesLabel() {
        val venues = defaultNftVenues()
        assertEquals("blur", resolveVenueToken("blur", venues)?.id)
        assertEquals("blur", resolveVenueToken("Blur", venues)?.id)
        assertNull(resolveVenueToken("nope", venues))
    }

    @Test
    fun productAndRampRole() {
        assertEquals("green-mintz", PRODUCT)
        assertEquals("edge_transfer_only", CASH_APP_ROLE)
    }

    @Test
    fun poolSplitAlwaysSumsTo100() {
        val d = defaultSplit()
        assertTrue(d.sumsToHundred())
        assertEquals(50, d.liquidCrypto)
        assertEquals(50, d.nft)
        for (n in 0..100) {
            assertTrue(d.withLiquid(n).sumsToHundred())
        }
        assertEquals(0, d.withLiquid(-4).liquidCrypto)
        assertEquals(100, d.withLiquid(140).liquidCrypto)
    }

    @Test
    fun nftDefaultWeightsSumTo100() {
        assertEquals(100, nftWeightsSum(defaultNftVenues()))
    }

    @Test
    fun zeroWeightGetsNoNewCapitalAndRenormalizes() {
        val after = setVenueWeight(defaultNftVenues(), "blur", 0)
        val blur = after.first { it.id == "blur" }
        assertEquals(0, blur.weight)
        assertEquals(100, nftWeightsSum(after))
        assertFalse(admitsNewCapital(false, blur))
        assertTrue(admitsNewCapital(false, after.first { it.id == "tensor" }))
        val stopped = after.first { it.id == "tensor" }
        assertFalse(admitsNewCapital(true, stopped))
    }

    @Test
    fun botmeshNeverFillsEvenWithWeight() {
        val botmesh = defaultNftVenues().first { it.id == "botmesh" }
        assertTrue(botmesh.discoveryOnly)
        assertTrue(admitsNewCapital(false, botmesh))
        assertFalse(mayFill(false, botmesh))
    }

    @Test
    fun hopPolicyLightningFirstUsdcSecondOnChainLocked() {
        assertEquals(HopPath.LIGHTNING, hopPolicy(8_000, lightningOk = true, usdcOk = true))
        assertEquals(HopPath.USDC, hopPolicy(8_000, lightningOk = false, usdcOk = true))
        assertEquals(HopPath.LOCKED, hopPolicy(8_000, lightningOk = false, usdcOk = false))
        assertEquals(HopPath.ON_CHAIN, hopPolicy(STANDARD_FLOOR_SATS, lightningOk = false, usdcOk = false))
        val tiny = usdToSats(8.0, 77_000.0)
        assertTrue(tiny < STANDARD_FLOOR_SATS)
        assertEquals(HopPath.LOCKED, hopPolicy(tiny, lightningOk = false, usdcOk = false))
    }

    @Test
    fun decentExitWaitSellNowSellAtExpiry() {
        val open = Tick(0.0, 100.0, "test")
        val wait = decentExit(open, listOf(open), windowSeconds = 600)
        assertEquals(ExitAction.WAIT, wait.action)

        val up = decentExit(open, listOf(open, Tick(30.0, 100.2, "test")))
        assertEquals(ExitAction.SELL_NOW, up.action)

        val bounce = decentExit(
            open,
            listOf(open, Tick(20.0, 99.7, "test"), Tick(40.0, 99.9, "test")),
        )
        assertEquals(ExitAction.SELL_NOW, bounce.action)

        val expired = decentExit(open, listOf(open, Tick(600.0, 99.0, "test")), windowSeconds = 600)
        assertEquals(ExitAction.SELL_AT_EXPIRY, expired.action)
    }

    @Test
    fun shareClassifierArtVsScreenshot() {
        assertEquals(ShareKind.ART, classifyShare(mime = "image/png", filename = "piece.png"))
        assertEquals(ShareKind.SCREENSHOT, classifyShare(mime = "image/png", filename = "Cash App screenshot.png"))
        assertEquals(ShareKind.SCREENSHOT, classifyShare(text = "cash app bitcoin tab"))
        assertEquals(ShareKind.NOTE, classifyShare(text = "hello"))
    }

    @Test
    fun commissionDoesNotMintWithoutRightsFlag() {
        assertTrue(mayMint(userOwnsArt = true, clientExclusive = false, rightsFlag = false))
        assertFalse(mayMint(userOwnsArt = true, clientExclusive = true, rightsFlag = false))
        assertTrue(mayMint(userOwnsArt = true, clientExclusive = true, rightsFlag = true))
        assertFalse(mayMint(userOwnsArt = false, clientExclusive = false, rightsFlag = true))
    }

    @Test
    fun stopEndsNewWork() {
        val live = CoachState()
        assertFalse(live.stopped)
        val halted = stop(live.copy(mineArmed = true))
        assertTrue(halted.stopped)
        assertFalse(halted.mineArmed)
        defaultNftVenues().forEach { assertFalse(admitsNewCapital(halted.stopped, it)) }
    }

    @Test
    fun neverEmitsAReceiveAddress() {
        val r = refuseCustody("send btc to grok bc1qexample")
        assertFalse(r.accepted)
        assertNull(r.address)
        assertNull(r.invoice)
        assertNotNull(r.message)
        assertFalse(r.message.contains("bc1", ignoreCase = true))
        lightningChecklist().forEach { line ->
            assertFalse(line.contains("bc1", ignoreCase = true))
            assertFalse(line.contains("lnbc", ignoreCase = true))
        }
        (onchainChecklist() + usdcChecklist() + commissionChecklist() + returnChecklist()).forEach { line ->
            assertFalse(line.contains("bc1", ignoreCase = true))
            assertFalse(line.contains("lnbc", ignoreCase = true))
        }
    }

    @Test
    fun developmentConfirmsPoolMathProductionDoesNot() {
        assertTrue(needsUserConfirm(ConfirmMode.DEVELOPMENT, ActionKind.POOL_MATH))
        assertFalse(needsUserConfirm(ConfirmMode.PRODUCTION, ActionKind.POOL_MATH))
        assertTrue(needsUserConfirm(ConfirmMode.PRODUCTION, ActionKind.VENUE_WALLET))
        assertTrue(needsUserConfirm(ConfirmMode.PRODUCTION, ActionKind.HOP))
        assertFalse(needsUserConfirm(ConfirmMode.DEVELOPMENT, ActionKind.STOP))
    }

    @Test
    fun stopUtterance() {
        assertEquals(Utterance.Stop, parseUtterance("stop"))
        assertEquals(Utterance.Stop, parseUtterance("kill it"))
        assertTrue(parseUtterance("something is wrong") is Utterance.Stop)
    }
}
