package com.allourmedia.gamebridge

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri

sealed interface ArtemisDispatchResult {
    /** startActivity returned normally. This is NOT proof that the stream started. */
    data object Dispatched : ArtemisDispatchResult

    data object NotInstalled : ArtemisDispatchResult

    data object Refused : ArtemisDispatchResult
}

/**
 * Dispatches a Remote Shortcut (`.art`) to Artemis. The file is not read or parsed: Artemis owns the format.
 * No fallback to GameNative or a deep link.
 *
 * The package and component were observed on Artemis 20.2.6 only (docs/investigation-log.md); no other Artemis
 * variant is known.
 */
class ArtemisDispatcher(
    private val context: Context,
) {
    fun dispatch(shortcut: Uri): ArtemisDispatchResult {
        val intent =
            Intent(Intent.ACTION_VIEW)
                .setComponent(ComponentName(PACKAGE, TRAMPOLINE_CLASS))
                .setData(shortcut)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            ArtemisDispatchResult.Dispatched
        } catch (_: ActivityNotFoundException) {
            ArtemisDispatchResult.NotInstalled
        } catch (_: SecurityException) {
            ArtemisDispatchResult.Refused
        }
    }

    companion object {
        const val PACKAGE = "com.limelight.noir"
        const val TRAMPOLINE_CLASS = "com.limelight.ShortcutTrampoline"
    }
}
