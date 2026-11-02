# DroidDeck as a second Target App: research notes

Researched 2026-11-02 from primary sources only: the official repository `Droid-Deck/DroidDeck` (source, manifest, docs and release notes, read at tag `0.6.9`, commit `0a07926e`, and at `main` `55da0afd` from 2026-11-02), the GitHub release page, and PR #277. I read the cloned source but did not build or run it. News articles were used only to find the repository. Anything I could not confirm against a primary source is marked **UNVERIFIED**.


## Verdict

**Difficulty: moderate.** DroidDeck is open source (GPL-3.0), its package is `com.droiddeck.launcher`, and it has a public launch contract. It deliberately copies GameNative's: it exports plain-ID **`.steam`** files and accepts GameNative's own `app.gamenative.LAUNCH_GAME` intent shape (`app_id`, `game_source=STEAM`). So the dispatch code barely changes. Two things make it more than a data change:

- **`.steam` collides completely with GameNative.** The extension is the same and so is the format, so a Shortcut alone can't say which Target App it belongs to. GameBridge needs a Target App choice made by user configuration.
- **DroidDeck's added and store games (GOG, Epic, Amazon and others) use unsigned 64-bit IDs.** These are all exported as `.steam` too. GameBridge's signed 32-bit Store ID rule rejects them, and the Int `app_id` extra can't carry them.

**Plainly:** DroidDeck accepts `ACTION_VIEW` on a `content://` Shortcut and reads the file itself. LaunchBox could therefore probably launch DroidDeck **directly** with no bridge, so GameBridge adds little for DroidDeck (**UNVERIFIED on device**).

## 1. Identity and frontend export

**Identity**
- **Name:** DroidDeck. **Package ID:** `com.droiddeck.launcher` ([`app/build.gradle` L134](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/build.gradle#L134); `versionName "0.6.9"` at [L138](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/build.gradle#L138)). The newest release, marked Latest, is 0.6.9 ([releases](https://github.com/Droid-Deck/DroidDeck/releases)).
- **Open source:** yes, GPL-3.0 ([LICENSE](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/LICENSE), [README "Credits and licence"](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/README.md#credits-and-licence)). The README says "100% open source. No ads. No telemetry. No source withheld."
- **Official sources:** only the GitHub repo and its Discord. The README says: "DroidDeck has no website. Only download it from this repository's Releases." ([README](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/README.md)). Some news articles link a `droiddeck.app` site. Per the README, that site is **not official**.
- **Extra packages:** each release also ships variant APKs under other packages. [`tools/release/variants.txt`](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/tools/release/variants.txt) lists `com.tencent.ig` (`-pubg`), `com.antutu.benchmark.full` (`-antutu`) and `com.ludashi.benchmark` (`-ludashi`). The 0.6.9 release assets include `DroidDeck-0.6.9-pubg.apk`, `-antutu.apk` and `-ludashi.apk` ([0.6.9 assets](https://github.com/Droid-Deck/DroidDeck/releases/tag/0.6.9)).

**Export: yes.** Each game page offers "Export game file", and there is also "Sync all game files to folder", which re-syncs every 30 s ([`strings.xml`](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/res/values/strings.xml) `game_file_export`, `game_file_sync`; [`GameFileSync.kt`](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/GameFileSync.kt)).

- **Extension `.steam`.** The only extension is `steam`, for every game, including added games and store installs:
  ```kotlin
  const val EXTENSION = "steam"
  private const val MAX_BYTES = 128
  ```
  [`frontend/GameFiles.kt` L15–16](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/GameFiles.kt#L15-L16)
- **History.** 0.3.1 exported `.droiddeck` files ([0.3.1 notes](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/docs/releases/0.3.1.md): "Export per-game `.droiddeck` launch files and Steam icons for Android frontends"). Those files contained `droiddeck://game/<id>\n`. PR #277 (merged 2026-10-05) replaced them: "Replace custom `.droiddeck` exports with `Game Name.steam` files containing only the decimal game ID" ([PR #277](https://github.com/Droid-Deck/DroidDeck/pull/277); diff in commit `2e6e35fb`). The PR's branch name was `frontend-steam-compatibility`. The current reader no longer accepts the old link-in-file form (`return text.takeIf(GameLaunchLink::validId)`, [GameFiles.kt L48](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/GameFiles.kt#L48)).
- **Contents.** The bare decimal ID as UTF-8 text with no newline (`AtomicFile.writeText(id)`):
  ```kotlin
  private fun contents(id: String): String {
      require(GameLaunchLink.validId(id))
      return id
  }
  ```
  [GameFiles.kt L67–70](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/GameFiles.kt#L67-L70)
- **Which ID.** "The ID is the value accepted by `steam://rungameid/`, including an AddedGames shortcut ID that occupies the unsigned 64-bit range" ([`docs/testing/game-launcher-shortcuts.md`](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/docs/testing/game-launcher-shortcuts.md)). The ID must match `[1-9][0-9]{0,19}` and parse as a `ULong` ([`GameLaunchLink.kt` L4](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/GameLaunchLink.kt#L4)).
  - **Steam games:** the Steam app ID, for example `620` or `8400`.
  - **Added games:** `(appId shl 32) or 0x02000000`, where `appId = crc or 0x80000000` ([`AddedGames.kt` L408, L423](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/AddedGames.kt#L408)). This is always ≥ 2⁶³ and is written unsigned (`Long.toUnsignedString`, [`Library.kt` L24](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/Library.kt#L24)). The exception is an added game that maps to a real Steam app ID.
  - **Store games:** DroidDeck's GOG, Epic, Amazon and other store installs are added games ("the on-disk contract that makes a store game an ordinary added game", [`docs/development/stores.md`](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/docs/development/stores.md)). They get these 64-bit IDs and are exported as `.steam` too. There are no `.gog`, `.epic` or `.amazon` exports.
- **File names:** `Game Name.steam`, or `Game Name (<id>).steam` when two games would get the same name. Icons are written to `images/<slug>-icon.<ext>` for Cocoon ([GameFiles.kt `filename`, `exportArt`](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/GameFiles.kt)). Sync keeps an inventory file, `.droiddeck-sync.json`, with format `droiddeck-game-files-v1`, in the export folder.

## 2. External launch interface

All entry points go to the exported `.FrontEndActivity` ([manifest L104–125](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/AndroidManifest.xml#L104-L125)):

```xml
<activity android:name=".FrontEndActivity" android:exported="true" ...>
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <category android:name="android.intent.category.CAR_LAUNCHER" />
        <data android:scheme="droiddeck" android:host="game" />
    </intent-filter>
    <intent-filter> <action android:name="android.intent.action.CREATE_SHORTCUT" /> ... </intent-filter>
    <intent-filter>
        <action android:name="com.droiddeck.launcher.LAUNCH_GAME" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.CAR_LAUNCHER" />
    </intent-filter>
</activity>
<activity-alias android:name=".MainActivity" android:targetActivity=".FrontEndActivity" android:exported="true"> (MAIN/LAUNCHER)
```

The parsing lives in [`frontend/GameLaunchIntent.kt`](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/GameLaunchIntent.kt) (unchanged between `0.6.9` and `main`):

```kotlin
const val ACTION = "com.droiddeck.launcher.LAUNCH_GAME"
const val EXTRA_APP_ID = "app_id"
const val EXTRA_GAME_SOURCE = "game_source"
private const val GAMENATIVE_ACTION = "app.gamenative.LAUNCH_GAME"

fun accepts(action: String?): Boolean = action == Intent.ACTION_VIEW || action == ACTION || action == GAMENATIVE_ACTION
fun read(context: Context, intent: Intent): String? = when (intent.action) {
    Intent.ACTION_VIEW -> GameFiles.readIntent(context, intent)
    ACTION, GAMENATIVE_ACTION -> readId(intent)
    else -> null
}
internal fun readId(intent: Intent): String? = runCatching {
    ...
    val source = intent.extras?.get(EXTRA_GAME_SOURCE)
    if (source != null && (source !is String || !source.equals("STEAM", ignoreCase = true))) return null
    val id = when (val value = intent.extras?.get(EXTRA_APP_ID)) {
        is Int -> value.takeIf { it > 0 }?.toString()
        is String -> value.takeIf(GameLaunchLink::validId)
        else -> null
    }
    id
}.getOrNull()
```

FrontEndActivity uses all of this through `readGameIntent` ([FrontEndActivity.kt L738](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/FrontEndActivity.kt#L738)). It accepts three shapes:

| Shape | Details |
|---|---|
| **A. Explicit action** | `com.droiddeck.launcher.LAUNCH_GAME`, **or GameNative's `app.gamenative.LAUNCH_GAME`**. `app_id` is an **Int > 0** or a **String** unsigned 64-bit decimal. `game_source` is optional; if present it must be the String `STEAM` (any case). Any other source (`GOG`, `EPIC`, `CUSTOM_GAME`) is **rejected**, not defaulted. A Long `app_id` is rejected. The GameNative action has no intent filter, so it is reached only by an explicit component. PR #277 calls this "explicitly retargeted GameNative profiles". Tests: [`GameLaunchIntentTest.kt`](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/test/java/com/droiddeck/launcher/frontend/GameLaunchIntentTest.kt). |
| **B. Deep link** | `ACTION_VIEW droiddeck://game/<id>`. Only this exact form is accepted: no query, no leading zero, ID ≤ 2⁶⁴−1 ([`GameLaunchLink.kt`](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/GameLaunchLink.kt)). |
| **C. File open** | `ACTION_VIEW` with a `content://`, `file://` or absolute-path URI to the Shortcut. DroidDeck opens the stream itself, reads at most 128 bytes, trims, and validates the ID ([GameFiles.kt L21–49](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/GameFiles.kt#L21-L49)). The doc says: "Frontends that scan files can instead pass a file or content URI for an exported `.droiddeck` launch file." That wording predates the rename to `.steam`. |

**Launch behaviour** ([game-launcher-shortcuts.md](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/docs/testing/game-launcher-shortcuts.md)):
- A request is accepted "only after a fresh library scan finds that game".
- A cold start begins a Steam session with `steam://rungameid/<id>`. A warm start is accepted only while a Steam session is running.
- An unknown ID shows a toast, "This game is no longer in your DroidDeck library." A malformed request shows "Invalid game launch link." ([strings.xml](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/res/values/strings.xml)). GameBridge can't observe either toast, which is consistent with Dispatch ≠ Launch.
- Sessions started from a frontend return to that frontend when they close ([0.3.2 notes](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/docs/releases/0.3.2.md)). Frontend launches were fixed in 0.6.9: "Fixed games launched from a frontend such as Cocoon or iisu sitting on the home screen instead of starting" ([0.6.9 notes L53](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/docs/releases/0.6.9.md)).

**Stability:**
- The contract is **public**: it is in the manifest, documented in `docs/testing/`, covered by unit tests, and mentioned in release notes. It is **not a versioned API**, and it has already changed once (`.droiddeck` → `.steam`, link-in-file → plain ID, within days of 0.3.1). Treat it as best-effort.
- **Pitfall:** `.MainActivity` is an activity-alias that the "Light" launcher-icon option **disables** (`setComponentEnabledSetting(... DISABLED)`, [`LauncherIcon.kt`](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/LauncherIcon.kt)). An explicit intent to `com.droiddeck.launcher/.MainActivity`, which DroidDeck's own adb examples use, would then fail with `ActivityNotFoundException`. Target **`com.droiddeck.launcher/.FrontEndActivity`** instead. It is always enabled and exported. (Inferred from code; **UNVERIFIED on device**.)

## 3. Official frontend setup

- **No LaunchBox setup is documented.** "LaunchBox", "Daijisho", "ES-DE" and "Beacon" appear nowhere in the repo (grep of the whole tree at `main`). The repo has no wiki ([github.com/Droid-Deck/DroidDeck/wiki](https://github.com/Droid-Deck/DroidDeck/wiki) redirects to the repo). The only frontends named are **Cocoon** and **iisu** ([0.6.9 notes](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/docs/releases/0.6.9.md); the Cocoon icon layout comment is in PR #277's `GameFiles.kt`).
- **LaunchBox can probably launch DroidDeck directly, with no GameBridge.** Shape C accepts the same `ACTION_VIEW` + `content://` URI that LaunchBox's Custom Emulator (With Code) sends GameBridge (`-d {file.uri}`, per our [spec](../../.scratch/gamebridge-mvp/spec.md)). A LaunchBox emulator such as:
  ```
  -n com.droiddeck.launcher/.FrontEndActivity -a android.intent.action.VIEW -d {file.uri}
  ```
  should work, covers 64-bit added-game IDs, and needs no bridge. **UNVERIFIED:** I found no DroidDeck or LaunchBox primary source showing this exact configuration, and it has not been tried on a device. Whether LaunchBox's read grant reaches DroidDeck's background reader thread is untested.
- **GameBridge's added value for DroidDeck users would be small:** controller-friendly error dialogs instead of toasts, and validation before dispatch. For a user who *only* uses DroidDeck, GameBridge is unnecessary. It matters only for a user who mixes GameNative and DroidDeck and wants one LaunchBox emulator entry. Even then, see §4: the user still has to say which app owns each `.steam` file.

## 4. Collisions

**Yes. `.steam` collides with GameNative, and the contents are the same format.**

| | GameNative `.steam` | DroidDeck `.steam` |
|---|---|---|
| Contents | Decimal ID, UTF-8, no newline ([our GameNative research §Addendum](android-toolchain-and-gamenative.md#addendum-2026-11-02-gamenative-shortcut-export-format)) | Decimal ID, UTF-8, no newline ([GameFiles.kt L67–70](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/app/src/main/java/com/droiddeck/launcher/frontend/GameFiles.kt#L67-L70)) |
| ID for a Steam game | GameNative's Int ID, which appears to be the Steam app ID (the repo's `Broforce.steam` fixture is `274190`, Broforce's Steam app ID; **UNVERIFIED** in GameNative source) | Steam app ID |
| ID for non-Steam games | Uses other extensions (`.gog`, `.epic`, `.amazon`, `.pcgame`) | Still `.steam`, with an unsigned 64-bit ID ≥ 2⁶³ |

**How GameBridge could tell them apart:**
1. **Contents can't do it for Steam games.** `274190` is valid in both apps. The only signal is an ID > 2³¹−1, which can only be a DroidDeck added or store game. That is a partial heuristic, not a rule.
2. **A different extension isn't available.** DroidDeck hard-codes `steam` and has no format selector. PR #277 says "no format selector".
3. **User configuration is the only reliable option.** Either:
   - **(a)** a separate LaunchBox emulator per Target App, each calling GameBridge with a different action or a Target App extra (for example `--es target droiddeck`) or a separate GameBridge activity-alias. The user puts DroidDeck's export folder on its own LaunchBox platform, or
   - **(b)** a GameBridge setting such as "`.steam` Shortcuts go to: GameNative | DroidDeck".

   (a) fits LaunchBox's per-platform emulator model and lets both apps coexist. (b) is simpler but all-or-nothing.
4. The folder path isn't usable: GameBridge only receives a `content://` URI and display name, not the folder.

## 5. Fit with GameBridge's design

- **Table:** one new row is **not** enough. The table is keyed by extension, and `.steam` would need two rows. The table needs a **Target App** dimension: a column, with the extension key becoming (Target App, extension), or a small Target App table holding package, component and dispatch adapter. DroidDeck would have a single row: `.steam` → `STEAM`.
- **Dispatch shape:** almost the same. GameBridge could send exactly what it sends GameNative today: action `app.gamenative.LAUNCH_GAME` (or `com.droiddeck.launcher.LAUNCH_GAME`), Int `app_id`, `game_source=STEAM`. Only the component changes, to `com.droiddeck.launcher/.FrontEndActivity`. The new parts:
  - IDs > 2³¹−1 need `app_id` as a **String** extra.
  - Alternatively, forward the Shortcut URI to DroidDeck as `ACTION_VIEW`. That breaks the spec rule "the incoming Shortcut URI and its grant are never forwarded", so it isn't recommended.
  - So this is a second, small dispatch adapter, or a parameterised one.
- **Parser:** no new format: it is still one decimal number per file. But the **Store ID range rule must vary by Target App**: signed 32-bit for GameNative, unsigned 64-bit for DroidDeck. The format is also stricter: DroidDeck reads at most 128 bytes and rejects leading zeros, `+` and a leading BOM. A BOM is not stripped, because `trim()` doesn't remove U+FEFF. GameBridge's 4 KiB cap and BOM stripping are more lenient, which is harmless.
- **Glossary:** needs edits (listed below).
- **Rating: moderate.** There is no new content format, and the dispatch shape is nearly identical. But it needs a Target App selection mechanism, which can't be derived from the Shortcut, and a per-target ID range and extra type. Supporting only DroidDeck's Steam games (≤ 2³¹−1) with a global setting would be close to trivial.

## 6. Licensing and terms

- DroidDeck is **GPL-3.0** ([LICENSE](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/LICENSE)). GameBridge would only send an Android intent to a separate app. It includes and links no DroidDeck code, so GPL obligations don't extend to GameBridge. That is a general reading of GPL-3.0, not legal advice. If GameBridge ever copied DroidDeck code (for example its ID regex), that code would bring GPL-3.0 with it. Re-implement it instead.
- No ToS, EULA, privacy policy or trademark policy exists in the repo (searched the top two directory levels). The README disclaims affiliation with Valve and others ([README](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/README.md#credits-and-licence)).
- No rule against third-party launching was found. Accepting GameNative's action shows the maintainers *want* other apps to launch them (PR #277).
- GameBridge's docs should say "DroidDeck" descriptively and not imply endorsement. **UNVERIFIED:** whether the project has a stated naming or branding preference, which might be on Discord.

## What would change in GameBridge

1. **Glossary**
   - **Target App:** "GameNative or DroidDeck for Local Shortcuts, Artemis for Remote Shortcuts".
   - **Storefront:** generalise from "The GameNative library…" to the Target App's library. Note that DroidDeck has only Steam-format Shortcuts (STEAM), even for its GOG and Epic installs.
   - **Store ID:** the range depends on the Target App (signed 32-bit for GameNative, unsigned 64-bit `steam://rungameid` ID for DroidDeck). Drop "GameNative's own number" as the definition.
   - Consider a new term for how the user assigns a Shortcut to a Target App, for example "Target App choice".
2. **ADR:** record how a `.steam` Shortcut's Target App is chosen: a per-emulator action, alias or extra in the LaunchBox command, or a global setting. Recommend the LaunchBox-command option.
3. **`:core` table:** add a Target App dimension. Rows become (Target App, extension, `game_source`, verified). Add DroidDeck `.steam` / `STEAM` / unverified.
4. **`:core` Store ID validation:** a per-Target-App range (Int for GameNative, `ULong` for DroidDeck). Reject leading zeros for DroidDeck so GameBridge never dispatches what DroidDeck will refuse.
5. **`:app` bridge activity:** accept the Target App choice (a second action or alias, or an extra) and pass it into interpretation. Reject an ambiguous request with a clear dialog instead of guessing.
6. **`:app` dispatch:** a DroidDeck adapter with an explicit component `com.droiddeck.launcher/.FrontEndActivity` (**not** `.MainActivity`, see §2), action `com.droiddeck.launcher.LAUNCH_GAME`, `game_source=STEAM`, and `app_id` as Int when it fits, otherwise String. Never forward the URI.
7. **Manifest:** add `<queries><package android:name="com.droiddeck.launcher"/></queries>`. Decide whether to support the variant packages (`com.tencent.ig`, …). Suggestion: not at first.
8. **Errors and UI strings:** "DroidDeck is not installed" (pointing to its GitHub Releases, its only official source), next to the GameNative message.
9. **Tests:**
   - fixtures `Portal 2.steam` (`620`) and an added-game `.steam` (`9223372036888330240`-style, ≥ 2⁶³)
   - the same `.steam` file dispatched to each Target App
   - Espresso-Intents checks of the DroidDeck component, action and extra types
10. **Setup guide:** document both options.
    - Direct LaunchBox → DroidDeck (`ACTION_VIEW {file.uri}` to `.FrontEndActivity`). This is probably the better choice for DroidDeck-only users.
    - GameBridge, for mixed libraries.
11. **Device gate:** verify one Steam game and one added or store game on the Odin before marking the row verified.

## UNVERIFIED

- That LaunchBox's Custom Emulator can launch DroidDeck directly with `ACTION_VIEW -d {file.uri}`, and that the read grant works for DroidDeck's background reader thread. Not tested; no primary source.
- That `.MainActivity` fails when the Light icon is chosen. Inferred from `LauncherIcon.kt` and Android's component-enable semantics; not tested on a device.
- That GameNative's `.steam` ID is always the Steam app ID. It is consistent with the `Broforce.steam` = `274190` fixture, but I did not re-check it in GameNative source here.
- Anything announced only on DroidDeck's Discord (https://discord.gg/JRGAvawjsm). Not readable without an account, so I didn't check it.
- Whether DroidDeck intends `app.gamenative.LAUNCH_GAME` compatibility and the `.steam` format to be long-term stable. Nothing documents a stability promise, and both changed within days in October 2026.
- The doc [game-launcher-shortcuts.md](https://github.com/Droid-Deck/DroidDeck/blob/0.6.9/docs/testing/game-launcher-shortcuts.md) still says "`.droiddeck` launch file". The code (`EXTENSION = "steam"`) is authoritative, but the doc is stale.
