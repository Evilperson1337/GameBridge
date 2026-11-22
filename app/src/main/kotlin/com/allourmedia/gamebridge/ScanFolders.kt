package com.allourmedia.gamebridge

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * The two folders the player chose, kept as tree URIs with persisted grants. A folder whose grant is gone (revoked,
 * or the app's data cleared) reads as not chosen, so the screen asks again instead of failing later.
 */
class ScanFolders(
    private val context: Context,
) {
    private val prefs = context.getSharedPreferences("scan_folders", Context.MODE_PRIVATE)

    fun get(folder: ScanFolder): Uri? {
        val tree = prefs.getString(folder.name, null)?.let(Uri::parse) ?: return null
        val held =
            context.contentResolver.persistedUriPermissions.any {
                it.uri == tree && it.isReadPermission && (folder == ScanFolder.GameNative || it.isWritePermission)
            }
        return if (held) tree else null
    }

    fun set(
        folder: ScanFolder,
        tree: Uri,
    ) {
        val resolver = context.contentResolver
        prefs.getString(folder.name, null)?.let(Uri::parse)?.takeIf { it != tree && it != otherFolder(folder) }?.let { old ->
            runCatching { resolver.releasePersistableUriPermission(old, flags(folder)) }
        }
        resolver.takePersistableUriPermission(tree, flags(folder))
        prefs.edit().putString(folder.name, tree.toString()).apply()
    }

    private fun otherFolder(folder: ScanFolder): Uri? =
        prefs.getString(ScanFolder.entries.first { it != folder }.name, null)?.let(Uri::parse)

    /** Read only for the GameNative folder; the Output Folder is the only place GameBridge writes. */
    private fun flags(folder: ScanFolder) =
        when (folder) {
            ScanFolder.GameNative -> Intent.FLAG_GRANT_READ_URI_PERMISSION
            ScanFolder.Output -> Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
}
