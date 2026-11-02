package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SetupCommandTest {
    @Test
    fun releaseBuildCommandIsExact() {
        assertEquals(
            "-n com.allourmedia.gamebridge/com.allourmedia.gamebridge.BridgeActivity " +
                "-a com.allourmedia.gamebridge.action.DISPATCH -d {file.uri}",
            setupCommand("com.allourmedia.gamebridge"),
        )
    }

    @Test
    fun debugBuildCommandIsExactAndKeepsTheUnsuffixedActivityClass() {
        assertEquals(
            "-n com.allourmedia.gamebridge.debug/com.allourmedia.gamebridge.BridgeActivity " +
                "-a com.allourmedia.gamebridge.action.DISPATCH -d {file.uri}",
            setupCommand("com.allourmedia.gamebridge.debug"),
        )
    }
}
