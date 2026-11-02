# Investigation log

Each entry says what was verified (observed in a real run) and what was assumed.

## 2026-11-02: Ticket 01, toolchain and project skeleton

### Verified (real runs on CachyOS, JDK 21.0.12, kernel 7.3.1-1-cachyos)

- Google's `commandlinetools-linux-16111833_latest.zip` unpacked to `~/Android/Sdk/cmdline-tools/latest` reports cmdline-tools 23.0. Installed via sdkmanager: platform-tools 37.0.1, build-tools 36.0.0, platforms;android-37.0, emulator 37.2.13, system-images;android-33;google_apis;x86_64 (r17). No sudo.
- Gradle 9.6.0 wrapper pinned with `distributionSha256Sum` (`bbaeb2fe...a529a01`, matches services.gradle.org's published checksum).
- With AGP 9.5.0 and Kotlin 2.4.21 (KGP on the root classpath via `alias(...) apply false`), `./gradlew clean check --rerun-tasks --no-build-cache` succeeds: ktlint 1.8.0 (plugin 14.2.0), Android Lint and the `:core` JVM test (1 test, 0 failures).
- `./gradlew :app:assembleDebug` produces `app/build/outputs/apk/debug/app-debug.apk`.
- `./gradlew :app:pixel2api33DebugAndroidTest` boots a headless emulator, runs `SmokeInstrumentedTest` (1 test, 0 failures, asserts package name `com.allourmedia.gamebridge.debug`).
- No KGP/AGP compatibility warning appeared in the `check` or `assembleDebug` output. Kotlin's table lists AGP only up to 9.3.1, so AGP 9.5.0 is formally unlisted, but nothing was reported on this skeleton.

### Warnings seen in the first build

- Managed device setup prints: `The device "pixel2api33" does not specify a "testedAbi"` (default changes to arm64-v8a in AGP 10.0). Setting `testedAbi = "x86_64"` in the device config did NOT silence the message; the test still passes. Cause unknown. Assumed to be an AGP 9.5 quirk; not investigated further.
- `sdkmanager` prints a deprecation notice (replaced by `android sdk`). Verified; harmless.

### Assumed / not verified

- That an app with a larger code base will not hit AGP 9.5 + KGP 2.4.21 incompatibilities. Only the empty skeleton was built.
- The skeleton manifest has no activity yet, so installing and starting the app was not tried.
- Waydroid was not installed or tried.

## 2026-11-02: Ticket 02, GOG Shortcut Dispatch tracer bullet

### Verified (JVM tests, `./gradlew check`, and `:app:pixel2api33DebugAndroidTest` on the managed Android 13 emulator)

- `:core` `interpretShortcut` turns the Akalabeth fixture (`1207666073`, no newline) into a GOG dispatch request. Case-insensitive extension, BOM and surrounding whitespace are tolerated; zero, negative, above 2^31-1, non-numeric, empty and two-number contents are rejected.
- On the emulator, the real `BridgeActivity` started with `com.allourmedia.gamebridge.action.DISPATCH` and a `content://` URI sends one explicit intent: action `app.gamenative.LAUNCH_GAME`, component `app.gamenative/app.gamenative.MainActivity`, `app_id` as Int, `game_source` `GOG`, no other extras, no data, no read-grant flag. Observed through Espresso-Intents, which stubs the result, so GameNative itself was not involved.
- The activity finishes (lifecycle DESTROYED). A wrong action or a `file:` URI dispatches nothing.
- Recreation does not dispatch again, tested with a debug-only `StayingBridgeActivity` (it ignores `finish()`), because the real activity finishes in `onCreate` and cannot be recreated through ActivityScenario. The guard under test is the real `savedInstanceState == null` check in `BridgeActivity`.
- Debug logcat line seen: `GameBridge: storefront=GOG storeId=1207666073 scheme=content dispatch=Dispatched`.
- Merged debug manifest: `<queries>` for `app.gamenative` only, no permissions.
- Android Lint caught `InputStream.readNBytes` (API 33 vs minSdk 30); replaced by a manual capped read.

### Findings about the test setup

- A provider declared in the androidTest manifest runs in the test APK's own process and uid. Test code runs in the app's uid, so `grantUriPermission` from the test fails with `SecurityException`. The test provider is therefore exported and the URI read grant is NOT exercised. The provider also has no Kotlin stdlib in its process, so it is written in Java.
- Espresso-Intents also records the intents that tests start themselves, so tests filter the recorded intents by the GameNative action.
- ktlintFormat dropped a literal BOM character from a string; the BOM prefix is now a named constant with a `\uFEFF` escape.

### Assumed / not verified

- That GameNative accepts this intent on the device: nothing was installed on the physical device or run against real GameNative, so no real Dispatch to GameNative was observed, let alone a Launch.
- That LaunchBox's Custom Emulator command produces this intent shape and grants URI reads. The action name `com.allourmedia.gamebridge.action.DISPATCH` is my choice and must be used in the setup guide.
- Behaviour with a revoked or missing grant, failures and dialogs (ticket 03).
- The `testedAbi` warning from ticket 01 still appears.

## 2026-11-02: Ticket 03, error dialog

### Verified (`./gradlew check --rerun-tasks`, and `:app:pixel2api33DebugAndroidTest`: 19 tests, 0 failures, twice in a row)

- A real URI read grant is now exercised. The fixture provider is unexported with `grantUriPermissions`. A test-APK activity (same package and uid as the provider, playing LaunchBox's role) starts the bridge with `FLAG_GRANT_READ_URI_PERMISSION`. Missing-grant and revoked-grant cases fail to read, so the happy path really depends on the grant. This closes the ticket 02 gap.
- Dialog cases shown with the expected text: no data (MAIN and own action), wrong action, `file:` scheme, missing grant, revoked grant, missing file, provider exception, no display name, GameNative not installed (real, unstubbed Dispatch, `ActivityNotFoundException`).
- Close (injected tap), Back, `BUTTON_B`, `BUTTON_A` and `DPAD_CENTER` all finish the activity.
- Release APK: no debuggable flag (aapt2 badging), merged release manifest has one component, `BridgeActivity`, exported, and no permissions. The debug-only test doubles (`StayingBridgeActivity`, `RefusingBridgeActivity`) are exported in the debug manifest only, so the test-APK trampoline can start them.

### Findings

- First A / D-pad press in touch mode only moves focus, so a Close that relies on the default focus would need two presses. The dialog now has a key listener that treats A, D-pad centre and Enter as Close.
- Espresso taps injected into a just-shown dialog on the cold emulator were sometimes lost (first frame took 1.3 s). Tests wait 1.5 s and for idle before tapping. Not seen as a product bug, but not investigated on a real device.
- Starting an Espresso-driven file: URI via another package throws FileUriExposedException, so that case uses `startActivitySync`.
- `KeyEvent.isConfirmKey` is not accessible from Kotlin here; an explicit key set is used.
- Added a MAIN/LAUNCHER filter so the "no data" case can occur from the app drawer. The validation order in the spec (action first) is bent for this: no data with MAIN, no action or the bridge action gives "started from LaunchBox".

### Assumed / not verified

- SecurityException: simulated by a debug-only activity whose `startActivity` throws. No real GameNative refusal was seen.
- Release builds log only the failure category: by code inspection (debug logs are gated on FLAG_DEBUGGABLE); not observed on a running release build.
- Dialog behaviour with a real gamepad and on the physical device: key events were injected on the emulator only. Nothing was installed on the device.
- The GameNative releases URL (github.com/utkarshdalal/GameNative/releases) is taken from the repo path in the docs; not fetched.
- Shortcut interpretation failures still finish silently (ticket 04).

## 2026-11-02: Ticket 05, LaunchBox setup guide, README and rollback (docs only)

### Taken from repo documents (not re-observed; nothing run on a device)

- `app/src/main/AndroidManifest.xml`: the exported activity is `.BridgeActivity`, the action is `com.allourmedia.gamebridge.action.DISPATCH`, data scheme `content`. `app/build.gradle.kts`: namespace `com.allourmedia.gamebridge`, applicationId `com.allourmedia.gamebridge`, debug suffix `.debug`.
- Consequence: the debug build's activity class stays in the `com.allourmedia.gamebridge` package while its application ID is `...gamebridge.debug`, so the setup guide writes the component with a fully qualified class (`<applicationId>/com.allourmedia.gamebridge.BridgeActivity`). This follows from Android resolving a leading-dot class name against the applicationId-derived package; it was not tested through `am start` or LaunchBox.

### Assumed / not verified

- That LaunchBox accepts this command as written, expands `{file.uri}` and grants the read. Only the earlier probe command (`-n dev.launchboxprobe/.ProbeActivity -a ... -d {file.uri}`, from the handoff) was seen working; that is prior evidence, not reproduced here.
- Where Custom Emulator (With Code) is: "top of the emulator picker" comes from the handoff (LaunchBox 1.23). Other menu paths, field names and any extension setup are not verified and are marked assumed in the guide.
- Name and location of LaunchBox's configuration XML on the device: not verified; the guide tells the reader to find it first.
- Error-dialog wording in the README troubleshooting table follows the spec and ticket 03 (built in parallel); final strings may differ and must be reconciled.
- The Gate G1 and rollback procedures were written, not performed.

## 2026-11-02: Ticket 04, full Shortcut interpretation rules

### Verified (`./gradlew check` and `:app:pixel2api33DebugAndroidTest`, both run for real: 30 instrumentation tests incl. smoke, 0 failures)

- JVM tests at the interpret-a-Shortcut seam cover the five-row Storefront table, every fixture, case-insensitive extensions, `.art`, unknown and missing extensions, BOM, CRLF and blank lines, zero, negative, above 2^31-1, the max Int, non-numeric, signs/hex/decimals/Unicode digits, two numbers, empty, exactly 4096 bytes (invalid, not too large) and 4097 bytes (too large). The "flip a row to verified" test passes a copied verified row through the optional `storefronts` parameter and sees a dispatch request with that row's `game_source`.
- Only `Shortcut.kt` names Storefronts (grep of `:app` and `:core` main sources). Dispatch reads `request.storefront.gameSource`.
- Each rejection category has its own dialog, shown on the emulator through the real activity: unsupported type, Storefront not verified (Steam, Epic, Amazon, Custom each naming the Storefront), Remote Shortcut unsupported, invalid contents (zero, out of range), too large. Each asserts no Dispatch was attempted. Upper-case extension and BOM plus blank lines dispatch with the right `app_id`.
- README troubleshooting table now uses the exact strings from `BridgeFailure.kt` (tickets 03 and 04). This closes the ticket 05 assumption that wording "must be reconciled".

### Assumed / not verified

- Reader stops at the cap: `ShortcutReader` reads at most 4097 bytes by inspection; no test observes how much was read (the ticket criterion stays unticked).
- `.art`: no fixture content invented. Instrumentation tests serve any `*.art` name as empty bytes from the test-only provider; the rejection depends on the extension only. The real `.art` format is unverified (Phase 3).
- Synthetic `.amazon` (4815162) and `.pcgame` (2342) are in GameNative's described format (Int, no newline) per the spec; not compared against real exports. Dialog wording for the new categories is new and not seen on a physical device. Nothing was installed on the device.
- An invalid-contents `.art`/unknown-type Shortcut over 4 KiB reports by extension first (unsupported / remote), not too large.

## 2026-11-02: Review fixes on the MVP integration branch

### Verified (`./gradlew check` and `:app:pixel2api33DebugAndroidTest`, both run for real: 30 instrumentation tests, 0 failures)

- Reading stops at the cap: a JVM test feeds a counting 10 MB stream to `readCapped(MAX_LOCAL_SHORTCUT_BYTES + 1)` (now in `:core`, used by `ShortcutReader`) and observes exactly 4097 bytes consumed. This closes the unticked ticket 04 criterion.
- Rejection to failure-category mapping, intent triage (NoShortcut / WrongAction / NotContentUri, no-data check first) and dialog-message assembly now live in `:core` with JVM tests; the emulator tests still pass.

### Decision recorded: leading zeros

- Contents with leading zeros (for example `007`) are currently accepted as 7. The spec does not say either way. Pinned by a JVM test (`000` is still invalid as zero). Not checked against real exports; revisit if a Storefront ever writes padded IDs.

### Assumed / not verified

- `ShortcutReader` now reports a failure kind (name vs contents) and the exception class name; debug builds log them, release builds log only the failure category. Seen in code, not read from a device log.

## 2026-11-02: Gate G1, first attempt (no Dispatch reached GameBridge)

Report: `docs/gamebridge-g1-launchbox-test-report-20261102.md`.

### Verified (read-only `adb` reads on the Odin)

- GameBridge's debug activity resolves for the explicit component (`cmd package resolve-activity`), and the manifest declares the `DISPATCH` action, `content` scheme and `DEFAULT` category.
- LaunchBox stores a platform-wide Custom Emulator (With Code) setting in `Windows.xml` (`<Platform>`: `DefaultEmulator`, `CustomEmulatorLaunchArguments`). Games can override it per game (`EmulatorOverride`, `CustomEmulator*`).
- At the time of the attempt the Windows platform's command was still the earlier probe command (target `dev.launchboxprobe`), and that package is not installed. The temporary entry `GBTemp_Akalabeth` has no per-game override, so it used that platform command. This is the observed explanation for LaunchBox's "launching process failed" message.
- No `GameBridge` logcat output appeared, consistent with no intent reaching `BridgeActivity`.

### Assumed / not verified

- The cause of the Calendar permission screen seen during the attempt is unknown.
- The GameBridge command in LaunchBox has not yet been saved and run; the Dispatch from LaunchBox is still unverified.

## 2026-11-02: Steam, Epic, Amazon and Custom enabled; tests updated

### Verified (`./gradlew check` and `:app:pixel2api33DebugAndroidTest`, both pass: JVM tests, and 30 instrumentation tests with 0 failures)

- The four rows are marked `verified = true` in the Storefront table. JVM tests show each fixture produces a Dispatch request with the right `game_source` (GOG, STEAM, EPIC, AMAZON, CUSTOM_GAME). The `StorefrontNotVerified` rejection is still covered in JVM through a custom table with an unverified row passed to `interpretShortcut`.
- Emulator tests (Espresso-Intents, GameNative stubbed) check the component, the Int `app_id` and `game_source` for the four Shortcuts. The "Storefront not verified" dialog is unreachable from the real table, so it is covered in JVM only (`BridgeFailureTest` and the interpreter tests).

### Assumed / not verified

- The four rows are enabled but NOT yet verified end to end by the agent. The user is testing them on the Odin; nothing in this log shows a Dispatch from these Storefronts reaching GameNative or a game Ready screen. Only GOG has device evidence. No Launch is claimed for any Storefront.
- Synthetic `.amazon` and `.pcgame` fixtures have not been compared with real exports (unchanged from ticket 04).
- Test-harness fix: `BridgeActivityTest.start` could return a previous test's bridge activity (its DESTROYED event reaches the lifecycle callback late) and so skip waiting for the real one; it now matches the activity by class and data. Two tests failed deterministically before this (BOM dispatch, zero Store ID); 30 pass, 0 fail after.

## 2026-11-02: Epic Dispatch observed on the Odin

### Verified (user observation plus read-only `dumpsys activity activities`)

- An Epic Shortcut (`ABZU.epic`) picked in LaunchBox was dispatched through GameBridge and the game started in GameNative (reported by the user).
- After exiting the game, the user saw a blank screen. `dumpsys` showed GameNative's `MainActivity` still resumed and focused in its own task (#220), with LaunchBox's task directly beneath it. GameBridge was not in the stack.

### Assumed / not verified

- Why GameNative stays on a blank screen after an Epic exit, and whether the GOG exit path differs (the GOG comparison snapshot has not been taken). Whether Back on the blank screen returns to LaunchBox has not been checked.
- Steam, Amazon and Custom have no device observation yet.

## 2026-11-02: Working LaunchBox configuration, read from the device

Read-only `adb pull` of `/storage/emulated/0/LaunchBox/Data/Platforms/Windows.xml`, compared with the pre-test backup. The user approved the pull. Nothing on the device was changed.

### Verified (file contents)

- The working setup is platform-wide: on the Windows platform, `DefaultEmulator` is `Custom Emulator (With Code)` and `CustomEmulatorLaunchArguments` is the debug Setup command from `docs/launchbox-setup.md`, byte for byte.
- The games' per-game `EmulatorOverride` values (`GameNative` in the backup) are now empty, so the games follow the platform default. The per-game `CustomEmulator*` fields are empty.
- Games are imported with `ApplicationPath` set to the Shortcut file under `/storage/emulated/0/Games/pc/` (`.epic`, `.gog`, `.amazon` seen).
- Whether the per-game `EmulatorOverride` was cleared by hand or by LaunchBox is not known.

### Assumed / not verified

- The LaunchBox screens and field names used to set this up. The XML shows the result, not the path taken.
- The user reports removing the per-game `GameNative` overrides in LaunchBox after testing, to use the platform-wide setup. The screens used are not recorded, so the guide's "clear per-game override" step is assumed, not verified.

## 2026-11-02: Distribution and setup phase; Gate G1 closed

Phase "Distribution and setup" (spec: `.scratch/distribution-and-setup/spec.md`). This entry covers the docs ticket (README, `docs/development.md`, `LICENSE`) and closes Gate G1.

### Gate G1: closed

Closed on two observations, both recorded above: the Epic Dispatch from LaunchBox that started the game in GameNative on the Odin, and the `Windows.xml` readback showing the platform-wide Custom Emulator (With Code) setup with the debug Setup command byte for byte. The first G1 attempt failed because of a stale platform command (see the earlier entry); the platform-wide setup is what works.

### Verified (by reading the repo for this ticket; nothing run on a device)

- The README troubleshooting reasons match `FailureCategory` in `BridgeFailure.kt` as of this commit. Steam, Epic, Amazon and Custom are enabled in the Storefront table. `minSdk` is 30 (`app/build.gradle.kts`).
- Tested versions stated in the README (LaunchBox 1.23, GameNative 1.3.0, Android 13 on an AYN Odin 2 Portal) come from the handoff document and the G1 report.
- The support table's GOG row rests on the handoff's `am start` of GameNative reaching Akalabeth's Ready screen (2026-11-01); the Epic row on the user's observation above (2026-11-02).

### Assumed / not verified (remaining doubts)

- GOG has not been rechecked through LaunchBox with GameBridge; its "verified" label rests on the direct GameNative request.
- The blank screen after an Epic exit is described in the README as observed once and undiagnosed. The GOG exit path was not compared.
- Steam, Amazon and Custom have no device observation. Synthetic `.amazon` and `.pcgame` files are unchecked against real exports.
- The LaunchBox screens used to set up the platform are not recorded; the guide's steps are written, not walked.
- Android versions other than 13, and LaunchBox and GameNative versions other than 1.23 and 1.3.0, are untested.
- The README download link is the relative `../../releases/latest`; not checked on the hosting site (no remote is configured here). No release exists yet.
- The signed release build and the keystore (tickets 03 and 04) were not seen working by this ticket.
- The G1 report's link to an investigation document missing from `docs/` was removed. The report also contains a USB ADB serial, which AGENTS.md says not to commit; left for the maintainer to redact.

## 2026-11-02: Setup screen observed on the Odin

The debug build (`com.allourmedia.gamebridge.debug`, from the `integration/distribution-and-setup` branch) was installed on the Odin with `adb install -r`.

### Verified (observed on the device)

- Opening GameBridge shows the setup screen. Its text, read back over adb, is the debug Setup command, a COPY button, "GameNative: installed" and the three checklist lines.
- The user pressed COPY and reports that it worked (the clipboard content was not read back by the agent).

### Assumed / not verified

- That the pasted text works in LaunchBox's Custom Emulator (With Code) field was not tried in this step.
- The release build's Setup command was not seen on a device.

## 2026-11-02: Stale setup screen after closing a game

### Verified (observed on the Odin)

- With the setup screen left open, a Dispatch from LaunchBox started `BridgeActivity` with `NEW_TASK` (`flg=0x10000001`). `dumpsys activity` afterwards showed GameBridge's task holding only `SetupActivity`, with LaunchBox in a separate task behind it. Closing the game then showed the setup screen instead of LaunchBox.
- A GOG Dispatch from LaunchBox with the debug build logged `storefront=GOG ... dispatch=Dispatched` (a Dispatch, not a Launch). The user reports it started and the game closed normally.
- After `SetupActivity` was made `noHistory`, the user reports closing a game returns to LaunchBox ("appears to be working"). Only the one scenario was tried.

### Assumed / not verified

- That the surviving GameBridge task caused the stale screen is inferred from the task dump and the fix working, not from a controlled comparison.
- The release build, other Storefronts and an exit from an Epic game were not retried.
- An earlier pick at 13:17 logged `failure=RemoteUnsupported`; which Shortcut it was is not known.

## 2026-11-02: Artemis entry points on the Odin (read-only)

Read with `adb shell dumpsys package com.limelight.noir` on the Odin. Nothing was installed, started or changed.

### Verified (observed on the device)

- `com.limelight.noir` is installed (versionName 20.2.6, versionCode 57, installed by Obtainium). Earlier logcat showed the user opening its `PcView`, `AppView` and `Game` activities.
- Its manifest declares `com.limelight.ShortcutTrampoline` with an `android.intent.action.VIEW` filter: categories DEFAULT and BROWSABLE, schemes `content` and `file`, path glob `.*.art`, any MIME type. So the package accepts a VIEW of a `content://` URI ending in `.art`.
- It also declares `com.limelight.preferences.AddComputerManually` for VIEW on the `art` scheme, and `com.limelight.PcView` as the launcher entry.
- `ShortcutTrampoline` is reachable from other apps in the sense that the resolver table lists its filter. Whether the activity is `exported`, and what it does with the file, was not read.

### Assumed / not verified

- That `com.limelight.noir` is Artemis (and not a Moonlight fork). The package name and `.art` handling suggest it, but the app's own label was not read.
- What `ShortcutTrampoline` does with the file, and whether it needs a read-URI grant from the sender.
- The `.art` file schema beyond the handoff's listed keys (`host_uuid`, `host_name`, `app_uuid`, `app_name`). No `.art` file was found under `/sdcard/LaunchBox` by name.
- Whether a Dispatch by VIEW starts the stream. A Dispatch is not a Launch.
- Whether LaunchBox could open `.art` through Artemis without GameBridge.

## 2026-11-02: `.art` file format (one real sample)

Read from one Artemis-generated file, `Cuphead (GOG).art` (198 bytes, ASCII text), supplied by the user. The file is not committed: it holds a host UUID and host name that identify the user's PC.

### Verified (observed in the file)

- Two header comment lines, `# Artemis app entry` and `# Generated by Artemis for Android`, then a blank line.
- Four `[key] value` lines in this order: `[host_uuid]` (a UUID), `[host_name]` (the PC's name), `[app_uuid]` (a UUID), `[app_name]` (here the game title with its Storefront, `<title> (GOG)`).
- Plain text with a trailing newline. No other keys.
- The filename is `<app_name>.art`.
- The header names Artemis, which supports (but does not prove) that `com.limelight.noir` is Artemis.

### Assumed / not verified

- That other `.art` files have the same keys and order, or that all four keys are always present.
- That the title suffix, `(GOG)`, is always the Storefront, and that it matters for GameBridge.
- That Artemis resolves a game from these UUIDs on a VIEW. No intent has been sent to Artemis yet.

## 2026-11-02: VIEW of a `.art` file sent to Artemis from adb

With the user's explicit yes, one `am start -a android.intent.action.VIEW -n com.limelight.noir/com.limelight.ShortcutTrampoline -d file:///storage/emulated/0/Games/pc/Cuphead%20(GOG).art` was sent from adb on the Odin. The original file in `Games/pc` was used in place and not modified. It was byte-identical (checked with `cmp`) to the maintainer's local sample in `docs/research`, which is gitignored and not committed because it holds a real host UUID and host name.

### Verified (observed on the device)

- `ShortcutTrampoline` accepted the VIEW from the adb shell and finished itself; `com.limelight.Game` became the top resumed activity about a second later.
- Logcat then showed a video stream being configured (HEVC, 1920x1080, 90 fps) and the line "Resumed existing game session". A session for this game may already have been running on the PC, so this does not show a cold start.
- The user's report of what appeared on screen was not collected in this step.

### Assumed / not verified

- That a `content://` URI with a read grant, as LaunchBox and GameBridge would send, behaves the same as this `file://` URI from the shell user. Not tested.
- That GameBridge can forward the grant to Artemis (`FLAG_GRANT_READ_URI_PERMISSION`). Not tested.
- That the stream shows the right game. The "resumed existing session" line leaves open whether Artemis started Cuphead or reattached to something else.
- A decoder `CodecException` and a restart appeared in the log while the stream came up; its effect was not examined.

## 2026-11-02: Remote Shortcut Dispatch to Artemis built (emulator only)

Ticket `.scratch/artemis-dispatch/issues/01-dispatch-art-shortcut-to-artemis.md`. No physical device was touched.

### Verified (JVM tests and the managed emulator, `pixel2api33DebugAndroidTest`)

- `isRemoteShortcut` is true for a display name ending `.art` in any case, and false for other names. `interpretShortcut` no longer special-cases `.art`.
- `BridgeActivity` reads only the display name of a `.art` Shortcut, then starts `com.limelight.noir/com.limelight.ShortcutTrampoline` with `ACTION_VIEW`, the same `content://` URI as data and `FLAG_GRANT_READ_URI_PERMISSION` (captured with Espresso-Intents; Artemis itself stubbed). No extras, no GameNative Dispatch.
- A provider that throws on opening the file still gets a Dispatch, so the `.art` bytes are not read.
- With Artemis absent (the emulator has none, unstubbed) the dialog shows the Artemis-not-installed reason and nothing else is started. A simulated `SecurityException` shows the refused reason.
- Local Shortcut tests still pass unchanged. Fixture `fixtures/Synthetic.art` uses placeholder values.

### Assumed / not verified

- That real Artemis accepts a `content://` URI with a grant from GameBridge (only a `file://` VIEW from adb is observed). Ticket 03.
- That a Dispatch starts a stream; a Dispatch is not a Launch.
- The refused path is simulated; real Artemis refusals are unobserved. Component and package were seen on 20.2.6 only.

## 2026-11-02: `.art` Dispatch from LaunchBox to Artemis (debug build, Odin)

Debug build installed with `adb install -r` (standing approval). The user picked the existing Cuphead (GOG) `.art` entry in LaunchBox, on the Windows platform with the debug Setup command already in place. No LaunchBox configuration was changed and the original `.art` file was not touched.

### Verified (observed on the device)

- LaunchBox started `BridgeActivity` with action `com.allourmedia.gamebridge.action.DISPATCH` on a `content://` URI (flags `0x10000001`).
- GameBridge then started `com.limelight.noir/com.limelight.ShortcutTrampoline` with `android.intent.action.VIEW` on a `content://` URI (flags `0x10000001`, i.e. new task plus read grant) and logged `remote scheme=content dispatch=Dispatched`.
- Artemis accepted that `content://` URI with the grant: `ShortcutTrampoline` was displayed and `com.limelight.Game` was started by Artemis about a second later.
- The user reports the stream started (a Launch, observed by a person).
- The pick was made twice in the capture (14:31:18 and 14:32:00) with the same sequence both times.
- After the user closed the stream with Artemis's hotkeys, they landed back in LaunchBox, not on the GameBridge setup screen. At the end of the capture the top activity was the Android launcher.

### Assumed / not verified

- That the stream showed the right game (Cuphead). The user said the stream started, not which game.
- Behaviour with a bad or missing `.art` file; not tested.
- Other Artemis versions (only 20.2.6 observed), and the release build.
- Whether the second pick started a new session or resumed the first.
