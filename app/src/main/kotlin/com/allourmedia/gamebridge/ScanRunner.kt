package com.allourmedia.gamebridge

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import com.allourmedia.gamebridge.core.BRIDGE_SHORTCUT_EXTENSION
import com.allourmedia.gamebridge.core.MAX_LOCAL_SHORTCUT_BYTES
import com.allourmedia.gamebridge.core.PlannedFile
import com.allourmedia.gamebridge.core.ScanPlan
import com.allourmedia.gamebridge.core.SourceShortcut
import com.allourmedia.gamebridge.core.planScan
import com.allourmedia.gamebridge.core.readCapped
import com.allourmedia.gamebridge.core.shortcutExtension

/** The folders a Scan uses. */
enum class ScanFolder { GameNative, Output }

sealed interface ScanRead {
    data class Planned(
        val plan: ScanPlan,
    ) : ScanRead

    /** The folder could not be listed or one of its files could not be read: usually a missing or revoked grant. */
    data class Unreadable(
        val folder: ScanFolder,
    ) : ScanRead
}

/**
 * Runs a Scan through the Storage Access Framework (ADR 0003). Reads the GameNative Shortcut Folder and the Output
 * Folder; writes only into the Output Folder, by creating new documents or, when the player confirmed, replacing
 * existing ones. Never throws: failures become [ScanRead.Unreadable] or a failed file name.
 */
class ScanRunner(
    private val resolver: ContentResolver,
) {
    private class Child(
        val documentId: String,
        val name: String,
    )

    fun plan(
        gameNativeTree: Uri,
        outputTree: Uri,
    ): ScanRead {
        val sources =
            readFiles(gameNativeTree, files(gameNativeTree) ?: return ScanRead.Unreadable(ScanFolder.GameNative))
                ?: return ScanRead.Unreadable(ScanFolder.GameNative)
        val outputChildren =
            files(outputTree)?.filter { shortcutExtension(it.name) == BRIDGE_SHORTCUT_EXTENSION }
                ?: return ScanRead.Unreadable(ScanFolder.Output)
        val existing = readFiles(outputTree, outputChildren) ?: return ScanRead.Unreadable(ScanFolder.Output)
        return ScanRead.Planned(planScan(sources, existing.associate { it.displayName to it.bytes }))
    }

    /**
     * Writes [files] into the Output Folder and returns the names that could not be written. A name that already
     * exists is replaced only when [replace] is true; otherwise it is left alone and reported as failed.
     */
    fun write(
        outputTree: Uri,
        files: List<PlannedFile>,
        replace: Boolean,
    ): List<String> {
        val existing = files(outputTree)?.associateBy { it.name.lowercase() } ?: return files.map { it.name }
        val root = DocumentsContract.buildDocumentUriUsingTree(outputTree, DocumentsContract.getTreeDocumentId(outputTree))
        return files
            .filterNot { file ->
                try {
                    val current = existing[file.name.lowercase()]
                    val target =
                        when {
                            current == null -> DocumentsContract.createDocument(resolver, root, MIME_TYPE, file.name)
                            replace -> DocumentsContract.buildDocumentUriUsingTree(outputTree, current.documentId)
                            else -> null
                        }
                    // "wt" truncates, so a replaced file never keeps trailing bytes of a longer old version.
                    target != null &&
                        resolver.openOutputStream(target, "wt")?.use { it.write(file.content.toByteArray(Charsets.UTF_8)) } != null
                } catch (_: Exception) {
                    false
                }
            }.map { it.name }
    }

    /** The files (not folders) directly in [tree], or null when it cannot be listed. */
    private fun files(tree: Uri): List<Child>? =
        try {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
            resolver.query(children, columns, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        if (cursor.getString(2) != Document.MIME_TYPE_DIR) add(Child(cursor.getString(0), cursor.getString(1)))
                    }
                }
            }
        } catch (_: Exception) {
            null
        }

    /** Reads each child, capped one byte over the Shortcut limit, or returns null if any read fails. */
    private fun readFiles(
        tree: Uri,
        children: List<Child>,
    ): List<SourceShortcut>? =
        children.map { child ->
            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, child.documentId)
            val bytes =
                try {
                    resolver.openInputStream(uri)?.use { it.readCapped(MAX_LOCAL_SHORTCUT_BYTES + 1) }
                } catch (_: Exception) {
                    null
                } ?: return null
            SourceShortcut(child.name, bytes)
        }

    private companion object {
        /** A generic type, so providers keep the `.gamebridge` name instead of appending an extension for the type. */
        const val MIME_TYPE = "application/octet-stream"
    }
}
