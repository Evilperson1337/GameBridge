package com.allourmedia.gamebridge

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

/**
 * Debug-build only. Behaves like [BridgeActivity] except that starting the Target App throws a SecurityException,
 * as a real GameNative that refuses the request would. Simulated: no real GameNative is involved.
 */
class RefusingBridgeActivity : BridgeActivity() {
    override fun dispatchContext(): Context =
        object : ContextWrapper(this) {
            override fun startActivity(intent: Intent) = throw SecurityException("refused (simulated)")
        }
}
