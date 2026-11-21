# 05: Slice 1 device validation

**What to build:** Record on the Odin what is currently unverified. Needs explicit approval first (temporary `.gamebridge` copies, possible LaunchBox platform import); back up LaunchBox configuration before any change; follow `docs/device-testing.md`.

**Blocked by:** 03, 04

**Status:** needs-triage

- [ ] URI LaunchBox sends with scoped storage set and unset (`content://`, read grant).
- [ ] The display name read by GameBridge still ends in `.gamebridge`.
- [ ] LaunchBox imports `.gamebridge` files.
- [ ] Behaviour on Android 14 to 16 recorded, or marked not tested.
- [ ] Whether SAF can read the real GameNative and Artemis export folders; installed app versions and GameNative flavor.
- [ ] Whether Artemis accepts a launch built from fields with no `.art` file; if not, the spec is revisited.
- [ ] Dispatch (logcat) and Launch (person observation) recorded separately in `docs/investigation-log.md`; `verified` flags flip only here.
