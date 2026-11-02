# 03: Error dialog for intent, read and dispatch failures

**What to build:** When GameBridge can't dispatch, the player sees a short, controller-friendly dialog over the transparent activity. It shows the Shortcut's filename (when known), a one-line reason, and a single Close button that returns to LaunchBox. This ticket covers the failures outside Shortcut interpretation:
- started without a Shortcut, or with the wrong action
- a non-`content` URI
- a Shortcut that can't be read
- GameNative not installed
- GameNative refusing the request

No retry, no details, no stack traces, and never a fallback to another app or a deep link. See the spec's "Error dialog", "Dispatch to GameNative", "Logging" and "Security" sections.

**Blocked by:** 02 (Tracer bullet: a GOG Shortcut is dispatched to GameNative)

**Status:** resolved

- [x] The dialog shows the filename (when known) and a reason mapped from a failure category. Close and Back both finish the activity, and the dialog works with D-pad, A/B buttons and touch.
  - Verified on the emulator: dialog text (filename plus reason, or reason alone) is asserted; Close (injected touch tap), Back, B (BUTTON_B), A (BUTTON_A) and D-pad centre each finish the activity.
- [x] Starting GameBridge with no data (for example from the app drawer) explains that it's meant to be started from LaunchBox.
  - Verified on the emulator for MAIN with no data (app drawer) and for the bridge action with no data. A launcher entry was added so the app drawer case can occur.
- [x] A wrong action and a non-`content` scheme each produce their own clear reason.
  - Verified on the emulator: each shows its own reason.
- [x] Unreadable Shortcuts (missing or revoked grant, missing file, provider error, unreadable display name) produce "the Shortcut couldn't be read".
  - Verified on the emulator with a real unexported provider: missing grant, revoked grant, missing file, provider exception, no display name.
- [ ] An `ActivityNotFoundException` produces "GameNative is not installed", naming GameNative's GitHub releases page in plain text. A `SecurityException` produces "GameNative refused the request".
  - Verified: ActivityNotFoundException from a real, unstubbed Dispatch (GameNative is not on the emulator) shows the not-installed reason with the releases URL. Not ticked: the SecurityException case is simulated (a debug-only activity throws it from startActivity), because the emulator has no GameNative that refuses.
- [ ] Release builds log only the failure category: no Shortcut contents, URIs or paths.
  - Not ticked: by code inspection only `failure=<CATEGORY>` is logged outside debug builds (other logs are gated on FLAG_DEBUGGABLE). Not observed on a running release build.
- [x] Release builds aren't debuggable, and the bridge activity is the only exported component.
  - Verified: aapt2 badging of the release APK and the merged release manifest show no debuggable flag, one activity (BridgeActivity, exported), no permissions and no other components.
- [x] Instrumentation tests cover each case above, checking the dialog's text and that Close finishes the activity. The "GameNative is not installed" case uses a real, unstubbed Dispatch on the emulator, where GameNative isn't installed.
  - Every case has a test (19 tests); the refusal case is simulated, see above.
- [x] `./gradlew check` and the instrumentation suite pass.
  - Verified: `./gradlew check --rerun-tasks` and `:app:pixel2api33DebugAndroidTest` (19 tests, 0 failures, run twice in a row) both pass.

## Answer

Implemented on `ticket-03`. Unticked: the SecurityException reason (simulated only) and release-build logging (code inspection only). Shortcut interpretation failures (unsupported type, bad contents) still just finish; ticket 04 adds their dialog. Details in `docs/investigation-log.md`.

## Answer

Implemented on `ticket-03` (merged, tip fa29b8b). Two criteria left unticked: the SecurityException case uses a simulated refusal, and release-build logging was checked by code inspection only.
