# Bridge Shortcut: versioned JSON data, fixed templates, no commands

LaunchBox passes GameBridge a `.gamebridge` file that GameBridge created. The file is a small UTF-8 JSON document (at most 4,096 bytes) with a `version`, a `target`, a `storefront` where relevant, and the IDs that Target App needs. GameBridge builds the Intent from a fixed template per Target App; the file never carries an Intent, component, action, extras or command. A file that could name any component would turn GameBridge into a generic intent launcher, which contradicts treating Shortcut text as untrusted. Unknown fields, targets and Storefronts are rejected, and an unknown newer `version` fails explicitly, so the format can grow without silent misreads. IDs are validated per Target App (GameNative: positive signed 32-bit, rejected not truncated). The Artemis target carries `host_uuid`, `host_name`, `app_uuid` and `app_name`. GameBridge Dispatches them as the String extras `UUID`, `Name`, `AppUUID` and `AppName` of an explicit VIEW to Artemis's `ShortcutTrampoline`, with no data. Ticket 04 saw a stream open this way on Android 13.

## Considered options

- Raw Intent or command in the file: rejected, see above.
- Keep native per-Storefront extensions as the entry: rejected, extension and ID range cannot identify the exporting Target App (`docs/gamebridge-2.0/02-UPSTREAM-INTEGRATIONS.md`).
