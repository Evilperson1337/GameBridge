package com.allourmedia.gamebridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.allourmedia.gamebridge.core.PlannedFile
import com.allourmedia.gamebridge.core.ScanEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Seam 2: a Scan through the Storage Access Framework. [FolderProvider] stands in for shared storage, with folders
 * `source` (the GameNative Shortcut Folder), `output` (the Output Folder) and `outside` (anything else).
 */
@RunWith(AndroidJUnit4::class)
class ScanRunnerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val resolver = instrumentation.targetContext.contentResolver
    private val runner = ScanRunner(resolver)

    private val broforce = """{"version":1,"target":"gamenative","storefront":"steam","store_id":274190}"""
    private val abzu = """{"version":1,"target":"gamenative","storefront":"epic","store_id":64}"""

    /** Resets the provider's folders and grants this app access, then waits until the new contents are visible. */
    private fun seed(
        source: Map<String, String>,
        output: Map<String, String> = emptyMap(),
        outside: Map<String, String> = emptyMap(),
    ) {
        val marker = "seed-${System.nanoTime()}.txt"
        val bundle =
            Bundle().apply {
                putBundle("source", bundleOf(source + (marker to "")))
                putBundle("output", bundleOf(output))
                putBundle("outside", bundleOf(outside))
            }
        val context = instrumentation.context
        context.startActivity(
            Intent(context, FolderSeedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(FolderSeedActivity.EXTRA_SEED, bundle)
                .putExtra(FolderSeedActivity.EXTRA_PACKAGE, instrumentation.targetContext.packageName),
        )
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (runCatching { marker in files(FolderProvider.SOURCE_TREE) }.getOrDefault(false)) return
            Thread.sleep(50)
        }
        throw AssertionError("Timed out seeding the folders")
    }

    private fun bundleOf(files: Map<String, String>) = Bundle().apply { files.forEach { (name, text) -> putString(name, text) } }

    /** Every file directly in [tree], name to text, read through SAF independently of [ScanRunner]. */
    private fun files(tree: Uri): Map<String, String> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        val result = sortedMapOf<String, String>()
        resolver.query(children, columns, null, null, null)!!.use { c ->
            while (c.moveToNext()) {
                if (c.getString(2) == Document.MIME_TYPE_DIR) continue
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                result[c.getString(1)] = resolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            }
        }
        return result
    }

    private fun sourceFiles() = files(FolderProvider.SOURCE_TREE).filterKeys { !it.startsWith("seed-") }

    private fun plan() = (runner.plan(FolderProvider.SOURCE_TREE, FolderProvider.OUTPUT_TREE) as ScanRead.Planned).plan

    @Test
    fun scanPlansFromTheShortcutFolderAndTheOutputFolder() {
        seed(
            source = mapOf("Broforce.steam" to "274190", "ABZU.epic" to "64", "Zero.gog" to "0", "notes.txt" to "hi", "images/" to ""),
            output = mapOf("ABZU.gamebridge" to "edited by hand"),
        )
        val plan = plan()
        assertEquals(listOf("Broforce.gamebridge"), plan.writes.map { it.name })
        assertEquals(listOf(PlannedFile("ABZU.gamebridge", abzu, "ABZU.epic")), plan.needsConfirmation)
        assertEquals(listOf(ScanEntry("Zero.gog", "This Shortcut's contents aren't valid.")), plan.rejected)
    }

    @Test
    fun writingCreatesNewFilesOnlyInTheOutputFolder() {
        val source = mapOf("Broforce.steam" to "274190", "ABZU.epic" to "64")
        seed(source = source, output = mapOf("ABZU.gamebridge" to "edited by hand"))
        assertEquals(emptyList<String>(), runner.write(FolderProvider.OUTPUT_TREE, plan().writes, replace = false))
        assertEquals(mapOf("ABZU.gamebridge" to "edited by hand", "Broforce.gamebridge" to broforce), files(FolderProvider.OUTPUT_TREE))
        assertEquals(source, sourceFiles())
        assertEquals(emptyMap<String, String>(), files(FolderProvider.OUTSIDE_TREE))
    }

    @Test
    fun confirmedReplaceOverwritesTheWholeFile() {
        seed(source = mapOf("ABZU.epic" to "64"), output = mapOf("abzu.gamebridge" to "x".repeat(500)))
        val plan = plan()
        assertEquals(emptyList<String>(), runner.write(FolderProvider.OUTPUT_TREE, plan.needsConfirmation, replace = true))
        // The existing file is replaced under its existing name, never duplicated, and not left with trailing bytes.
        assertEquals(mapOf("abzu.gamebridge" to abzu), files(FolderProvider.OUTPUT_TREE))
    }

    @Test
    fun writeWithoutReplaceNeverClobbersAFileThatAppearedSinceThePlan() {
        seed(source = mapOf("ABZU.epic" to "64"), output = mapOf("ABZU.gamebridge" to "edited by hand"))
        val failed = runner.write(FolderProvider.OUTPUT_TREE, listOf(PlannedFile("ABZU.gamebridge", abzu, "ABZU.epic")), replace = false)
        assertEquals(listOf("ABZU.gamebridge"), failed)
        assertEquals(mapOf("ABZU.gamebridge" to "edited by hand"), files(FolderProvider.OUTPUT_TREE))
    }

    @Test
    fun aFolderWithoutAGrantIsReportedUnreadable() {
        seed(source = mapOf("ABZU.epic" to "64"))
        val notGranted = DocumentsContract.buildTreeDocumentUri(FolderProvider.AUTHORITY, "outside-not-granted")
        assertEquals(ScanRead.Unreadable(ScanFolder.GameNative), runner.plan(notGranted, FolderProvider.OUTPUT_TREE))
        assertEquals(ScanRead.Unreadable(ScanFolder.Output), runner.plan(FolderProvider.SOURCE_TREE, notGranted))
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
