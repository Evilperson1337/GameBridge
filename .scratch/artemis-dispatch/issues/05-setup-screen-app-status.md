# 05: Setup screen reports Artemis and LaunchBox

**What to build:** The setup screen already says whether GameNative is installed. It also says whether Artemis (`com.limelight.noir`) and LaunchBox (`com.unbrokensoftware.launchbox`) are installed, in the same plain text style ("Artemis: installed" / "Artemis: not installed", and the same for LaunchBox), so a player finds out before they try to play. Text only: no link-out, no version check, no launching. Package visibility is added for LaunchBox in the manifest `<queries>` (Artemis is already there). The package names are constants next to the existing GameNative one. No permission, network, service or background work.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Instrumented tests show each of the three apps reported as installed or not installed independently, including an installed case for at least one app (use a real installed package such as the test app's own or a stub package that is registered, and say how).
- [ ] The setup screen text for GameNative is unchanged.
- [ ] `<queries>` lists LaunchBox; no new permission, network access, service or background work; no link-out or version check.
- [ ] The README and `docs/launchbox-setup.md` setup-screen description mention the extra lines if they describe the screen.
- [ ] `./gradlew check` and the instrumentation task pass.
