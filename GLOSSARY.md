# GameBridge

GameBridge sits between the LaunchBox frontend and the apps that actually run games. It turns a Shortcut chosen in LaunchBox into a validated request to the right Target App.

## Language

### Shortcuts

**Shortcut**:
A small text file, exported by a Target App, that identifies one game. Scan reads Shortcuts; LaunchBox can also hand one to GameBridge directly as the game's file.
_Avoid_: ROM, ROM file, identifier file, game file

**Bridge Shortcut**:
A `.gamebridge` file created by GameBridge that names a Target App and the IDs it needs, and nothing else (no Intent, component or command; ADR 0002). LaunchBox hands it to GameBridge, which Dispatches using a fixed template for that Target App.
_Avoid_: launcher file, profile, shortcut (unqualified, for this meaning)

**Local Shortcut**:
A Shortcut naming a Storefront and a Store ID, for a game installed on this device. Every Storefront uses the same format; only the extension differs.

**Remote Shortcut**:
A Shortcut naming a host and app, for a game streamed from a PC (`.art`), exported by Artemis or Ariane. Opened directly, it always goes to Artemis; only a Bridge Shortcut can name Ariane.

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
An app GameBridge hands a game to: GameNative for local games, Artemis or Ariane for streamed games.
_Avoid_: launcher, emulator

**Ariane**:
A fork of Artemis (`com.limelight.ariane`) and a Target App for streamed games. It keeps its own paired hosts, so a Bridge Shortcut names Ariane or Artemis explicitly.

**Setup command**:
The exact text a user enters in LaunchBox's Custom Emulator (With Code) so that LaunchBox hands a Shortcut to GameBridge. There is one for each build of GameBridge.
_Avoid_: launch arguments, command line

### Scan

**Scan**:
A player-started pass that reads the Shortcut Folders and writes a Bridge Shortcut for each game found. It never runs in the background, and it only adds or (after confirmation) replaces files in the Output Folder (ADR 0003).
_Avoid_: discovery, sync, auto-import

**Shortcut Folder**:
A folder the player grants GameBridge read-only access to, containing the Shortcuts exported by one Target App. The folder chosen defines which Target App owns the files in it.

**Output Folder**:
The folder the player grants GameBridge write access to, where Scan puts Bridge Shortcuts. LaunchBox is pointed at it manually.

### Outcomes

**Dispatch**:
GameBridge handing a validated request to a Target App. This is the only success GameBridge can observe.
_Avoid_: launch (for this meaning)

**Launch**:
The game actually starting inside the Target App. GameBridge cannot observe it; only a person or device logs can confirm it.
_Avoid_: dispatch, start (for this meaning)
