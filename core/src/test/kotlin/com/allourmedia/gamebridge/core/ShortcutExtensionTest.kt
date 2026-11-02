package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutExtensionTest {
    @Test
    fun extensionIsLowercasedAndTakenAfterTheLastDot() {
        assertEquals("art", shortcutExtension("My.Game.ART"))
        assertEquals("steam", shortcutExtension("x.steam"))
    }

    @Test
    fun noDotMeansNoExtension() {
        assertEquals("", shortcutExtension("noextension"))
    }
}
