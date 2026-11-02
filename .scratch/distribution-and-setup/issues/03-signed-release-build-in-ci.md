# 03: Signed release build in CI

**What to build:** Pushing a version tag builds a signed release APK in GitHub Actions and attaches it to a GitHub Release marked as a pre-release. Signing configuration reads from environment variables. When they are absent, the release build is unsigned and `./gradlew check` still passes for contributors and forks. The release application ID stays unchanged. No keystore or secret is committed.

**Blocked by:** None (can start immediately). Ticket 04 supplies the real keystore and secrets; this ticket must work without them.

**Status:** ready-for-agent

- [x] With no signing variables set, `./gradlew check` and the release assemble task still succeed (unsigned APK). Checked: ran `./gradlew check :app:assembleRelease --offline` with no variables; passed and produced `app-release-unsigned.apk`.
- [x] With signing variables set, the release APK is signed (verified with Android build tools). Checked: built with a throwaway keystore generated in the scratchpad (outside the repo); `apksigner verify --verbose` printed `Verifies` (v2 scheme).
- [ ] The workflow runs only on version tags, builds the release APK and publishes it to a pre-release GitHub Release with generated notes that link to the README support table. Written (`.github/workflows/release.yml`: trigger `v*.*.*` tags only, `gh release create --prerelease --generate-notes` plus a note linking to `README.md#support`) but not run; needs a real tag build (ticket 08). The README must have a heading that anchors as `#support` (ticket for the README support table), or the link in the workflow needs updating.
- [x] No keystore, passwords or local paths appear in the repo, workflow logs or release notes. Checked by reading the diff: secrets come only from `secrets.*`, the keystore is decoded to `$RUNNER_TEMP` and removed in an `always()` step, no commands echo secret values, `*.jks`/`*.keystore` are already gitignored. Workflow logs not observed (not run).
- [ ] The workflow is checked as far as possible without secrets (syntax, dry run), and any remaining verification is left to ticket 08. Only YAML parse checked (python yaml); `actionlint` is not installed and no dry run was possible. Left to ticket 08.

## Implementation notes

Signing is configured in `app/build.gradle.kts` from environment variables. All four must be set and non-blank, otherwise the release build is unsigned. The workflow fails early if any secret is missing, so a tag never publishes an unsigned release.

### Names ticket 04 must create

GitHub Actions repository secrets (names only, no values here):

| Secret | Contents |
| --- | --- |
| `GAMEBRIDGE_KEYSTORE_BASE64` | the release keystore file, base64-encoded (`base64 -w0 release.jks`) |
| `GAMEBRIDGE_KEYSTORE_PASSWORD` | keystore password |
| `GAMEBRIDGE_KEY_ALIAS` | key alias |
| `GAMEBRIDGE_KEY_PASSWORD` | key password |

The workflow maps these to the Gradle environment variables `GAMEBRIDGE_SIGNING_STORE_FILE` (path to the decoded keystore), `GAMEBRIDGE_SIGNING_STORE_PASSWORD`, `GAMEBRIDGE_SIGNING_KEY_ALIAS` and `GAMEBRIDGE_SIGNING_KEY_PASSWORD`.

Tag format that triggers the workflow: `vMAJOR.MINOR.PATCH` with optional suffix (for example `v0.1.0`). The tag should match `versionName` in `app/build.gradle.kts`; the workflow does not check this.
