package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BridgeFailureTest {
    @Test
    fun everyRejectionMapsToItsOwnFailureCategory() {
        val expected =
            mapOf(
                Rejection.UnsupportedType to FailureCategory.UnsupportedType,
                Rejection.StorefrontNotVerified(Storefront.Steam) to FailureCategory.StorefrontNotVerified,
                Rejection.InvalidContents to FailureCategory.InvalidContents,
                Rejection.TooLarge to FailureCategory.TooLarge,
            )
        for ((rejection, category) in expected) assertEquals(category, rejection.toFailure("x.gog").category)
    }

    @Test
    fun rejectionFailureCarriesFileNameAndStorefrontOnlyWhenUnverified() {
        assertEquals(
            BridgeFailure(FailureCategory.StorefrontNotVerified, "B.steam", Storefront.Steam),
            Rejection.StorefrontNotVerified(Storefront.Steam).toFailure("B.steam"),
        )
        assertEquals(BridgeFailure(FailureCategory.TooLarge, "B.gog"), Rejection.TooLarge.toFailure("B.gog"))
    }

    @Test
    fun unverifiedStorefrontReasonNamesTheStorefront() {
        assertEquals(
            "Support for Epic games isn't verified yet.",
            BridgeFailure(FailureCategory.StorefrontNotVerified, "A.epic", Storefront.Epic).reason,
        )
        assertEquals("This Storefront isn't verified yet.", BridgeFailure(FailureCategory.StorefrontNotVerified).reason)
    }

    @Test
    fun messageIsJustTheReasonWithoutAFileName() {
        assertEquals(FailureCategory.NoShortcut.reason, BridgeFailure(FailureCategory.NoShortcut).message)
    }

    @Test
    fun messagePutsTheFileNameAboveTheReasonAndTruncatesLongNames() {
        assertEquals(
            "A.gog\n\n" + FailureCategory.TooLarge.reason,
            BridgeFailure(FailureCategory.TooLarge, "A.gog").message,
        )
        val long = "n".repeat(200)
        assertEquals(
            "n".repeat(80) + "\n\n" + FailureCategory.TooLarge.reason,
            BridgeFailure(FailureCategory.TooLarge, long).message,
        )
    }

    @Test
    fun everyReasonIsPinnedAndTellsThePlayerWhatToChange() {
        val expected =
            mapOf(
                FailureCategory.NoShortcut to "LaunchBox didn't pass a Shortcut. The emulator command must end with -d {file.uri}.",
                FailureCategory.WrongAction to
                    "LaunchBox sent GameBridge an unexpected request. In the emulator command, " +
                    "use the action com.allourmedia.gamebridge.action.DISPATCH. " +
                    "Copy the command from GameBridge's setup screen.",
                FailureCategory.NotContentUri to
                    "LaunchBox didn't pass the Shortcut as a content URI. " +
                    "The emulator command must end with -d {file.uri}, not {file.path}.",
                FailureCategory.Unreadable to
                    "GameBridge couldn't read the Shortcut. Check the game's Shortcut file still exists in LaunchBox, " +
                    "then pick the game again.",
                FailureCategory.UnsupportedType to
                    "This isn't a Shortcut type GameBridge supports. Import the Shortcut file exported by GameNative " +
                    "(.gog, .steam, .epic, .amazon, .pcgame).",
                FailureCategory.StorefrontNotVerified to "This Storefront isn't verified yet.",
                FailureCategory.InvalidContents to "This Shortcut's contents aren't valid.",
                FailureCategory.TooLarge to
                    "This file is too large to be a Shortcut. Check that LaunchBox is passing the Shortcut, " +
                    "not another file.",
                FailureCategory.GameNativeNotInstalled to
                    "GameNative is not installed. Get it from https://github.com/utkarshdalal/GameNative/releases",
                FailureCategory.GameNativeRefused to "GameNative refused the request.",
                FailureCategory.ArtemisNotInstalled to "Artemis is not installed. Install Artemis to play Remote Shortcuts (.art).",
                FailureCategory.ArtemisRefused to "Artemis refused the request.",
            )
        assertEquals(FailureCategory.entries.toSet(), expected.keys)
        for ((category, reason) in expected) assertEquals(reason, category.reason)
    }

    @Test
    fun noReasonPointsAtARepoFile() {
        for (category in FailureCategory.entries) {
            assert(!category.reason.contains("docs/") && !category.reason.contains(".md")) { category.name }
        }
    }

    @Test
    fun readmeTroubleshootingTableListsEveryReasonExactly() {
        val readme = java.io.File("../README.md").readText()
        for (category in FailureCategory.entries) {
            // The README folds the unverified-Storefront reason into one row, with a note after it.
            val row =
                if (category == FailureCategory.StorefrontNotVerified) {
                    "| Support for Steam games isn't verified yet. (also Epic, Amazon, Custom) |"
                } else {
                    "| ${category.reason} |"
                }
            assert(readme.contains(row)) { category.name }
        }
    }
}
