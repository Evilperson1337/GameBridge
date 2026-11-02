package com.allourmedia.gamebridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.allourmedia.gamebridge.core.Rejection
import com.allourmedia.gamebridge.core.ShortcutResult
import com.allourmedia.gamebridge.core.interpretShortcut

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
            outState.putString(KEY_STOREFRONT, it.storefront)
        }
    }

    private fun restoreFailure(state: Bundle): BridgeFailure? {
        val category = FailureCategory.entries.firstOrNull { it.name == state.getString(KEY_CATEGORY) }
        return category?.let { BridgeFailure(it, state.getString(KEY_FILE_NAME), state.getString(KEY_STOREFRONT)) }
    }

    /** The context used to start the Target App. Debug-only subclasses override it to simulate refusals. */
    protected open fun dispatchContext(): Context = this

    private fun handle(intent: Intent): BridgeFailure? {
        val uri = intent.data
        val action = intent.action
        // Started without data (for example from the app drawer, which sends MAIN).
        if (uri == null && (action == null || action == Intent.ACTION_MAIN || action == ACTION)) {
            return fail(FailureCategory.NoShortcut)
        }
        if (action != ACTION || uri == null) return fail(FailureCategory.WrongAction)
        if (uri.scheme != "content") return fail(FailureCategory.NotContentUri)
        val file =
            when (val read = ShortcutReader(contentResolver).read(uri)) {
                is ReadResult.Unreadable -> return fail(FailureCategory.Unreadable, read.displayName)
                is ReadResult.Read -> read.file
            }
        when (val result = interpretShortcut(file.displayName, file.bytes)) {
            is ShortcutResult.Rejected -> {
                return rejection(result.reason, file.displayName)
            }

            is ShortcutResult.Request -> {
                val outcome = GameNativeDispatcher(dispatchContext()).dispatch(result.request)
                debugLog(
                    "storefront=${result.request.storefront} storeId=${result.request.storeId} " +
                        "scheme=${uri.scheme} dispatch=$outcome",
                )
                return when (outcome) {
                    DispatchResult.Dispatched -> {
                        null
                    }

                    DispatchResult.GameNativeNotInstalled -> {
                        fail(FailureCategory.GameNativeNotInstalled, file.displayName)
                    }

                    DispatchResult.GameNativeRefused -> {
                        fail(FailureCategory.GameNativeRefused, file.displayName)
                    }
                }
            }
        }
    }

    private fun rejection(
        reason: Rejection,
        fileName: String,
    ): BridgeFailure =
        when (reason) {
            Rejection.UnsupportedType -> fail(FailureCategory.UnsupportedType, fileName)
            is Rejection.StorefrontNotVerified -> fail(FailureCategory.StorefrontNotVerified, fileName, reason.storefront.name)
            Rejection.RemoteUnsupported -> fail(FailureCategory.RemoteUnsupported, fileName)
            Rejection.InvalidContents -> fail(FailureCategory.InvalidContents, fileName)
            Rejection.TooLarge -> fail(FailureCategory.TooLarge, fileName)
        }

    private fun fail(
        category: FailureCategory,
        fileName: String? = null,
        storefront: String? = null,
    ): BridgeFailure {
        // Release builds log the category only: never contents, URIs or paths.
        Log.w(TAG, "failure=$category")
        return BridgeFailure(category, fileName, storefront)
    }

    private fun showDialog(failure: BridgeFailure) {
        shown = failure
        val message =
            if (failure.fileName == null) {
                failure.reason
            } else {
                "${failure.fileName.take(MAX_NAME)}\n\n${failure.reason}"
            }
        val dialog =
            AlertDialog
                .Builder(this)
                .setTitle("GameBridge")
                .setMessage(message)
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
        const val ACTION = "com.allourmedia.gamebridge.action.DISPATCH"
        const val TAG = "GameBridge"
        private const val KEY_CATEGORY = "failure.category"
        private const val KEY_FILE_NAME = "failure.fileName"
        private const val KEY_STOREFRONT = "failure.storefront"
        private const val MAX_NAME = 80
        private val CONFIRM_KEYS =
            setOf(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
            )
    }
}
