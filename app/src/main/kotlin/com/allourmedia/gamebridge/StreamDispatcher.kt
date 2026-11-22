package com.allourmedia.gamebridge

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.allourmedia.gamebridge.core.RemoteRequest

sealed interface StreamDispatchResult {
    /** startActivity returned normally. This is NOT proof that the stream started. */
    data object Dispatched : StreamDispatchResult

    data object NotInstalled : StreamDispatchResult

    data object Refused : StreamDispatchResult
}

/**
 * Dispatches a streamed game from a Bridge Shortcut to the client it names (Artemis or Ariane): an explicit VIEW to the
 * client's `ShortcutTrampoline` with String extras and no data, the template Artemis uses for its own pinned shortcuts
 * (ticket 04). No file and no grant are involved. Never falls back to the other client.
 */
class StreamDispatcher(
    private val context: Context,
) {
    fun dispatch(request: RemoteRequest): StreamDispatchResult {
        val intent =
            Intent(Intent.ACTION_VIEW)
                .setComponent(ComponentName(request.client.packageName, TRAMPOLINE_CLASS))
                .putExtra(EXTRA_HOST_UUID, request.hostUuid)
                .putExtra(EXTRA_HOST_NAME, request.hostName)
                .putExtra(EXTRA_APP_UUID, request.appUuid)
                .putExtra(EXTRA_APP_NAME, request.appName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            StreamDispatchResult.Dispatched
        } catch (_: ActivityNotFoundException) {
            StreamDispatchResult.NotInstalled
        } catch (_: SecurityException) {
            StreamDispatchResult.Refused
        }
    }

    companion object {
        /** The same class name in Artemis and its fork Ariane. */
        const val TRAMPOLINE_CLASS = ArtemisDispatcher.TRAMPOLINE_CLASS
        const val EXTRA_HOST_UUID = "UUID"
        const val EXTRA_HOST_NAME = "Name"
        const val EXTRA_APP_UUID = "AppUUID"
        const val EXTRA_APP_NAME = "AppName"
    }
}
