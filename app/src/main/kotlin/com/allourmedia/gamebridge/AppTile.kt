package com.allourmedia.gamebridge

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.allourmedia.gamebridge.core.appStatusLine
import com.allourmedia.gamebridge.core.appTileInitial
import com.allourmedia.gamebridge.core.appTileStatus

/**
 * A rounded, bounded tile for one app the setup screen checks: logo above name above status.
 * The logo is the installed app's own launcher icon; a missing app gets its first letter on a flat tone instead.
 * Not interactive: it dispatches nothing.
 */
class AppTile(
    context: Context,
    appName: String,
    packageName: String,
) : LinearLayout(context) {
    /** The app's launcher icon, or null when the app is not installed. */
    val iconView: ImageView?

    /** The first-letter placeholder, or null when the app's icon is shown. */
    val placeholderView: TextView?

    init {
        val icon = launcherIcon(context.packageManager, packageName)
        val installed = icon != null
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        minimumWidth = dp(MIN_WIDTH_DP)
        setPadding(dp(4), dp(12), dp(4), dp(12))
        background =
            GradientDrawable().apply {
                setColor(context.getColor(R.color.gb_surface))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), context.getColor(R.color.gb_edge))
            }
        contentDescription = appStatusLine(appName, installed)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES

        val logoSize = LayoutParams(dp(LOGO_DP), dp(LOGO_DP))
        if (icon != null) {
            iconView = ImageView(context).apply { setImageDrawable(icon) }
            placeholderView = null
            addView(iconView, logoSize)
        } else {
            iconView = null
            placeholderView =
                TextView(context).apply {
                    text = appTileInitial(appName)
                    textSize = SIZE_PLACEHOLDER
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setTextColor(context.getColor(R.color.gb_grey))
                    background =
                        GradientDrawable().apply {
                            setColor(context.getColor(R.color.gb_tone))
                            cornerRadius = dp(8).toFloat()
                        }
                }
            addView(placeholderView, logoSize)
        }
        addView(label(appName, if (installed) R.color.gb_ink else R.color.gb_grey, bold = true, top = 8))
        addView(label(appTileStatus(installed), if (installed) R.color.gb_grey else R.color.gb_negative, bold = false, top = 2))
        for (i in 0 until childCount) getChildAt(i).importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun label(
        value: String,
        colorId: Int,
        bold: Boolean,
        top: Int,
    ) = TextView(context).apply {
        text = value
        textSize = SIZE_BODY
        gravity = Gravity.CENTER
        setTextColor(context.getColor(colorId))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val MIN_WIDTH_DP = 96
        const val LOGO_DP = 48
        const val SIZE_BODY = 16f
        const val SIZE_PLACEHOLDER = 24f
    }
}

/** The app's own launcher icon, or null when it is not installed or not visible to GameBridge. */
private fun launcherIcon(
    packageManager: PackageManager,
    packageName: String,
) = try {
    packageManager.getApplicationIcon(packageName)
} catch (_: PackageManager.NameNotFoundException) {
    null
}

/** Lays children out left to right and wraps to a new row when the next one does not fit. */
class TileFlow(
    context: Context,
    private val gapPx: Int,
) : android.view.ViewGroup(context) {
    override fun onMeasure(
        widthSpec: Int,
        heightSpec: Int,
    ) {
        val maxWidth = MeasureSpec.getSize(widthSpec)
        var x = 0
        var rowHeight = 0
        var y = 0
        var widest = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED)
            if (x > 0 && x + child.measuredWidth > maxWidth) {
                y += rowHeight + gapPx
                x = 0
                rowHeight = 0
            }
            x += child.measuredWidth + gapPx
            widest = maxOf(widest, x - gapPx)
            rowHeight = maxOf(rowHeight, child.measuredHeight)
        }
        setMeasuredDimension(resolveSize(widest, widthSpec), resolveSize(y + rowHeight, heightSpec))
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        val maxWidth = r - l
        var x = 0
        var y = 0
        var rowHeight = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (x > 0 && x + child.measuredWidth > maxWidth) {
                y += rowHeight + gapPx
                x = 0
                rowHeight = 0
            }
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
            x += child.measuredWidth + gapPx
            rowHeight = maxOf(rowHeight, child.measuredHeight)
        }
    }
}
