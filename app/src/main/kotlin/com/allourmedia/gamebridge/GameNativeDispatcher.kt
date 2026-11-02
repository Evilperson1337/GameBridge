package com.allourmedia.gamebridge

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.allourmedia.gamebridge.core.DispatchRequest

sealed interface DispatchResult {
    /** startActivity returned normally. This is NOT proof that the game started. */
    data object Dispatched : DispatchResult

    data object GameNativeNotInstalled : DispatchResult

    data object GameNativeRefused : DispatchResult
}

/** Dispatches a validated request to GameNative. No fallback to a deep link or another app. */
class GameNativeDispatcher(
    private val context: Context,
) {
    fun dispatch(request: DispatchRequest): DispatchResult {
        // Only the action, component and two extras. The incoming URI and its grant are never forwarded.
        val intent =
            Intent(ACTION)
                .setComponent(ComponentName(PACKAGE, "$PACKAGE.MainActivity"))
                .putExtra("app_id", request.storeId)
                .putExtra("game_source", request.storefront.gameSource)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            DispatchResult.Dispatched
        } catch (_: ActivityNotFoundException) {
            DispatchResult.GameNativeNotInstalled
        } catch (_: SecurityException) {
            DispatchResult.GameNativeRefused
        }
    }

    companion object {
        const val ACTION = "app.gamenative.LAUNCH_GAME"
        const val PACKAGE = "app.gamenative"
    }
}
