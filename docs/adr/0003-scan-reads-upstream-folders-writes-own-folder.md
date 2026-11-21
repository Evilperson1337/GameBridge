# Scan reads upstream Shortcut Folders read-only and writes only to GameBridge's Output Folder

Scan discovers games by reading the Shortcut files each Target App already exports, from a folder the player grants once through the Storage Access Framework. The folder the player picks for a Target App defines which Target App owns the files, which solves the provenance problem that extension and ID range cannot. GameBridge writes `.gamebridge` files only to a separate Output Folder the player grants, additively; overwriting or deleting needs the player's confirmation. It never reads another app's private storage or database, never writes into an upstream folder, never writes to LaunchBox, and runs only when the player taps Scan (no background service). All three upstream apps leave the export folder to the player (`docs/gamebridge-2.0/04-EXPORT-FORMATS-AND-FOLDERS.md`), so the setup screen has a grant step per Target App.

## Consequences

- Games unknown to the export folders cannot be found. That is accepted: it keeps `MANAGE_EXTERNAL_STORAGE` and root out.
- Whether SAF can read each app's actual export folder on the Odin is unverified; ticket 05 records it.
