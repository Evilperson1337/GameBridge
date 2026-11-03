package com.allourmedia.gamebridge

import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Seam 2: the setup screen the player sees when opening GameBridge from the app drawer. */
@RunWith(AndroidJUnit4::class)
class SetupActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun launch() = ActivityScenario.launch(SetupActivity::class.java)

    /** The Setup command as an independent literal for the installed build. */
    private val expectedCommand =
        "-n ${context.packageName}/com.allourmedia.gamebridge.BridgeActivity " +
            "-a com.allourmedia.gamebridge.action.DISPATCH -d {file.uri}"

    @Test
    fun showsTheSetupCommandForTheInstalledBuild() {
        launch().use {
            onView(withText(expectedCommand)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun copyPutsExactlyTheSetupCommandOnTheClipboard() {
        launch().use {
            onView(withText("Copy")).perform(click())
            var copied: String? = null
            instrumentation.runOnMainSync {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                copied =
                    clipboard.primaryClip
                        ?.getItemAt(0)
                        ?.text
                        ?.toString()
            }
            assertEquals(expectedCommand, copied)
        }
    }

    @Test
    fun showsOneTileForEachAppWithItsNameAndStatus() {
        // The expected status per app comes from the device's own install state.
        val apps =
            listOf(
                "GameNative" to GameNativeDispatcher.PACKAGE,
                "Artemis" to ArtemisDispatcher.PACKAGE,
                "LaunchBox" to LAUNCHBOX_PACKAGE,
            )
        launch().use { scenario ->
            for ((name, pkg) in apps) {
                val installed = isPackageInstalled(context.packageManager, pkg)
                val description = if (installed) "$name: installed" else "$name: not installed"
                var tiles = 0
                var tileText = emptyList<String>()
                scenario.onActivity { activity ->
                    val tile =
                        allViews(activity.findViewById<View>(android.R.id.content))
                            .filterIsInstance<AppTile>()
                            .filter { it.contentDescription == description }
                    tiles = tile.size
                    tileText =
                        tile.firstOrNull()?.let { t -> allViews(t).filterIsInstance<TextView>().map { it.text.toString() } }.orEmpty()
                }
                assertEquals("one tile for $name", 1, tiles)
                assertTrue("$name tile shows its name: $tileText", name in tileText)
                assertTrue("$name tile shows its status: $tileText", (if (installed) "Installed" else "Not installed") in tileText)
            }
        }
    }

    @Test
    fun installedTileShowsTheAppsRealIconAndNoPlaceholder() {
        // The test app's own package stands in for an installed app.
        launch().use { scenario ->
            var tile: AppTile? = null
            scenario.onActivity { tile = AppTile(it, "Stub", context.packageName) }
            val shown = tile!!
            assertNotNull("icon drawable", shown.iconView?.drawable)
            assertEquals(null, shown.placeholderView)
            assertEquals("Stub: installed", shown.contentDescription)
            assertTrue(allViews(shown).filterIsInstance<TextView>().any { it.text == "Installed" })
        }
    }

    @Test
    fun missingTileShowsThePlaceholderLetterAndNoIcon() {
        launch().use { scenario ->
            var tile: AppTile? = null
            scenario.onActivity { tile = AppTile(it, "Nothing", "com.allourmedia.gamebridge.nonexistent") }
            val shown = tile!!
            assertEquals(null, shown.iconView)
            assertEquals("N", shown.placeholderView?.text?.toString())
            assertEquals("Nothing: not installed", shown.contentDescription)
            assertTrue(allViews(shown).filterIsInstance<TextView>().any { it.text == "Not installed" })
        }
    }

    private fun allViews(root: View): List<View> =
        listOf(root) + if (root is ViewGroup) (0 until root.childCount).flatMap { allViews(root.getChildAt(it)) } else emptyList()

    @Test
    fun installedCheckSeesARegisteredPackageAndNotAMissingOne() {
        // The test app's own package is registered, so it stands in for an installed app.
        assertEquals(true, isPackageInstalled(context.packageManager, context.packageName))
        assertEquals(false, isPackageInstalled(context.packageManager, "com.allourmedia.gamebridge.nonexistent"))
    }

    @Test
    fun copyLabelReadsCopiedAfterwardsThenReturnsToCopy() {
        launch().use { scenario ->
            onView(withText("Copy")).perform(click())
            onView(withText("Copied")).check(matches(isDisplayed()))
            val deadline = System.currentTimeMillis() + 5_000
            var label = ""
            while (System.currentTimeMillis() < deadline) {
                scenario.onActivity { label = findCopyButton(it)?.text?.toString().orEmpty() }
                if (label == "Copy") break
                Thread.sleep(100)
            }
            assertEquals("Copy", label)
        }
    }

    @Test
    fun copyHasFocusWhenTheScreenOpens() {
        launch().use { scenario ->
            var focused = false
            scenario.onActivity { focused = findCopyButton(it)?.hasFocus() == true }
            assertTrue("Copy should hold focus on open", focused)
        }
    }

    @Test
    fun everyInteractiveViewIsAtLeast48dpTall() {
        launch().use { scenario ->
            val tooSmall = mutableListOf<String>()
            scenario.onActivity { activity ->
                val minPx = 48 * activity.resources.displayMetrics.density

                fun walk(view: View) {
                    if ((view.isClickable || view.isFocusable) && view !is ViewGroup && view.height < minPx - 1) {
                        tooSmall += "${view.javaClass.simpleName} ${view.height}px"
                    }
                    if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
                }
                walk(activity.findViewById<View>(android.R.id.content))
            }
            assertTrue("interactive views under 48dp: $tooSmall", tooSmall.isEmpty())
        }
    }

    @Test
    fun showsTheReadinessLineNamingWhatIsMissing() {
        // Independent literals per known device state; other states are skipped, not recomputed.
        val installed =
            setOf(GameNativeDispatcher.PACKAGE, ArtemisDispatcher.PACKAGE, LAUNCHBOX_PACKAGE)
                .filter { isPackageInstalled(context.packageManager, it) }
                .toSet()
        val expected =
            when (installed) {
                emptySet<String>() -> {
                    "GameNative, Artemis and LaunchBox are not installed"
                }

                setOf(GameNativeDispatcher.PACKAGE, ArtemisDispatcher.PACKAGE, LAUNCHBOX_PACKAGE) -> {
                    "GameNative, Artemis and LaunchBox are installed"
                }

                else -> {
                    null
                }
            }
        assumeTrue("device has a mixed install state", expected != null)
        launch().use { onView(withText(expected!!)).check(matches(isDisplayed())) }
    }

    private fun findCopyButton(activity: android.app.Activity): android.widget.Button? {
        fun walk(view: View): android.widget.Button? {
            if (view is android.widget.Button) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))?.let { return it }
            return null
        }
        return walk(activity.findViewById<View>(android.R.id.content))
    }

    @Test
    fun theAppDrawerEntryIsTheSetupScreenNotTheBridgeActivity() {
        val drawer =
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val entries = context.packageManager.queryIntentActivities(drawer, 0).map { it.activityInfo.name }
        assertEquals(listOf("com.allourmedia.gamebridge.SetupActivity"), entries)
    }

    /**
     * A Dispatch from LaunchBox joins GameBridge's existing task. If the setup screen were still in it,
     * the task would survive and closing the game would show the setup screen instead of LaunchBox.
     */
    @Test
    fun leavingTheSetupScreenRemovesItFromTheBackStack() {
        val info = context.packageManager.getActivityInfo(ComponentName(context, SetupActivity::class.java), 0)
        assertEquals(ActivityInfo.FLAG_NO_HISTORY, info.flags and ActivityInfo.FLAG_NO_HISTORY)
    }

    @Test
    fun showsTheThreeLineChecklist() {
        launch().use {
            onView(withText("1. Set the Windows platform's default emulator to Custom Emulator (With Code).")).check(matches(isDisplayed()))
            onView(withText("2. Paste this Setup command:")).check(matches(isDisplayed()))
            onView(withText("3. Clear any per-game emulator override.")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun displayedActionAndComponentResolveToTheManifestsBridgeActivity() {
        launch().use { scenario ->
            var shown = ""
            scenario.onActivity { shown = it.displayedCommand() }
            val words = shown.split(" ")
            val component = ComponentName.unflattenFromString(words[words.indexOf("-n") + 1])
            val action = words[words.indexOf("-a") + 1]
            // The intent LaunchBox builds from the command: explicit component, action, content:// data.
            val intent = Intent(action).setComponent(component).setData(Uri.parse("content://example/Game.gog"))
            val resolved = context.packageManager.resolveActivity(intent, 0)
            assertNotNull("the displayed command does not resolve to an activity", resolved)
            assertEquals(context.packageName, resolved!!.activityInfo.packageName)
            assertEquals("com.allourmedia.gamebridge.BridgeActivity", resolved.activityInfo.name)
        }
    }
}
