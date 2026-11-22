package com.allourmedia.gamebridge.core

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** The extension of a Bridge Shortcut: the file GameBridge creates and LaunchBox passes back (ADR 0002). */
const val BRIDGE_SHORTCUT_EXTENSION = "gamebridge"

/** The only format version this build reads and writes. A higher version is a [Rejection.NewerVersion]. */
const val BRIDGE_SHORTCUT_VERSION = 1

private const val KEY_VERSION = "version"
private const val KEY_TARGET = "target"
private const val KEY_STOREFRONT = "storefront"
private const val KEY_STORE_ID = "store_id"
private const val TARGET_GAMENATIVE = "gamenative"
private val GAMENATIVE_KEYS = setOf(KEY_VERSION, KEY_TARGET, KEY_STOREFRONT, KEY_STORE_ID)
private const val KEY_HOST_UUID = "host_uuid"
private const val KEY_HOST_NAME = "host_name"
private const val KEY_APP_UUID = "app_uuid"
private const val KEY_APP_NAME = "app_name"
private val REMOTE_KEYS = setOf(KEY_VERSION, KEY_TARGET, KEY_HOST_UUID, KEY_HOST_NAME, KEY_APP_UUID, KEY_APP_NAME)

/** How a Storefront is named inside a Bridge Shortcut: its lowercase name, independent of file extensions. */
private val Storefront.bridgeKey: String get() = name.lowercase()

/**
 * Interprets a Bridge Shortcut's raw bytes (already capped by the caller). The file carries data only; the Dispatch
 * template is fixed per Target App, so nothing in the file can name a component, action or extra.
 */
internal fun interpretBridgeShortcut(
    bytes: ByteArray,
    storefronts: List<Storefront>,
    clients: List<StreamClient>,
): ShortcutResult {
    val invalid = ShortcutResult.Rejected(Rejection.InvalidContents)
    val text = decodeUtf8(bytes)?.removePrefix(BYTE_ORDER_MARK) ?: return invalid
    val fields = readFlatJsonObject(text) ?: return invalid
    val version = (fields[KEY_VERSION] as? FlatValue.Integer)?.toIntOrNull() ?: return invalid
    if (version > BRIDGE_SHORTCUT_VERSION) return ShortcutResult.Rejected(Rejection.NewerVersion)
    if (version < BRIDGE_SHORTCUT_VERSION) return invalid
    val target = (fields[KEY_TARGET] as? FlatValue.Text)?.value ?: return invalid
    if (target == TARGET_GAMENATIVE) return interpretGameNative(fields, storefronts)
    val client = clients.firstOrNull { it.bridgeKey == target } ?: return invalid
    return interpretRemote(fields, client)
}

private fun interpretRemote(
    fields: Map<String, FlatValue>,
    client: StreamClient,
): ShortcutResult {
    // Unknown fields are rejected, not ignored, so a misread can't be silent.
    if (fields.keys != REMOTE_KEYS) return ShortcutResult.Rejected(Rejection.InvalidContents)

    fun text(key: String) = (fields[key] as? FlatValue.Text)?.value
    val request =
        remoteRequestOrNull(client, text(KEY_HOST_UUID), text(KEY_HOST_NAME), text(KEY_APP_UUID), text(KEY_APP_NAME))
            ?: return ShortcutResult.Rejected(Rejection.InvalidContents)
    if (!client.verified) return ShortcutResult.Rejected(Rejection.TargetNotVerified(client))
    return ShortcutResult.Remote(request)
}

private fun interpretGameNative(
    fields: Map<String, FlatValue>,
    storefronts: List<Storefront>,
): ShortcutResult {
    val invalid = ShortcutResult.Rejected(Rejection.InvalidContents)
    // Unknown fields are rejected, not ignored, so a misread can't be silent.
    if (fields.keys != GAMENATIVE_KEYS) return invalid
    val storefrontKey = (fields[KEY_STOREFRONT] as? FlatValue.Text)?.value ?: return invalid
    val storefront = storefronts.firstOrNull { it.bridgeKey == storefrontKey } ?: return invalid
    val storeId = (fields[KEY_STORE_ID] as? FlatValue.Integer)?.toIntOrNull()
    if (storeId == null || storeId <= 0) return invalid
    if (!storefront.verified) return ShortcutResult.Rejected(Rejection.StorefrontNotVerified(storefront))
    return ShortcutResult.Request(DispatchRequest(storefront, storeId))
}

/** The Bridge Shortcut text for a GameNative request. Stable key order, no whitespace, no title. */
fun writeBridgeShortcut(request: DispatchRequest): String =
    "{\"$KEY_VERSION\":$BRIDGE_SHORTCUT_VERSION,\"$KEY_TARGET\":\"$TARGET_GAMENATIVE\"," +
        "\"$KEY_STOREFRONT\":\"${request.storefront.bridgeKey}\",\"$KEY_STORE_ID\":${request.storeId}}"

/** The Bridge Shortcut text for a streamed game. Stable key order, no whitespace. */
fun writeBridgeShortcut(request: RemoteRequest): String =
    "{\"$KEY_VERSION\":$BRIDGE_SHORTCUT_VERSION,\"$KEY_TARGET\":\"${request.client.bridgeKey}\"," +
        "\"$KEY_HOST_UUID\":${jsonString(request.hostUuid)},\"$KEY_HOST_NAME\":${jsonString(request.hostName)}," +
        "\"$KEY_APP_UUID\":${jsonString(request.appUuid)},\"$KEY_APP_NAME\":${jsonString(request.appName)}}"

/** A JSON string literal. Requests never hold control characters, so only quote and backslash need escaping. */
private fun jsonString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

internal fun decodeUtf8(bytes: ByteArray): String? =
    try {
        Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }
