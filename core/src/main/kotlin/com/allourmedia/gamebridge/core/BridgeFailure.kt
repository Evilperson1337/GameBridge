package com.allourmedia.gamebridge.core

/** Why GameBridge could not Dispatch. The reason is the one line shown to the player. */
enum class FailureCategory(
    val reason: String,
) {
    NoShortcut("GameBridge is meant to be started from LaunchBox. Pick a game there."),
    WrongAction(
        "LaunchBox sent GameBridge an unexpected request. In the emulator command, " +
            "use the action com.allourmedia.gamebridge.action.DISPATCH. " +
            "Copy the command from GameBridge's setup screen.",
    ),
    NotContentUri(
        "LaunchBox didn't pass the Shortcut as a content URI. " +
            "The emulator command must end with -d {file.uri}, not {file.path}.",
    ),
    Unreadable(
        "GameBridge couldn't read the Shortcut. Check the game's Shortcut file still exists in LaunchBox, " +
            "then pick the game again.",
    ),
    UnsupportedType(
        "This isn't a Shortcut GameBridge knows. Check that the platform's default emulator is only used " +
            "for Shortcut files (.gog, .steam, .epic, .amazon, .pcgame).",
    ),
    StorefrontNotVerified("This Storefront isn't verified yet."),
    RemoteUnsupported("Remote Shortcuts aren't supported yet."),
    InvalidContents("This Shortcut's contents aren't valid."),
    TooLarge(
        "This file is too large to be a Shortcut. Check that LaunchBox is passing the Shortcut, " +
            "not another file.",
    ),
    GameNativeNotInstalled(
        "GameNative is not installed. Get it from https://github.com/utkarshdalal/GameNative/releases",
    ),
    GameNativeRefused("GameNative refused the request."),
}

private const val MAX_NAME = 80

/** A failure plus the Shortcut's filename and, for an unverified Storefront, that Storefront. */
data class BridgeFailure(
    val category: FailureCategory,
    val fileName: String? = null,
    val storefront: Storefront? = null,
) {
    val reason: String =
        if (category == FailureCategory.StorefrontNotVerified && storefront != null) {
            "Support for ${storefront.name} games isn't verified yet."
        } else {
            category.reason
        }

    /** The dialog text: the (truncated) filename above the reason, or just the reason. */
    val message: String = if (fileName == null) reason else "${fileName.take(MAX_NAME)}\n\n$reason"
}

fun Rejection.toFailure(fileName: String): BridgeFailure =
    when (this) {
        Rejection.UnsupportedType -> BridgeFailure(FailureCategory.UnsupportedType, fileName)
        is Rejection.StorefrontNotVerified -> BridgeFailure(FailureCategory.StorefrontNotVerified, fileName, storefront)
        Rejection.RemoteUnsupported -> BridgeFailure(FailureCategory.RemoteUnsupported, fileName)
        Rejection.InvalidContents -> BridgeFailure(FailureCategory.InvalidContents, fileName)
        Rejection.TooLarge -> BridgeFailure(FailureCategory.TooLarge, fileName)
    }

const val DISPATCH_ACTION = "com.allourmedia.gamebridge.action.DISPATCH"
const val MAIN_ACTION = "android.intent.action.MAIN"

sealed interface IntentTriage {
    data object Accepted : IntentTriage

    data class Failed(
        val category: FailureCategory,
    ) : IntentTriage
}

/**
 * Classifies an incoming intent from its action, whether it carries data, and that data's URI scheme.
 * The no-data check comes first on purpose (ticket 03).
 */
fun triageIntent(
    action: String?,
    scheme: String?,
    hasData: Boolean,
): IntentTriage =
    when {
        !hasData && (action == null || action == MAIN_ACTION || action == DISPATCH_ACTION) -> {
            IntentTriage.Failed(FailureCategory.NoShortcut)
        }

        action != DISPATCH_ACTION || !hasData -> {
            IntentTriage.Failed(FailureCategory.WrongAction)
        }

        scheme != "content" -> {
            IntentTriage.Failed(FailureCategory.NotContentUri)
        }

        else -> {
            IntentTriage.Accepted
        }
    }
