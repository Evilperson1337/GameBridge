package com.allourmedia.gamebridge

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.allourmedia.gamebridge.core.RemoteRequest
import com.allourmedia.gamebridge.core.StreamClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The Dispatch template for streaming clients (ticket 04), for each client, independent of its `verified` flag. */
@RunWith(AndroidJUnit4::class)
class StreamDispatcherTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun request(client: StreamClient) =
        RemoteRequest(client, "11111111-2222-4333-8444-555555555555", "Synthetic Host", "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee", "Desktop")

    @Before
    fun setUp() = Intents.init()

    @After
    fun tearDown() = Intents.release()

    @Test
    fun arianeRequestGoesToArianesTrampolineOnly() {
        val ariane = ComponentName("com.limelight.ariane", "com.limelight.ShortcutTrampoline")
        intending(hasComponent(ariane)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
        assertEquals(StreamDispatchResult.Dispatched, StreamDispatcher(context).dispatch(request(StreamClient.Ariane)))
        val sent = Intents.getIntents().single()
        assertEquals(ariane, sent.component)
        assertEquals(Intent.ACTION_VIEW, sent.action)
        assertEquals(null, sent.data)
        assertEquals("Synthetic Host", sent.getStringExtra("Name"))
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, sent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun aMissingClientIsNotInstalledNeverTheOtherClient() {
        // Neither client is installed on the test emulator.
        for (client in StreamClient.all) {
            assertEquals(StreamDispatchResult.NotInstalled, StreamDispatcher(context).dispatch(request(client)))
        }
    }
}
