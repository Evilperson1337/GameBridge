package com.allourmedia.gamebridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.allourmedia.gamebridge.core.BridgeFailure
import com.allourmedia.gamebridge.core.DISPATCH_ACTION
import com.allourmedia.gamebridge.core.DispatchRequest
import com.allourmedia.gamebridge.core.FailureCategory
import com.allourmedia.gamebridge.core.IntentTriage
import com.allourmedia.gamebridge.core.ShortcutResult
import com.allourmedia.gamebridge.core.Storefront
import com.allourmedia.gamebridge.core.interpretShortcut
import com.allourmedia.gamebridge.core.isRemoteShortcut
import com.allourmedia.gamebridge.core.toFailure
import com.allourmedia.gamebridge.core.triageIntent

/**
 * Receives a Shortcut from the Frontend, dispatches it to GameNative and finishes. On failure it shows an error
 * dialog over the transparent activity; Close and Back both finish.
 */
open class BridgeActivity : Activity() {
    private var shown: BridgeFailure? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recreation (rotation, process restore) must never dispatch a second time.
        val failure =
            if (savedInstanceState == null) {
                handle(intent)
            } else {
                restoreFailure(savedInstanceState)
            }
        if (failure == null) finish() else showDialog(failure)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        shown?.let {
            outState.putString(KEY_CATEGORY, it.category.name)
            outState.putString(KEY_FILE_NAME, it.fileName)
            outState.putString(KEY_STOREFRONT, it.storefront?.name)
        }
    }

    private fun restoreFailure(state: Bundle): BridgeFailure? {
        val category = FailureCategory.entries.firstOrNull { it.name == state.getString(KEY_CATEGORY) }
        val storefront = Storefront.all.firstOrNull { it.name == state.getString(KEY_STOREFRONT) }
        return category?.let { BridgeFailure(it, state.getString(KEY_FILE_NAME), storefront) }
    }

    /** The context used to start the Target App. Debug-only subclasses override it to simulate refusals. */
    protected open fun dispatchContext(): Context = this

    private fun handle(intent: Intent): BridgeFailure? {
        val uri = intent.data
        when (val triage = triageIntent(intent.action, uri?.scheme, hasData = uri != null)) {
            is IntentTriage.Failed -> return fail(BridgeFailure(triage.category))
            IntentTriage.Accepted -> Unit
        }
        val name =
            when (val named = ShortcutReader(contentResolver).readName(checkNotNull(uri))) {
                is NameResult.Unreadable -> {
                    debugLog("unreadable kind=${ReadFailureKind.Name} error=${named.errorClass}")
                    return fail(BridgeFailure(FailureCategory.Unreadable))
                }

                is NameResult.Named -> {
                    named.displayName
                }
            }
        // A Remote Shortcut is handed to Artemis unread: Artemis owns the .art format.
        if (isRemoteShortcut(name)) return dispatchRemote(uri, name)
        val file =
            when (val read = ShortcutReader(contentResolver).read(checkNotNull(uri))) {
                is ReadResult.Unreadable -> {
                    debugLog("unreadable kind=${read.kind} error=${read.errorClass}")
                    return fail(BridgeFailure(FailureCategory.Unreadable, read.displayName))
                }

                is ReadResult.Read -> {
                    read.file
                }
            }
        return when (val result = interpretShortcut(file.displayName, file.bytes)) {
            is ShortcutResult.Rejected -> {
                fail(result.reason.toFailure(file.displayName))
            }

            is ShortcutResult.Request -> {
                dispatch(result.request, file.displayName, uri?.scheme)
            }
        }
    }

    private fun dispatchRemote(
        uri: Uri,
        fileName: String,
    ): BridgeFailure? {
        val outcome = ArtemisDispatcher(dispatchContext()).dispatch(uri)
        debugLog("remote scheme=${uri.scheme} dispatch=$outcome")
        val category =
            when (outcome) {
                DispatchResult.Dispatched -> return null
                DispatchResult.ArtemisNotInstalled -> FailureCategory.ArtemisNotInstalled
                DispatchResult.ArtemisRefused -> FailureCategory.ArtemisRefused
                else -> error("unexpected Artemis outcome $outcome")
            }
        return fail(BridgeFailure(category, fileName))
    }

    private fun dispatch(
        request: DispatchRequest,
        fileName: String,
        scheme: String?,
    ): BridgeFailure? {
        val outcome = GameNativeDispatcher(dispatchContext()).dispatch(request)
        debugLog("storefront=${request.storefront} storeId=${request.storeId} scheme=$scheme dispatch=$outcome")
        val category =
            when (outcome) {
                DispatchResult.Dispatched -> return null
                DispatchResult.GameNativeNotInstalled -> FailureCategory.GameNativeNotInstalled
                DispatchResult.GameNativeRefused -> FailureCategory.GameNativeRefused
                else -> error("unexpected GameNative outcome $outcome")
            }
        return fail(BridgeFailure(category, fileName))
    }

    private fun fail(failure: BridgeFailure): BridgeFailure {
        // Release builds log the category only: never contents, URIs or paths.
        Log.w(TAG, "failure=${failure.category}")
        return failure
    }

    private fun showDialog(failure: BridgeFailure) {
        shown = failure
        val dialog =
            AlertDialog
                .Builder(this)
                .setTitle("GameBridge")
                .setMessage(failure.message)
                .setPositiveButton("Close", null)
                .setOnDismissListener { finish() }
                .create()
        // Close is the only action, so any confirm key (A, D-pad centre, Enter) closes it even in touch mode,
        // where the first key press would otherwise only move focus. Back (B) is handled by the dialog itself.
        dialog.setOnKeyListener { _, keyCode, event ->
            val confirm = keyCode in CONFIRM_KEYS
            if (confirm && event.action == KeyEvent.ACTION_UP) dialog.dismiss()
            confirm
        }
        dialog.show()
    }

    private fun debugLog(message: String) {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) Log.d(TAG, message)
    }

    companion object {
        const val ACTION = DISPATCH_ACTION
        const val TAG = "GameBridge"
        private const val KEY_CATEGORY = "failure.category"
        private const val KEY_FILE_NAME = "failure.fileName"
        private const val KEY_STOREFRONT = "failure.storefront"
        private val CONFIRM_KEYS =
            setOf(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
            )
    }
}
