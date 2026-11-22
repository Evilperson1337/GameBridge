package com.allourmedia.gamebridge.core

/** The extension of a Remote Shortcut exported by Artemis or Ariane. */
const val ART_EXTENSION = "art"

/**
 * Reads a `.art` export the way Artemis's `ShortcutTrampoline` does (research 04): blank lines and lines starting
 * with `#` are skipped; every other line must be `[key] value`, split on the first space; a later key wins.
 * Returns null for invalid UTF-8 or any malformed line. Callers have already capped the bytes.
 */
internal fun readArtFields(bytes: ByteArray): Map<String, String>? {
    val text = decodeUtf8(bytes)?.removePrefix(BYTE_ORDER_MARK) ?: return null
    val fields = mutableMapOf<String, String>()
    for (raw in text.split('\n')) {
        val line = raw.trimEnd('\r').trim()
        if (line.isEmpty() || line.startsWith("#")) continue
        if (!line.startsWith("[")) return null
        val space = line.indexOf(' ')
        val keyPart = if (space < 0) line else line.substring(0, space)
        if (!keyPart.endsWith("]") || keyPart.length < 3) return null
        fields[keyPart.substring(1, keyPart.length - 1)] = if (space < 0) "" else line.substring(space + 1).trim()
    }
    return fields
}

/** Interprets a `.art` export found in [client]'s Shortcut Folder as a request for that client. */
internal fun interpretArtShortcut(
    bytes: ByteArray,
    client: StreamClient,
): ShortcutResult {
    if (bytes.size > MAX_LOCAL_SHORTCUT_BYTES) return ShortcutResult.Rejected(Rejection.TooLarge)
    val fields = readArtFields(bytes) ?: return ShortcutResult.Rejected(Rejection.InvalidContents)
    val request =
        remoteRequestOrNull(client, fields["host_uuid"], fields["host_name"], fields["app_uuid"], fields["app_name"])
            ?: return ShortcutResult.Rejected(Rejection.InvalidContents)
    if (!client.verified) return ShortcutResult.Rejected(Rejection.TargetNotVerified(client))
    return ShortcutResult.Remote(request)
}
