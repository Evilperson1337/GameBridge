package com.allourmedia.gamebridge

/** Why GameBridge could not Dispatch. The reason is the one line shown to the player. */
enum class FailureCategory(
    val reason: String,
) {
    NoShortcut("GameBridge is meant to be started from LaunchBox. Pick a game there."),
    WrongAction("GameBridge didn't recognise this request. Check the command set up in LaunchBox."),
    NotContentUri("The Shortcut wasn't shared in a form GameBridge accepts."),
    Unreadable("The Shortcut couldn't be read."),
    UnsupportedType("Not a supported Shortcut."),
    StorefrontNotVerified("This Storefront isn't verified yet."),
    RemoteUnsupported("Remote Shortcuts aren't supported yet."),
    InvalidContents("This Shortcut's contents aren't valid."),
    TooLarge("This Shortcut is too large to be a real one."),
    GameNativeNotInstalled(
        "GameNative is not installed. Get it from https://github.com/utkarshdalal/GameNative/releases",
    ),
    GameNativeRefused("GameNative refused the request."),
}

/** A failure plus the Shortcut's filename and, for an unverified Storefront, the Storefront's name. */
data class BridgeFailure(
    val category: FailureCategory,
    val fileName: String? = null,
    val storefront: String? = null,
) {
    val reason: String =
        if (category == FailureCategory.StorefrontNotVerified && storefront != null) {
            "Support for $storefront games isn't verified yet."
        } else {
            category.reason
        }
}
