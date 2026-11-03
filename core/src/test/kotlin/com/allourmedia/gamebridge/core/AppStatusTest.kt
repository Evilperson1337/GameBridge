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
    fun tileStatusTextIsCapitalised() {
        assertEquals("Installed", appTileStatus(installed = true))
        assertEquals("Not installed", appTileStatus(installed = false))
    }

    @Test
    fun tilePlaceholderLetterIsTheFirstLetterOfTheName() {
        assertEquals("G", appTileInitial("GameNative"))
        assertEquals("A", appTileInitial("Artemis"))
        assertEquals("L", appTileInitial("LaunchBox"))
    }

    @Test
    fun gameNativeTextKeepsItsExistingWording() {
        assertEquals("GameNative: installed", appStatusLine("GameNative", installed = true))
        assertEquals("GameNative: not installed", appStatusLine("GameNative", installed = false))
    }
}
