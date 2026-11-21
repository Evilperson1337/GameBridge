# Spec: GameBridge 2.0 (Bridge Shortcuts and Scan)

**Status:** needs-triage (drafted from a grilling session on 2026-11-21; awaiting maintainer approval before any code)

**Source:** grilling session with the maintainer, 2026-11-21, grounded in `docs/gamebridge-2.0/` (research 01 to 05). Vocabulary follows `GLOSSARY.md`. Version numbers are working names: slices ship as 0.x; "2.0" is used once the model is proven on all three Target Apps.

## Problem Statement

Today LaunchBox hands GameBridge a Shortcut exported by a Target App, and GameBridge's only knowledge of the game is the extension plus a number. Which Target App wrote a file cannot be told from its extension or ID range (`docs/gamebridge-2.0/02-UPSTREAM-INTEGRATIONS.md`), and each Target App needs its own LaunchBox handling. The player does the work of knowing which app owns which game.

## Solution

GameBridge owns the logic. LaunchBox only passes a **Bridge Shortcut** (`.gamebridge`) that GameBridge itself created. The file names the Target App, Storefront and IDs; GameBridge validates it and Dispatches using a fixed per-target template. The player makes Bridge Shortcuts by tapping **Scan**, which reads the Shortcut Folder the player granted for each Target App and writes a `.gamebridge` file per game into GameBridge's own **Output Folder**. The player points LaunchBox's platform at that folder and imports manually, as today.

## Decisions

- **Scope.** 2.0 is an approved phase, released in evidence-gated slices. AGENTS.md is updated in the same commit.
- **LaunchBox boundary.** GameBridge never writes to LaunchBox, its database or its configuration. Importing remains manual (`docs/gamebridge-2.0/03-LAUNCHBOX-INTEGRATION.md`).
- **Entry point.** The existing exported `BridgeActivity` and the existing Setup command are reused; `.gamebridge` is a new branch next to `.art` and the Local Shortcut extensions. No new exported component. Conditional on the device checks in ticket 05 (`docs/gamebridge-2.0/05-LAUNCH-MECHANISM.md`).
- **Format.** UTF-8 JSON, at most 4,096 bytes, with a `version` field. Fixed per-target templates only: no raw Intent, component, action or extras in the file. Unknown `target`, `storefront` or fields are rejected; an unknown newer `version` fails explicitly with an update message. Exact schema is ADR 0002.
- **Validation per Target App.** GameNative: positive signed 32-bit Store ID, out of range is rejected, never truncated. DroidDeck (slice 2): its own recorded range, enabled only after its contract is pinned on a device. Artemis: `host_uuid`, `host_name`, `app_uuid`, `app_name`, length-capped, no control characters.
- **Scan.** Manual only (button), no background service. It reads each Target App's Shortcut Folder read-only through the Storage Access Framework. Which folder was granted defines the owning Target App. It writes only to the Output Folder, additively; an overwrite or delete needs the player's confirmation. A file name is the source file's stem; on collision across Target Apps a Target App suffix is added. The summary lists added, skipped, and rejected files by name and reason, never by contents. Unverified Target Apps are reported "found, not enabled" and get no file.
- **Never** read another app's private storage, call its internals, or write into its folder. No network permission, so no title lookup.
- **Evidence gate.** A Target App's `verified` flag flips only after an investigation-log entry records the Target App version, the Android version, a logcat-observed Dispatch, and a separately noted Launch. The setup screen shows "verified with <app> <version>".
- **Legacy path.** Native `.steam`, `.epic`, `.gog`, `.amazon`, `.pcgame` and `.art` Shortcuts keep working through `BridgeActivity` for now. Its removal is a later decision.
- **Code layout.** `:core`: Bridge Shortcut parser and writer, per-target validators, the scan planner (names and contents in, files to write and rejections out), failure categories; JVM tests, planner written test-first. `:app`: SAF access, file IO, setup UI.
- **Changed earlier decision.** The Artemis Dispatch spec says GameBridge does not parse `.art` ("Artemis owns the format"). Scan must read `.art` to build the Bridge Shortcut. Dispatch of a legacy `.art` stays unparsed.

## Slices

1. **Slice 1:** format, GameNative and Artemis targets, Scan, `.gamebridge` Dispatch, device validation (tickets 01 to 06).
2. **Slice 2:** DroidDeck (ticket 07), after slice 1's gate.

## Known gaps and open questions

- **Stale entries.** A `.gamebridge` whose source is gone is not detected in slice 1. A later optional `source` field could link them.
- **Artemis without a `.art` file.** Artemis's intent filter requires a `.art` path (`docs/gamebridge-2.0/04-EXPORT-FORMATS-AND-FOLDERS.md`). Whether Artemis accepts a launch rebuilt from fields is unverified. If it does not, the design needs another decision; no fallback is assumed.
- **LaunchBox URI.** What `{file.uri}` becomes, whether it is a granted `content://` URI, whether LaunchBox imports `.gamebridge`, and Android 14 to 16 behaviour are unverified.
- **Export folders.** None of the three apps has a default folder, and GameNative and DroidDeck write plain paths. Whether SAF can read those folders on the Odin is unverified; DroidDeck's single mixed folder (with `.droiddeck-sync.json` and `images/`) is the harder case.
- **Installed versions.** Research pinned upstream HEADs, not the APKs on the Odin; GameNative flavor is unknown.

## Out of scope

Writing to LaunchBox, auto-import, background scanning, title lookup, stale-entry cleanup, a launcher picker, Associations across Target Apps, MANAGE_EXTERNAL_STORAGE.
