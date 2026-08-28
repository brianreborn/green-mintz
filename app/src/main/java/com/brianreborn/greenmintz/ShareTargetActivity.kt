package com.brianreborn.greenmintz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Share / PROCESS_TEXT land here, then the main coach opens on Art.
 * No Accessibility. You Confirm hops. Coach never presses Cash App Confirm.
 */
class ShareTargetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoachStore.ingestIntent(this, intent)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
        finish()
    }
}
