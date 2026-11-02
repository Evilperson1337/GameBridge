# 01: Setup screen with Setup command

**What to build:** A player who opens GameBridge from the app drawer sees a setup screen instead of an error dialog. It shows the Setup command for the installed build (debug and release each show their own), a Copy button that puts exactly that text on the clipboard, whether GameNative is installed (text only), and a three-line checklist: set the Windows platform's default emulator to Custom Emulator (With Code), paste the command, clear any per-game emulator override. The Setup command comes from a pure function in the core module that takes the application ID. The screen dispatches nothing and adds no permission, network, service or background work. The existing no-Shortcut dialog stays for calls from LaunchBox that carry no Shortcut. See the spec's "Implementation Decisions" and "Testing Decisions".

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] JVM tests show the Setup command builder returns the exact debug and release strings given in the LaunchBox setup guide.
- [ ] An instrumented test shows the screen displays the command for the installed build, Copy places that text on the clipboard, and GameNative present or absent is reported.
- [ ] An instrumented test asserts the displayed action and component match what the manifest resolves, so the screen cannot drift from the real app.
- [ ] Opening GameBridge from the app drawer no longer shows the "meant to be started from LaunchBox" dialog; a call from LaunchBox with no Shortcut still does, and its existing test passes.
- [ ] No new permission, network access, service or background work; no link-out or version check for GameNative.
- [ ] `./gradlew check` and the instrumentation task pass.
