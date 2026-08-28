package mintz.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BookTest {
    private val marks = listOf(
        PriceMark("BTC-USDC", 100_000.0),
        PriceMark("ETH-USDC", 4_000.0),
        PriceMark("SOL-USDC", 200.0),
    )

    @Test
    fun liquidBookWeightsSumTo100() {
        assertEquals(100, defaultLiquidBook().sumOf { it.weight })
    }

    @Test
    fun nftSleeveStaysUsdcAndBtcGetsHalfOfLiquid() {
        val plan = planBook(
            balances = listOf(Balance("USDC", 1000.0)),
            marks = marks,
            split = PoolSplit(50, 50),
        )
        val btc = plan.targets.first { it.currency == "BTC" }
        val usdc = plan.targets.first { it.currency == "USDC" }
        assertEquals(250.0, btc.targetUsdc, 0.01)
        assertEquals(500.0, usdc.targetUsdc, 0.01)
        assertTrue(plan.intents.any { it.side == Side.BUY && it.productId == "BTC-USDC" })
    }

    @Test
    fun convertBtcSellsBaseAndSkipsRebalanceSameTick() {
        val plan = planBook(
            balances = listOf(Balance("BTC", 0.01), Balance("USDC", 0.0)),
            marks = marks,
            split = PoolSplit(50, 50),
            convertBtc = true,
        )
        assertEquals(1, plan.intents.size)
        val i = plan.intents.first()
        assertEquals(Side.SELL, i.side)
        assertEquals("BTC-USDC", i.productId)
        assertEquals(0.01, i.baseSize)
        assertEquals(BookAction.CONVERT_BTC, i.action)
    }

    @Test
    fun overweightBtcSellsIntoBand() {
        val plan = planBook(
            balances = listOf(Balance("BTC", 1.0), Balance("USDC", 0.0)),
            marks = marks,
            split = PoolSplit(50, 50),
            bandPct = 10,
        )
        assertTrue(plan.totalUsdc > 0)
        assertTrue(plan.intents.any { it.side == Side.SELL && it.productId == "BTC-USDC" })
        assertTrue(plan.intents.any { it.side == Side.BUY && it.productId == "ETH-USDC" })
    }

    @Test
    fun stopAndOpenOrdersEmitNothing() {
        val bal = listOf(Balance("USDC", 1000.0))
        val stopped = planBook(bal, marks, defaultSplit(), stopped = true)
        assertTrue(stopped.intents.isEmpty())
        val waiting = planBook(bal, marks, defaultSplit(), hasOpenOrders = true)
        assertTrue(waiting.intents.isEmpty())
        val disarmed = planBook(bal, marks, defaultSplit(), bookArmed = false)
        assertTrue(disarmed.intents.isEmpty())
    }

    @Test
    fun refuseTransferAndWithdrawPaths() {
        assertTrue(isForbiddenBrokerPath("/api/v3/brokerage/orders/do-withdraw"))
        assertTrue(isForbiddenBrokerPath("/transfers"))
        assertTrue(isForbiddenBrokerPath("/api/v3/brokerage/accounts/abc/addresses"))
        assertTrue(isForbiddenBrokerPath("/api/v3/brokerage/convert/trade"))
        assertFalse(isForbiddenBrokerPath("/api/v3/brokerage/orders"))
        assertFalse(isForbiddenBrokerPath("/api/v3/brokerage/accounts"))
        assertFalse(isForbiddenBrokerPath("/api/v3/brokerage/best_bid_ask"))
        assertTrue(isForbiddenBrokerPath("/api/v3/brokerage/convert"))
        assertTrue(isForbiddenBrokerPath("https://evil.example/api/v3/brokerage/orders"))
        assertTrue(isAllowedBrokerPath("/api/v3/brokerage/orders"))
        assertTrue(isAllowedBrokerPath("/api/v3/brokerage/orders/batch_cancel"))
        assertFalse(isAllowedBrokerPath("/api/v3/brokerage/orders/do-withdraw"))
        assertFalse(isAllowedBrokerPath("/api/v3/brokerage/convert"))
        assertTrue(isAllowedProduct("BTC-USDC"))
        assertFalse(isAllowedProduct("BTC-USD"))
        assertFalse(isAllowedProduct("BTC-USDT"))
    }

    @Test
    fun parseCdpJsonPaste() {
        val raw = """
            {"name":"organizations/org/apiKeys/key-1","privateKey":"-----BEGIN EC PRIVATE KEY-----\\nMHcCAQEEFAKE\\n-----END EC PRIVATE KEY-----\\n"}
        """.trimIndent()
        val k = parseCdpPaste(raw)
        assertNotNull(k)
        assertEquals("organizations/org/apiKeys/key-1", k.name)
        assertTrue(k.privateKeyPem.contains("BEGIN EC PRIVATE KEY"))
        assertTrue(k.privateKeyPem.contains("\n"))
        assertFalse(k.privateKeyPem.contains("\\n"))
    }

    @Test
    fun productionAutoBookOrderDevelopmentConfirms() {
        assertTrue(needsUserConfirm(ConfirmMode.DEVELOPMENT, ActionKind.BOOK_ORDER))
        assertFalse(needsUserConfirm(ConfirmMode.PRODUCTION, ActionKind.BOOK_ORDER))
        assertTrue(needsUserConfirm(ConfirmMode.PRODUCTION, ActionKind.HOP))
    }

    @Test
    fun stopDisarmsBook() {
        val halted = stop(CoachState(bookArmed = true, mineArmed = true))
        assertTrue(halted.stopped)
        assertFalse(halted.bookArmed)
        assertFalse(halted.mineArmed)
    }

    @Test
    fun armUtterances() {
        assertEquals(Utterance.ArmBook, parseUtterance("arm book"))
        assertEquals(Utterance.DisarmBook, parseUtterance("disarm"))
        assertEquals(Utterance.ConvertBtc, parseUtterance("convert btc"))
    }
}
