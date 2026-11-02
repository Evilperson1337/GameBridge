package com.allourmedia.gamebridge

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BridgeActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val gameNative = ComponentName("app.gamenative", "app.gamenative.MainActivity")

    @Before
    fun setUp() {
        Intents.init()
        intending(hasAction(GAMENATIVE_ACTION)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun tearDown() = Intents.release()

    private fun fixtureUri(name: String): Uri = Uri.withAppendedPath(FixtureProvider.BASE, name)

    /** Dispatches only: Espresso-Intents also records the intents the tests themselves start. */
    private fun dispatches() = Intents.getIntents().filter { it.action == GAMENATIVE_ACTION }

    /**
     * Built the way LaunchBox's Custom Emulator (With Code) command builds it. The read-grant flag is set, but
     * it cannot be honoured: the test provider runs in the test APK's process and the test code in the app's uid,
     * so the provider is exported and the grant itself is not exercised.
     */
    private fun bridgeIntent(
        uri: Uri,
        activity: Class<*> = BridgeActivity::class.java,
        action: String = BRIDGE_ACTION,
    ) = Intent(action, uri)
        .setClassName(instrumentation.targetContext.packageName, activity.name)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    @Test
    fun gogShortcutIsDispatchedToGameNativeAndActivityFinishes() {
        ActivityScenario.launch<BridgeActivity>(bridgeIntent(fixtureUri("Akalabeth.gog"))).use { scenario ->
            intended(
                allOf(
                    hasAction(GAMENATIVE_ACTION),
                    hasComponent(gameNative),
                    hasExtra("app_id", 1207666073),
                    hasExtra("game_source", "GOG"),
                ),
            )
            val dispatched = dispatches().single()
            // app_id must be an Int (GameNative reads it with getIntExtra), and nothing else is sent.
            assertEquals(1207666073, dispatched.getIntExtra("app_id", -1))
            assertEquals(Integer::class.java, dispatched.extras!!.getSerializable("app_id")!!.javaClass)
            assertEquals(setOf("app_id", "game_source"), dispatched.extras!!.keySet())
            // The incoming URI and its grant are never forwarded.
            assertEquals(null, dispatched.data)
            assertEquals(0, dispatched.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun recreatingTheActivityNeverDispatchesASecondTime() {
        // The real activity finishes in onCreate and cannot be recreated, so use the debug-only subclass that stays.
        val intent = bridgeIntent(fixtureUri("Akalabeth.gog"), StayingBridgeActivity::class.java)
        ActivityScenario.launch<StayingBridgeActivity>(intent).use { scenario ->
            assertEquals(1, dispatches().size)
            scenario.recreate()
            assertEquals(1, dispatches().size)
            scenario.onActivity { it.finishForReal() }
        }
    }

    @Test
    fun wrongActionOrNonContentSchemeDispatchesNothing() {
        ActivityScenario
            .launch<BridgeActivity>(
                bridgeIntent(fixtureUri("Akalabeth.gog"), action = Intent.ACTION_VIEW),
            ).close()
        ActivityScenario.launch<BridgeActivity>(bridgeIntent(Uri.parse("file:///sdcard/Akalabeth.gog"))).close()
        assertEquals(0, dispatches().size)
    }

    private companion object {
        const val BRIDGE_ACTION = "com.allourmedia.gamebridge.action.DISPATCH"
        const val GAMENATIVE_ACTION = "app.gamenative.LAUNCH_GAME"
    }
}
