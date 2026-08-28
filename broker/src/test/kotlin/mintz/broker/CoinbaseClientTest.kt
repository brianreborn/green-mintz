package mintz.broker

import mintz.domain.BookAction
import mintz.domain.OrderIntent
import mintz.domain.Side
import mintz.domain.isForbiddenBrokerPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CoinbaseClientTest {
    @Test
    fun withdrawPathNeverLeavesTheClient() {
        val http = RawHttp { _, path, _ -> error("should not call $path") }
        val c = CoinbaseClient("n", "pem", http)
        assertFailsWith<TransferForbiddenException> {
            OkHttpRawHttp({ _, _ -> "jwt" }).execute("POST", "/api/v3/brokerage/withdrawals", "{}")
        }
        assertTrue(isForbiddenBrokerPath("/api/v3/brokerage/accounts/x/addresses"))
        // client methods only hit trade/view paths
        val recorded = mutableListOf<String>()
        val fake = RawHttp { method, path, _ ->
            recorded += "$method $path"
            when {
                path.startsWith("/api/v3/brokerage/accounts") ->
                    """{"accounts":[{"currency":"BTC","available_balance":{"value":"0.01","currency":"BTC"}}],"has_next":false}"""
                path.startsWith("/api/v3/brokerage/best_bid_ask") ->
                    """{"pricebooks":[{"product_id":"BTC-USDC","bids":[{"price":"100000"}],"asks":[{"price":"100002"}]}]}"""
                path.startsWith("/api/v3/brokerage/orders/historical") ->
                    """{"orders":[{"order_id":"abc"}]}"""
                path == "/api/v3/brokerage/orders" ->
                    """{"success":true,"success_response":{"order_id":"oid-1"}}"""
                path.endsWith("batch_cancel") ->
                    """{"results":[{"success":true,"order_id":"abc"}]}"""
                else -> error(path)
            }
        }
        val live = CoinbaseClient("n", "pem", fake)
        assertEquals("BTC", live.listBalances().first().currency)
        assertEquals(100001.0, live.bestBidAsk(listOf("BTC-USDC")).first().mid)
        assertEquals(listOf("abc"), live.listOpenOrderIds())
        val id = live.createOrder(
            OrderIntent("BTC-USDC", Side.SELL, baseSize = 0.01, reason = "t", action = BookAction.CONVERT_BTC),
        )
        assertEquals("oid-1", id)
        assertEquals(1, live.cancelOrders(listOf("abc")))
        assertTrue(recorded.none { isForbiddenBrokerPath(it.substringAfter(" ")) })
        assertTrue(recorded.any { it.startsWith("POST /api/v3/brokerage/orders") })
    }
}
