package com.allourmedia.gamebridge

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeInstrumentedTest {
    @Test
    fun appRunsUnderTheDebugApplicationId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.allourmedia.gamebridge.debug", context.packageName)
    }
}
