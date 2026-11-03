package com.allourmedia.gamebridge.core

/** The setup screen's plain-text line for one app, for example "Artemis: installed". */
fun appStatusLine(
    appName: String,
    installed: Boolean,
): String = if (installed) "$appName: installed" else "$appName: not installed"
