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

/** The Shortcut Folders a Scan reads. The folder a file was found in decides its Target App (ADR 0003). */
enum class ShortcutFolder { GameNative, Artemis, Ariane }

/** Plans a Scan of the GameNative Shortcut Folder only. */
fun planScan(
    gameNative: List<SourceShortcut>,
    existingOutput: Map<String, ByteArray>,
    storefronts: List<Storefront> = Storefront.all,
): ScanPlan = planScan(mapOf(ShortcutFolder.GameNative to gameNative), existingOutput, storefronts)

/** Plans a Scan of the Shortcut Folders in [sources] into the Output Folder ([existingOutput]). */
fun planScan(
    sources: Map<ShortcutFolder, List<SourceShortcut>>,
    existingOutput: Map<String, ByteArray>,
    storefronts: List<Storefront> = Storefront.all,
    clients: List<StreamClient> = StreamClient.all,
): ScanPlan {
    // Each accepted file: its source, the suffix used on a name collision, and the Bridge Shortcut text.
    val accepted = mutableListOf<Triple<SourceShortcut, String, String>>()
    val rejected = mutableListOf<ScanEntry>()
    val notEnabled = mutableListOf<ScanEntry>()
    val exportExtensions = storefronts.map { it.extension }.toSet()
    for (folder in ShortcutFolder.entries) {
        val client = clients.firstOrNull { it.name == folder.name }
        for (source in sources[folder].orEmpty()) {
            // Each folder is Scanned for its own Target App's exports only; anything else is not ours to report on.
            val extension = shortcutExtension(source.displayName)
            val result =
                when {
                    folder == ShortcutFolder.GameNative && extension in exportExtensions -> {
                        interpretShortcut(source.displayName, source.bytes, storefronts)
                    }

                    client != null && extension == ART_EXTENSION -> {
                        interpretArtShortcut(source.bytes, client)
                    }

                    else -> {
                        continue
                    }
                }
            when (result) {
                is ShortcutResult.Rejected -> {
                    val entry = ScanEntry(source.displayName, result.reason.toFailure(source.displayName).reason)
                    val unverified = result.reason is Rejection.StorefrontNotVerified || result.reason is Rejection.TargetNotVerified
                    if (unverified) notEnabled += entry else rejected += entry
                }

                is ShortcutResult.Request -> {
                    accepted += Triple(source, result.request.storefront.name, writeBridgeShortcut(result.request))
                }

                is ShortcutResult.Remote -> {
                    accepted += Triple(source, result.request.client.name, writeBridgeShortcut(result.request))
                }
            }
        }
    }
    // Output folders are usually case-insensitive, so "Broforce" and "broforce" collide. Every file in a collision gets
    // its Storefront or client as a suffix, so which one keeps the plain name never depends on listing order.
    val stemCounts = accepted.groupingBy { stemOf(it.first).lowercase() }.eachCount()
    val planned =
        accepted.map { (source, suffix, content) ->
            val stem = stemOf(source)
            val name = if (stemCounts.getValue(stem.lowercase()) > 1) "$stem ($suffix)" else stem
            PlannedFile("$name.$BRIDGE_SHORTCUT_EXTENSION", content, source.displayName)
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
    if (found == 0) return "No Shortcuts found."
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
