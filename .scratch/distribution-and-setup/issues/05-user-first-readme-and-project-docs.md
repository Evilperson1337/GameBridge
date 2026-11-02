# 05: User-first README, developer doc, licence, support table

**What to build:** A player landing on the repo sees a README written for them: what GameBridge is, the download link, the Setup command and setup screen, a Storefront support table, troubleshooting, requirements and limitations. Build, SDK, emulator and device-testing material moves to a developer document linked from the README. An MIT licence is added. The investigation log records the "Distribution and setup" phase and closes Gate G1 on the Epic observation and the `Windows.xml` readback, listing what remains unverified. See the spec's user stories 4, 19 to 24 and 29 to 33.

**Blocked by:** 01 (Setup screen with Setup command), 02 (Failure dialog wording)

**Status:** ready-for-agent

- [ ] The README opens with what it does and the download link, and no longer says Gate G1 or the LaunchBox path has not been run.
- [ ] The support table marks GOG and Epic verified (with the observation date) and Steam, Amazon and Custom enabled-untested; the rule for moving a row to verified is stated.
- [ ] Known behaviour includes the blank GameNative screen after exiting an Epic game, described as observed and not diagnosed.
- [ ] Requirements state the minimum Android version and the LaunchBox and GameNative versions actually tested.
- [ ] A short note says which Shortcut files to import and that GameNative exports them.
- [ ] The key-loss warning for the signing key is present.
- [ ] Developer material is in its own document with the README linking to it in one line.
- [ ] An MIT `LICENSE` is added with the maintainer as copyright holder.
- [ ] The investigation log records the new phase and G1 closure, separating verified from assumed.
- [ ] The broken link in the G1 test report is fixed or removed.
- [ ] All docs use glossary terms.
