package com.allourmedia.gamebridge

/**
 * Debug-build only, not exported. Instrumentation tests use it because the real activity finishes in onCreate and
 * so can never be recreated; this subclass ignores finish() so recreation can be exercised.
 */
class StayingBridgeActivity : BridgeActivity() {
    override fun finish() = Unit

    fun finishForReal() = super.finish()
}
