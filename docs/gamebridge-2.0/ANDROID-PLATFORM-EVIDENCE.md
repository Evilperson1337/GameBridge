# Android 13–16 platform evidence

Research date: 2026-11-21 UTC. Official Android pages linked below were fetched and their relevant sections read. **Official** means documented platform behavior, not a verified result on a particular device; **repository** means static app evidence; **historical** means prior project observation, not a fresh compatibility test. Coverage is targeted to the LaunchBox → GameBridge → Target App boundary, not an exhaustive Android release audit. Read alongside [LaunchBox integration](03-LAUNCHBOX-INTEGRATION.md).

## Verdict and applicability

The current repository declares minimum SDK 30, compile SDK 37 and target SDK 36 ([build](../../app/build.gradle.kts)). Thus GameBridge's target-dependent Android 13–16 rules are relevant when it runs on those releases, subject to each rule's exact scope. LaunchBox and each Target App have their own target SDKs and must be assessed separately; their values are not established here. Android 16 cross-app intent matching is a priority compatibility risk, not an observed failure. No device or emulator was exercised in this research; there is no evidence here of a pass on all four releases.

## Baseline platform contracts

| Area | Official evidence | Implication / unresolved check |
|---|---|---|
| Routing | [Intents and intent filters](https://developer.android.com/guide/components/intents-filters) distinguishes explicit component routing from implicit action/filter resolution. | A named component is not evidence that it is exported, accessible, accepts the data, or succeeds. Check the actual sender and receiver on each hop. |
| URI access | [Secure file sharing](https://developer.android.com/training/secure-file-sharing/share-file) describes content-provider URIs and temporary URI permissions. | URI syntax alone is not a grant; verify provider eligibility and read permission at GameBridge and independently at Artemis. A granted incoming URI does not itself prove a valid onward grant. |
| File selection | [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files) describes picker and persisted-grant behavior and Android 11+ directory restrictions. | A persisted grant depends on the particular provider, flags and operation; do not assume it from a LaunchBox intent. Broad media permissions do not substitute for arbitrary Shortcut access. |
| Visibility | [Package visibility](https://developer.android.com/training/package-visibility) describes filtered package queries for apps targeting Android 11+. | Visibility used for readiness is distinct from a successful Dispatch or Launch. The [manifest](../../app/src/main/AndroidManifest.xml) lists specific package queries; verify package variants and actual activity resolution. |
| Untrusted input | **Repository policy:** [project rules](../../AGENTS.md) require content URIs, known extensions and Storefronts, signed 32-bit Store IDs, explicit failures and no silent fallback. | Do not treat URI strings, display names, extras or Shortcut contents as trusted; do not log contents or paths in release builds. |

## Release matrix

Each row links both Google's all-apps and target-specific behavior pages. These are documented platform constraints, not a claim the app has been tested against them.

| OS | All-apps source | Target-specific source and relevant evidence | Test implication |
|---|---|---|---|
| Android 13 / API 33 | [All apps](https://developer.android.com/about/versions/13/behavior-changes-all) | [Target 33](https://developer.android.com/about/versions/13/behavior-changes-13): granular media permissions concern images, video and audio; external intent delivery to exported filtered components is subject to matching rules. | Test exact cross-app intent/filter and provider access, not a media permission workaround. Confirm exceptions and receiver target conditions in the linked section for the concrete receiver. |
| Android 14 / API 34 | [All apps](https://developer.android.com/about/versions/14/behavior-changes-all) | [Target 34](https://developer.android.com/about/versions/14/behavior-changes-14): implicit intents to internal unexported components are restricted; mutable pending intents lacking a component or package are restricted. The page also describes additional background-activity-start restrictions. | Check actual sender target and visibility state; current explicit activity Dispatch is not automatically equivalent to the pending-intent case. Exercise user-initiated foreground flow and failure paths. |
| Android 15 / API 35 | [All apps](https://developer.android.com/about/versions/15/behavior-changes-all) | [Target 35](https://developer.android.com/about/versions/15/behavior-changes-15): additional background activity launch protections; edge-to-edge by default on Android 15 for apps targeting 35. | Check task return, any background-start conditions and readable setup/error screens with system bars/insets. Do not infer a block without a reproduction. |
| Android 16 / API 36 | [All apps](https://developer.android.com/about/versions/16/behavior-changes-all): default hardening against intent redirection. | [Target 36](https://developer.android.com/about/versions/16/behavior-changes-16): cross-app explicit intents must match the target component's filters when applicable, and intents without an action do not match filters; the page gives scope and exceptions. The edge-to-edge opt-out is disabled for target 36. | Check exact action, categories, data and MIME against the installed receiver manifest and documented exceptions on *both* hops. Do not assume redirection hardening applies to this flow or disable protections preemptively. Test inset handling. |

Android 13–16 release pages distinguish rules applying to all apps from target-gated changes. OEM behavior, installed application versions, provider implementation, URI grant lifetime, and target SDK changes can change outcomes. A build's compile SDK alone does not prove runtime compatibility.

## Reproducible verification matrix (not performed)

For each supported OS/API and device or emulator, record patch level, GameBridge build and target SDK, LaunchBox version/target SDK, Target App version/target SDK, provider type, and sanitized configuration. With the approvals and backup described in [LaunchBox integration](03-LAUNCHBOX-INTEGRATION.md#authorized-reproducible-apk-inspection-plan-not-performed):

1. Verify the configured explicit component, action, categories, scheme, MIME, flags and ClipData without retaining URI paths or contents; distinguish saved Setup command from actual emitted intent.
2. Exercise a user-selected temporary Local Shortcut for each supported Storefront and a temporary Remote Shortcut; independently check receiver read access and Artemis onward read access. Include denied/missing grants, non-content URI, malformed and out-of-range IDs, missing/inaccessible Target App, and failure reporting.
3. Capture activity/task transitions, rotation/insets and return behavior; separately record Dispatch and a human-observed or otherwise independently evidenced Launch. Do not imply one from the other.
4. Repeat with relevant target SDK changes and sender/receiver combinations, including Android 16 filter matching. Record exact failure and source applicability, then restore any approved configuration change.

Existing historical observations are narrower than this matrix; see [architecture audit](01-CURRENT-ARCHITECTURE.md#external-compatibility-evidence-and-limits) and [investigation log](../investigation-log.md). No network permission, broad storage access, root, accessibility service or background service is justified by this plan.
