package com.allourmedia.gamebridge

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream

class ShortcutFile(
    val displayName: String,
    val bytes: ByteArray,
)

sealed interface ReadResult {
    data class Read(
        val file: ShortcutFile,
    ) : ReadResult

    /** [displayName] is set when the name could be read but the bytes could not. */
    data class Unreadable(
        val displayName: String?,
    ) : ReadResult
}

/** Reads a Shortcut through the content resolver. Never throws: every provider failure is [ReadResult.Unreadable]. */
class ShortcutReader(
    private val resolver: ContentResolver,
) {
    fun read(uri: Uri): ReadResult {
        val name =
            try {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            } catch (_: Exception) {
                null
            } ?: return ReadResult.Unreadable(null)
        val bytes =
            try {
                resolver.openInputStream(uri)?.use { it.readCapped(MAX_BYTES + 1) }
            } catch (_: Exception) {
                null
            } ?: return ReadResult.Unreadable(name)
        return ReadResult.Read(ShortcutFile(name, bytes))
    }

    companion object {
        /** Local Shortcuts are capped at 4 KiB. One extra byte is read so oversize is detectable. */
        const val MAX_BYTES = 4096
    }
}

/** Reads at most [limit] bytes (InputStream.readNBytes needs API 33; minSdk is 30). */
private fun InputStream.readCapped(limit: Int): ByteArray {
    val buffer = ByteArray(limit)
    var total = 0
    while (total < limit) {
        val n = read(buffer, total, limit - total)
        if (n < 0) break
        total += n
    }
    return buffer.copyOf(total)
}
