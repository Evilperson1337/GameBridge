# GameBridge — Implementation Handoff and Engineering Specification

**Version:** 0.1 | **Date:** 2026-11-01 | **Status:** Ready for Phase 0/1 implementation | **Target:** LaunchBox Android 1.23 → GameNative 1.3.0; Artemis later

## 1. Executive summary

GameBridge is a small standalone Android application that lets LaunchBox for Android launch locally installed Windows games through GameNative and, in a later phase, remotely streamed games through Artemis/Moonlight. LaunchBox remains the game library and frontend. GameBridge acts as a trusted, narrow adapter: receive a shortcut URI, read a small identifier file, resolve a supported launch target, and issue the appropriate Android intent. When both local and remote targets exist for one game, show a controller-accessible choice every time; never silently prefer the previous choice.

**Critical discovery:** LaunchBox 1.23 Custom Emulator (With Code) successfully starts a custom exported activity, expands `{file.uri}` to a SAF `content://` URI, grants read access, and a diagnostic receiver successfully reads the `.gog` file. The built-in GameNative route failed with “ROM file ... could not be found” before GameNative activity dispatch. Therefore use Custom Emulator (With Code), not the built-in GameNative route. The exact built-in validation bug is unknown and need not be solved for MVP.

## 2. Goals, scope, and exclusions

### Goals
- One LaunchBox Windows-platform emulator configuration, where supported, for local and remote Windows games.
- Read GameNative-exported `.gog`, `.steam`, `.epic` files without modifying them.
- Launch GameNative using its supported external launch interface.
- Later read Artemis `.art` files, discover matching local/remote copies, and prompt on dual availability.
- No root, game reinstall, changes to game saves, invasive package inspection, or broad storage permissions.
- Develop on CachyOS using Gradle/Kotlin, test Android UI in Waydroid, then validate on AYN Odin 2 Portal.

### Non-goals for MVP
- Game installation, game emulation, streaming implementation, game metadata scraping, LaunchBox database editing, cloud sync, automatic cover-art downloads, or background service.
- Dependence on GameNative internal database schemas or undocumented private activities.
- Automatic fallback to another launcher after a failed launch without user confirmation.

## 3. Verified evidence and confidence ledger

**Evidence supplied from the 2026-11-01 device investigation; treat as previously observed, not independently reproduced in this handoff.**

- Device: AYN Odin 2 Portal, Android 13; LaunchBox 1.23 `com.unbrokensoftware.launchbox`; GameNative 1.3.0 `app.gamenative`.
- LaunchBox Custom Emulator (With Code) is present in the installed UI (scroll to top of emulator picker).
- Tested LaunchBox command: `-n dev.launchboxprobe/.ProbeActivity -a dev.launchboxprobe.CAPTURE -d {file.uri} --es shortcut_path {file.path}`.
- Diagnostic receiver received `content://com.android.externalstorage.documents/tree/.../document/.../Akalabeth.gog`, `FLAG_GRANT_READ_URI_PERMISSION`, and read `1207666073` successfully with `ContentResolver`; neither file placeholder produced a raw path in that test.
- A normally imported `.gog` entry under the built-in GameNative emulator displayed missing-ROM error; Android logs showed no GameNative activity start. Same entry worked with Custom Emulator (With Code).
- Verified GameNative explicit launch: `am start -n app.gamenative/.MainActivity -a app.gamenative.LAUNCH_GAME --ei app_id 1207666073 --es game_source GOG`; Akalabeth reached its Ready screen.
- Verified GameNative deep link shape: `gamenative://run?appid=1207666073&gamesource=GOG`.
- Steam and Epic launch behavior on the Odin not yet verified. Artemis external launch contract not yet verified.
- Probe APK and temporary test data were removed; original LaunchBox XML restored with SHA-256 `4d138de4b2cfe9a2de2dcb60049af6a56c4f245db3bb56c7c72e9cf719416684`.

**Reference sources:**
- LaunchBox custom emulator guide: https://feedback.launchbox-app.com/en/help/articles/7096169-custom-emulator-with-code
- LaunchBox Android getting started: https://feedback.launchbox-app.com/articles/1026472-getting-started-with-launchbox-for-android
- GameNative v1.3.0 source: https://raw.githubusercontent.com/utkarshdalal/GameNative/v1.3.0/app/src/main/java/app/gamenative/utils/IntentLaunchManager.kt

## 4. Sample shortcut fixtures

Attached fixture examples and their contents:

| File | Content | Meaning / caveat |
|---|---|---|
| `Broforce.gog` | `1470490225` | GOG identifier; game installation known incomplete, **do not use for successful-launch test** |
| `Broforce.steam` | `274190` | Steam App ID |
| `ABZU.epic` | `64` | GameNative-exported Epic identifier; do not assume universal Epic catalog ID |
| `Broforce (GOG).art` | `[host_uuid] ...`, `[host_name] ...`, `[app_uuid] ...`, `[app_name] ...` | Artemis shortcut; confirm parser/launcher behavior in later phase |

Known working GOG fixture: `Akalabeth_ World of Doom.gog` with text `1207666073` at `/storage/emulated/0/Games/pc/` on the Odin. Use a temporary copy and a temporary LaunchBox entry for tests; do not alter originals.

## 5. Architecture

```
LaunchBox (Windows platform)
  Custom Emulator (With Code) → Android Intent (data = SAF URI + read grant)
        ↓
GameBridge LaunchActivity (exported; validates incoming URI and grant)
        ↓
ShortcutReader (ContentResolver, size-bounded) → ShortcutParser
        ↓
GameIdentity / TargetResolver (MVP: supplied local shortcut)
        ↓
LaunchCoordinator → GameNativeAdapter (MVP)
                 ↘ ArtemisAdapter + LauncherPicker (later)
```

### Modules
- `:app`: exported entry activity, Android integration, Compose UI, intent dispatch, error states.
- `:core`: pure Kotlin shortcut formats, models, validation, matching; JVM unit tests.
- Optional later `:testing:fake-target`: harmless mock launcher used in Waydroid tests, not a production dependency.

Avoid overmodularizing before a functional MVP. Interfaces should be thin and testable; don't create a plugin framework prematurely.

### Models (suggested)
- `ShortcutSource`: `GOG`, `STEAM`, `EPIC`, `ARTEMIS`.
- `ParsedShortcut`: sealed types `LocalShortcut(source, numericId, displayName?)` and `RemoteShortcut(hostUuid, appUuid, hostName?, appName?)`.
- `LaunchTarget`: `GameNativeTarget` or `ArtemisTarget`, with stable identity and origin URI.
- `LaunchResult`: success-dispatched vs recoverable failure; **intent dispatch success does not prove a game booted**.
- `GameAssociation`: user-confirmed match between local and remote records, with provenance; later phase only.

## 6. Android intent and storage contract

### LaunchBox configuration (proposed production contract)

```
-n dev.gamebridge/.LaunchActivity -a dev.gamebridge.LAUNCH -d {file.uri}
```

`dev.gamebridge` is a placeholder package ID; select a permanent unique `applicationId` before publishing. The bridge activity must be exported for explicit external invocation, validate `intent.action`, validate `intent.data` is a `content://` URI, and require a readable grant. The probe observed `0x10000001` flags, including read permission. Do not rely on a filesystem path, and do not assume `{file.path}` returns one. Test with the final package name and actual LaunchBox UI.

### Reading the shortcut
- Query `OpenableColumns.DISPLAY_NAME` for filename/extension; fall back cautiously to document metadata if necessary.
- Use `ContentResolver.openInputStream(uri)` with a hard maximum byte count (e.g. 4 KiB for local shortcuts, 16 KiB for `.art`); reject oversized or unreadable documents.
- Decode UTF-8, trim whitespace, optionally strip UTF-8 BOM; local shortcut must contain one positive decimal integer, no other meaningful lines.
- Map extension to a fixed allowlist; reject unknown extensions and unsupported source types with actionable errors.
- Never log entire shortcut content or persist granted URI without a deliberate persisted SAF grant. A one-shot LaunchBox read grant may expire; use directory picker grants for future indexing.
- Handle missing URI, `SecurityException`, `FileNotFoundException`, provider failures, invalid metadata, overflow and malformed text.

### GameNative dispatch

```kotlin
Intent("app.gamenative.LAUNCH_GAME").apply {
    setClassName("app.gamenative", "app.gamenative.MainActivity")
    putExtra("app_id", parsedId) // Int, checked range
    putExtra("game_source", "GOG") // use verified canonical source value
}
```

Launch with `startActivity` from a foreground activity, catch `ActivityNotFoundException` and `SecurityException`, and provide a useful failure UI. Verify target package availability without requesting unnecessarily broad package visibility; use explicit package queries if needed. Never silently downgrade to a deep link if the explicit intent fails; fallback behavior must be deliberately specified and tested.

**Open question:** GOG ID is read as an Android Int in the verified contract. Reject IDs outside signed 32-bit range rather than truncating. Confirm canonical source enum strings and on-device behavior for Steam and Epic before enabling those launch buttons.

## 7. UX and user journeys

### MVP
- LaunchBox entry selected → GameBridge receives URI → read and validate `.gog` → dispatch GameNative → finish bridge activity.
- Failure → minimal dialog/screen with filename (when available), reason, and Back/Close; do not expose raw stack traces.
- Diagnostic build optionally displays parsed source, ID, URI scheme and dispatch status; redact sensitive paths from release logs.

### Later: local/remote selection
- Only local target: launch local directly.
- Only remote target: launch remote directly.
- Both targets: **always prompt** with “Play locally (GameNative)” and “Stream from PC (Artemis)”; no automatic remembered selection.
- Multiple remote hosts: display host selection; no silent choice.
- Neither target: actionable error; don't claim availability based solely on shortcut existence.
- Picker must support touchscreen, D-pad, A/B buttons, accessibility focus and landscape orientation.
- Matching: prefer stable IDs where an authoritative cross-source mapping exists; otherwise normalized titles with confirmation. Don't automatically merge ambiguous editions or same-name games.

## 8. Repository structure

```
gamebridge/
├── AGENTS.md
├── README.md
├── docs/
│   ├── decisions.md
│   ├── launchbox-setup.md
│   ├── device-testing.md
│   └── investigation-log.md
├── app/src/main/
│   ├── AndroidManifest.xml
│   └── java/dev/gamebridge/{LaunchActivity.kt,android/,ui/}
├── core/src/main/kotlin/dev/gamebridge/{model/,parser/,matching/}
├── core/src/test/kotlin/dev/gamebridge/
├── test-fixtures/shortcuts/
├── gradle/libs.versions.toml
├── gradlew
└── settings.gradle.kts
```

Use Gradle wrapper, pinned dependency versions, reproducible build instructions, formatting/linting, JVM unit tests, and Android instrumentation tests. Avoid committing secrets, local SDK paths, device identifiers, or actual game files.

## 9. CachyOS and Waydroid development environment

1. Inventory `java`, `adb`, Android SDK, Gradle wrapper support, Waydroid and binder device availability; don't change the user's existing KDE/NVIDIA configuration just to install tooling.
2. Use JDK 21 only if compatible with the selected Android Gradle Plugin; confirm current AGP/Kotlin/Compose compatibility before pinning versions.
3. Install Android SDK command-line tools, platform-tools, build-tools and a chosen SDK platform. Prefer SDK manager; document `ANDROID_HOME`/`ANDROID_SDK_ROOT` conventions and shell-specific setup.
4. Start Waydroid and confirm an APK can install and run. NVIDIA proprietary driver may require software rendering; treat it as a test-environment issue, not a blocker for JVM parser tests.
5. Build with `./gradlew :core:test :app:assembleDebug`; run Android instrumentation when Waydroid is usable.
6. In Waydroid use a mock receiver/launcher if GameNative cannot run correctly. Real GameNative launches must be tested on the Odin.
7. On the Odin use `adb devices`, `adb install -r ...`, LaunchBox Custom Emulator (With Code), and filtered `adb logcat` to confirm the complete path.

Avoid changing GPU drivers, kernel modules, system-wide security policies, or existing game installations without explicit permission.

## 10. Phased delivery and gates

### Phase 0 — Repository and environment (Gate G0)
- Create repository, docs, Gradle wrapper, basic app/core modules, CI-friendly unit-test command.
- Confirm SDK/JDK toolchain and Waydroid status; capture setup commands.
- **Exit:** clean checkout builds debug APK and passes placeholder JVM tests.

### Phase 1 — GOG bridge MVP (Gate G1)
- Implement exported `LaunchActivity` and safe URI read.
- Parse Akalabeth `.gog` ID `1207666073`.
- Dispatch verified GameNative GOG intent; error handling and tests.
- Test with temporary Akalabeth LaunchBox entry on Odin, after configuration backup.
- **Exit:** LaunchBox → GameBridge → GameNative → Akalabeth Ready screen on Odin; no original file or save modifications.

### Phase 2 — Steam and Epic (Gate G2)
- Add parsers with fixture tests; investigate actual GameNative `GameSource` strings and Android launch behavior for installed examples.
- Add per-store launch tests; if no installed game is available, mark platform unverified rather than inventing success.
- **Exit:** supported stores verified with representative on-device examples or explicitly disabled pending evidence.

### Phase 3 — Artemis research and adapter (Gate G3)
- Validate `.art` file schema and whether Artemis exposes an exported activity, deep link, or file-open action.
- Verify host/app UUID semantics, availability and launch errors; implement only supported public interface.
- **Exit:** one confirmed remote launch from LaunchBox through bridge, or a documented blocker and safe stop.

### Phase 4 — Discovery, matching, picker (Gate G4)
- Let user select shortcut directories through SAF `ACTION_OPEN_DOCUMENT_TREE`; persist only explicitly granted directory access.
- Index local and remote shortcuts; support incremental rescan and missing-file handling; store only necessary metadata locally.
- Add confirmed associations, conflict resolution and launcher choice UI.
- **Exit:** same game with local + remote shortcut prompts every launch; single target launches directly; controller works.

### Phase 5 — Release readiness (Gate G5)
- Test reinstall/upgrade, Android process recreation, provider access failures, controller focus, multiple hosts, and absence of target apps.
- Produce APK, changelog, setup guide, privacy statement and troubleshooting guide.
- **Exit:** documented regression suite passes on Odin and a second Android environment when available.

## 11. Test matrix

| Case | JVM | Waydroid | Odin |
|---|---|---|---|
| Valid `.gog` ID, BOM, newline | Yes | Yes | Yes |
| `.steam` and `.epic` parser | Yes | Yes | Once target available |
| Malformed/oversized/negative/overflow ID | Yes | Yes | Spot-check |
| Missing URI / revoked grant | N/A | Yes | Yes |
| SAF display-name query failure | Mock | Yes | Spot-check |
| LaunchBox sends URI and read grant | N/A | If LaunchBox installed | **Required** |
| Explicit GameNative GOG launch | N/A | Mock possible | **Required** |
| Steam/Epic real launch | N/A | Mock possible | **Required before claim** |
| `.art` parser and launch | Yes | Mock possible | Later required |
| Controller picker and Back handling | N/A | Simulate | Later required |
| No writes to originals | Yes | Yes | **Required** |

Record logs and screenshots only when necessary; remove personally identifying host/device details from published artifacts.

## 12. Security and reliability requirements

- Export only the necessary bridge activity. Use explicit action checks; avoid an overly broad implicit intent filter.
- Treat incoming URIs and shortcut text as untrusted; allowlist `content://`, extensions, supported storefront values, and numeric range.
- Do not fetch arbitrary URLs from shortcut content or treat text as shell commands.
- No `MANAGE_EXTERNAL_STORAGE`, root, accessibility service, background service, or network permission for MVP.
- Never grant target apps arbitrary access to incoming shortcut URIs; translate to validated identifiers.
- Ensure no unintended exported components, debuggable production build, or sensitive logging.
- If target app is missing, show install/configuration guidance rather than crash.
- Use a foreground launch flow consistent with Android background activity restrictions.
- Protect against duplicate rapid launches and lifecycle recreation; don't accidentally launch twice on rotation.

## 13. Acceptance criteria and definition of done

MVP is complete only when all are true:
1. `./gradlew :core:test :app:assembleDebug` passes from clean checkout.
2. LaunchBox's Windows platform Custom Emulator (With Code) invokes GameBridge with `-d {file.uri}`.
3. Bridge reads Akalabeth's GOG shortcut via ContentResolver without broad storage permissions.
4. Bridge sends `LAUNCH_GAME` with `app_id` as Int and `game_source=GOG`.
5. Akalabeth reaches its ready screen on the Odin, verified via log/screen observation.
6. Missing URI, unreadable URI, invalid ID, and missing GameNative produce actionable errors.
7. No originals, saves, or unrelated LaunchBox entries are modified; rollback is documented.
8. README includes exact setup, build, deployment, troubleshooting and limitations.

## 14. Open questions / decisions to resolve

- Permanent package ID and application display name (working name: **GameBridge**).
- Whether LaunchBox reliably grants URI read access across additional Android versions/providers.
- Canonical Steam/Epic `game_source` strings and on-device external-launch success.
- Artemis's actual public launch interface; avoid assuming `.art` UUIDs are directly launchable.
- How to identify duplicate titles/editions without false matches.
- Whether Artemis uses a separate Android package or is integrated into a Moonlight client in the target environment.
- Target/minimum SDK and dependency versions after current compatibility audit.
- Whether to add an optional in-app shortcut test harness before indexing.

## 15. Agent execution rules (AGENTS.md seed)

**Mission:** Implement GameBridge in phases, optimizing for evidence-based behavior and a small maintainable codebase.

- Read this spec and `docs/decisions.md` before coding. Create/update an investigation log and explicit acceptance tests.
- Start with Phase 0 and Phase 1 only. Do not build Artemis discovery, database, or launcher picker prematurely.
- Before each phase, inspect actual toolchain and relevant upstream source/docs; record verified vs assumed behavior.
- Keep all tests non-destructive; use temporary shortcut copies and back up LaunchBox configuration before Odin tests.
- Do not modify GameNative/LaunchBox sources, original exported shortcuts, installed games, or saves.
- Request approval before installing on the physical device, changing its LaunchBox configuration, or making system-wide CachyOS changes beyond agreed dependencies.
- Favor minimal, independently testable Kotlin classes; use Compose only where Android UI is needed.
- Implement failures explicitly, do not mask launch errors or claim game startup from `startActivity` alone.
- At each gate deliver: source diff, tests run/results, device evidence, unresolved issues, rollback steps, and recommendation to proceed or stop.

### First Codex/Claude task

> Initialize the GameBridge Android repository for CachyOS development. Review this specification, inspect available JDK/Android SDK/Waydroid tools, choose compatible pinned Kotlin/AGP/Compose versions, and implement **only Phase 0 and Phase 1**. Use the verified LaunchBox Custom Emulator (With Code) URI contract and GameNative GOG explicit intent. Write parser and Android error-path tests. First prove the debug APK can receive a SAF URI and read a temporary Akalabeth shortcut; then request approval before changing LaunchBox configuration or installing/testing on the Odin. Do not implement Artemis, automatic matching, or Steam/Epic dispatch yet. Provide exact build/deployment commands, acceptance results, and rollback plan.

## 16. Rollback and preservation checklist

- Before Odin work: capture LaunchBox platform emulator configuration and back up relevant XML/config; note original default.
- Use only temporary Akalabeth copy and temporary LaunchBox record; record file paths.
- Install debug bridge under its own unique package; do not replace GameNative or LaunchBox.
- To revert: restore original LaunchBox emulator selection/config, remove temporary entry and file, uninstall debug bridge if desired, verify original shortcut contents and game data unchanged.
- Do not assume a previously recorded `/tmp` backup still exists: the prior investigation cleaned its temporary artifacts.

---

**Engineering recommendation:** Start Phase 0/1 immediately. The LaunchBox dispatch and SAF-read gates have been demonstrated; the remaining highest-value proof is a real bridge APK launching the known-good GOG game on the Odin.
