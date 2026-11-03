package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadinessTest {
    private fun line(
        gameNative: Boolean,
        artemis: Boolean,
        launchBox: Boolean,
    ) = readinessLine(gameNativeInstalled = gameNative, artemisInstalled = artemis, launchBoxInstalled = launchBox)

    @Test
    fun allInstalledSaysSo() {
        assertEquals("GameNative, Artemis and LaunchBox are installed", line(true, true, true))
    }

    @Test
    fun oneMissingNamesOnlyThatApp() {
        assertEquals("Artemis is not installed", line(true, false, true))
        assertEquals("GameNative is not installed", line(false, true, true))
        assertEquals("LaunchBox is not installed", line(true, true, false))
    }

    @Test
    fun twoMissingNamesBothInOrder() {
        assertEquals("GameNative and LaunchBox are not installed", line(false, true, false))
        assertEquals("Artemis and LaunchBox are not installed", line(true, false, false))
    }

    @Test
    fun noneInstalledNamesAllThree() {
        assertEquals("GameNative, Artemis and LaunchBox are not installed", line(false, false, false))
    }
}
