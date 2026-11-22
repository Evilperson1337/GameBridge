package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Each Target App is verified with a recorded version, and the setup screen says which (ticket 06). */
class VerificationTest {
    @Test
    fun aClientIsVerifiedExactlyWhenItHasARecordedVersion() {
        for (client in StreamClient.all) {
            assertEquals(client.name, client.verifiedVersion != null, client.verified)
        }
        assertEquals("20.2.6", StreamClient.Artemis.verifiedVersion)
        assertTrue(StreamClient.Ariane.copyVerified().verified)
    }

    @Test
    fun gameNativeIsVerifiedWithTheRecordedVersion() {
        assertEquals("1.4.0", GAMENATIVE_VERIFIED_VERSION)
    }

    @Test
    fun verificationLinesNameTheVersionOrSayNotYet() {
        val ariane = StreamClient.Ariane
        val lines = verificationLines(listOf(StreamClient.Artemis, ariane))
        assertEquals("Verified with GameNative 1.4.0", lines[0])
        assertEquals("Verified with Artemis 20.2.6", lines[1])
        if (ariane.verified) {
            assertEquals("Verified with Ariane ${ariane.verifiedVersion}", lines[2])
        } else {
            assertEquals("Ariane: not verified yet", lines[2])
        }
        assertEquals(3, lines.size)
    }

    @Test
    fun readmeBridgeShortcutTableMatchesTheRecordedVersions() {
        val rows = readmeRows()
        assertTrue("GameNative row", rows.any { it.startsWith("| GameNative | $GAMENATIVE_VERIFIED_VERSION |") })
        for (client in StreamClient.all) {
            val prefix = "| ${client.name} (`${client.packageName}`) |"
            val row = rows.singleOrNull { it.startsWith(prefix) }
            val expected = client.verifiedVersion ?: "Not verified"
            assertEquals(client.name, "$prefix $expected |", row?.let { it.substring(0, it.indexOf('|', prefix.length) + 1) })
        }
    }

    private fun readmeRows(): List<String> {
        val lines = java.io.File("../README.md").readLines()
        return lines.filter { it.startsWith("| ") }
    }

    @Test
    fun anUnverifiedClientIsNeverShownAsVerified() {
        val unverified = StreamClient("Test", "test", "com.example.test", verifiedVersion = null)
        assertFalse(unverified.verified)
        assertEquals(listOf("Verified with GameNative 1.4.0", "Test: not verified yet"), verificationLines(listOf(unverified)))
    }
}
