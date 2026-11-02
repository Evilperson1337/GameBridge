# 02: Failure dialog wording

**What to build:** A player who sees a failure dialog is told what to change in LaunchBox, not pointed at a repo document they do not have. Reasons that currently say to check a command in the repo docs are reworded (for example, the command must end with the Shortcut placeholder as a content URI). The dialog is otherwise unchanged: title, filename when known, one reason, one Close button that returns to LaunchBox. The README troubleshooting table is updated to the exact new strings.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] No failure reason refers to a repo file or a document the player would not have.
- [ ] The existing failure tests are updated to pin the new wording, and every reason is still covered.
- [ ] The dialog still shows one Close button and Close or Back returns to LaunchBox.
- [ ] The README troubleshooting table matches the dialog strings exactly.
- [ ] `./gradlew check` passes.
