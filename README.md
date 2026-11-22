# GameBridge

GameBridge lets LaunchBox for Android start your GameNative games and your Artemis or Ariane streams. Pick a game in LaunchBox and GameBridge hands it to the right app. GameBridge can also Scan the Shortcuts those apps export and write its own Bridge Shortcuts (`.gamebridge`) for LaunchBox to import. It asks for no permissions and does nothing in the background. Scan only reads the folders you choose and only writes to the Output Folder you choose.

> **Bridge Shortcuts and Scan are not in a release yet.** Release 0.1.1 and earlier treat a `.gamebridge` file as an unsupported type. Until the next release, they work only in a build from the `feature/gamebridge-01-dispatch` branch.

**Download:** the latest APK is on the [Releases page](https://github.com/Evilperson1337/GameBridge/releases/latest). Releases are pre-releases until every Storefront below has been checked on a device.

## Set up

1. Install the APK, then open GameBridge once from the app drawer.
2. The setup screen shows:
   - a readiness line, for example "GameNative, Artemis and LaunchBox are installed";
   - a tile for each of GameNative, Artemis and LaunchBox, with the app's own icon, its name and "Installed" or "Not installed";
   - the version each Target App was verified with, for example "Verified with Ariane 1.1.0";
   - the setup steps, the **Setup command** for the build you installed, a Copy button, and a **Scan for games** button.

   The screen only shows and copies. It dispatches nothing.
3. In LaunchBox, set the Windows platform's default emulator to Custom Emulator (With Code), paste the Setup command, and clear any per-game emulator override.
4. Optional: tap **Scan for games** to create Bridge Shortcuts. See below.

Every game on the Windows platform then goes through GameBridge, not only the GameNative ones. Write down or screenshot your current emulator settings first. GameBridge never edits LaunchBox, so to undo the setup you remove the Custom Emulator.

Step-by-step guide, backup and undo: [docs/launchbox-setup.md](docs/launchbox-setup.md).

### Which files to import

You import one of two kinds of file into LaunchBox as the games' files:

- **Bridge Shortcuts (`.gamebridge`)**, which Scan creates. They name the Target App explicitly, so they are the only way to play through Ariane.
- **Shortcuts the Target Apps export.** GameNative exports `.gog`, `.steam`, `.epic`, `.amazon` and `.pcgame` files, each holding GameNative's number for the game. Artemis exports `.art` files. GameBridge still accepts these directly, and an `.art` file always goes to Artemis.

### Scan

Scan is manual. It runs only when you tap **Scan**, never in the background.

1. On the Scan screen, choose the folder each app exports to: GameNative, Artemis and Ariane. Each folder is optional. Android asks you to grant access to each folder, and GameBridge only reads them.
2. Choose an **Output Folder**. This is the only folder GameBridge writes to. Point LaunchBox at it when you import.
3. Tap **Scan**. GameBridge writes one `.gamebridge` file per game, then shows a summary: added, unchanged, rejected, and found but not enabled, with a reason for each file that was skipped.

Scan only adds files:

- It never deletes anything.
- It doesn't remove a file whose game is no longer exported.
- When a file with the same name but different contents is already there, Scan asks before replacing it.
- When two exports would get the same name, each file gets its Storefront or app as a suffix, for example `Portal (Steam).gamebridge`.

If a folder's access is lost, its button shows no folder again and you choose it once more. A rescan then reports the existing files as unchanged.

## Support

A Storefront is the GameNative library a game belongs to. All five are enabled. Only GOG, Epic and Steam have been seen working on a device. Remote Shortcuts (`.art`) are a separate row: they go to Artemis, not GameNative. "Verified" below means observed on a device and recorded in the investigation log; anything else is labelled as not verified.

| Storefront | Status | Evidence |
|---|---|---|
| GOG | Verified | 2026-11-01: GameNative started a GOG game (Akalabeth) from the same request GameBridge sends. Not rechecked through LaunchBox since. |
| Epic | Verified | 2026-11-02: an Epic Shortcut (ABZU) picked in LaunchBox started in GameNative on the test device. |
| Steam | Verified | 2026-11-19: with the signed v0.1.0 release build, a Steam Shortcut (Portal) picked in LaunchBox was dispatched and the game started in GameNative on the test device (reported by the maintainer). Only IDs in the signed 32-bit range; see below. |
| Amazon | Enabled, untested | No device observation yet. Test files were made to match GameNative's described format, not real exports. |
| Custom | Enabled, untested | No device observation yet. |
| Remote Shortcuts (`.art`) | Verified | 2026-11-03: with the signed v0.1.0 release build, a `.art` entry (`Desktop.art`) picked in LaunchBox was dispatched by GameBridge to Artemis (`com.limelight.noir`, 20.2.6), Artemis accepted the `content://` grant, the stream started (reported by the user), and closing it returned to LaunchBox. Earlier, 2026-11-02, the same worked with the debug build. Not understood: one other `.art` pick on 2026-11-03 did not start a stream and was not investigated. |

A row moves to verified only when a device observation of that Storefront (the game starting from a LaunchBox pick) is recorded in [docs/investigation-log.md](docs/investigation-log.md). The Remote Shortcuts row moves to verified only when the stream starting for the right game is recorded in the log; it is not enough that the Dispatch succeeds.

### Bridge Shortcuts (`.gamebridge`)

A Bridge Shortcut is Dispatched only to a Target App with a recorded version. The setup screen shows these versions. All checks were made on an AYN Odin 2 Portal running Android 13.

| Target App | Verified with | Evidence |
|---|---|---|
| GameNative | 1.4.0 | 2026-11-22: a `.gamebridge` file for a GOG game, picked in LaunchBox 1.23, was Dispatched by a debug build, and the game started (reported by the maintainer). |
| Artemis (`com.limelight.noir`) | 20.2.6 | 2026-11-22: a `.gamebridge` file created by Scan from an Artemis export was Dispatched by GameBridge, and the stream started (reported by the maintainer). |
| Ariane (`com.limelight.ariane`) | 1.1.0 | 2026-11-22: a `.gamebridge` file created by Scan from an Ariane export was Dispatched by GameBridge, and the stream started (reported by the maintainer). It was sent from adb, not LaunchBox. |

Not verified:

- Android 14, 15 and 16.
- Picking an Artemis or Ariane `.gamebridge` file in LaunchBox. The GameNative row shows that LaunchBox passes `.gamebridge` files to GameBridge.
- Any app version other than those listed.

When the host PC still has a game running, Ariane reports that an app is already running and does not stream. Quit that game, or restart the host, then pick the game again.

**Target App for `.art`:** Artemis (`com.limelight.noir`, seen on 20.2.6 only). GameBridge does not read `.art` files; it passes the `content://` URI to Artemis with a read grant, and Artemis reads the file. Verified on the Odin with both the debug and the release build: Artemis accepted that grant and started its stream. Bad or missing `.art` files are reported by Artemis itself, in its own words (seen with `file://` URIs from adb, not through LaunchBox). Not verified: other Artemis versions, why one `.art` pick did not start a stream, and that the package is Artemis rather than another Moonlight fork (the file header names Artemis).

## Requirements

- Android 11 (API 30) or newer. Only Android 13 has been tried.
- LaunchBox for Android 1.23, and GameNative 1.3.0 or 1.4.0 (the standard `app.gamenative` build, not the "gold" variant).
- For streams: Artemis 20.2.6, or Ariane 1.1.0 (Bridge Shortcuts only).
- These are the only versions tested, all on an AYN Odin 2 Portal. Other versions may work, but that isn't known.

## Known behaviour

- A successful hand-off to GameNative (a Dispatch) is not proof the game started (a Launch). GameBridge cannot see the game start.
- After exiting an Epic game, GameNative was observed showing a blank screen. This was seen once and has not been diagnosed. GameNative was still the front app, and GameBridge was not running.
- GameBridge never falls back to another app or to a deep link. If it cannot hand over, it says why.

## Troubleshooting

When something is wrong, GameBridge shows a dialog with the Shortcut's filename (when known), one reason and a Close button. Close or Back returns you to LaunchBox; pick the game again to retry. The reasons are listed exactly as shown.

| Dialog reason | What to do |
|---|---|
| LaunchBox didn't pass a Shortcut. The emulator command must end with -d {file.uri}. | LaunchBox started GameBridge without a Shortcut. Make sure the Setup command ends with `-d {file.uri}`. |
| LaunchBox sent GameBridge an unexpected request. In the emulator command, use the action com.allourmedia.gamebridge.action.DISPATCH. Copy the command from GameBridge's setup screen. | The command's `-a` is wrong. Copy the Setup command again from the setup screen. |
| LaunchBox didn't pass the Shortcut as a content URI. The emulator command must end with -d {file.uri}, not {file.path}. | Replace `{file.path}` with `{file.uri}`. |
| GameBridge couldn't read the Shortcut. Check the game's Shortcut file still exists in LaunchBox, then pick the game again. | The file is gone or LaunchBox did not share it. Check the game's file in LaunchBox. |
| This isn't a Shortcut type GameBridge supports. Import the Shortcut file exported by GameNative (.gog, .steam, .epic, .amazon, .pcgame). | A game on this platform has a different kind of file. Import the Shortcut file GameNative exported, or a `.gamebridge` file from Scan. Release 0.1.1 and earlier also show this for `.gamebridge` files. |
| Support for Steam games isn't verified yet. (also Epic, Amazon, Custom) | Not reachable at present, because all five Storefronts are enabled. |
| Artemis is not installed. Install Artemis to play Remote Shortcuts (.art). | Install Artemis (`com.limelight.noir`) and pick the game again. |
| Artemis refused the request. | Android refused the hand-off to Artemis. This was simulated in tests, never seen on a device. |
| Ariane is not installed. Install Ariane to play this game. | A `.gamebridge` file names Ariane (`com.limelight.ariane`). Install it, or rescan with the Artemis folder instead. |
| Ariane refused the request. | Android refused the hand-off to Ariane. Simulated in tests only. |
| This Target App isn't verified yet. | The `.gamebridge` file names a streaming app whose Dispatch isn't verified in this build. The dialog names the app. This can't happen at present, because Artemis and Ariane are both verified. |
| This Shortcut's contents aren't valid. | The file must hold exactly one positive whole number that fits in a signed 32-bit integer. GameNative writes `0` for some GOG games, which is rejected. |
| This file is too large to be a Shortcut. Check that LaunchBox is passing the Shortcut, not another file. | The file is over 4 KiB. |
| This Shortcut was made by a newer GameBridge. Update GameBridge, then pick the game again. | A `.gamebridge` file uses a format version this build doesn't know. |
| GameNative is not installed. Get it from https://github.com/utkarshdalal/GameNative/releases | Install the standard GameNative build. |
| GameNative refused the request. | Android refused the hand-off. Make sure the standard GameNative build is installed and up to date. |

## Keep your signing key

Releases are signed by the maintainer's key. If that key is lost, a new key cannot update the installed app: players would have to uninstall GameBridge (and set it up again) before installing the next release. The key is kept offline and never committed.

## For developers

Build, tests, emulator and device testing: [docs/development.md](docs/development.md). Terms are in [GLOSSARY.md](GLOSSARY.md) and decisions in [docs/adr/](docs/adr/).

## Licence

MIT, see [LICENSE](LICENSE).
