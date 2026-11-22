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
import com.allourmedia.gamebridge.core.scanSummary

/**
 * The Scan screen (ADR 0003): the player grants the GameNative Shortcut Folder (read) and the Output Folder
 * (read/write) once, then taps Scan to write a Bridge Shortcut per game. Runs only when the player taps Scan.
 * Overwriting a file that differs needs a second, explicit tap. Not exported: reached from the setup screen only.
 */
class ScanActivity : Activity() {
    private val folders by lazy { ScanFolders(this) }
    private lateinit var gameNativeLabel: TextView
    private lateinit var outputLabel: TextView
    private lateinit var scanButton: Button
    private lateinit var replaceButton: Button
    private lateinit var summary: TextView
    private var pendingReplace: List<PlannedFile> = emptyList()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameNativeLabel = text("")
        outputLabel = text("")
        summary = text("").apply { setTextIsSelectable(true) }
        scanButton = button("Scan") { scan() }
        replaceButton = button("") { replace() }.apply { visibility = View.GONE }
        val content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val gutter = dp(16)
                setPadding(gutter, gutter, gutter, gutter)
                addView(text("Scan for games", bold = true))
                addView(text("1. Choose the folder GameNative exports its Shortcuts to."))
                addView(gameNativeLabel)
                addView(button("Choose GameNative folder") { choose(ScanFolder.GameNative) })
                addView(text("2. Choose a folder for GameBridge's Shortcuts, then point LaunchBox at it."))
                addView(outputLabel)
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
        val gameNative = folders.get(ScanFolder.GameNative)
        val output = folders.get(ScanFolder.Output)
        gameNativeLabel.text = label(gameNative)
        outputLabel.text = label(output)
        scanButton.isEnabled = gameNative != null && output != null
    }

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
        val gameNative = folders.get(ScanFolder.GameNative) ?: return
        val output = folders.get(ScanFolder.Output) ?: return
        busy("Scanning…")
        Thread {
            val runner = ScanRunner(contentResolver)
            val text =
                when (val read = runner.plan(gameNative, output)) {
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
            ScanFolder.GameNative -> "GameBridge can't read the GameNative folder. Choose it again."
            ScanFolder.Output -> "GameBridge can't read the Output Folder. Choose it again."
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
    }
}
