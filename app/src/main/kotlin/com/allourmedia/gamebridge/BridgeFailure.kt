package com.allourmedia.gamebridge

/** Why GameBridge could not Dispatch. The reason is the one line shown to the player. */
enum class FailureCategory(
    val reason: String,
) {
    NoShortcut("GameBridge is meant to be started from LaunchBox. Pick a game there."),
    WrongAction("GameBridge didn't recognise this request. Check the command set up in LaunchBox."),
    NotContentUri("The Shortcut wasn't shared in a form GameBridge accepts."),
    Unreadable("The Shortcut couldn't be read."),
    GameNativeNotInstalled(
        "GameNative is not installed. Get it from https://github.com/utkarshdalal/GameNative/releases",
    ),
    GameNativeRefused("GameNative refused the request."),
}

/** A failure plus the Shortcut's filename, when it is known. */
data class BridgeFailure(
    val category: FailureCategory,
    val fileName: String? = null,
)
