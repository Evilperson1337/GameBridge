# 02: Tracer bullet: a GOG Shortcut is dispatched to GameNative

**What to build:** This is the narrowest complete path. When the bridge activity is started the way LaunchBox starts it (GameBridge's launch action plus a `content://` URI with a read grant pointing at a valid `.gog` Shortcut), GameBridge reads the Shortcut and dispatches it to GameNative. The Dispatch is an explicit `app.gamenative.LAUNCH_GAME` intent to GameNative's main activity, with `app_id` as an Int and `game_source` set to `GOG`. GameBridge then finishes, without ever showing anything on screen. Only the happy path is needed here; failures can simply finish the activity until ticket 03 adds the dialog. See the spec's "Bridge activity contract", "Dispatch to GameNative" and "Testing Decisions" sections.

**Blocked by:** 01 (Toolchain and project skeleton)

**Status:** ready-for-agent

- [x] `:core` exposes the interpret-a-Shortcut operation (display name and bytes in; dispatch request or rejection out). A valid `.gog` Shortcut produces a GOG dispatch request with its Store ID, tested with JVM tests against the Akalabeth fixture (`1207666073`).
  - Verified: `ShortcutInterpreterTest` (5 JVM tests) passes under `./gradlew check`, including the Akalabeth fixture (1207666073).
- [x] The Storefront table exists in `:core` with just the GOG row (extension `.gog`, `game_source` `GOG`, verified). The parser and Dispatch read the extension and `game_source` from the table, never from hard-coded GOG checks, so ticket 04 only adds rows.
  - Verified: `Storefront.all` has the GOG row; `interpretShortcut` and `GameNativeDispatcher` read extension and `game_source` only from it (code inspected, no GOG checks elsewhere).
- [x] The fixture directory exists, holding the Akalabeth `.gog`.
  - Verified: `fixtures/Akalabeth.gog` (shared by JVM tests and the androidTest assets).
- [x] The single exported activity accepts only GameBridge's own action with `content://` data, uses a transparent theme, and reads the display name and bytes through the content resolver.
  - Verified: merged manifest has one exported activity with the DISPATCH action and `content` scheme; instrumentation test shows a wrong action and a `file:` URI dispatch nothing; transparent theme; reads via ContentResolver.
- [x] The outgoing intent is explicit to `app.gamenative`'s main activity, with only `app_id` (Int) and `game_source` (`GOG`). The incoming URI and its grant are never forwarded.
  - Verified on the emulator via Espresso-Intents: action, component `app.gamenative/app.gamenative.MainActivity`, Int `app_id`, `game_source` GOG, extras exactly those two, no data, no grant flag.
- [x] The manifest declares package visibility for `app.gamenative` only, and requests no permissions.
  - Verified: merged debug manifest has `<queries>` for `app.gamenative` only and no `uses-permission`.
- [x] The activity finishes after Dispatch, and recreating it (for example by rotation) never dispatches a second time.
  - Verified on the emulator: scenario reaches DESTROYED; recreation (via a debug-only subclass that ignores finish) leaves exactly one Dispatch.
- [x] Debug builds log the Storefront, Store ID, URI scheme and Dispatch result under the `GameBridge` logcat tag.
  - Verified: logcat from the emulator run shows `GameBridge: storefront=GOG storeId=1207666073 scheme=content dispatch=Dispatched`.
- [ ] A test-only document provider (in the instrumentation test APK only, never in the app) serves fixture Shortcuts as granted `content://` URIs.
  - Not ticked: the provider is in the test APK only and serves the fixtures, but the read grant is not exercised. It runs in the test APK's process while the test code runs in the app's uid, so the grant call is refused; the provider is exported instead. Needs another approach (for example a grant made from the provider's own process).
- [x] Instrumentation tests on the managed Android 13 emulator use Espresso-Intents to capture and stub the Dispatch and check the action, component and extras (including that `app_id` is an Int). They also cover finishing and the no-duplicate-on-recreation behaviour.
  - Verified: `:app:pixel2api33DebugAndroidTest` ran 4 tests, 0 failures.
- [x] Code and tests use "Dispatch", never "Launch", for this outcome.
  - Verified: grep for launch/launched in app and core sources finds only the external action name, LaunchBox and ActivityScenario.
- [x] `./gradlew check` and the instrumentation suite pass.
  - Verified: both run for real and pass (see investigation log).
