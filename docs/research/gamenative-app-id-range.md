# Which app IDs GameNative accepts, and what to do about 64-bit `.steam` IDs

Researched 2026-11-19 from primary sources: GameNative source at `v1.3.0`, `v1.4.0` and `v1.4.1-prerelease`, DroidDeck source at `0.7.1`. Nothing here was run on a device except the file read and package listing noted under "Observed on the Odin".

## Question

`Warhammer Survivors.steam` on the Odin holds `1474103636937985288`. GameBridge rejects it (`InvalidContents`) because it is outside the signed 32-bit range. Can GameNative be made to launch it?

## Verified from source

- **GameNative cannot take a 64-bit ID.** `IntentLaunchManager.kt` reads `intent.getIntExtra("app_id", -1)` for `app.gamenative.LAUNCH_GAME`, and `toIntOrNull()` on the `appid` query parameter of its URI form. Anything above 2,147,483,647 becomes `-1` or `null` and the launch is dropped ("Invalid or missing app_id"). The code is the same in v1.3.0, v1.4.0 and v1.4.1-prerelease. The Odin has GameNative 1.4.0.
- **GameNative never writes such an ID.** `FrontendSyncManager.kt` writes `appId.toString()` for an `Int` `appId` (Steam, Custom, Epic, Amazon, and GOG via `toIntOrNull`). So GameNative's own `.steam` files are always in range.
- **DroidDeck does write them.** `GameFiles.kt` exports `.steam` files; `GameLaunchLink.validId` accepts `[1-9][0-9]{0,19}` that parses as an unsigned 64-bit number.
- **DroidDeck accepts a launch from GameBridge's current action.** `GameLaunchIntent.kt` accepts `app.gamenative.LAUNCH_GAME` and `com.droiddeck.launcher.LAUNCH_GAME`. `app_id` may be an `Int` > 0 or a `String` of an unsigned 64-bit decimal. `game_source` must be absent or `STEAM`, otherwise DroidDeck ignores the intent. `FrontEndActivity` is exported with a filter for `com.droiddeck.launcher.LAUNCH_GAME`.

## Observed on the Odin (2026-11-19)

- `Warhammer Survivors.steam` is 20 bytes: the ID above. `Portal.steam` is 3 bytes.
- GameNative 1.4.0 and DroidDeck 0.7.1 are both installed.
- GameBridge v0.1.0 logged `failure=InvalidContents` for one pick and dispatched the next to GameNative (`app.gamenative.LAUNCH_GAME`). The log does not name the Shortcut. The rejection fits Warhammer and the dispatch fits Portal, but that pairing is inferred.

## Assumed, not verified

- That DroidDeck wrote `Warhammer Survivors.steam`. The source says it is the only one of the two that can; the file was not traced to its writer.
- That a Dispatch to DroidDeck with a `String` `app_id` starts the game. Not run.
- That the 64-bit value is the right ID for DroidDeck's launch path (it comes from the same library as its export, but this was not exercised).

## Consequence

Supporting these games means a second Target App (DroidDeck), not widening GameNative's range. `AGENTS.md` and the spec say a Target App must be chosen explicitly, never by silent fallback. Two open design points, both for an ADR before any code:

1. **Selection.** `.steam` is ambiguous: GameNative and DroidDeck both export it. An ID above 2,147,483,647 can only be DroidDeck's, but a small ID could be either, and picking by range alone would be a silent routing rule.
2. **Interim behaviour.** Until then the rejection is correct; its dialog could say the game needs DroidDeck instead of "invalid contents". That is a wording change, not new dispatch.

## Sources

- GameNative `IntentLaunchManager.kt`: https://github.com/utkarshdalal/GameNative/blob/v1.4.0/app/src/main/java/app/gamenative/utils/IntentLaunchManager.kt
- GameNative `FrontendSyncManager.kt`: https://github.com/utkarshdalal/GameNative/blob/v1.4.0/app/src/main/java/app/gamenative/sync/FrontendSyncManager.kt
- DroidDeck `GameLaunchIntent.kt`, `GameLaunchLink.kt`, `GameFiles.kt`, `AndroidManifest.xml` at tag `0.7.1`: https://github.com/Droid-Deck/DroidDeck/tree/0.7.1
- Earlier notes: `docs/research/droiddeck-target-app.md`
