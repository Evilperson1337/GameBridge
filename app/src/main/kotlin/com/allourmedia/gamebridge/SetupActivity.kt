package com.allourmedia.gamebridge

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.allourmedia.gamebridge.core.appStatusLine
import com.allourmedia.gamebridge.core.readinessLine
import com.allourmedia.gamebridge.core.setupCommand

/**
 * The app-drawer entry. Answers one question: what to paste into LaunchBox, and is everything ready. Shows a readiness line,
 * the status of GameNative, Artemis and LaunchBox, three steps, and the Setup command with a Copy button.
 * It dispatches nothing and changes nothing.
 */
class SetupActivity : Activity() {
    private val command by lazy { setupCommand(packageName) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var copyButton: Button
    private val restoreCopyLabel = Runnable { copyButton.text = COPY_LABEL }

    /** The Setup command text currently on screen. */
    fun displayedCommand(): String = command

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gutter = dp(GUTTER_DP)

        val gameNative = isPackageInstalled(packageManager, GameNativeDispatcher.PACKAGE)
        val artemis = isPackageInstalled(packageManager, ArtemisDispatcher.PACKAGE)
        val launchBox = isPackageInstalled(packageManager, LAUNCHBOX_PACKAGE)

        val status =
            column().apply {
                addView(text(readinessLine(gameNative, artemis, launchBox), R.color.gb_ink, bold = true))
                addView(statusLine("GameNative", gameNative))
                addView(statusLine("Artemis", artemis))
                addView(statusLine("LaunchBox", launchBox))
            }
        val steps =
            column().apply {
                addView(text("1. Set the Windows platform's default emulator to Custom Emulator (With Code).", R.color.gb_ink))
                addView(text("2. Paste this Setup command:", R.color.gb_ink))
                addView(commandBlock())
                addView(copyButton())
                addView(text("3. Clear any per-game emulator override.", R.color.gb_ink))
            }

        val wide = resources.configuration.screenWidthDp >= WIDE_DP
        val content =
            LinearLayout(this).apply {
                setPadding(gutter, gutter, gutter, gutter)
                if (wide) {
                    orientation = LinearLayout.HORIZONTAL
                    addView(status, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = gutter })
                    addView(steps, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                } else {
                    orientation = LinearLayout.VERTICAL
                    addView(status, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = gutter })
                    addView(steps, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                }
            }
        setContentView(ScrollView(this).apply { addView(content) })
        copyButton.requestFocus()
    }

    override fun onDestroy() {
        handler.removeCallbacks(restoreCopyLabel)
        super.onDestroy()
    }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    private fun text(
        value: CharSequence,
        colorId: Int,
        bold: Boolean = false,
    ) = TextView(this).apply {
        text = value
        textSize = SIZE_BODY
        setTextColor(getColor(colorId))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(6))
    }

    /** "Artemis: installed" in grey; only "not installed" is coloured. */
    private fun statusLine(
        name: String,
        installed: Boolean,
    ): TextView {
        val line = appStatusLine(name, installed)
        val spannable = SpannableString(line)
        val colorId = if (installed) R.color.gb_grey else R.color.gb_negative
        val start = line.indexOf(':') + 2
        spannable.setSpan(ForegroundColorSpan(getColor(colorId)), start, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return text(spannable, R.color.gb_ink)
    }

    private fun commandBlock() =
        TextView(this).apply {
            text = command
            textSize = SIZE_COMMAND
            typeface = Typeface.MONOSPACE
            setTextColor(getColor(R.color.gb_ink))
            setTextIsSelectable(true)
            minHeight = dp(MIN_TARGET_DP)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background =
                GradientDrawable().apply {
                    setColor(getColor(R.color.gb_surface))
                    cornerRadius = dp(8).toFloat()
                }
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(4) }
        }

    private fun copyButton(): Button {
        copyButton =
            Button(this).apply {
                text = COPY_LABEL
                isAllCaps = false
                textSize = SIZE_BODY
                setTextColor(getColor(R.color.gb_on_accent))
                stateListAnimator = null
                elevation = 0f
                minHeight = dp(MIN_TARGET_DP)
                minimumHeight = dp(MIN_TARGET_DP)
                isFocusableInTouchMode = true
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                background = copyBackground()
                layoutParams =
                    LinearLayout.LayoutParams(MATCH_PARENT, dp(MIN_TARGET_DP)).apply {
                        topMargin = dp(8)
                        bottomMargin = dp(8)
                    }
                setOnClickListener {
                    val clipboard = getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("GameBridge Setup command", command))
                    text = COPIED_LABEL
                    handler.removeCallbacks(restoreCopyLabel)
                    handler.postDelayed(restoreCopyLabel, COPIED_MILLIS)
                }
            }
        return copyButton
    }

    /** Flat accent fill; focus adds a 2dp ink ring, pressed darkens the fill, so both show on either theme. */
    private fun copyBackground(): StateListDrawable {
        fun shape(
            ring: Boolean,
            pressed: Boolean,
        ) = GradientDrawable().apply {
            val accent = getColor(R.color.gb_accent)
            setColor(if (pressed) blend(accent, getColor(R.color.gb_ink)) else accent)
            cornerRadius = dp(8).toFloat()
            if (ring) setStroke(dp(2), getColor(R.color.gb_ink))
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), shape(ring = false, pressed = true))
            addState(intArrayOf(android.R.attr.state_focused), shape(ring = true, pressed = false))
            addState(intArrayOf(), shape(ring = false, pressed = false))
        }
    }

    private fun blend(
        base: Int,
        over: Int,
    ): Int {
        fun mix(shift: Int): Int {
            val b = (base shr shift) and 0xFF
            val o = (over shr shift) and 0xFF
            return (b + ((o - b) * PRESSED_BLEND).toInt()) shl shift
        }
        return (0xFF shl 24) or mix(16) or mix(8) or mix(0)
    }

    private companion object {
        const val GUTTER_DP = 16
        const val MIN_TARGET_DP = 48
        const val WIDE_DP = 600
        const val COPY_LABEL = "Copy"
        const val COPIED_LABEL = "Copied"
        const val COPIED_MILLIS = 2_000L
        const val PRESSED_BLEND = 0.25f
        const val SIZE_BODY = 16f
        const val SIZE_COMMAND = 14f
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
