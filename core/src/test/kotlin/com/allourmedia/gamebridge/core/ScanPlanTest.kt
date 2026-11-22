package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Seam 1: planning a Scan of the GameNative Shortcut Folder into the Output Folder (ADR 0003). */
class ScanPlanTest {
    private fun source(
        name: String,
        text: String,
    ) = SourceShortcut(name, text.toByteArray(Charsets.UTF_8))

    private val broforceSteam = """{"version":1,"target":"gamenative","storefront":"steam","store_id":274190}"""

    @Test
    fun gameNativeExportBecomesABridgeShortcutNamedAfterItsStem() {
        val plan = planScan(listOf(source("Broforce.steam", "274190")), emptyMap())
        assertEquals(listOf(PlannedFile("Broforce.gamebridge", broforceSteam, "Broforce.steam")), plan.writes)
    }

    @Test
    fun invalidExportsAreRejectedByNameAndReasonAndGetNoFile() {
        val plan =
            planScan(
                listOf(source("Zero.gog", "0"), source("Words.steam", "not a number"), source("Big.epic", "1".repeat(5000))),
                emptyMap(),
            )
        assertEquals(emptyList<PlannedFile>(), plan.writes)
        assertEquals(
            listOf(
                ScanEntry("Zero.gog", "This Shortcut's contents aren't valid."),
                ScanEntry("Words.steam", "This Shortcut's contents aren't valid."),
                ScanEntry(
                    "Big.epic",
                    "This file is too large to be a Shortcut. Check that LaunchBox is passing the Shortcut, not another file.",
                ),
            ),
            plan.rejected,
        )
    }

    @Test
    fun filesThatAreNotGameNativeExportsAreIgnored() {
        val names = listOf("notes.txt", "Desktop.art", "Old.gamebridge", ".droiddeck-sync.json", "images", "README", "x.gog.bak")
        val plan = planScan(names.map { source(it, "1") }, emptyMap())
        assertEquals(ScanPlan(emptyList(), emptyList()), plan)
    }

    @Test
    fun exportsForAnUnverifiedStorefrontAreFoundButNotEnabled() {
        val unverifiedSteam = Storefront("Steam", "steam", "STEAM", verified = false)
        val table = Storefront.all.map { if (it == Storefront.Steam) unverifiedSteam else it }
        val plan = planScan(listOf(source("Broforce.steam", "274190"), source("Zero.steam", "0")), emptyMap(), table)
        assertEquals(emptyList<PlannedFile>(), plan.writes)
        assertEquals(listOf(ScanEntry("Broforce.steam", "Support for Steam games isn't verified yet.")), plan.notEnabled)
        // Invalid contents are still rejected, whatever the Storefront.
        assertEquals(listOf(ScanEntry("Zero.steam", "This Shortcut's contents aren't valid.")), plan.rejected)
    }

    @Test
    fun collidingStemsAllGetAStorefrontSuffixIgnoringCase() {
        val plan =
            planScan(
                listOf(source("Broforce.gog", "1207666073"), source("broforce.steam", "274190"), source("ABZU.epic", "64")),
                emptyMap(),
            )
        assertEquals(
            listOf("Broforce (GOG).gamebridge", "broforce (Steam).gamebridge", "ABZU.gamebridge"),
            plan.writes.map { it.name },
        )
    }

    @Test
    fun existingOutputFilesAreNeverOverwrittenWithoutConfirmation() {
        val existing =
            mapOf(
                "broforce.GAMEBRIDGE" to broforceSteam.toByteArray(),
                "ABZU.gamebridge" to "edited by hand".toByteArray(),
            )
        val plan = planScan(listOf(source("Broforce.steam", "274190"), source("ABZU.epic", "64"), source("Cuphead.gog", "5")), existing)
        assertEquals(listOf("Cuphead.gamebridge"), plan.writes.map { it.name })
        assertEquals(listOf("broforce.GAMEBRIDGE"), plan.unchanged)
        assertEquals(
            listOf(
                PlannedFile("ABZU.gamebridge", """{"version":1,"target":"gamenative","storefront":"epic","store_id":64}""", "ABZU.epic"),
            ),
            plan.needsConfirmation,
        )
    }

    @Test
    fun summaryNamesFilesAndReasonsButNeverContents() {
        val plan =
            ScanPlan(
                writes = listOf(PlannedFile("Cuphead.gamebridge", "SECRET-CONTENT", "Cuphead.gog")),
                rejected = listOf(ScanEntry("Zero.gog", "This Shortcut's contents aren't valid.")),
                notEnabled = listOf(ScanEntry("Broforce.steam", "Support for Steam games isn't verified yet.")),
                unchanged = listOf("Portal.gamebridge"),
                needsConfirmation = listOf(PlannedFile("ABZU.gamebridge", "SECRET-CONTENT", "ABZU.epic")),
            )
        assertEquals(
            """
            Added 1, unchanged 1, rejected 1, not enabled 1.
            Already in the Output Folder with different contents (1):
            ABZU.gamebridge
            Rejected:
            Zero.gog: This Shortcut's contents aren't valid.
            Found, not enabled:
            Broforce.steam: Support for Steam games isn't verified yet.
            """.trimIndent(),
            scanSummary(plan),
        )
    }

    @Test
    fun plannedFilesAreBridgeShortcutsThatDispatchTheSameGame() {
        val exports = listOf(source("Akalabeth.gog", "1207666073"), source("Synthetic.pcgame", "2342"))
        val dispatched = planScan(exports, emptyMap()).writes.map { interpretShortcut(it.name, it.content.toByteArray()) }
        assertEquals(
            listOf(
                ShortcutResult.Request(DispatchRequest(Storefront.GOG, 1207666073)),
                ShortcutResult.Request(DispatchRequest(Storefront.Custom, 2342)),
            ),
            dispatched,
        )
    }

    @Test
    fun summaryOfAnEmptyScanSaysNothingWasFound() {
        assertEquals("No GameNative Shortcuts found.", scanSummary(planScan(emptyList(), emptyMap())))
    }

    @Test
    fun summaryListsFilesThatCouldNotBeWritten() {
        val plan =
            ScanPlan(
                writes = listOf(PlannedFile("A.gamebridge", "x", "A.gog"), PlannedFile("B.gamebridge", "x", "B.gog")),
                rejected = emptyList(),
            )
        assertEquals(
            "Added 1, unchanged 0, rejected 0, not enabled 0.\nCouldn't write:\nB.gamebridge",
            scanSummary(plan, failedWrites = listOf("B.gamebridge")),
        )
    }
}
