package com.allourmedia.gamebridge.core

/** One row per Storefront. Parsing, the extension allowlist and Dispatch read only from [Storefront.all]. */
class Storefront internal constructor(
    val name: String,
    val extension: String,
    val gameSource: String,
    val verified: Boolean,
) {
    override fun toString() = name

    /** The same row with Dispatch verified. Exists so tests can show that flipping a row is enough. */
    internal fun copyVerified() = Storefront(name, extension, gameSource, verified = true)

    companion object {
        val GOG = Storefront("GOG", "gog", "GOG", verified = true)
        val Steam = Storefront("Steam", "steam", "STEAM", verified = false)
        val Epic = Storefront("Epic", "epic", "EPIC", verified = false)
        val Amazon = Storefront("Amazon", "amazon", "AMAZON", verified = false)
        val Custom = Storefront("Custom", "pcgame", "CUSTOM_GAME", verified = false)
        val all: List<Storefront> = listOf(GOG, Steam, Epic, Amazon, Custom)
    }
}

/** A validated request to Dispatch one game. [storeId] is always a positive Int. */
data class DispatchRequest(
    val storefront: Storefront,
    val storeId: Int,
)

sealed interface Rejection {
    /** Unknown extension. */
    data object UnsupportedType : Rejection

    /** A Local Shortcut for a Storefront whose Dispatch has not been verified yet. */
    data class StorefrontNotVerified(
        val storefront: Storefront,
    ) : Rejection

    /** A Remote Shortcut (`.art`). */
    data object RemoteUnsupported : Rejection

    data object InvalidContents : Rejection

    data object TooLarge : Rejection
}

sealed interface ShortcutResult {
    data class Request(
        val request: DispatchRequest,
    ) : ShortcutResult

    data class Rejected(
        val reason: Rejection,
    ) : ShortcutResult
}

/** Local Shortcuts larger than this are rejected. Readers must stop at this size plus one byte. */
const val MAX_LOCAL_SHORTCUT_BYTES = 4096

private const val REMOTE_EXTENSION = "art"
private const val BYTE_ORDER_MARK = "﻿"

/**
 * Interprets a Shortcut from its display name (for the extension) and its raw bytes, which the caller has already
 * capped at [MAX_LOCAL_SHORTCUT_BYTES] plus one.
 */
fun interpretShortcut(
    displayName: String,
    bytes: ByteArray,
    storefronts: List<Storefront> = Storefront.all,
): ShortcutResult {
    val extension = displayName.substringAfterLast('.', "").lowercase()
    if (extension == REMOTE_EXTENSION) return ShortcutResult.Rejected(Rejection.RemoteUnsupported)
    val storefront =
        storefronts.firstOrNull { it.extension == extension }
            ?: return ShortcutResult.Rejected(Rejection.UnsupportedType)
    if (bytes.size > MAX_LOCAL_SHORTCUT_BYTES) return ShortcutResult.Rejected(Rejection.TooLarge)
    val text = bytes.toString(Charsets.UTF_8).removePrefix(BYTE_ORDER_MARK).trim()
    val id = if (text.isNotEmpty() && text.all { it in '0'..'9' }) text.toIntOrNull() else null
    if (id == null || id <= 0) return ShortcutResult.Rejected(Rejection.InvalidContents)
    if (!storefront.verified) return ShortcutResult.Rejected(Rejection.StorefrontNotVerified(storefront))
    return ShortcutResult.Request(DispatchRequest(storefront, id))
}
