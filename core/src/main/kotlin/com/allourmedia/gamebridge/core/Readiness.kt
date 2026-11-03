package com.allourmedia.gamebridge.core

/** The setup screen's one-line finding: that all three apps are installed, or exactly which are not. */
fun readinessLine(
    gameNativeInstalled: Boolean,
    artemisInstalled: Boolean,
    launchBoxInstalled: Boolean,
): String {
    val missing =
        listOf("GameNative" to gameNativeInstalled, "Artemis" to artemisInstalled, "LaunchBox" to launchBoxInstalled)
            .filterNot { it.second }
            .map { it.first }
    return when (missing.size) {
        0 -> "GameNative, Artemis and LaunchBox are installed"
        1 -> "${missing[0]} is not installed"
        else -> "${missing.dropLast(1).joinToString(", ")} and ${missing.last()} are not installed"
    }
}
