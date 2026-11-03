# 01: Setup screen with Setup command

**What to build:** A player who opens GameBridge from the app drawer sees a setup screen instead of an error dialog. It shows the Setup command for the installed build (debug and release each show their own), a Copy button that puts exactly that text on the clipboard, whether GameNative is installed (text only), and a three-line checklist: set the Windows platform's default emulator to Custom Emulator (With Code), paste the command, clear any per-game emulator override. The Setup command comes from a pure function in the core module that takes the application ID. The screen dispatches nothing and adds no permission, network, service or background work. The existing no-Shortcut dialog stays for calls from LaunchBox that carry no Shortcut. See the spec's "Implementation Decisions" and "Testing Decisions".

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] JVM tests show the Setup command builder returns the exact debug and release strings given in the LaunchBox setup guide.
- [x] An instrumented test shows the screen displays the command for the installed build, Copy places that text on the clipboard, and GameNative present or absent is reported. Command text and clipboard: `SetupActivityTest` (emulator). Present or absent: later work (tickets artemis-05 and artemis-07) added emulator tests for installed (a registered stand-in package, real icon) and not-installed tiles; the emulator has no real GameNative. On the Odin, with real GameNative installed, the screen was seen showing it as installed (2026-11-02 and 2026-11-03, see the investigation log).
- [x] An instrumented test asserts the displayed action and component match what the manifest resolves, so the screen cannot drift from the real app.
- [x] Opening GameBridge from the app drawer no longer shows the "meant to be started from LaunchBox" dialog; a call from LaunchBox with no Shortcut still does, and its existing test passes.
- [x] No new permission, network access, service or background work; no link-out or version check for GameNative.
- [x] `./gradlew check` and the instrumentation task pass.

## Notes

- Criterion 1: `SetupCommandTest` (JVM) compares against literal strings; red first (unresolved reference), then green.
- Criterion 2 left unticked: screen text, Copy to clipboard (real ClipboardManager) and "GameNative: not installed" pass on the managed emulator (`pixel2api33DebugAndroidTest`, 6 SetupActivityTest tests, 0 failures). The "installed" case is not covered: the emulator has no GameNative and I did not install it.
- Device check (2026-11-02, Odin): the setup screen showed "GameNative: installed" and the user confirmed Copy worked. This was a manual observation, not an instrumented test, so criterion 2 stays unticked. See `docs/investigation-log.md`.
- Criterion 3: `SetupActivityTest` parses the displayed `-n`/`-a` and checks `PackageManager.resolveActivity` returns BridgeActivity in this package.
- Criterion 4: a test asserts the only launcher entry is `SetupActivity`; `BridgeActivityTest` (29 tests, including the no-Shortcut MAIN/own-action cases) passes unchanged. Not exercised: tapping the drawer icon on a real launcher.
- Criterion 5: manifest diff only moves the LAUNCHER filter to the new activity; no permissions, services or network code; no GameNative link-out.
- Criterion 6: `./gradlew check` and `pixel2api33DebugAndroidTest` both passed.
