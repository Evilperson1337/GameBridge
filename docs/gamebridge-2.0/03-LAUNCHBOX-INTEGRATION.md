# LaunchBox for Android integration evidence

Research date: 2026-11-21 UTC. Scope: the existing Frontend-to-GameBridge boundary and potential import integration, not a 2.0 implementation. Read with [current architecture](01-CURRENT-ARCHITECTURE.md), [upstream integrations](02-UPSTREAM-INTEGRATIONS.md), and [Android platform evidence](ANDROID-PLATFORM-EVIDENCE.md). Terms follow the [glossary](../../GLOSSARY.md).

## Verdict

**Conditional, version-specific integration; no basis for autonomous import.** LaunchBox documents a user-configured Custom Emulator (With Code), and historical project records show successful Dispatches for particular configurations. The documentation does not promise that its URI placeholder always produces a readable `content://` URI or specify the grant chain. The available evidence does not establish a supported API for GameBridge to trigger imports, nor prove no such API exists. Continue the documented manual workflow; do not implement discovery, matching, a picker, database mutation, or background automation based on this evidence.

## Evidence classification

- **Official documentation**: directly read LaunchBox-owned pages on the research date; a documented interface is not a runtime guarantee for every version.
- **Historical observation**: a dated project record of device behavior or a maintainer report; not reproduced in this research.
- **Repository fact**: current project files, not upstream behavior.
- **Inference / unknown**: hypothesis, design implication, or missing evidence; not a verified interface.

## Documented interface and observed boundary

| Question | Evidence | Conclusion and limit |
|---|---|---|
| Configuration | **Official documentation:** [getting started](https://feedback.launchbox-app.com/help/articles/1026472-getting-started-with-launchbox-for-android) describes Import Games, Add Game, and Custom Emulator (With Code), which accepts custom launch arguments. [Custom Emulator guide](https://feedback.launchbox-app.com/en/help/articles/7096169-custom-emulator-with-code) describes component, action, data, extras, and placeholders. | This is a user-configured interface, not a documented programmatic import API. |
| Placeholder | **Official documentation:** the Custom Emulator guide says `{file.uri}` substitutes a URI and explicitly describes both `file://` and `content://`, with a `file://` example. | Do not assume `content://`, a particular provider, or any read grant. GameBridge intentionally rejects non-content schemes; see [reader](../../app/src/main/kotlin/com/allourmedia/gamebridge/ShortcutReader.kt) and [core triage](../../core/src/main/kotlin/com/allourmedia/gamebridge/core/BridgeFailure.kt). |
| Existing Setup command | **Repository fact:** [setup guide](../launchbox-setup.md) specifies an explicit component, custom action and URI data placeholder, with separate release and debug package IDs. **Historical observation:** its working debug configuration was read back, and an Epic Dispatch was reported. | Readback is not proof of all LaunchBox versions, all Shortcut types, UI steps, or a game Launch. Setup changes the platform default and can affect other Windows entries. |
| Remote Shortcut | **Historical observation:** [architecture audit](01-CURRENT-ARCHITECTURE.md#external-compatibility-evidence-and-limits) cites an Artemis content-URI/grant Dispatch and a user-reported stream, alongside an unexplained failed release pick. | Specific observed paths only; no universal grant or Launch guarantee. |
| Failed trial | **Historical observation:** [G1 report](../gamebridge-g1-launchbox-test-report-20261102.md) records a failed per-game attempt without verified saved command; a separate diagnostic probe is not a GameBridge success. | Do not infer the failure's cause or conflate the probe with a successful GameBridge Dispatch. |
| Import/update | **Official documentation:** [Android 1.21 release notes](https://feedback.launchbox-app.com/changelog/launchbox-for-android-121) describe manually selecting Update Games to scan a platform folder. **Historical observation:** [investigation log](../investigation-log.md) records a Windows platform update that instead requested Steam login and did not add the Shortcut. | The documented general feature and the specific unsuccessful Windows observation coexist. Platform configuration and versions may matter; cause is unknown. Manual import is the confirmed path in that record. |
| Automatic import | **Inference / unknown:** [prior research](../research/launchbox-auto-import.md) surveyed sources, but its categorical assertions that no import endpoint exists and that absence establishes no auto-import exceed the evidence. | No supported integration was established by this research. Absence from surveyed docs is not proof of nonexistence, including in other releases. |

A successful activity start is **Dispatch**, not proof of a game or stream Launch. Existing [GameNative and DroidDeck research](02-UPSTREAM-INTEGRATIONS.md) describes separate upstream contracts and does not verify LaunchBox's URI handling.

## Unknowns and decision gates

1. LaunchBox version/build, installed APK provenance and target SDK for each configuration; exact per-platform versus per-game precedence and update behavior.
2. Actual outgoing component, action, categories, MIME type, data scheme, extras, flags and ClipData for each version and Shortcut source; whether the receiver can read the URI and whether Artemis can read an onward grant. Never record Shortcut contents or device paths in shared evidence.
3. Whether a documented or support-confirmed import endpoint exists, its stability, authorization and supported input; whether Update Games behaves differently on an unlinked platform. Ask LaunchBox maintainers or obtain documented confirmation rather than infer from missing search results.
4. Cross-version compatibility on Android 13–16 and actual Target App versions; distinguish a Dispatch from a separately observed Launch. A synthetic command or a standalone resolver test is not an end-to-end Frontend test.

## Authorized, reproducible APK inspection plan (not performed)

Obtain explicit authorization from the APK owner and comply with applicable license, law and distribution restrictions. Prefer an official, legally obtained APK or a user-provided copy; record official download origin, package name, version name/code, signing certificate digest, SHA-256, acquisition date, and all split APKs. Do not publish or commit APKs, decompiled code, private library data, device identifiers, paths, game files, or secrets. Inspection is static and read-only; do not bypass protection or run unknown code. Inspect locally in a private temporary working directory, remove artifacts when authorized, and record exact tool versions and commands.

On a suitable host, examine each base/split with Android SDK tools such as `apkanalyzer manifest print <apk>`, `aapt2 dump badging <apk>`, and `apksigner verify --print-certs <apk>`; hash with `sha256sum <apk>`. If authorized and necessary, use a versioned local decompiler such as JADX to trace the custom command parser, placeholder construction, provider, flags/ClipData and potential import entry points, recording class/method and offset or line references plus limitations such as obfuscation, reflection and native code. A manifest declares possible exported components and permissions; it does not prove a caller can use them or establish runtime behavior. Compare the same identified artifact across releases; a source-level finding remains version-specific.

Any device acquisition, installation, temporary Shortcut copy, or LaunchBox configuration change requires the approvals in [project rules](../../AGENTS.md); the standing approval only covers GameBridge's own debug install/uninstall and reading logcat. Back up LaunchBox configuration before any approved change. No such APK or device inspection was performed here. A later authorized runtime test should use only temporary copies, record sanitized intent metadata and outcomes, restore configuration, and separate provider-read success, Dispatch and independently observed Launch.
