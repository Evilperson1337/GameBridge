# 03: Error dialog for intent, read and dispatch failures

**What to build:** When GameBridge can't dispatch, the player sees a short, controller-friendly dialog over the transparent activity. It shows the Shortcut's filename (when known), a one-line reason, and a single Close button that returns to LaunchBox. This ticket covers the failures outside Shortcut interpretation:
- started without a Shortcut, or with the wrong action
- a non-`content` URI
- a Shortcut that can't be read
- GameNative not installed
- GameNative refusing the request

No retry, no details, no stack traces, and never a fallback to another app or a deep link. See the spec's "Error dialog", "Dispatch to GameNative", "Logging" and "Security" sections.

**Blocked by:** 02 (Tracer bullet: a GOG Shortcut is dispatched to GameNative)

**Status:** ready-for-agent

- [ ] The dialog shows the filename (when known) and a reason mapped from a failure category. Close and Back both finish the activity, and the dialog works with D-pad, A/B buttons and touch.
- [ ] Starting GameBridge with no data (for example from the app drawer) explains that it's meant to be started from LaunchBox.
- [ ] A wrong action and a non-`content` scheme each produce their own clear reason.
- [ ] Unreadable Shortcuts (missing or revoked grant, missing file, provider error, unreadable display name) produce "the Shortcut couldn't be read".
- [ ] An `ActivityNotFoundException` produces "GameNative is not installed", naming GameNative's GitHub releases page in plain text. A `SecurityException` produces "GameNative refused the request".
- [ ] Release builds log only the failure category: no Shortcut contents, URIs or paths.
- [ ] Release builds aren't debuggable, and the bridge activity is the only exported component.
- [ ] Instrumentation tests cover each case above, checking the dialog's text and that Close finishes the activity. The "GameNative is not installed" case uses a real, unstubbed Dispatch on the emulator, where GameNative isn't installed.
- [ ] `./gradlew check` and the instrumentation suite pass.
