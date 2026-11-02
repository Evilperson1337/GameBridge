# 03: Artemis device validation

**What to build:** Confirm on the Odin that picking a `.art` game in LaunchBox goes through GameBridge to Artemis, with a `content://` URI and grant (not the `file://` URI used from adb). Record what a person saw. Also record what Artemis does with a missing or bad `.art` file, using temporary copies only. Back up LaunchBox configuration before any change. Follow `docs/device-testing.md`.

**Blocked by:** 01 (Dispatch a .art Shortcut to Artemis), 02 (Artemis failure dialogs)

**Status:** ready-for-human

- [ ] The debug build is installed with `adb install -r` (standing approval).
- [ ] With the maintainer's go-ahead, a `.art` game in LaunchBox is picked and a `GameBridge` logcat excerpt shows the Dispatch.
- [ ] The maintainer confirms what Artemis showed (stream started or error), recorded as a Launch only if the stream is seen.
- [ ] If Artemis ignores the read grant, work stops and the spec is revisited.
- [ ] Behaviour with a bad or missing `.art` copy is recorded; nothing is changed on the original.
- [ ] Everything not observed is recorded as not verified in `docs/investigation-log.md`.
