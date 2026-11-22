# 05: Artemis and Ariane through Bridge Shortcut and Scan

**What to build:** The Artemis and Ariane targets end to end. Both use the template from ticket 04: an explicit VIEW to `com.limelight.ShortcutTrampoline` in the client's package, with no data and the String extras `UUID`, `Name`, `AppUUID` and `AppName`. Ariane (`com.limelight.ariane`) gets its own target value, its own optional Shortcut Folder and its own `verified` flag. Covered: format fields (per ticket 04's conclusion), validator (length-capped, no control characters), Dispatch, Artemis Shortcut Folder grant, and Scan rules (read `.art`, reject files missing both UUIDs). GameNative behaviour is unchanged.

**Blocked by:** 03, 04

**Status:** ready-for-agent

- [ ] `:core` tests for the Artemis validator and `.art` reading; planner tests for rejected files.
- [ ] Instrumented test for the Artemis Dispatch template; no fallback to another target.
- [ ] Legacy `.art` Dispatch (unparsed) still works.
- [ ] Ariane: Dispatch to `com.limelight.ariane`, never to Artemis as a fallback (or the reverse). `verified` stays off until ticket 06.
