package com.allourmedia.gamebridge.core

/** One row per Storefront. Parsing and Dispatch read only from [Storefront.all]. */
class Storefront private constructor(
    val name: String,
    val extension: String,
    val gameSource: String,
    val verified: Boolean,
) {
    override fun toString() = name

    companion object {
        val GOG = Storefront("GOG", "gog", "GOG", verified = true)
        val all: List<Storefront> = listOf(GOG)
    }
}

/** A validated request to Dispatch one game. [storeId] is always a positive Int. */
data class DispatchRequest(
    val storefront: Storefront,
    val storeId: Int,
)

enum class Rejection { UnsupportedType, InvalidContents }

sealed interface ShortcutResult {
    data class Request(
        val request: DispatchRequest,
    ) : ShortcutResult

    data class Rejected(
        val reason: Rejection,
    ) : ShortcutResult
}

/** Interprets a Shortcut from its display name (for the extension) and its raw bytes. */
fun interpretShortcut(
    displayName: String,
    bytes: ByteArray,
): ShortcutResult {
    val extension = displayName.substringAfterLast('.', "").lowercase()
    val storefront =
        Storefront.all.firstOrNull { it.extension == extension }
            ?: return ShortcutResult.Rejected(Rejection.UnsupportedType)
    val text = bytes.toString(Charsets.UTF_8).removePrefix(BYTE_ORDER_MARK).trim()
    val id = text.toIntOrNull()
    if (id == null || id <= 0 || text.any { !it.isDigit() }) {
        return ShortcutResult.Rejected(Rejection.InvalidContents)
    }
    return ShortcutResult.Request(DispatchRequest(storefront, id))
}

private const val BYTE_ORDER_MARK = "\uFEFF"
