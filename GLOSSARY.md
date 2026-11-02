# GameBridge

GameBridge sits between the LaunchBox frontend and the apps that actually run games. It turns a Shortcut chosen in LaunchBox into a validated request to the right Target App.

## Language

### Shortcuts

**Shortcut**:
A small text file, exported by a Target App, that identifies one game. LaunchBox treats it as the game's file and hands it to GameBridge.
_Avoid_: ROM, ROM file, identifier file, game file

**Local Shortcut**:
A Shortcut naming a Storefront and a Store ID, for a game installed on this device. Every Storefront uses the same format; only the extension differs.

**Remote Shortcut**:
A Shortcut naming an Artemis host and app, for a game streamed from a PC (`.art`).

**Storefront**:
The GameNative library a locally installed game belongs to: GOG, Steam, Epic, Amazon, or Custom (games the user added by hand).
_Avoid_: source, game source, store type, library

**Store ID**:
The positive whole number inside a Local Shortcut that identifies the game. It's GameNative's own number for the game, not necessarily the store's catalogue ID.
_Avoid_: app ID, game ID, catalogue ID

### Apps

**Frontend**:
LaunchBox: the game library the user browses and picks from.
_Avoid_: launcher

**Target App**:
An app GameBridge hands a game to: GameNative for Local Shortcuts, Artemis for Remote Shortcuts.
_Avoid_: launcher, emulator

**Setup command**:
The exact text a user enters in LaunchBox's Custom Emulator (With Code) so that LaunchBox hands a Shortcut to GameBridge. There is one for each build of GameBridge.
_Avoid_: launch arguments, command line

### Outcomes

**Dispatch**:
GameBridge handing a validated request to a Target App. This is the only success GameBridge can observe.
_Avoid_: launch (for this meaning)

**Launch**:
The game actually starting inside the Target App. GameBridge cannot observe it; only a person or device logs can confirm it.
_Avoid_: dispatch, start (for this meaning)
