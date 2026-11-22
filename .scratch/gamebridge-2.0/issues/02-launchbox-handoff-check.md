# 02: LaunchBox hands over a `.gamebridge` file

**What to build:** A recorded answer to the biggest launch-mechanism risk (`docs/gamebridge-2.0/05-LAUNCH-MECHANISM.md`): whether LaunchBox, using the existing Setup command, delivers a usable `.gamebridge` file to GameBridge. If it does not, the design is revisited before any Scan work. Needs explicit approval first: temporary copies on the Odin and any LaunchBox change. Back up the LaunchBox configuration first; follow `docs/device-testing.md`.

**Blocked by:** 01

**Status:** resolved

- [x] URI LaunchBox sends with scoped storage set (scheme, read grant), recorded without device paths. (Unset was not tested; see the log.)
- [x] The display name GameBridge reads still ends in `.gamebridge`, including upper case.
- [x] Whether LaunchBox imports `.gamebridge` files on the platform.
- [x] Dispatch (logcat) and any Launch (person) recorded separately; LaunchBox configuration restored.
- [x] Everything not observed is marked not verified in `docs/investigation-log.md`.

## Comments

- 2026-11-22: The hand-off works with Scoped Storage set: `content://`, read grant, `.gamebridge` display name, Dispatch to GameNative, and a Launch reported by the maintainer. The design holds. Not tested, and noted in the log: Scoped Storage unset, upper-case extension, Android 14–16. Findings for the setup guide (ticket 07):
  - LaunchBox 1.23 ignores a per-game Custom Emulator (With Code) command, so the platform command is the one that counts.
  - Importing re-created the existing Windows games (new IDs and dates, lost image links, one play count changed).
  - LaunchBox overwrites `Windows.xml` edits made outside the app, so change its settings through the UI.
