package com.allourmedia.gamebridge.core

/**
 * A Target App for streamed games. Each client keeps its own paired hosts, so a Bridge Shortcut names one explicitly
 * and GameBridge never sends it to the other. Dispatch reads only from [StreamClient.all].
 */
class StreamClient internal constructor(
    val name: String,
    /** How the client is named in a Bridge Shortcut's `target`. */
    val bridgeKey: String,
    val packageName: String,
    /** The client version a stream was recorded with in docs/investigation-log.md, or null when not verified. */
    val verifiedVersion: String?,
) {
    /** Dispatch to this client is enabled only once a version has been recorded. */
    val verified: Boolean get() = verifiedVersion != null

    override fun toString() = name

    /** The same row with Dispatch verified. Exists so tests can show that flipping a row is enough. */
    internal fun copyVerified() = StreamClient(name, bridgeKey, packageName, verifiedVersion = "test")

    /** The same row with Dispatch not verified. Exists so tests keep covering the not-verified path. */
    internal fun copyUnverified() = StreamClient(name, bridgeKey, packageName, verifiedVersion = null)

    companion object {
        /** Verified: a stream opened from this Dispatch template on the Odin (docs/investigation-log.md, ticket 04). */
        val Artemis = StreamClient("Artemis", "artemis", "com.limelight.noir", verifiedVersion = "20.2.6")

        /** A fork of Artemis. Verified: a stream started from a Bridge Shortcut through GameBridge (ticket 06). */
        val Ariane = StreamClient("Ariane", "ariane", "com.limelight.ariane", verifiedVersion = "1.1.0")
        val all: List<StreamClient> = listOf(Artemis, Ariane)
    }
}

/** A validated request to Dispatch one streamed game to [client]. UUIDs and names are checked, never trusted. */
data class RemoteRequest(
    val client: StreamClient,
    val hostUuid: String,
    val hostName: String,
    val appUuid: String,
    val appName: String,
)

/** Longest host or app name accepted. */
internal const val MAX_REMOTE_NAME = 256

private val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

internal fun isValidUuid(value: String): Boolean = UUID_PATTERN.matches(value)

internal fun isValidRemoteName(value: String): Boolean =
    value.isNotEmpty() && value.length <= MAX_REMOTE_NAME && value.none { it < ' ' || it == '\u007F' }

/** Builds a request from untrusted fields, or null when any field is invalid. */
internal fun remoteRequestOrNull(
    client: StreamClient,
    hostUuid: String?,
    hostName: String?,
    appUuid: String?,
    appName: String?,
): RemoteRequest? {
    if (hostUuid == null || hostName == null || appUuid == null || appName == null) return null
    if (!isValidUuid(hostUuid) || !isValidUuid(appUuid)) return null
    if (!isValidRemoteName(hostName) || !isValidRemoteName(appName)) return null
    return RemoteRequest(client, hostUuid, hostName, appUuid, appName)
}
