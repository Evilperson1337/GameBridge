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
