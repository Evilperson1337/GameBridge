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
    fun anUnverifiedClientIsNeverShownAsVerified() {
        val unverified = StreamClient("Test", "test", "com.example.test", verifiedVersion = null)
        assertFalse(unverified.verified)
        assertEquals(listOf("Verified with GameNative 1.4.0", "Test: not verified yet"), verificationLines(listOf(unverified)))
    }
}
