package com.brianreborn.greenmintz

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mintz.broker.CoinbaseClient
import mintz.domain.ActionKind
import mintz.domain.defaultLiquidBook
import mintz.domain.needsUserConfirm
import mintz.domain.planBook

class WatchService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF, notice("Watching Coinbase *-USDC"))
        if (loop?.isActive != true) loop = scope.launch { runLoop() }
        return START_STICKY
    }

    private suspend fun runLoop() {
        while (scope.isActive) {
            val s = CoachStore.state.value
            if (s.stopped || !s.bookArmed) {
                stopSelf()
                break
            }
            try {
                tick()
            } catch (e: Exception) {
                CoachStore.bookError(e.message ?: e.javaClass.simpleName)
            }
            delay(20_000)
        }
    }

    private suspend fun tick() {
        val key = KeyVault.load() ?: run {
            CoachStore.bookError("No CDP key on this phone")
            CoachStore.disarmBook()
            return
        }
        val client = CoinbaseClient(key.name, key.privateKeyPem)
        val s = CoachStore.state.value
        if (s.stopped) {
            val n = runCatching { client.cancelAllOpen() }.getOrDefault(0)
            CoachStore.bookNote("Stopped. Cancelled $n open orders.")
            CoachStore.disarmBook()
            stopSelf()
            return
        }
        val balances = client.listBalances()
        CoachStore.setBalances(balances)
        val opens = client.listOpenOrderIds()
        val marks = client.bestBidAsk(defaultLiquidBook().map { it.productId })
        val plan = planBook(
            balances = balances,
            marks = marks,
            split = s.split,
            convertBtc = s.convertArmed,
            stopped = s.stopped,
            bookArmed = s.bookArmed,
            hasOpenOrders = opens.isNotEmpty(),
        )
        CoachStore.setPlanNotes(plan)
        if (plan.intents.isEmpty()) return
        for (intent in plan.intents) {
            if (CoachStore.state.value.stopped || !CoachStore.state.value.bookArmed) return
            val body = buildString {
                append(intent.side.name).append(' ').append(intent.productId).append(" · ")
                append(intent.reason)
                append(". Transfer is off. You can watch this on coinbase.com.")
            }
            val pending = Pending(
                kind = ActionKind.BOOK_ORDER,
                title = "${intent.side.name} ${intent.productId}",
                body = body,
                apply = {
                    val id = client.createOrder(intent)
                    CoachStore.bookNote("Sent ${intent.side} ${intent.productId} · $id")
                    if (intent.action.name == "CONVERT_BTC") CoachStore.clearConvert()
                },
            )
            if (needsUserConfirm(CoachStore.state.value.confirmMode, ActionKind.BOOK_ORDER)) {
                if (!CoachStore.requestAndAwait(pending)) return
            } else {
                pending.apply()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notice(text: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH, "green-mintz book", NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CH)
            .setContentTitle("green-mintz")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CH = "mintz_book"
        private const val NOTIF = 17
        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, WatchService::class.java))
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, WatchService::class.java))
        }
    }
}
