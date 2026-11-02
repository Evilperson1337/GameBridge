package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.DataInputStream

/** Placeholder JVM test: proves `:core` tests run and that bytecode targets Java 17. */
class ToolchainTest {
    @Test
    fun bytecodeTargetsJava17() {
        val resource = "/" + javaClass.name.replace('.', '/') + ".class"
        DataInputStream(javaClass.getResourceAsStream(resource)).use { input ->
            input.readInt() // magic number
            input.readUnsignedShort() // minor version
            val major = input.readUnsignedShort()
            assertEquals("class file major version (61 = Java 17)", 61, major)
        }
    }
}
