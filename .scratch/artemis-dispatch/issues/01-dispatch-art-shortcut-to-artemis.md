# 01: Dispatch a .art Shortcut to Artemis

**What to build:** A `.art` Remote Shortcut handed to GameBridge by LaunchBox is validated (content URI and `.art` extension; the file is never opened, so the read-size cap does not apply) and Dispatched to Artemis as an explicit `ACTION_VIEW` on the same URI with a read grant. GameBridge does not parse the file. Local Shortcuts are unchanged. See the spec's "Implementation Decisions" and "Testing Decisions".

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] JVM tests show a `content://` URI ending `.art` is classified as a Remote Shortcut, and non-content URIs or other extensions are rejected as before. Checked: `isRemoteShortcut` tests in `ShortcutInterpreterTest`; non-content URIs still fail in `IntentTriageTest`; `./gradlew check` green.
- [x] An instrumented test shows a `.art` content URI produces an explicit VIEW to Artemis's `ShortcutTrampoline` with the same data URI and the read-grant flag. Checked: `artShortcutIsDispatchedToArtemisAsAViewOfTheSameUriWithAReadGrant` on the managed emulator. The intent is the real one captured by Espresso-Intents; Artemis's side is stubbed, so real acceptance is ticket 03.
- [x] `<queries>` lists `com.limelight.noir`; no new permission, network, service or background work. Checked: manifest diff adds only the `<package>` entry; grep finds no `uses-permission` or `<service`.
- [x] A Local Shortcut Dispatch to GameNative is unchanged and its existing tests pass. Checked: all existing `BridgeActivityTest` Local Shortcut tests pass unchanged on the emulator (41 tests, 0 failures in the main run).
- [x] No fallback to GameNative or a deep link when Artemis cannot be used. Checked: Artemis-absent test is unstubbed on the emulator (no Artemis installed) and asserts zero GameNative Dispatches and one attempted Artemis component.
- [x] `./gradlew check` and the instrumentation task pass. Checked: `./gradlew check` and `pixel2api33DebugAndroidTest` both ran successful.
