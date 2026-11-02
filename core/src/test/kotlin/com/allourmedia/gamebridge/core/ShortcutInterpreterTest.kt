package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

class ShortcutInterpreterTest {
    @Test
    fun leadingZerosAreAcceptedAsTheNumberTheyWrite() {
        // Unspecified in the spec: pinned as current behaviour (see docs/investigation-log.md).
        assertEquals(request(Storefront.GOG, 7), interpretShortcut("x.gog", "007".toByteArray()))
        assertEquals(rejected(Rejection.InvalidContents), interpretShortcut("x.gog", "000".toByteArray()))
    }

    private class CountingStream(
        private val size: Int,
    ) : InputStream() {
        var consumed = 0
            private set

        override fun read(): Int = if (consumed < size) consumed++.let { '1'.code } else -1

        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int {
            if (consumed >= size) return -1
            val n = minOf(len, size - consumed, 1000)
            b.fill('1'.code.toByte(), off, off + n)
            consumed += n
            return n
        }
    }

    @Test
    fun readingStopsAtTheCapPlusOneByteWithoutReadingTheWholeStream() {
        val stream = CountingStream(size = 10_000_000)
        val bytes = stream.readCapped(MAX_LOCAL_SHORTCUT_BYTES + 1)
        assertEquals(MAX_LOCAL_SHORTCUT_BYTES + 1, bytes.size)
        assertEquals(MAX_LOCAL_SHORTCUT_BYTES + 1, stream.consumed)
    }

    @Test
    fun readingShorterStreamsReturnsEverythingAndEmptyIsEmpty() {
        assertEquals(3, ByteArrayInputStream(ByteArray(3)).readCapped(10).size)
        assertEquals(0, ByteArrayInputStream(ByteArray(0)).readCapped(10).size)
    }

    private fun fixture(name: String): ByteArray = File("../fixtures/$name").readBytes()

    private fun interpretFixture(name: String) = interpretShortcut(name, fixture(name))

    private fun request(
        storefront: Storefront,
        storeId: Int,
    ) = ShortcutResult.Request(DispatchRequest(storefront, storeId))

    private fun rejected(reason: Rejection) = ShortcutResult.Rejected(reason)

    @Test
    fun storefrontTableHasTheFiveRowsFromTheSpec() {
        val rows = Storefront.all.map { listOf(it.name, it.extension, it.gameSource, it.verified) }
        assertEquals(
            listOf(
                listOf("GOG", "gog", "GOG", true),
                listOf("Steam", "steam", "STEAM", false),
                listOf("Epic", "epic", "EPIC", false),
                listOf("Amazon", "amazon", "AMAZON", false),
                listOf("Custom", "pcgame", "CUSTOM_GAME", false),
            ),
            rows,
        )
    }

    @Test
    fun validGogShortcutsProduceGogDispatchRequests() {
        assertEquals(request(Storefront.GOG, 1207666073), interpretFixture("Akalabeth.gog"))
        assertEquals(request(Storefront.GOG, 1470490225), interpretFixture("Broforce.gog"))
    }

    @Test
    fun unverifiedStorefrontsAreRejectedNamingTheStorefront() {
        assertEquals(rejected(Rejection.StorefrontNotVerified(Storefront.Steam)), interpretFixture("Broforce.steam"))
        assertEquals(rejected(Rejection.StorefrontNotVerified(Storefront.Epic)), interpretFixture("ABZU.epic"))
        assertEquals(rejected(Rejection.StorefrontNotVerified(Storefront.Amazon)), interpretFixture("Synthetic.amazon"))
        assertEquals(rejected(Rejection.StorefrontNotVerified(Storefront.Custom)), interpretFixture("Synthetic.pcgame"))
    }

    @Test
    fun flippingARowToVerifiedIsEnoughToProduceADispatchRequestWithItsGameSource() {
        val verifiedSteam = Storefront.Steam.copyVerified()
        val result = interpretShortcut("Broforce.steam", fixture("Broforce.steam"), listOf(Storefront.GOG, verifiedSteam))
        assertEquals(request(verifiedSteam, 274190), result)
        assertEquals("STEAM", (result as ShortcutResult.Request).request.storefront.gameSource)
    }

    @Test
    fun extensionsMatchCaseInsensitively() {
        assertEquals(request(Storefront.GOG, 1470490225), interpretFixture("Synthetic-upper.GOG"))
        assertEquals(rejected(Rejection.StorefrontNotVerified(Storefront.Steam)), interpretShortcut("X.STEAM", "1".toByteArray()))
    }

    @Test
    fun remoteShortcutsAreRejectedAsUnsupportedWhateverTheirContents() {
        assertEquals(rejected(Rejection.RemoteUnsupported), interpretShortcut("Game.art", ByteArray(0)))
        assertEquals(rejected(Rejection.RemoteUnsupported), interpretShortcut("Game.ART", "anything".toByteArray()))
    }

    @Test
    fun unknownOrMissingExtensionsAreUnsupported() {
        assertEquals(rejected(Rejection.UnsupportedType), interpretFixture("Synthetic-unknown.txt"))
        assertEquals(rejected(Rejection.UnsupportedType), interpretShortcut("NoExtension", "1".toByteArray()))
        assertEquals(rejected(Rejection.UnsupportedType), interpretShortcut("x.gog.bak", "1".toByteArray()))
    }

    @Test
    fun invalidContentsAreRejectedNeverTruncated() {
        val invalid =
            listOf(
                "Synthetic-zero.gog",
                "Synthetic-negative.gog",
                "Synthetic-above-int-max.gog",
                "Synthetic-non-numeric.gog",
                "Synthetic-two-numbers.gog",
                "Synthetic-two-lines.gog",
                "Synthetic-empty.gog",
            )
        for (name in invalid) assertEquals(name, rejected(Rejection.InvalidContents), interpretFixture(name))
        for (text in listOf("+5", "0x10", "1.5", "1,000", "٥", "99999999999999999999")) {
            assertEquals(text, rejected(Rejection.InvalidContents), interpretShortcut("x.gog", text.toByteArray()))
        }
    }

    @Test
    fun invalidContentsOfAnUnverifiedStorefrontAreInvalidNotUnverified() {
        assertEquals(rejected(Rejection.InvalidContents), interpretShortcut("x.steam", "0".toByteArray()))
    }

    @Test
    fun largestSignedIntIsAccepted() {
        assertEquals(request(Storefront.GOG, Int.MAX_VALUE), interpretFixture("Synthetic-int-max.gog"))
    }

    @Test
    fun byteOrderMarkAndSurroundingWhitespaceAreTolerated() {
        assertEquals(request(Storefront.GOG, 1470490225), interpretFixture("Synthetic-bom.gog"))
        assertEquals(request(Storefront.GOG, 1470490225), interpretFixture("Synthetic-crlf-blank-lines.gog"))
    }

    @Test
    fun shortcutsOverFourKiBAreTooLargeAndExactlyFourKiBIsNot() {
        assertEquals(rejected(Rejection.TooLarge), interpretFixture("Synthetic-oversized.gog"))
        assertEquals(rejected(Rejection.InvalidContents), interpretFixture("Synthetic-max-size.gog"))
    }
}
