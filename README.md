# GameBridge

GameBridge lets LaunchBox for Android start your GameNative games, and your Artemis streams. Pick a game in LaunchBox and GameBridge hands it to GameNative (local games) or Artemis (Remote Shortcuts, `.art`). It has no menus of its own, asks for no permissions and does nothing in the background.

**Download:** the latest APK is on the [Releases page](https://github.com/Evilperson1337/GameBridge/releases/latest). Releases are pre-releases until every Storefront below has been checked on a device.

## Set up

1. Install the APK, then open GameBridge once from the app drawer.
2. The setup screen shows a readiness line (for example "GameNative, Artemis and LaunchBox are installed"), then a tile for each of GameNative, Artemis and LaunchBox with the app's own icon, its name and "Installed" or "Not installed". Below are three steps, the **Setup command** for the build you installed, and a Copy button. The screen only shows and copies; it dispatches nothing.
3. In LaunchBox, set the Windows platform's default emulator to Custom Emulator (With Code), paste the Setup command, and clear any per-game emulator override.

Every game on the Windows platform then goes through GameBridge, not only the GameNative ones. Write down or screenshot your current emulator settings first. GameBridge never edits LaunchBox, so to undo the setup you remove the Custom Emulator.

Step-by-step guide, backup and undo: [docs/launchbox-setup.md](docs/launchbox-setup.md).

### Which files to import

Import the Shortcut files that GameNative exports (`.gog`, `.steam`, `.epic`, `.amazon`, `.pcgame`) into LaunchBox as the games' files. GameNative creates them when you export your library. Artemis's `.art` files are imported the same way. On the device used for testing they sit in `Games/pc`. A Shortcut is a tiny text file holding GameNative's number for the game.

## Support

A Storefront is the GameNative library a game belongs to. All five are enabled. Only the first two have been seen working on a device. Remote Shortcuts (`.art`) are a separate row: they go to Artemis, not GameNative. "Verified" below means observed on a device and recorded in the investigation log; anything else is labelled as not verified.

| Storefront | Status | Evidence |
|---|---|---|
| GOG | Verified | 2026-11-01: GameNative started a GOG game (Akalabeth) from the same request GameBridge sends. Not rechecked through LaunchBox since. |
| Epic | Verified | 2026-11-02: an Epic Shortcut (ABZU) picked in LaunchBox started in GameNative on the test device. |
| Steam | Enabled, untested | No device observation yet. |
| Amazon | Enabled, untested | No device observation yet. Test files were made to match GameNative's described format, not real exports. |
| Custom | Enabled, untested | No device observation yet. |
| Remote Shortcuts (`.art`) | Verified | 2026-11-03: with the signed v0.1.0 release build, a `.art` entry (`Desktop.art`) picked in LaunchBox was dispatched by GameBridge to Artemis (`com.limelight.noir`, 20.2.6), Artemis accepted the `content://` grant, the stream started (reported by the user), and closing it returned to LaunchBox. Earlier, 2026-11-02, the same worked with the debug build. Not understood: one other `.art` pick on 2026-11-03 did not start a stream and was not investigated. |

A row moves to verified only when a device observation of that Storefront (the game starting from a LaunchBox pick) is recorded in [docs/investigation-log.md](docs/investigation-log.md). The Remote Shortcuts row moves to verified only when the stream starting for the right game is recorded in the log; it is not enough that the Dispatch succeeds.

**Target App for `.art`:** Artemis (`com.limelight.noir`, seen on 20.2.6 only). GameBridge does not read `.art` files; it passes the `content://` URI to Artemis with a read grant, and Artemis reads the file. Verified on the Odin with both the debug and the release build: Artemis accepted that grant and started its stream. Bad or missing `.art` files are reported by Artemis itself, in its own words (seen with `file://` URIs from adb, not through LaunchBox). Not verified: other Artemis versions, why one `.art` pick did not start a stream, and that the package is Artemis rather than another Moonlight fork (the file header names Artemis).

## Requirements

- Android 11 (API 30) or newer. Only Android 13 has been tried.
- LaunchBox for Android 1.23 and GameNative 1.3.0 (the standard `app.gamenative` build, not the "gold" variant). For Remote Shortcuts, Artemis 20.2.6. These are the only versions tested, on an AYN Odin 2 Portal. Other versions may work; that is not known.

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
| This isn't a Shortcut type GameBridge supports. Import the Shortcut file exported by GameNative (.gog, .steam, .epic, .amazon, .pcgame). | A game on this platform has a different kind of file. Import the Shortcut file GameNative exported. |
| Support for Steam games isn't verified yet. (also Epic, Amazon, Custom) | Not reachable at present, because all five Storefronts are enabled. |
| Artemis is not installed. Install Artemis to play Remote Shortcuts (.art). | Install Artemis (`com.limelight.noir`) and pick the game again. |
| Artemis refused the request. | Android refused the hand-off to Artemis. This was simulated in tests, never seen on a device. |
| This Shortcut's contents aren't valid. | The file must hold exactly one positive whole number that fits in a signed 32-bit integer. GameNative writes `0` for some GOG games, which is rejected. |
| This file is too large to be a Shortcut. Check that LaunchBox is passing the Shortcut, not another file. | The file is over 4 KiB. |
| GameNative is not installed. Get it from https://github.com/utkarshdalal/GameNative/releases | Install the standard GameNative build. |
| GameNative refused the request. | Android refused the hand-off. Make sure the standard GameNative build is installed and up to date. |

## Keep your signing key

Releases are signed by the maintainer's key. If that key is lost, a new key cannot update the installed app: players would have to uninstall GameBridge (and set it up again) before installing the next release. The key is kept offline and never committed.

## For developers

Build, tests, emulator and device testing: [docs/development.md](docs/development.md). Terms are in [GLOSSARY.md](GLOSSARY.md) and decisions in [docs/adr/](docs/adr/).

## Licence

MIT, see [LICENSE](LICENSE).
