# 03: Signed release build in CI

**What to build:** Pushing a version tag builds a signed release APK in GitHub Actions and attaches it to a GitHub Release marked as a pre-release. Signing configuration reads from environment variables. When they are absent, the release build is unsigned and `./gradlew check` still passes for contributors and forks. The release application ID stays unchanged. No keystore or secret is committed.

**Blocked by:** None (can start immediately). Ticket 04 supplies the real keystore and secrets; this ticket must work without them.

**Status:** ready-for-agent

- [ ] With no signing variables set, `./gradlew check` and the release assemble task still succeed (unsigned APK).
- [ ] With signing variables set, the release APK is signed (verified with Android build tools).
- [ ] The workflow runs only on version tags, builds the release APK and publishes it to a pre-release GitHub Release with generated notes that link to the README support table.
- [ ] No keystore, passwords or local paths appear in the repo, workflow logs or release notes.
- [ ] The workflow is checked as far as possible without secrets (syntax, dry run), and any remaining verification is left to ticket 08.
