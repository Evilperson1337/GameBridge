package com.allourmedia.gamebridge.core

/** One file found in a Shortcut Folder: its display name and its contents, capped by the reader. */
class SourceShortcut(
    val displayName: String,
    val bytes: ByteArray,
)

/** A Bridge Shortcut the Scan will write to the Output Folder, and the Shortcut it came from. */
data class PlannedFile(
    val name: String,
    val content: String,
    val source: String,
)

/** A file the Scan reports by name with the one-line reason. Never its contents. */
data class ScanEntry(
    val name: String,
    val reason: String,
)

/** What a Scan would do. Pure: nothing has been read from or written to a device. */
data class ScanPlan(
    val writes: List<PlannedFile>,
    val rejected: List<ScanEntry>,
    /** Valid exports for a Storefront whose Dispatch is not verified: found, not enabled, no file. */
    val notEnabled: List<ScanEntry> = emptyList(),
    /** Output files that already hold exactly what the Scan would write, by their existing name. */
    val unchanged: List<String> = emptyList(),
    /** Planned files whose name is taken by a different existing file. Written only if the player confirms. */
    val needsConfirmation: List<PlannedFile> = emptyList(),
)

/** Plans a Scan of the GameNative Shortcut Folder ([gameNative]) into the Output Folder ([existingOutput]). */
fun planScan(
    gameNative: List<SourceShortcut>,
    existingOutput: Map<String, ByteArray>,
    storefronts: List<Storefront> = Storefront.all,
): ScanPlan {
    val accepted = mutableListOf<Pair<SourceShortcut, DispatchRequest>>()
    val rejected = mutableListOf<ScanEntry>()
    val notEnabled = mutableListOf<ScanEntry>()
    val exportExtensions = storefronts.map { it.extension }.toSet()
    // Only GameNative's export extensions are Scanned: anything else in the folder is not ours to report on.
    for (source in gameNative.filter { shortcutExtension(it.displayName) in exportExtensions }) {
        when (val result = interpretShortcut(source.displayName, source.bytes, storefronts)) {
            is ShortcutResult.Rejected -> {
                val entry = ScanEntry(source.displayName, result.reason.toFailure(source.displayName).reason)
                if (result.reason is Rejection.StorefrontNotVerified) notEnabled += entry else rejected += entry
            }

            is ShortcutResult.Request -> {
                accepted += source to result.request
            }
        }
    }
    // Output folders are usually case-insensitive, so "Broforce" and "broforce" collide. Every file in a collision gets
    // its Storefront as a suffix, so which one keeps the plain name never depends on the order files were listed in.
    val stemCounts = accepted.groupingBy { stemOf(it.first).lowercase() }.eachCount()
    val planned =
        accepted.map { (source, request) ->
            val stem = stemOf(source)
            val name = if (stemCounts.getValue(stem.lowercase()) > 1) "$stem (${request.storefront.name})" else stem
            PlannedFile("$name.$BRIDGE_SHORTCUT_EXTENSION", writeBridgeShortcut(request), source.displayName)
        }
    val existingByName = existingOutput.entries.associateBy { it.key.lowercase() }
    val writes = mutableListOf<PlannedFile>()
    val unchanged = mutableListOf<String>()
    val needsConfirmation = mutableListOf<PlannedFile>()
    for (file in planned) {
        val existing = existingByName[file.name.lowercase()]
        when {
            existing == null -> writes += file
            existing.value.contentEquals(file.content.toByteArray(Charsets.UTF_8)) -> unchanged += existing.key
            else -> needsConfirmation += file
        }
    }
    return ScanPlan(writes, rejected, notEnabled, unchanged, needsConfirmation)
}

private fun stemOf(source: SourceShortcut): String = source.displayName.substringBeforeLast('.')

/**
 * The text shown after a Scan: counts, then each file needing attention by name and reason. Never file contents.
 * [failedWrites] are planned files the Output Folder refused; they are not counted as added.
 */
fun scanSummary(
    plan: ScanPlan,
    failedWrites: List<String> = emptyList(),
): String {
    val found = plan.writes.size + plan.unchanged.size + plan.rejected.size + plan.notEnabled.size + plan.needsConfirmation.size
    if (found == 0) return "No GameNative Shortcuts found."
    val added = plan.writes.count { it.name !in failedWrites }
    val lines =
        mutableListOf(
            "Added $added, unchanged ${plan.unchanged.size}, rejected ${plan.rejected.size}, not enabled ${plan.notEnabled.size}.",
        )
    if (plan.needsConfirmation.isNotEmpty()) {
        lines += "Already in the Output Folder with different contents (${plan.needsConfirmation.size}):"
        lines += plan.needsConfirmation.map { it.name }
    }
    if (failedWrites.isNotEmpty()) {
        lines += "Couldn't write:"
        lines += failedWrites
    }
    if (plan.rejected.isNotEmpty()) {
        lines += "Rejected:"
        lines += plan.rejected.map { "${it.name}: ${it.reason}" }
    }
    if (plan.notEnabled.isNotEmpty()) {
        lines += "Found, not enabled:"
        lines += plan.notEnabled.map { "${it.name}: ${it.reason}" }
    }
    return lines.joinToString("\n")
}
