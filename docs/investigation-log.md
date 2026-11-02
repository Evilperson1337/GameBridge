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

### Verified (by reading the repo; nothing run on a device)

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
