# 05: Artemis through Bridge Shortcut and Scan

**What to build:** The Artemis target end to end: format fields (per ticket 04's conclusion), validator (length-capped, no control characters), Dispatch, Artemis Shortcut Folder grant, and Scan rules (read `.art`, reject files missing both UUIDs). GameNative behaviour is unchanged.

**Blocked by:** 03, 04

**Status:** needs-triage

- [ ] `:core` tests for the Artemis validator and `.art` reading; planner tests for rejected files.
- [ ] Instrumented test for the Artemis Dispatch template; no fallback to another target.
- [ ] Legacy `.art` Dispatch (unparsed) still works.
