package com.allourmedia.gamebridge

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.allourmedia.gamebridge.core.MAX_LOCAL_SHORTCUT_BYTES
import com.allourmedia.gamebridge.core.readCapped

class ShortcutFile(
    val displayName: String,
    val bytes: ByteArray,
)

/** Which step of reading failed. Both are shown to the player as "couldn't be read". */
enum class ReadFailureKind {
    /** The display name could not be queried (provider error, missing grant, no row, no name). */
    Name,

    /** The name was read but the bytes could not be (provider error or no stream). */
    Contents,
}

sealed interface NameResult {
    data class Named(
        val displayName: String,
    ) : NameResult

    /** [errorClass] is the simple class name of the exception thrown, if any (never its message). */
    data class Unreadable(
        val errorClass: String?,
    ) : NameResult
}

sealed interface ReadResult {
    data class Read(
        val file: ShortcutFile,
    ) : ReadResult

    /**
     * [displayName] is set when the name could be read but the bytes could not. [errorClass] is the simple class
     * name of the exception thrown, if any (never its message, which could contain a URI or path).
     */
    data class Unreadable(
        val displayName: String?,
        val kind: ReadFailureKind,
        val errorClass: String? = null,
    ) : ReadResult
}

/** Reads a Shortcut through the content resolver. Never throws: every provider failure is [ReadResult.Unreadable]. */
class ShortcutReader(
    private val resolver: ContentResolver,
) {
    /** Queries the display name only; the Shortcut's contents are not opened. */
    fun readName(uri: Uri): NameResult =
        try {
            resolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
                ?.let { NameResult.Named(it) } ?: NameResult.Unreadable(null)
        } catch (e: Exception) {
            NameResult.Unreadable(e.javaClass.simpleName)
        }

    fun read(uri: Uri): ReadResult {
        var errorClass: String? = null
        val name =
            when (val named = readName(uri)) {
                is NameResult.Named -> named.displayName
                is NameResult.Unreadable -> return ReadResult.Unreadable(null, ReadFailureKind.Name, named.errorClass)
            }
        val bytes =
            try {
                resolver.openInputStream(uri)?.use { it.readCapped(MAX_BYTES + 1) }
            } catch (e: Exception) {
                errorClass = e.javaClass.simpleName
                null
            } ?: return ReadResult.Unreadable(name, ReadFailureKind.Contents, errorClass)
        return ReadResult.Read(ShortcutFile(name, bytes))
    }

    companion object {
        /** Local Shortcuts are capped at 4 KiB. One extra byte is read so oversize is detectable. */
        const val MAX_BYTES = MAX_LOCAL_SHORTCUT_BYTES
    }
}
