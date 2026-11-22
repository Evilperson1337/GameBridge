package com.allourmedia.gamebridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.allourmedia.gamebridge.core.PlannedFile
import com.allourmedia.gamebridge.core.ShortcutFolder
import com.allourmedia.gamebridge.core.scanSummary

/**
 * The Scan screen (ADR 0003): the player grants one or more Shortcut Folders (GameNative, Artemis, Ariane; read) and
 * the Output Folder (read/write) once, then taps Scan to write a Bridge Shortcut per game. Runs only when the player taps Scan.
 * Overwriting a file that differs needs a second, explicit tap. Not exported: reached from the setup screen only.
 */
class ScanActivity : Activity() {
    private val folders by lazy { ScanFolders(this) }
    private val labels = mutableMapOf<ScanFolder, TextView>()
    private lateinit var scanButton: Button
    private lateinit var replaceButton: Button
    private lateinit var summary: TextView
    private var pendingReplace: List<PlannedFile> = emptyList()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        summary = text("").apply { setTextIsSelectable(true) }
        scanButton = button("Scan") { scan() }
        replaceButton = button("") { replace() }.apply { visibility = View.GONE }
        val content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val gutter = dp(16)
                setPadding(gutter, gutter, gutter, gutter)
                addView(text("Scan for games", bold = true))
                addView(text("1. Choose the folders your apps export Shortcuts to. Each is optional; choose at least one."))
                for ((folder, what) in SOURCES) {
                    addView(text(what))
                    addView(text("").also { labels[folder] = it })
                    addView(button("Choose ${folder.name} folder") { choose(folder) })
                }
                addView(text("2. Choose a folder for GameBridge's Shortcuts, then point LaunchBox at it."))
                addView(text("").also { labels[ScanFolder.Output] = it })
                addView(button("Choose Output Folder") { choose(ScanFolder.Output) })
                addView(text("3. Scan. Files already in the Output Folder are never changed without asking."))
                addView(scanButton)
                addView(summary)
                addView(replaceButton)
            }
        setContentView(ScrollView(this).apply { addView(content) })
        refresh()
    }

    private fun refresh() {
        for ((folder, view) in labels) view.text = label(folders.get(folder))
        scanButton.isEnabled = folders.get(ScanFolder.Output) != null && sources().isNotEmpty()
    }

    /** The Shortcut Folders chosen so far, each with a grant still held. */
    private fun sources(): Map<ShortcutFolder, Uri> =
        ShortcutFolder.entries.mapNotNull { folder -> folders.get(folder.scanFolder)?.let { folder to it } }.toMap()

    /** The folder as the provider names it (for example `primary:Games/GameNative`), shown on screen only, never logged. */
    private fun label(tree: Uri?) = if (tree == null) "Not chosen" else DocumentsContract.getTreeDocumentId(tree)

    private fun choose(folder: ScanFolder) {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), folder.ordinal)
    }

    @Deprecated("Activity result API needs AndroidX; this app has none.")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        val folder = ScanFolder.entries.getOrNull(requestCode) ?: return
        val tree = data?.data
        if (resultCode != RESULT_OK || tree == null) return
        folders.set(folder, tree)
        refresh()
    }

    private fun scan() {
        val sources = sources().takeIf { it.isNotEmpty() } ?: return
        val output = folders.get(ScanFolder.Output) ?: return
        busy("Scanning…")
        Thread {
            val runner = ScanRunner(contentResolver)
            val text =
                when (val read = runner.plan(sources, output)) {
                    is ScanRead.Unreadable -> {
                        unreadable(read.folder)
                    }

                    is ScanRead.Planned -> {
                        val failed = runner.write(output, read.plan.writes, replace = false)
                        pendingReplace = read.plan.needsConfirmation
                        scanSummary(read.plan, failed)
                    }
                }
            runOnUiThread { if (!isDestroyed) done(text) }
        }.start()
    }

    private fun replace() {
        val output = folders.get(ScanFolder.Output) ?: return
        val files = pendingReplace
        busy("Replacing…")
        Thread {
            val failed = ScanRunner(contentResolver).write(output, files, replace = true)
            pendingReplace = emptyList()
            val text =
                if (failed.isEmpty()) {
                    "Replaced ${files.size}."
                } else {
                    "Replaced ${files.size - failed.size}.\nCouldn't write:\n" + failed.joinToString("\n")
                }
            runOnUiThread { if (!isDestroyed) done(text) }
        }.start()
    }

    private fun unreadable(folder: ScanFolder) =
        when (folder) {
            ScanFolder.Output -> "GameBridge can't read the Output Folder. Choose it again."
            else -> "GameBridge can't read the ${folder.name} folder. Choose it again."
        }

    private fun busy(message: String) {
        summary.text = message
        scanButton.isEnabled = false
        replaceButton.visibility = View.GONE
    }

    private fun done(text: String) {
        summary.text = text
        refresh()
        if (pendingReplace.isNotEmpty()) {
            replaceButton.text = "Replace ${pendingReplace.size} file(s) with the scanned version"
            replaceButton.visibility = View.VISIBLE
        }
    }

    private fun text(
        value: CharSequence,
        bold: Boolean = false,
    ) = TextView(this).apply {
        text = value
        textSize = SIZE_BODY
        setTextColor(getColor(R.color.gb_ink))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun button(
        label: String,
        onClick: () -> Unit,
    ) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = SIZE_BODY
        minHeight = dp(48)
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setOnClickListener { onClick() }
    }

    private companion object {
        const val SIZE_BODY = 16f

        /** The Shortcut Folders on screen, in order, with what each holds. */
        val SOURCES =
            listOf(
                ScanFolder.GameNative to "GameNative: its exported Shortcuts (.gog, .steam, .epic, .amazon, .pcgame).",
                ScanFolder.Artemis to "Artemis: its exported app entries (.art).",
                ScanFolder.Ariane to "Ariane: its exported app entries (.art).",
            )
    }
}
