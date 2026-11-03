package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppStatusTest {
    @Test
    fun installedAppIsReportedAsInstalled() {
        assertEquals("Artemis: installed", appStatusLine("Artemis", installed = true))
    }

    @Test
    fun missingAppIsReportedAsNotInstalled() {
        assertEquals("LaunchBox: not installed", appStatusLine("LaunchBox", installed = false))
    }

    @Test
    fun gameNativeTextKeepsItsExistingWording() {
        assertEquals("GameNative: installed", appStatusLine("GameNative", installed = true))
        assertEquals("GameNative: not installed", appStatusLine("GameNative", installed = false))
    }
}
