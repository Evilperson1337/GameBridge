package com.allourmedia.gamebridge

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import com.allourmedia.gamebridge.core.ShortcutResult
import com.allourmedia.gamebridge.core.interpretShortcut

/**
 * Receives a Shortcut from the Frontend, dispatches it to GameNative and finishes.
 * Ticket 02 covers the happy path only: every failure just finishes (ticket 03 adds the dialog).
 */
open class BridgeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recreation (rotation, process restore) must never dispatch a second time.
        if (savedInstanceState == null) {
            handle(intent)
        }
        finish()
    }

    private fun handle(intent: Intent) {
        val uri = intent.data
        if (intent.action != ACTION || uri == null || uri.scheme != "content") {
            debugLog("rejected intent, scheme=${uri?.scheme}")
            return
        }
        val file = ShortcutReader(contentResolver).read(uri)
        if (file == null) {
            debugLog("unreadable shortcut, scheme=${uri.scheme}")
            return
        }
        when (val result = interpretShortcut(file.displayName, file.bytes)) {
            is ShortcutResult.Rejected -> {
                debugLog("rejected: ${result.reason}, scheme=${uri.scheme}")
            }

            is ShortcutResult.Request -> {
                val outcome = GameNativeDispatcher(this).dispatch(result.request)
                debugLog(
                    "storefront=${result.request.storefront} storeId=${result.request.storeId} " +
                        "scheme=${uri.scheme} dispatch=$outcome",
                )
            }
        }
    }

    private fun debugLog(message: String) {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) Log.d(TAG, message)
    }

    companion object {
        const val ACTION = "com.allourmedia.gamebridge.action.DISPATCH"
        const val TAG = "GameBridge"
    }
}
