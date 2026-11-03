# Spec: GameBridge MVP (Phase 0 + Phase 1: GOG Dispatch)

**Status:** implemented; Gate G1 closed in `docs/investigation-log.md`. Statements below that Artemis is unsupported or later describe this phase only; Artemis Dispatch is specified in `.scratch/artemis-dispatch/spec.md`.

**Source:** `docs/overview/GameBridge_Implementation_Handoff.md` (§10 Phases 0–1), refined by a grilling session on 2026-11-01. Vocabulary follows `GLOSSARY.md`; decisions follow `docs/adr/`; toolchain facts come from `docs/research/android-toolchain-and-gamenative.md`.

## Problem Statement

I use LaunchBox for Android on my AYN Odin 2 Portal to browse my game library. My Windows games are installed in GameNative, which exports a small Shortcut file per game (for example a `.gog` file containing the game's Store ID). LaunchBox's built-in GameNative emulator entry fails on these Shortcuts with a "ROM file could not be found" error before GameNative is ever started. So I can't start my GameNative games from my frontend, and I have to leave LaunchBox and find each game in GameNative by hand.

## Solution

A small, separate Android app, GameBridge, that LaunchBox calls through its Custom Emulator (With Code) feature. When I pick a game in LaunchBox, LaunchBox hands GameBridge the Shortcut as a `content://` URI with a read grant. GameBridge reads and validates the Shortcut and dispatches the game to GameNative using GameNative's published external launch intent. Then it disappears. If anything goes wrong, I see a short, controller-friendly dialog explaining what failed, and closing it brings me back to LaunchBox.

GameNative exports the same Shortcut format for every Storefront (GOG, Steam, Epic, Amazon and Custom), so GameBridge treats them all through one convention. This phase dispatches GOG games only, the one Storefront verified on the device. The other Storefronts are recognised but explicitly reported as "not verified yet"; enabling one later is a data change plus a device check. Remote (Artemis) Shortcuts are recognised and reported as unsupported.

Phase 0 sets up the repository and a reproducible toolchain on CachyOS so all of the above can be built and tested from a clean checkout.

## User Stories

### Player: happy path

1. As a player, I want to pick a GOG game in LaunchBox and have it open in GameNative, so that I can use LaunchBox as my single game library.
2. As a player, I want GameBridge to show nothing on screen when the Dispatch succeeds, so that the hand-off from LaunchBox to GameNative feels direct.
3. As a player, I want GameBridge to close itself after a successful Dispatch, so that pressing Back from GameNative doesn't land me on an empty GameBridge screen.
4. As a player, I want one LaunchBox emulator configuration for my Windows platform to cover my GameNative games, so that I don't have to configure each game.
5. As a player, I want GameBridge to work without broad storage permissions, so that I don't have to grant it access to all my files.
6. As a player, I want GameBridge to never modify my Shortcuts, games or saves, so that trying it carries no risk to my library.

### Player: failures

7. As a player, when something goes wrong, I want a dialog that names the Shortcut (when known) and gives a one-line reason, so that I know what to fix.
8. As a player, I want the error dialog to have a single Close button that returns me to LaunchBox, so that I can recover with a controller in one press.
9. As a player, I want the error dialog to work with D-pad, A/B buttons and touch, so that I can dismiss it on a handheld without reaching for the screen.
10. As a player, I want to retry simply by picking the game in LaunchBox again, so that the dialog doesn't need its own retry logic.
11. As a player, I never want to see a stack trace, so that errors are readable.
12. As a player, if GameNative isn't installed, I want a message saying so and naming where to get it (its GitHub releases page, in text), so that I know what's missing.
13. As a player, if I pick a Steam, Epic, Amazon or Custom game, I want to be told that support for that Storefront isn't verified yet, so that I don't mistake it for a bug in my setup.
14. As a player, if I pick an Artemis (`.art`) game, I want to be told that Remote Shortcuts aren't supported yet, so that I know streaming is a later feature.
15. As a player, if LaunchBox hands GameBridge a file that isn't a Shortcut (wrong extension), I want a "Not a supported Shortcut" message, so that I can spot a misconfigured platform.
16. As a player, if the Shortcut is empty, malformed, contains more than one number, or contains an ID that's zero, negative or too large, I want a "this Shortcut's contents aren't valid" message, so that I know the file itself is the problem.
17. As a player, if the Shortcut file is unexpectedly large, I want GameBridge to refuse it rather than read it, so that a wrong file can't hang or crash it.
18. As a player, if GameBridge can't read the file (permission missing or revoked, file gone, provider error), I want a message saying the Shortcut couldn't be read, so that I know to check LaunchBox's configuration.
19. As a player, if GameBridge is opened without a Shortcut (for example from the app drawer, or from a misconfigured emulator command), I want a message explaining that it's meant to be started from LaunchBox, so that I'm not left confused.
20. As a player, I want a game to be dispatched only once even if the screen rotates or the activity is recreated, so that GameNative doesn't receive duplicate requests.
21. As a player, I want GameBridge to never silently fall back to another app or a deep link when Dispatch fails, so that I always know what happened.

### Player: setup

22. As a player, I want a setup guide that gives the exact LaunchBox Custom Emulator (With Code) command for GameBridge, so that I can configure it correctly the first time.
23. As a player, I want the guide to tell me how to back up and restore my LaunchBox emulator configuration, so that I can undo the change.
24. As a player, I want the README to list limitations (GOG only, GameNative standard build only), so that my expectations are set.

### Developer / maintainer: Phase 0 toolchain

25. As a developer on CachyOS, I want documented steps to install the Android SDK in my home directory without sudo, so that tooling doesn't change my system.
26. As a developer, I want the SDK location kept in a gitignored local file plus `ANDROID_HOME`, so that no machine-specific paths are committed.
27. As a developer, I want every tool version pinned in one version catalogue and the Gradle wrapper committed, so that a clean checkout builds the same way everywhere.
28. As a developer, I want a single command that runs formatting checks, Android Lint and all tests, so that "is it green?" has one answer.
29. As a developer, I want a single command that builds the debug APK, so that deploying to a device is one step.
30. As a developer, I want the core logic to live in a pure Kotlin module with fast JVM tests, so that most behaviour is tested without an emulator.
31. As a developer, I want instrumentation tests to run on a headless Android 13 emulator started by Gradle, so that Android behaviour is tested without manual emulator setup.
32. As a developer, I want Waydroid documented as optional, so that it's available for manual exploration without being required.

### Developer / maintainer: behaviour and safety

33. As a maintainer, I want the bridge's outgoing request to be an explicit intent to GameNative's main activity, with the action, an Int `app_id`, and an upper-case `game_source`, so that it matches GameNative's verified contract exactly.
34. As a maintainer, I want only known Storefront values ever sent, so that GameNative's silent fall-back-to-Steam behaviour can never be triggered.
35. As a maintainer, I want Store IDs outside the signed 32-bit range rejected rather than truncated, so that a wrong game is never dispatched.
36. As a maintainer, I want only the bridge activity exported, and it to accept only GameBridge's own action and only `content://` data, so that other apps can't misuse it.
37. As a maintainer, I want GameBridge to declare package visibility for GameNative only, so that it never needs broad package queries.
38. As a maintainer, I want the debug build to log the Storefront, Store ID, URI scheme and Dispatch result under one logcat tag, so that device checks can be done with filtered logcat.
39. As a maintainer, I want the release build to log only the failure category, never Shortcut contents or paths, so that no personal data ends up in logs.
40. As a maintainer, I want results named as Dispatch (not Launch) throughout code and UI, so that nothing ever claims a game started.
41. As a maintainer, I want debug builds installed under a `.debug` application ID, so that they never replace a release install.
42. As a maintainer, I want every Storefront defined as one row in a single table (extension, `game_source` value, verified or not), so that adding or enabling a Storefront needs no new parsing or dispatch code.
43. As a maintainer, I want an investigation log recording what was verified versus assumed, so that later phases build on evidence.

### Developer / maintainer: device validation (Gate G1)

44. As a maintainer, I want a documented, reversible procedure for validating on the Odin using a temporary copy of the Akalabeth Shortcut and a temporary LaunchBox entry, so that the user's real library is never touched.
45. As a maintainer, I want the procedure to require asking before any LaunchBox configuration change or temporary file creation on the device, so that the user stays in control.
46. As a maintainer, I want Gate G1 evidence (logcat excerpt plus the observed Akalabeth Ready screen) recorded, so that Dispatch to Launch is confirmed by a person, not inferred.
47. As a maintainer, I want a rollback checklist covering how to restore the emulator configuration, remove the temporary entry and file, and uninstall the debug build, so that the device can be returned to its original state.

## Implementation Decisions

### Identity and repository

- Application ID `com.allourmedia.gamebridge`, display name "GameBridge" (ADR 0001). Debug builds add the `.debug` suffix. Kotlin packages live under the same namespace.
- The Gradle project lives at the repository root (no `gamebridge/` subfolder). The default branch is `main`.
- Decisions are recorded as ADRs in `docs/adr/`, not in a single decisions file. Planned docs: investigation log, LaunchBox setup guide, device-testing guide (including SDK and emulator setup), and the README.

### Toolchain (pinned)

- Android Gradle Plugin 9.5.0; Gradle 9.6.0 via the committed wrapper.
- Kotlin 2.4.21 for both modules. AGP's built-in Kotlin support is used for `:app` (no separate Kotlin Android plugin), with the Kotlin Gradle plugin raised to 2.4.21 on the root build classpath so `:core` (Kotlin JVM) and `:app` share one Kotlin version.
- compileSdk 37, targetSdk 36, minSdk 30. JDK 21 runs the build; bytecode targets Java 17.
- No Jetpack Compose in this phase (revisit for the Phase 4 picker).
- ktlint via its Gradle plugin, plus Android Lint. `./gradlew check` is the single verification command. No CI until the repo has a remote.
- Android SDK: Google's command-line tools 23.0 in `~/Android/Sdk`, with platform-tools, build-tools 36, platform 37, the emulator, and an Android 13 (API 33) system image. The SDK path goes in gitignored `local.properties`; `ANDROID_HOME` is added to the user's fish config only after asking.

### Modules

- **`:core`** (pure Kotlin JVM, no Android dependencies): Shortcut kinds, Storefront, Store ID validation, and the single **interpret-a-Shortcut** operation. Input: the Shortcut's display name (for its extension) and its raw bytes (already size-capped). Output: either a **dispatch request** (Storefront plus Store ID) or a **rejection** carrying a reason category. Rejection categories:
  - unsupported Shortcut type (unknown extension)
  - Storefront not verified yet (every Storefront except GOG in this phase)
  - Remote Shortcut unsupported (Artemis `.art`)
  - invalid contents (empty, non-numeric, more than one number or meaningful line, zero, negative, outside signed 32-bit range)
  - too large
- **`:app`** (Android): the exported bridge activity, Shortcut reading through the content resolver, Dispatch to GameNative, the error dialog, and logging. Internal pieces (reader, dispatcher, dialog) are plain classes, not public seams.

### Storefront table

- Every Storefront is one row in a single table in `:core`. Each row holds the Shortcut extension, the GameNative `game_source` value, and whether Dispatch is verified. Parsing, the extension allowlist and Dispatch all read from this table. Adding or enabling a Storefront means one row, one fixture and a device check, with no new parsing or dispatch code.
- The initial rows come from GameNative v1.3.0's source (see the research doc):

  | Storefront | Extension | `game_source` | Verified |
  |---|---|---|---|
  | GOG | `.gog` | `GOG` | yes |
  | Steam | `.steam` | `STEAM` | no |
  | Epic | `.epic` | `EPIC` | no |
  | Amazon | `.amazon` | `AMAZON` | no |
  | Custom | `.pcgame` | `CUSTOM_GAME` | no |

- GameNative writes every Local Shortcut the same way: its own Int ID for the game, as UTF-8 text with no newline. Epic and Amazon Store IDs are GameNative's internal numbers, not store catalogue IDs, so they always fit an Int. GameNative writes `0` when it can't turn a GOG ID into a number, which the zero rule rejects.

### Shortcut parsing rules

- The extension is taken from the content provider's display name and matched case-insensitively against the Storefront table, plus `.art` for Remote Shortcuts. Anything else is unsupported.
- Local Shortcuts are read with a hard cap of 4 KiB; anything larger is rejected without being fully read. (The 16 KiB `.art` cap applies from Phase 3.)
- Contents are decoded as UTF-8 with an optional BOM stripped and surrounding whitespace (including CR/LF and blank lines) trimmed. They must be exactly one positive decimal integer within signed 32-bit range.
- Every Local Shortcut type in the table is parsed. Only verified rows (GOG in this phase) produce a dispatch request; the rest produce the "not verified yet" rejection.

### Bridge activity contract

- The single exported activity accepts only the GameBridge launch action, namespaced under the application ID, with `content://` data. The LaunchBox Custom Emulator (With Code) command uses an explicit component, that action, and `-d {file.uri}`. The exact command string is given in the setup guide and must use the final application ID (`.debug` suffix for debug builds).
- Validation order: action correct, then data present, then scheme is `content`, then display name readable, then bytes readable within the cap, then interpretation. The first failure produces its error dialog.
- The theme is transparent: nothing is visible on a successful Dispatch, and the activity finishes immediately after dispatching.
- Dispatch happens only on first creation, never on recreation (rotation or process restore), and never more than once per incoming intent.

### Dispatch to GameNative

- Explicit intent with action `app.gamenative.LAUNCH_GAME`, component `app.gamenative/.MainActivity`, extra `app_id` as an Int, and extra `game_source` taken from the Storefront table (`GOG` in this phase). No `container_config` and no other extras. The incoming Shortcut URI and its grant are never forwarded.
- The manifest declares package visibility for `app.gamenative` only.
- An `ActivityNotFoundException` is reported as "GameNative is not installed". A `SecurityException` is reported as "GameNative refused the request". There is no fallback to the deep link and no other Target App.
- The GameNative "gold" variant (`app.gamenative.gold`) is not targeted.
- A successful `startActivity` is recorded as **Dispatched**, never as Launched.

### Error dialog

- An `AlertDialog` over the transparent activity shows the Shortcut's filename (when known), a one-line reason mapped from the failure category, and a single Close button. Close and Back both finish the activity. No Retry, no details, no stack traces. It must be focusable and operable by D-pad and A/B buttons.

### Logging

- One logcat tag, `GameBridge`. Debug builds log the Storefront, Store ID, URI scheme and Dispatch result. Release builds log only the failure category; they never log Shortcut contents, URIs or paths.

### Security

- No storage, network or other dangerous permissions. Only the bridge activity is exported. Release builds are not debuggable.

## Testing Decisions

- **Good tests check external behaviour only**: inputs at a seam and observable outputs. They don't test private helpers or the structure of internal classes. There are exactly two seams.
- **Seam 1: the `:core` interpret-a-Shortcut operation (JVM unit tests).** Every parsing and support rule is tested here, driven by fixture files:
  - from the handoff: `Broforce.gog` (1470490225), `Broforce.steam` (274190), `ABZU.epic` (64)
  - synthetic, one per remaining Storefront: an `.amazon` and a `.pcgame` Shortcut, in GameNative's format
  - the known-good Akalabeth `.gog` (1207666073)
  - synthetic bad cases: BOM, CRLF, surrounding blank lines, negative, zero, above 2³¹−1, non-numeric, two numbers, empty, oversized, unknown extension, upper-case extension

  Expected outcomes are dispatch requests or specific rejection categories. No `.art` fixture is created until Phase 3 confirms the real format.
- **Seam 2: the bridge activity as a whole (instrumentation tests on a Gradle-managed headless Android 13 emulator).** Tests start the real activity with an intent built the way LaunchBox builds it. A test-only document provider serves fixture Shortcuts as `content://` URIs with a read grant. Assertions:
  - **Success:** Espresso-Intents captures and stubs the outgoing intent and verifies the action, component, Int `app_id` and `game_source`. The activity finishes.
  - **Failure:** the dialog shows the expected filename and reason, and Close finishes the activity. Cases:
    - missing data
    - wrong action
    - non-`content` scheme
    - unreadable or revoked URI
    - each rejection category
  - **GameNative missing:** an unstubbed Dispatch on the emulator (where GameNative isn't installed) produces the "GameNative is not installed" dialog.
  - **No duplicate dispatch:** recreating the activity after a Dispatch doesn't dispatch again.
- **Device checks (Gate G1, manual, on the Odin)**: LaunchBox → GameBridge → GameNative → Akalabeth Ready screen, confirmed by a person and filtered logcat. Verify that originals are unchanged. Run the rollback checklist.
- **Prior art:** none. This is a new repository, so these tests establish the patterns.
- `./gradlew check` runs ktlint, Lint and the JVM tests. The instrumentation suite runs through the managed-device task, which is documented alongside.

## Out of Scope

- Dispatching Steam, Epic, Amazon or Custom Shortcuts (Phase 2): each needs on-device verification before its row is marked verified.
- Artemis / Remote Shortcut parsing beyond recognising the extension, and any Artemis dispatch (Phase 3).
- Discovery, indexing, directory pickers, matching local and remote copies of a game, and the local/remote picker (Phase 4).
- Release packaging, changelog, privacy statement, and second-device regression suite (Phase 5).
- The GameNative gold variant, `container_config` overrides, and the GameNative deep link.
- DroidDeck (`com.droiddeck.launcher`) as a second Target App. It's feasible but moderate work: its `.steam` Shortcuts collide with GameNative's, so it needs a Target App column in the Storefront table, Target App–specific Store ID ranges, a small dispatch adapter, and a user choice of Target App. See `docs/research/droiddeck-target-app.md`.
- Jetpack Compose, CI, and an in-app "test a Shortcut" picker.
- Fixing LaunchBox's built-in GameNative emulator route.

## Further Notes

- Gate G0 exit: a clean checkout passes `./gradlew check` and builds the debug APK.
- Gate G1 exit: Akalabeth reaches its Ready screen on the Odin through LaunchBox → GameBridge → GameNative. No original file, save or unrelated LaunchBox entry is modified, and rollback is documented and performed.
- Standing approval (AGENTS.md): installing or uninstalling the `.debug` GameBridge build on the Odin and reading logcat. Everything else on the device (LaunchBox configuration, temporary Shortcut copies) needs asking first.
- AGP 9.5 is newer than the last AGP version Kotlin formally lists as compatible (9.3.1). Warnings at first build are expected; record them in the investigation log.
- Open questions carried forward:
  - whether LaunchBox grants URI reads consistently on other Android versions and providers
  - on-device Dispatch behaviour for Steam, Epic, Amazon and Custom
  - support for the gold variant
  - whether LaunchBox can start DroidDeck directly (opening the Shortcut itself), which would decide whether DroidDeck support in GameBridge is worth building
