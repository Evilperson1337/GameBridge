package com.allourmedia.gamebridge.core

/** The setup screen's plain-text line for one app, for example "Artemis: installed". */
fun appStatusLine(
    appName: String,
    installed: Boolean,
): String = if (installed) "$appName: installed" else "$appName: not installed"

/** The status text under an app's tile: "Installed" or "Not installed". */
fun appTileStatus(installed: Boolean): String = if (installed) "Installed" else "Not installed"

/** The letter on a not-installed app's placeholder logo: the first letter of its name. */
fun appTileInitial(appName: String): String = appName.take(1).uppercase()
