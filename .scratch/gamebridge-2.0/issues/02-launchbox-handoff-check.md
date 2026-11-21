# 02: LaunchBox hands over a `.gamebridge` file

**What to build:** A recorded answer to the biggest launch-mechanism risk (`docs/gamebridge-2.0/05-LAUNCH-MECHANISM.md`): whether LaunchBox, using the existing Setup command, delivers a usable `.gamebridge` file to GameBridge. If it does not, the design is revisited before any Scan work. Needs explicit approval first: temporary copies on the Odin and any LaunchBox change. Back up the LaunchBox configuration first; follow `docs/device-testing.md`.

**Blocked by:** 01

**Status:** ready-for-human

- [ ] URI LaunchBox sends with scoped storage set and unset (scheme, read grant), recorded without device paths.
- [ ] The display name GameBridge reads still ends in `.gamebridge`, including upper case.
- [ ] Whether LaunchBox imports `.gamebridge` files on the platform.
- [ ] Dispatch (logcat) and any Launch (person) recorded separately; LaunchBox configuration restored.
- [ ] Everything not observed is marked not verified in `docs/investigation-log.md`.
