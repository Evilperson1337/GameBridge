# 06: LaunchBox setup guide rewrite

**What to build:** A player can follow the LaunchBox guide from a clean install to a working Dispatch, and undo it. The guide documents the platform-wide Windows setup as the only verified path, based on the `Windows.xml` readback in the investigation log. It uses the release Setup command first and the debug one second, warns that every game on the Windows platform then goes through GameBridge, and tells the player to clear any per-game emulator override (flagged as assumed, since the screens used were not recorded). Backup is a note or screenshot of current settings in the main guide, with the adb pull and SHA-256 procedure under "Advanced". It includes undo steps and a checklist for what LaunchBox shows when the setup fails, taken from the G1 report. Per-game overrides and other platforms are listed as not verified.

**Blocked by:** 01 (Setup screen with Setup command)

**Status:** ready-for-agent

- [ ] The guide's Setup commands match the strings pinned by the core tests.
- [ ] Every LaunchBox step is labelled verified or assumed, matching the investigation log.
- [ ] The platform-wide warning, the clear-override step, simple backup, Advanced backup and undo steps are present.
- [ ] A failure checklist covers the LaunchBox "custom emulator failed" message and the unexpected permission screen seen in the G1 attempt, without claiming a cause that was not established.
- [ ] The guide mentions the setup screen as the place to copy the command.
- [ ] Any step the agent cannot confirm from the repo or the `Windows.xml` readback is left marked assumed and listed for the maintainer to confirm on the device.
