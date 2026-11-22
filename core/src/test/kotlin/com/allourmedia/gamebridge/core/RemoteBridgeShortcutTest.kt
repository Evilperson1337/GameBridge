package com.allourmedia.gamebridge.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** The Artemis and Ariane targets of the `.gamebridge` format (ADR 0002, ticket 05). Placeholder UUIDs only. */
class RemoteBridgeShortcutTest {
    private val hostUuid = "11111111-2222-4333-8444-555555555555"
    private val appUuid = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"

    private fun json(
        target: String = "artemis",
        hostUuid: String = this.hostUuid,
        hostName: String = "Living Room PC",
        appUuid: String = this.appUuid,
        appName: String = "Desktop",
        extra: String = "",
    ) =
        """{"version":1,"target":"$target","host_uuid":"$hostUuid","host_name":"$hostName","app_uuid":"$appUuid","app_name":"$appName"$extra}"""

    private fun interpret(
        text: String,
        clients: List<StreamClient> = StreamClient.all,
    ) = interpretShortcut("Desktop.gamebridge", text.toByteArray(Charsets.UTF_8), clients = clients)

    private fun remote(client: StreamClient) = ShortcutResult.Remote(RemoteRequest(client, hostUuid, "Living Room PC", appUuid, "Desktop"))

    private val invalid = ShortcutResult.Rejected(Rejection.InvalidContents)

    private val bothVerified = listOf(StreamClient.Artemis, StreamClient.Ariane.copyVerified())

    @Test
    fun artemisBridgeShortcutBecomesARemoteRequestForArtemis() {
        assertEquals(remote(StreamClient.Artemis), interpret(json()))
    }

    @Test
    fun arianeBridgeShortcutBecomesARemoteRequestForAriane() {
        assertEquals(remote(bothVerified[1]), interpret(json(target = "ariane"), bothVerified))
    }

    @Test
    fun bothClientsAreVerifiedWithTheirRecordedVersions() {
        assertEquals(listOf("Artemis" to "20.2.6", "Ariane" to "1.1.0"), StreamClient.all.map { it.name to it.verifiedVersion })
        assertEquals("com.limelight.noir", StreamClient.Artemis.packageName)
        assertEquals("com.limelight.ariane", StreamClient.Ariane.packageName)
    }

    @Test
    fun unverifiedClientIsReportedAfterTheContentsAreValid() {
        val unverified = StreamClient.Ariane.copyUnverified()
        val clients = listOf(StreamClient.Artemis, unverified)
        assertEquals(
            ShortcutResult.Rejected(Rejection.TargetNotVerified(unverified)),
            interpret(json(target = "ariane"), clients),
        )
        assertEquals(invalid, interpret(json(target = "ariane", hostUuid = "nope"), clients))
    }

    @Test
    fun uppercaseUuidsAreAccepted() {
        val upper = interpret(json(hostUuid = hostUuid.uppercase(), appUuid = appUuid.uppercase()))
        assertEquals(
            remote(StreamClient.Artemis).request.copy(hostUuid = hostUuid.uppercase(), appUuid = appUuid.uppercase()),
            (upper as ShortcutResult.Remote).request,
        )
    }

    @Test
    fun malformedUuidsAreRejected() {
        val bad =
            listOf(
                "",
                "nope",
                "11111111222243338444555555555555",
                "11111111-2222-4333-8444-55555555555",
                "g1111111-2222-4333-8444-555555555555",
                " $hostUuid",
            )
        for (uuid in bad) {
            assertEquals(uuid, invalid, interpret(json(hostUuid = uuid)))
            assertEquals(uuid, invalid, interpret(json(appUuid = uuid)))
        }
    }

    @Test
    fun namesMustBeNonEmptyShortAndFreeOfControlCharacters() {
        for (name in listOf("", "a".repeat(257), "Tab\\there", "Line\\nbreak", "Del\\u007f")) {
            assertEquals(name, invalid, interpret(json(hostName = name)))
            assertEquals(name, invalid, interpret(json(appName = name)))
        }
        val ok = interpret(json(appName = "a".repeat(256))) as ShortcutResult.Remote
        assertEquals(256, ok.request.appName.length)
    }

    @Test
    fun missingOrExtraFieldsAndGameNativeFieldsAreRejected() {
        assertEquals(invalid, interpret(json(extra = ""","app_id":"5"""")))
        assertEquals(invalid, interpret(json(extra = ""","storefront":"steam"""")))
        assertEquals(
            invalid,
            interpret("""{"version":1,"target":"artemis","host_uuid":"$hostUuid","app_uuid":"$appUuid","app_name":"Desktop"}"""),
        )
        assertEquals(
            invalid,
            interpret(
                """{"version":1,"target":"gamenative","host_uuid":"$hostUuid","host_name":"x","app_uuid":"$appUuid","app_name":"Desktop"}""",
            ),
        )
    }

    @Test
    fun writerRoundTripsAndEscapesNames() {
        for (client in bothVerified) {
            val request = RemoteRequest(client, hostUuid, "Josh's \"PC\" \\ Den", appUuid, "Cuphead: 'Don't Deal'")
            assertEquals(ShortcutResult.Remote(request), interpret(writeBridgeShortcut(request), bothVerified))
        }
    }

    @Test
    fun writerOutputIsStable() {
        assertEquals(
            json(),
            writeBridgeShortcut(RemoteRequest(StreamClient.Artemis, hostUuid, "Living Room PC", appUuid, "Desktop")),
        )
    }
}
