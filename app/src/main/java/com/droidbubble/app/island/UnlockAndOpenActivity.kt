package com.droidbubble.app.island

import android.app.Activity
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.IntentCompat

/**
 * Brings up the lock screen's unlock prompt (fingerprint, face, PIN...) before opening what was tapped on the island.
 * Only an activity can ask for that, so the overlay starts this invisible one and it opens the target once unlocked.
 */
class UnlockAndOpenActivity : Activity() {

    private var requested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
    }

    // Asking before this window covers the lock screen gets the prompt cancelled by that transition
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || requested) return
        requested = true
        getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                open()
                finish()
            }

            override fun onDismissCancelled() = finish()

            override fun onDismissError() = finish()
        })
    }

    private fun open() {
        val pendingIntent = IntentCompat.getParcelableExtra(intent, EXTRA_INTENT, PendingIntent::class.java)
        val sent = pendingIntent != null && runCatching { sendFromIsland(this, pendingIntent) }.isSuccess
        if (!sent) {
            intent.getStringExtra(EXTRA_PACKAGE)?.let { packageManager.getLaunchIntentForPackage(it) }?.let {
                startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    companion object {
        private const val EXTRA_INTENT = "intent"
        private const val EXTRA_PACKAGE = "package"

        /** Opens after unlocking when the device is locked; false means it isn't, so the caller opens right away. */
        fun openAfterUnlock(context: Context, packageName: String?, intent: PendingIntent?): Boolean {
            if (context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked != true) return false
            context.startActivity(
                Intent(context, UnlockAndOpenActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_INTENT, intent)
                    .putExtra(EXTRA_PACKAGE, packageName)
            )
            return true
        }
    }
}
