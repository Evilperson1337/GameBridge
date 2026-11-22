package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** The `.gamebridge` format (ADR 0002): data only, versioned, strict. */
class BridgeShortcutTest {
    private fun interpret(
        json: String,
        name: String = "Game.gamebridge",
        storefronts: List<Storefront> = Storefront.all,
    ) = interpretShortcut(name, json.toByteArray(Charsets.UTF_8), storefronts)

    private fun request(
        storefront: Storefront,
        id: Int,
    ) = ShortcutResult.Request(DispatchRequest(storefront, id))

    private fun rejected(reason: Rejection) = ShortcutResult.Rejected(reason)

    private fun gameNative(
        storefront: String = "steam",
        id: String = "274190",
    ) = """{"version":1,"target":"gamenative","storefront":"$storefront","store_id":$id}"""

    @Test
    fun gameNativeBridgeShortcutBecomesARequest() {
        assertEquals(request(Storefront.Steam, 274190), interpret(gameNative()))
    }

    @Test
    fun everyStorefrontIsNamedByItsLowercaseName() {
        val names =
            mapOf(
                "gog" to Storefront.GOG,
                "steam" to Storefront.Steam,
                "epic" to Storefront.Epic,
                "amazon" to Storefront.Amazon,
                "custom" to Storefront.Custom,
            )
        for ((key, storefront) in names) assertEquals(key, request(storefront, 5), interpret(gameNative(key, "5")))
    }

    @Test
    fun extensionMatchesCaseInsensitively() {
        assertEquals(request(Storefront.Steam, 274190), interpret(gameNative(), name = "GAME.GameBridge"))
    }

    @Test
    fun writerRoundTripsEveryStorefront() {
        for (storefront in Storefront.all) {
            for (id in listOf(1, 274190, Int.MAX_VALUE)) {
                val request = DispatchRequest(storefront, id)
                assertEquals(ShortcutResult.Request(request), interpret(writeBridgeShortcut(request)))
            }
        }
    }

    @Test
    fun writerOutputIsStable() {
        assertEquals(
            """{"version":1,"target":"gamenative","storefront":"steam","store_id":274190}""",
            writeBridgeShortcut(DispatchRequest(Storefront.Steam, 274190)),
        )
    }

    @Test
    fun whitespaceKeyOrderBomAndEscapesAreAccepted() {
        val json = "{\n  \"store_id\" : 7,\r\n  \"storefront\": \"st\\u0065am\",\t\"target\":\"gamenative\", \"version\": 1 }\n"
        assertEquals(request(Storefront.Steam, 7), interpret(json))
    }

    @Test
    fun newerVersionIsItsOwnFailure() {
        val json = """{"version":2,"target":"gamenative","storefront":"steam","store_id":1}"""
        assertEquals(rejected(Rejection.NewerVersion), interpret(json))
        // Even when the rest is unknown: a newer GameBridge may add targets and fields.
        assertEquals(rejected(Rejection.NewerVersion), interpret("""{"version":99,"target":"droiddeck","x":"y"}"""))
    }

    @Test
    fun invalidOrMissingVersionIsInvalid() {
        for (version in listOf("0", "-1", "\"1\"", "1.0", "99999999999999999999")) {
            assertEquals(
                version,
                rejected(Rejection.InvalidContents),
                interpret("""{"version":$version,"target":"gamenative","storefront":"steam","store_id":1}"""),
            )
        }
        assertEquals(rejected(Rejection.InvalidContents), interpret("""{"target":"gamenative","storefront":"steam","store_id":1}"""))
    }

    @Test
    fun unknownTargetStorefrontOrFieldIsRejected() {
        val cases =
            listOf(
                """{"version":1,"target":"artemis","storefront":"steam","store_id":1}""",
                """{"version":1,"target":"GameNative","storefront":"steam","store_id":1}""",
                """{"version":1,"target":"gamenative","storefront":"Steam","store_id":1}""",
                """{"version":1,"target":"gamenative","storefront":"itch","store_id":1}""",
                """{"version":1,"target":"gamenative","storefront":"steam","store_id":1,"title":"x"}""",
                """{"version":1,"target":"gamenative","storefront":"steam","store_id":1,"component":"a/.B"}""",
                """{"version":1,"target":"gamenative","store_id":1}""",
                """{"version":1,"target":"gamenative","storefront":"steam"}""",
                """{"version":1,"storefront":"steam","store_id":1}""",
            )
        for (json in cases) assertEquals(json, rejected(Rejection.InvalidContents), interpret(json))
    }

    @Test
    fun storeIdOutsidePositiveSigned32BitIsRejectedNotTruncated() {
        for (id in listOf("0", "-1", "2147483648", "4294967297", "99999999999999999999", "-0")) {
            assertEquals(id, rejected(Rejection.InvalidContents), interpret(gameNative(id = id)))
        }
        assertEquals(request(Storefront.Steam, Int.MAX_VALUE), interpret(gameNative(id = "2147483647")))
    }

    @Test
    fun storeIdMustBeAPlainJsonInteger() {
        for (id in listOf("\"5\"", "5.0", "5e0", "1E3", "+5", "05", "0x10", "true", "null", "[5]", "{}")) {
            assertEquals(id, rejected(Rejection.InvalidContents), interpret(gameNative(id = id)))
        }
    }

    @Test
    fun malformedJsonIsRejected() {
        val cases =
            listOf(
                "",
                "   ",
                "274190",
                "[]",
                "{",
                "{}",
                """{"version":1,"target":"gamenative","storefront":"steam","store_id":1,}""",
                """{"version":1,"target":"gamenative","storefront":"steam","store_id":1}{}""",
                """{"version":1,"target":"gamenative","storefront":"steam","store_id":1} x""",
                """{"version":1,"version":1,"target":"gamenative","storefront":"steam","store_id":1}""",
                """{'version':1}""",
                """{"version":1 "target":"gamenative"}""",
                """{"version":1,"target":"game\qnative","storefront":"steam","store_id":1}""",
                "{\"version\":1,\"target\":\"game\u0001native\",\"storefront\":\"steam\",\"store_id\":1}",
                """{"version":1,"target":"gamenative","storefront":"steam","store_id":1,"x":{"y":1}}""",
                """{"version":1,"target":"gamenative","storefront":"st\u00""",
            )
        for (json in cases) assertEquals(json, rejected(Rejection.InvalidContents), interpret(json))
    }

    @Test
    fun invalidUtf8IsRejected() {
        val bytes = byteArrayOf('{'.code.toByte(), 0xC3.toByte(), 0x28, '}'.code.toByte())
        assertEquals(rejected(Rejection.InvalidContents), interpretShortcut("Game.gamebridge", bytes))
    }

    @Test
    fun sizeLimitIsTheSameAsForLocalShortcuts() {
        val padded = gameNative().replace("{", "{" + " ".repeat(MAX_LOCAL_SHORTCUT_BYTES - gameNative().length))
        assertEquals(MAX_LOCAL_SHORTCUT_BYTES, padded.length)
        assertEquals(request(Storefront.Steam, 274190), interpret(padded))
        assertEquals(rejected(Rejection.TooLarge), interpret(" $padded"))
    }

    @Test
    fun deeplyNestedInputFailsWithoutCrashing() {
        assertEquals(rejected(Rejection.InvalidContents), interpret("""{"x":""" + "[".repeat(3000)))
    }

    @Test
    fun unverifiedStorefrontIsReportedAfterTheContentsAreValid() {
        val unverifiedSteam = Storefront("Steam", "steam", "STEAM", verified = false)
        val table = Storefront.all.map { if (it == Storefront.Steam) unverifiedSteam else it }
        assertEquals(rejected(Rejection.StorefrontNotVerified(unverifiedSteam)), interpret(gameNative(), storefronts = table))
        assertEquals(rejected(Rejection.InvalidContents), interpret(gameNative(id = "0"), storefronts = table))
    }

    @Test
    fun bridgeShortcutIsNeitherRemoteNorALegacyExtension() {
        assertEquals(false, isRemoteShortcut("Game.gamebridge"))
        assertEquals(rejected(Rejection.UnsupportedType), interpret(gameNative(), name = "Game.gamebridge.txt"))
    }

    @Test
    fun newerVersionFailureTellsThePlayerToUpdate() {
        assertEquals(
            "Game.gamebridge\n\nThis Shortcut was made by a newer GameBridge. Update GameBridge, then pick the game again.",
            Rejection.NewerVersion.toFailure("Game.gamebridge").message,
        )
    }
}
