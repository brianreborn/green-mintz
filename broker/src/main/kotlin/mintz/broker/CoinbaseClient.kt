package mintz.broker

import mintz.domain.Balance
import mintz.domain.OrderIntent
import mintz.domain.PriceMark
import mintz.domain.Side
import mintz.domain.isForbiddenBrokerPath
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

fun interface RawHttp {
    fun execute(method: String, path: String, jsonBody: String?): String
}

class TransferForbiddenException(path: String) : IllegalArgumentException("Transfer/withdraw path blocked: $path")

class CoinbaseClient(
    private val keyName: String,
    private val pem: String,
    private val http: RawHttp = liveHttp(keyName, pem),
) {
    fun listBalances(): List<Balance> {
        val out = mutableListOf<Balance>()
        var cursor: String? = null
        repeat(8) {
            val q = buildString {
                append("/api/v3/brokerage/accounts?limit=250")
                if (!cursor.isNullOrBlank()) append("&cursor=").append(cursor)
            }
            val root = JSONObject(http.execute("GET", q, null))
            val accounts = root.optJSONArray("accounts") ?: JSONArray()
            for (i in 0 until accounts.length()) {
                val a = accounts.getJSONObject(i)
                val ccy = a.optString("currency")
                val amt = a.optJSONObject("available_balance")?.optString("value")?.toDoubleOrNull() ?: 0.0
                if (ccy.isNotBlank() && amt > 0.0) out += Balance(ccy, amt)
            }
            if (!root.optBoolean("has_next")) return out
            cursor = root.optString("cursor").ifBlank { return out }
        }
        return out
    }

    fun bestBidAsk(productIds: List<String>): List<PriceMark> {
        if (productIds.isEmpty()) return emptyList()
        val q = productIds.joinToString("&") { "product_ids=$it" }
        val root = JSONObject(http.execute("GET", "/api/v3/brokerage/best_bid_ask?$q", null))
        val books = root.optJSONArray("pricebooks") ?: JSONArray()
        val marks = mutableListOf<PriceMark>()
        for (i in 0 until books.length()) {
            val b = books.getJSONObject(i)
            val pid = b.optString("product_id")
            val bid = b.optJSONArray("bids")?.optJSONObject(0)?.optString("price")?.toDoubleOrNull()
            val ask = b.optJSONArray("asks")?.optJSONObject(0)?.optString("price")?.toDoubleOrNull()
            val mid = when {
                bid != null && ask != null -> (bid + ask) / 2.0
                bid != null -> bid
                ask != null -> ask
                else -> continue
            }
            if (pid.isNotBlank() && mid > 0) marks += PriceMark(pid, mid)
        }
        return marks
    }

    fun listOpenOrderIds(): List<String> {
        val root = JSONObject(
            http.execute("GET", "/api/v3/brokerage/orders/historical/batch?order_status=OPEN&limit=50", null),
        )
        val orders = root.optJSONArray("orders") ?: JSONArray()
        val ids = mutableListOf<String>()
        for (i in 0 until orders.length()) {
            val id = orders.getJSONObject(i).optString("order_id")
            if (id.isNotBlank()) ids += id
        }
        return ids
    }

    fun createOrder(intent: OrderIntent): String {
        val ioc = JSONObject()
        intent.quoteSize?.let { ioc.put("quote_size", trimQty(it)) }
        intent.baseSize?.let { ioc.put("base_size", trimQty(it)) }
        val body = JSONObject()
            .put("client_order_id", UUID.randomUUID().toString())
            .put("product_id", intent.productId)
            .put("side", if (intent.side == Side.BUY) "BUY" else "SELL")
            .put("order_configuration", JSONObject().put("market_market_ioc", ioc))
        val root = JSONObject(http.execute("POST", "/api/v3/brokerage/orders", body.toString()))
        val orderId = root.optJSONObject("success_response")?.optString("order_id")
            ?: root.optJSONObject("order")?.optString("order_id")
            ?: ""
        val success = root.optBoolean("success", orderId.isNotBlank())
        if (!success && orderId.isBlank()) {
            val msg = root.optJSONObject("error_response")?.optString("message")
                ?: root.optString("message").ifBlank { root.toString() }
            error(msg)
        }
        return orderId.ifBlank { "ok" }
    }

    fun cancelOrders(ids: List<String>): Int {
        if (ids.isEmpty()) return 0
        val body = JSONObject().put("order_ids", JSONArray(ids))
        val root = JSONObject(http.execute("POST", "/api/v3/brokerage/orders/batch_cancel", body.toString()))
        val results = root.optJSONArray("results") ?: return 0
        var n = 0
        for (i in 0 until results.length()) {
            if (results.getJSONObject(i).optBoolean("success")) n++
        }
        return n
    }

    fun cancelAllOpen(): Int = cancelOrders(listOpenOrderIds())
}

private fun liveHttp(keyName: String, pem: String): RawHttp =
    OkHttpRawHttp(jwtFor = { method, path -> restJwt(keyName, pem, method, path) })

class OkHttpRawHttp(
    private val jwtFor: (method: String, path: String) -> String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) : RawHttp {
    override fun execute(method: String, path: String, jsonBody: String?): String {
        val pathOnly = path.substringBefore("?")
        if (isForbiddenBrokerPath(path) || isForbiddenBrokerPath(pathOnly)) {
            throw TransferForbiddenException(path)
        }
        val jwt = jwtFor(method, pathOnly)
        val url = "https://api.coinbase.com$path"
        val b = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $jwt")
            .header("Accept", "application/json")
            .header("User-Agent", "green-mintz/1.1")
        if (method == "GET") b.get()
        else {
            val body = (jsonBody ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType())
            b.method(method, body)
        }
        client.newCall(b.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Coinbase ${resp.code}: ${text.take(400)}")
            return text
        }
    }
}

private fun trimQty(n: Double): String =
    if (n >= 1) "%.2f".format(n) else "%.8f".format(n).trimEnd('0').trimEnd('.')
