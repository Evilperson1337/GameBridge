package com.allourmedia.gamebridge

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.allourmedia.gamebridge.core.BridgeFailure
import com.allourmedia.gamebridge.core.FailureCategory
import com.allourmedia.gamebridge.core.Storefront
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Seam 2: the whole bridge activity. Intents are started the way LaunchBox starts them: by another package
 * ([GrantTrampolineActivity], which owns the unexported [FixtureProvider]) with a real URI read grant.
 */
@RunWith(AndroidJUnit4::class)
class BridgeActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetPackage = instrumentation.targetContext.packageName
    private val gameNative = ComponentName(GameNativeDispatcher.PACKAGE, "${GameNativeDispatcher.PACKAGE}.MainActivity")
    private val seen = CopyOnWriteArrayList<Activity>()
    private val callback = ActivityLifecycleCallback { activity, _ -> if (activity !in seen) seen.add(activity) }

    @Before
    fun setUp() {
        Intents.init()
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(callback)
    }

    @After
    fun tearDown() {
        ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(callback)
        instrumentation.runOnMainSync {
            seen.filterIsInstance<StayingBridgeActivity>().forEach { it.finishForReal() }
            seen.filter { it !is StayingBridgeActivity }.forEach { it.finish() }
        }
        Intents.release()
    }

    /** Stubs GameNative's side: the Dispatch is captured and answered, GameNative itself is not involved. */
    private fun stubGameNative() {
        intending(hasAction(GameNativeDispatcher.ACTION)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    private fun fixtureUri(name: String): Uri = Uri.withAppendedPath(FixtureProvider.BASE, name)

    /** Dispatches only: Espresso-Intents also records the intents the tests themselves start. */
    private fun dispatches() = Intents.getIntents().filter { it.action == GameNativeDispatcher.ACTION }

    private fun bridgeIntent(
        uri: Uri?,
        activity: Class<*> = BridgeActivity::class.java,
        action: String? = BridgeActivity.ACTION,
    ) = Intent(action).setClassName(targetPackage, activity.name).also { it.data = uri }

    /** Starts [intent] through the stand-in for LaunchBox and returns the bridge activity that appears. */
    private fun start(
        intent: Intent,
        grant: String = "grant",
    ): Activity {
        if (grant == "direct") {
            // A file: URI cannot go through another package (FileUriExposedException), and needs no grant anyway.
            return instrumentation.startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        val context = instrumentation.context
        context.startActivity(
            Intent(context, GrantTrampolineActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(GrantTrampolineActivity.EXTRA_FORWARD, intent)
                .putExtra(GrantTrampolineActivity.EXTRA_GRANT, grant),
        )
        // Match on class and data: a previous test's activity can still report DESTROYED to the callback after setUp.
        return await("a bridge activity to start") {
            seen.firstOrNull { it is BridgeActivity && it.javaClass.name == intent.component?.className && it.intent?.data == intent.data }
        }
    }

    private fun <T : Any> await(
        what: String,
        block: () -> T?,
    ): T {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            block()?.let { return it }
            Thread.sleep(50)
        }
        throw AssertionError("Timed out waiting for $what")
    }

    /** Taps Close once the dialog has settled: on a cold emulator the first frame can take over a second. */
    private fun tapClose() {
        Thread.sleep(SETTLE_MS)
        instrumentation.waitForIdleSync()
        onView(withText("Close")).inRoot(isDialog()).perform(click())
    }

    private fun awaitFinished(activity: Activity) = await("the activity to finish") { activity.takeIf { it.isFinishing || it.isDestroyed } }

    private fun assertDialog(failure: BridgeFailure) {
        onView(withText(failure.message)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Close")).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    private fun failureDialogFor(
        intent: Intent,
        failure: BridgeFailure,
        grant: String = "grant",
        dispatchAttempts: Int = 0,
    ): Activity {
        val activity = start(intent, grant)
        assertDialog(failure)
        assertEquals(dispatchAttempts, dispatches().size)
        return activity
    }

    /** A launcher-style start with no data at all: the app drawer sends MAIN. */
    private fun startWithoutShortcut() = start(bridgeIntent(null, action = Intent.ACTION_MAIN), grant = "none")

    private val noShortcut = BridgeFailure(FailureCategory.NoShortcut)

    @Test
    fun gogShortcutIsDispatchedToGameNativeAndActivityFinishes() {
        stubGameNative()
        val activity = start(bridgeIntent(fixtureUri("Akalabeth.gog")))
        awaitFinished(activity)
        intended(
            allOf(
                hasAction(GameNativeDispatcher.ACTION),
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
    }

    @Test
    fun recreatingTheActivityNeverDispatchesASecondTime() {
        stubGameNative()
        // The real activity finishes in onCreate and cannot be recreated, so use the debug-only subclass that stays.
        val activity = start(bridgeIntent(fixtureUri("Akalabeth.gog"), StayingBridgeActivity::class.java))
        assertEquals(1, dispatches().size)
        instrumentation.runOnMainSync { activity.recreate() }
        await("recreation") { seen.firstOrNull { it !== activity && it is StayingBridgeActivity } }
        instrumentation.waitForIdleSync()
        assertEquals(1, dispatches().size)
    }

    @Test
    fun startedWithoutDataExplainsItIsStartedFromLaunchBox() {
        failureDialogFor(bridgeIntent(null, action = Intent.ACTION_MAIN), noShortcut, grant = "none")
    }

    @Test
    fun startedWithItsOwnActionButNoDataExplainsItIsStartedFromLaunchBox() {
        failureDialogFor(bridgeIntent(null), noShortcut, grant = "none")
    }

    @Test
    fun wrongActionShowsItsOwnReason() {
        failureDialogFor(
            bridgeIntent(fixtureUri("Akalabeth.gog"), action = Intent.ACTION_VIEW),
            BridgeFailure(FailureCategory.WrongAction),
        )
    }

    @Test
    fun nonContentSchemeShowsItsOwnReason() {
        failureDialogFor(
            bridgeIntent(Uri.parse("file:///sdcard/Akalabeth.gog")),
            BridgeFailure(FailureCategory.NotContentUri),
            grant = "direct",
        )
    }

    @Test
    fun missingGrantShowsTheShortcutCouldNotBeRead() {
        failureDialogFor(bridgeIntent(fixtureUri("Akalabeth.gog")), BridgeFailure(FailureCategory.Unreadable), grant = "none")
    }

    @Test
    fun revokedGrantShowsTheShortcutCouldNotBeRead() {
        failureDialogFor(bridgeIntent(fixtureUri("Akalabeth.gog")), BridgeFailure(FailureCategory.Unreadable), grant = "revoke")
    }

    @Test
    fun missingFileShowsTheFilenameAndTheShortcutCouldNotBeRead() {
        failureDialogFor(bridgeIntent(fixtureUri("Missing.gog")), BridgeFailure(FailureCategory.Unreadable, "Missing.gog"))
    }

    @Test
    fun providerErrorShowsTheShortcutCouldNotBeRead() {
        failureDialogFor(bridgeIntent(fixtureUri("Error.gog")), BridgeFailure(FailureCategory.Unreadable))
    }

    @Test
    fun unreadableDisplayNameShowsTheShortcutCouldNotBeRead() {
        failureDialogFor(bridgeIntent(fixtureUri("NoName.gog")), BridgeFailure(FailureCategory.Unreadable))
    }

    @Test
    fun gameNativeNotInstalledShowsItsReasonFromARealUnstubbedDispatch() {
        // No stub: the emulator has no GameNative, so startActivity really throws ActivityNotFoundException.
        failureDialogFor(
            bridgeIntent(fixtureUri("Akalabeth.gog")),
            BridgeFailure(FailureCategory.GameNativeNotInstalled, "Akalabeth.gog"),
            dispatchAttempts = 1,
        )
        assertTrue(BridgeFailure(FailureCategory.GameNativeNotInstalled).reason.contains("github.com/utkarshdalal/GameNative/releases"))
    }

    @Test
    fun gameNativeRefusingShowsItsReason() {
        // Simulated refusal: a debug-only activity makes startActivity throw SecurityException.
        val intent = bridgeIntent(fixtureUri("Akalabeth.gog"), RefusingBridgeActivity::class.java)
        val activity = start(intent)
        assertDialog(BridgeFailure(FailureCategory.GameNativeRefused, "Akalabeth.gog"))
        tapClose()
        awaitFinished(activity)
    }

    private fun interpretationFailure(
        fileName: String,
        category: FailureCategory,
        storefront: Storefront? = null,
    ) = failureDialogFor(bridgeIntent(fixtureUri(fileName)), BridgeFailure(category, fileName, storefront))

    /**
     * Dispatches a Shortcut and checks the component, the Int app_id and the game_source. The "Storefront not verified"
     * dialog is unreachable from the real Storefront table now (every row is verified), so that path is covered in JVM only.
     */
    private fun assertDispatched(
        fileName: String,
        appId: Int,
        gameSource: String,
    ) {
        stubGameNative()
        awaitFinished(start(bridgeIntent(fixtureUri(fileName))))
        intended(
            allOf(
                hasAction(GameNativeDispatcher.ACTION),
                hasComponent(gameNative),
                hasExtra("app_id", appId),
                hasExtra("game_source", gameSource),
            ),
        )
        val dispatched = dispatches().single()
        assertEquals(Integer::class.java, dispatched.extras!!.getSerializable("app_id")!!.javaClass)
        assertEquals(setOf("app_id", "game_source"), dispatched.extras!!.keySet())
    }

    @Test
    fun steamShortcutIsDispatchedAsSteam() = assertDispatched("Broforce.steam", 274190, "STEAM")

    @Test
    fun epicShortcutIsDispatchedAsEpic() = assertDispatched("ABZU.epic", 64, "EPIC")

    @Test
    fun amazonShortcutIsDispatchedAsAmazon() = assertDispatched("Synthetic.amazon", 4815162, "AMAZON")

    @Test
    fun customShortcutIsDispatchedAsCustomGame() = assertDispatched("Synthetic.pcgame", 2342, "CUSTOM_GAME")

    @Test
    fun remoteShortcutShowsRemoteShortcutsAreUnsupported() {
        // The provider serves any .art name with empty contents: the real format is not known yet (Phase 3).
        interpretationFailure("Anything.art", FailureCategory.RemoteUnsupported)
    }

    @Test
    fun unknownExtensionShowsNotASupportedShortcut() {
        interpretationFailure("Synthetic-unknown.txt", FailureCategory.UnsupportedType)
    }

    @Test
    fun zeroStoreIdShowsInvalidContents() {
        interpretationFailure("Synthetic-zero.gog", FailureCategory.InvalidContents)
    }

    @Test
    fun outOfRangeStoreIdShowsInvalidContents() {
        interpretationFailure("Synthetic-above-int-max.gog", FailureCategory.InvalidContents)
    }

    @Test
    fun oversizedShortcutShowsTooLarge() {
        interpretationFailure("Synthetic-oversized.gog", FailureCategory.TooLarge)
    }

    @Test
    fun upperCaseExtensionIsDispatched() {
        stubGameNative()
        awaitFinished(start(bridgeIntent(fixtureUri("Synthetic-upper.GOG"))))
        assertEquals(1470490225, dispatches().single().getIntExtra("app_id", -1))
    }

    @Test
    fun shortcutWithBomAndBlankLinesIsDispatched() {
        stubGameNative()
        awaitFinished(start(bridgeIntent(fixtureUri("Synthetic-bom.gog"))))
        assertEquals(1470490225, dispatches().single().getIntExtra("app_id", -1))
    }

    @Test
    fun closeButtonFinishesTheActivity() {
        val activity = startWithoutShortcut()
        tapClose()
        awaitFinished(activity)
    }

    @Test
    fun backFinishesTheActivity() {
        val activity = startWithoutShortcut()
        assertDialog(noShortcut)
        pressBackUnconditionally()
        awaitFinished(activity)
    }

    @Test
    fun controllerAButtonClosesTheDialog() {
        val activity = startWithoutShortcut()
        assertDialog(noShortcut)
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_A)
        awaitFinished(activity)
    }

    @Test
    fun dpadCenterClosesTheDialog() {
        val activity = startWithoutShortcut()
        assertDialog(noShortcut)
        instrumentation.sendKeySync(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
        instrumentation.sendKeySync(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        awaitFinished(activity)
    }

    @Test
    fun controllerBButtonClosesTheDialog() {
        val activity = startWithoutShortcut()
        assertDialog(noShortcut)
        instrumentation.sendKeySync(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_B))
        instrumentation.sendKeySync(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_B))
        awaitFinished(activity)
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val SETTLE_MS = 1_500L
    }
}
