# AGENTS.md

GameBridge is a small Android app that takes a Shortcut chosen in LaunchBox and dispatches it to the right Target App (GameNative for Local Shortcuts, Artemis for Remote Shortcuts). The full background is in `docs/overview/GameBridge_Implementation_Handoff.md`. Terms are defined in `GLOSSARY.md`, and decisions are recorded in `docs/adr/`.

## Rules

- Read `GLOSSARY.md` and the relevant ADRs in `docs/adr/` before coding. Use glossary terms in code, tests and UI.
- Record what you verified and what you assumed in `docs/investigation-log.md`. Never present an assumption as verified behaviour.
- A successful `startActivity` is a Dispatch, not a Launch. Never claim a game started based on it. Name results accordingly (for example `DispatchResult`, not `LaunchResult`).
- Fail explicitly. Don't mask dispatch errors, and never silently fall back to another Target App or to a deep link.
- Treat incoming URIs and Shortcut text as untrusted. Accept only `content://` URIs, known extensions, known Storefronts, and Store IDs in the signed 32-bit range. Reject out-of-range IDs instead of truncating them.
- Never log full Shortcut contents or device paths in release builds.
- Don't add `MANAGE_EXTERNAL_STORAGE`, network permission, root, an accessibility service, or a background service.
- Only build what the current phase requires. Don't build Artemis support, discovery, matching or the launcher picker early.
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
