package com.allourmedia.gamebridge

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.allourmedia.gamebridge.core.appStatusLine
import com.allourmedia.gamebridge.core.setupCommand

/**
 * The app-drawer entry. Shows the Setup command for this build, a Copy button, whether GameNative, Artemis and LaunchBox are installed,
 * and a checklist. It dispatches nothing and changes nothing.
 */
class SetupActivity : Activity() {
    private val command by lazy { setupCommand(packageName) }

    /** The Setup command text currently on screen. */
    fun displayedCommand(): String = command

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val column =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad, pad, pad)
            }

        fun text(
            value: String,
            size: Float = 16f,
        ) = TextView(this).apply {
            text = value
            textSize = size
            setPadding(0, pad / 2, 0, pad / 2)
        }
        column.addView(text("GameBridge", 24f))
        column.addView(text("Enter this Setup command in LaunchBox's Custom Emulator (With Code):"))
        column.addView(text(command).apply { setTextIsSelectable(true) })
        column.addView(
            Button(this).apply {
                text = "Copy"
                setOnClickListener {
                    val clipboard = getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("GameBridge Setup command", command))
                }
            },
        )
        column.addView(text(appStatusLine("GameNative", isPackageInstalled(packageManager, GameNativeDispatcher.PACKAGE))))
        column.addView(text(appStatusLine("Artemis", isPackageInstalled(packageManager, ArtemisDispatcher.PACKAGE))))
        column.addView(text(appStatusLine("LaunchBox", isPackageInstalled(packageManager, LAUNCHBOX_PACKAGE))))
        column.addView(text("1. Set the Windows platform's default emulator to Custom Emulator (With Code)."))
        column.addView(text("2. Paste the command above."))
        column.addView(text("3. Clear any per-game emulator override."))
        setContentView(ScrollView(this).apply { addView(column) })
    }
}

/** LaunchBox's package. Only its presence is reported; GameBridge never launches it. */
const val LAUNCHBOX_PACKAGE = "com.unbrokensoftware.launchbox"

/** Whether [packageName] is installed and visible to GameBridge (see the manifest's `<queries>`). */
fun isPackageInstalled(
    packageManager: PackageManager,
    packageName: String,
): Boolean =
    try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
