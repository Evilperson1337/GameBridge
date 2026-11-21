# AGENTS.md

GameBridge is a small Android app that takes a Shortcut chosen in LaunchBox and dispatches it to the right Target App (GameNative for Local Shortcuts, Artemis for Remote Shortcuts). The full background is in `docs/overview/GameBridge_Implementation_Handoff.md`. Terms are defined in `GLOSSARY.md`, and decisions are recorded in `docs/adr/`.

## Rules

- Read `GLOSSARY.md` and the relevant ADRs in `docs/adr/` before coding. Use glossary terms in code, tests and UI.
- Record what you verified and what you assumed in `docs/investigation-log.md`. Never present an assumption as verified behaviour.
- A successful `startActivity` is a Dispatch, not a Launch. Never claim a game started based on it. Name results accordingly (for example `DispatchResult`, not `LaunchResult`).
- Fail explicitly. Don't mask dispatch errors, and never silently fall back to another Target App or to a deep link.
- Treat incoming URIs and Shortcut text as untrusted. Accept only `content://` URIs, known extensions, known Storefronts, and IDs valid for the Target App's recorded range (GameNative: signed 32-bit). Reject out-of-range IDs instead of truncating them. A Bridge Shortcut carries data only, never an Intent, component or command (ADR 0002).
- Never log full Shortcut contents or device paths in release builds.
- Don't add `MANAGE_EXTERNAL_STORAGE`, network permission, root, an accessibility service, or a background service.
- Only build what the current phase requires. Artemis Dispatch of a `.art` Shortcut is built. GameBridge 2.0 (`.scratch/gamebridge-2.0/spec.md`) is approved in evidence-gated slices: Bridge Shortcuts and manual Scan, slice 1 for GameNative and Artemis only. Build a ticket only when its blockers are resolved. DroidDeck waits for slice 2. A launcher picker, Associations across Target Apps, background scanning, and writing to LaunchBox stay out. Scan reads Shortcut Folders read-only and writes only to its Output Folder (ADR 0003).
- Keep classes small and independently testable. Pure logic belongs in `:core` with JVM tests; Android integration belongs in `:app`.

## Ask first

Standing approval: you may `adb install`/`uninstall` GameBridge's own debug build (`com.allourmedia.gamebridge.debug`) on the physical device (AYN Odin 2 Portal) and read `adb logcat`.

Get explicit approval before you:

- install anything else on the physical device, or create temporary Shortcut copies on it,
- change LaunchBox configuration on any device (back up the configuration first),
- make system-wide changes on the CachyOS host (drivers, kernel modules, packages beyond agreed tooling).

## Never

- Modify GameNative or LaunchBox, original exported Shortcuts, installed games, or saves. Tests use temporary copies only.
- Commit secrets, local SDK paths, device identifiers, or game files.

## Agent skills

### Issue tracker

Issues live as local markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
