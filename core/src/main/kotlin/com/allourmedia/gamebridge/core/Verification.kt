package com.allourmedia.gamebridge.core

/** The GameNative version the Storefront Dispatches were recorded with in docs/investigation-log.md (Odin, 2026-11-22). */
const val GAMENATIVE_VERIFIED_VERSION = "1.4.0"

/** The setup screen's verification lines: GameNative, then each streaming client, with the recorded version or "not yet". */
fun verificationLines(clients: List<StreamClient> = StreamClient.all): List<String> =
    listOf("Verified with GameNative $GAMENATIVE_VERIFIED_VERSION") +
        clients.map { client ->
            client.verifiedVersion?.let { "Verified with ${client.name} $it" } ?: "${client.name}: not verified yet"
        }
