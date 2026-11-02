# 02: Tracer bullet: a GOG Shortcut is dispatched to GameNative

**What to build:** This is the narrowest complete path. When the bridge activity is started the way LaunchBox starts it (GameBridge's launch action plus a `content://` URI with a read grant pointing at a valid `.gog` Shortcut), GameBridge reads the Shortcut and dispatches it to GameNative. The Dispatch is an explicit `app.gamenative.LAUNCH_GAME` intent to GameNative's main activity, with `app_id` as an Int and `game_source` set to `GOG`. GameBridge then finishes, without ever showing anything on screen. Only the happy path is needed here; failures can simply finish the activity until ticket 03 adds the dialog. See the spec's "Bridge activity contract", "Dispatch to GameNative" and "Testing Decisions" sections.

**Blocked by:** 01 (Toolchain and project skeleton)

**Status:** ready-for-agent

- [ ] `:core` exposes the interpret-a-Shortcut operation (display name and bytes in; dispatch request or rejection out). A valid `.gog` Shortcut produces a GOG dispatch request with its Store ID, tested with JVM tests against the Akalabeth fixture (`1207666073`).
- [ ] The Storefront table exists in `:core` with just the GOG row (extension `.gog`, `game_source` `GOG`, verified). The parser and Dispatch read the extension and `game_source` from the table, never from hard-coded GOG checks, so ticket 04 only adds rows.
- [ ] The fixture directory exists, holding the Akalabeth `.gog`.
- [ ] The single exported activity accepts only GameBridge's own action with `content://` data, uses a transparent theme, and reads the display name and bytes through the content resolver.
- [ ] The outgoing intent is explicit to `app.gamenative`'s main activity, with only `app_id` (Int) and `game_source` (`GOG`). The incoming URI and its grant are never forwarded.
- [ ] The manifest declares package visibility for `app.gamenative` only, and requests no permissions.
- [ ] The activity finishes after Dispatch, and recreating it (for example by rotation) never dispatches a second time.
- [ ] Debug builds log the Storefront, Store ID, URI scheme and Dispatch result under the `GameBridge` logcat tag.
- [ ] A test-only document provider (in the instrumentation test APK only, never in the app) serves fixture Shortcuts as granted `content://` URIs.
- [ ] Instrumentation tests on the managed Android 13 emulator use Espresso-Intents to capture and stub the Dispatch and check the action, component and extras (including that `app_id` is an Int). They also cover finishing and the no-duplicate-on-recreation behaviour.
- [ ] Code and tests use "Dispatch", never "Launch", for this outcome.
- [ ] `./gradlew check` and the instrumentation suite pass.
