package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ShortcutInterpreterTest {
    private fun fixture(name: String): ByteArray = File("../fixtures/$name").readBytes()

    @Test
    fun validGogShortcutProducesGogDispatchRequest() {
        val result = interpretShortcut("Akalabeth.gog", fixture("Akalabeth.gog"))
        assertEquals(
            ShortcutResult.Request(DispatchRequest(Storefront.GOG, 1207666073)),
            result,
        )
        assertEquals("GOG", Storefront.GOG.gameSource)
    }

    @Test
    fun extensionMatchIsCaseInsensitive() {
        val result = interpretShortcut("AKALABETH.GOG", "42".toByteArray())
        assertEquals(ShortcutResult.Request(DispatchRequest(Storefront.GOG, 42)), result)
    }

    @Test
    fun unknownExtensionIsRejected() {
        assertEquals(
            ShortcutResult.Rejected(Rejection.UnsupportedType),
            interpretShortcut("Akalabeth.txt", "42".toByteArray()),
        )
    }

    @Test
    fun invalidContentsAreRejected() {
        for (text in listOf("", "abc", "0", "-5", "2147483648", "1 2")) {
            assertEquals(
                text,
                ShortcutResult.Rejected(Rejection.InvalidContents),
                interpretShortcut("x.gog", text.toByteArray()),
            )
        }
    }

    @Test
    fun surroundingWhitespaceAndBomAreTolerated() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "\r\n 7 \r\n\r\n".toByteArray()
        assertEquals(
            ShortcutResult.Request(DispatchRequest(Storefront.GOG, 7)),
            interpretShortcut("x.gog", bytes),
        )
    }
}
