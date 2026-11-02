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
                Rejection.RemoteUnsupported to FailureCategory.RemoteUnsupported,
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
            "A.gog\n\nThis Shortcut is too large to be a real one.",
            BridgeFailure(FailureCategory.TooLarge, "A.gog").message,
        )
        val long = "n".repeat(200)
        assertEquals(
            "n".repeat(80) + "\n\n" + FailureCategory.TooLarge.reason,
            BridgeFailure(FailureCategory.TooLarge, long).message,
        )
    }
}
