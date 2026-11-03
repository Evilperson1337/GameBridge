# 08: First pre-release, v0.1.0

**What to build:** A player can download a signed APK from GitHub Releases. The maintainer tags the first version, CI publishes it as a pre-release, and the maintainer installs it on the Odin, changes the Setup command from the debug build to the release one, and confirms a Dispatch from LaunchBox. The result is recorded in the investigation log.

**Blocked by:** 03 (Signed release build in CI), 04 (Keystore and GitHub secrets), 05 (User-first README, developer doc, licence, support table), 06 (LaunchBox setup guide rewrite)

**Status:** ready-for-human

- [x] The tag build succeeds and the APK on the release is signed. Run 41697285718 (second attempt; the first failed in setup-android and published nothing); `apksigner verify` on the downloaded APK passes (v2).
- [x] The release is marked pre-release and its notes link to the support table. `gh release view v0.1.0`: prerelease=true, draft=false, notes link to README.md#support.
- [ ] Installing the release APK on the Odin works, and an update over it keeps the setup.
- [ ] A Dispatch from LaunchBox through the release build is observed and recorded as verified, and anything not observed is recorded as not verified.
- [ ] `versionCode` in `app/build.gradle.kts` is higher than the previous release (and `versionName` matches the tag), or the update will not install over the old one.
- [ ] Debug build uninstalled afterwards only with the maintainer's agreement.
