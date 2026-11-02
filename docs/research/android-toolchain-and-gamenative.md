# Android toolchain and GameNative launch contract: research notes

Researched 2026-11-01 from primary sources only (developer.android.com, kotlinlang.org,
docs.gradle.org, Google Maven / SDK repository manifests, GitHub source at tag `v1.3.0`,
Arch package databases, kernel.org docs). Build host: CachyOS, kernel `7.3.1-1-cachyos`,
OpenJDK 21. Target device: Android 13.

Anything I could not confirm against a primary source is marked **UNVERIFIED**.

## Recommended pins

| Item | Pin | Why / source |
|---|---|---|
| AGP | `9.5.0` | Latest stable. 9.6 is still alpha. [AGP release notes](https://developer.android.com/build/releases/gradle-plugin), [Google Maven metadata](https://dl.google.com/android/maven2/com/android/tools/build/gradle/maven-metadata.xml) |
| Gradle (wrapper) | `9.6.0` | AGP 9.5's minimum and default. The newest Gradle is 9.8.2 ([Gradle compat](https://docs.gradle.org/current/userguide/compatibility.html)). It should work, but 9.6.0 is the version AGP 9.5 was tested with. |
| Kotlin (KGP + Compose compiler plugin) | `2.4.21` | Latest stable ([Kotlin releases](https://kotlinlang.org/docs/releases.html)). See the caveat in §2. |
| Compose BOM | `androidx.compose:compose-bom:2026.09.00` | Latest stable. [BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping), [Maven metadata](https://dl.google.com/android/maven2/androidx/compose/compose-bom/maven-metadata.xml) |
| compileSdk | `37` | Compose 1.12 (in BOM 2026.09.00) needs compileSdk 37 and AGP 9 ([Compose compiler page](https://developer.android.com/develop/ui/compose/compiler)). AGP 9.5 supports up to API 37. |
| targetSdk | `36` | Google Play's floor since 2026-08-31 ([target-sdk](https://developer.android.com/google/play/requirements/target-sdk)). `37` is also valid. See §4. |
| minSdk | `33` for this app (Android 13 device). The Compose floor is 23. | §3, §4 |
| JDK | 21 (installed) to run Gradle. Java/Kotlin bytecode target 17. | AGP needs at least JDK 17. Gradle runs on JVM 17–27. §1 |

## 1. Android Gradle Plugin

- **Latest stable is AGP 9.5.0** ("Android Gradle plugin 9.5.0 (October 2026)"). The page lists 9.5.0-alpha01…alpha10, rc01, rc02, then 9.5.0. 9.6 is a preview. Source: https://developer.android.com/build/releases/gradle-plugin
  - Google Maven confirms this: the newest versions are `9.5.0`, `9.6.0-alpha01`, `9.6.0-alpha02`. Source: https://dl.google.com/android/maven2/com/android/tools/build/gradle/maven-metadata.xml
- **Compatibility table for AGP 9.5** (quoted from the release notes):

  | | Minimum | Default |
  |---|---|---|
  | Gradle | 9.6.0 | 9.6.0 |
  | SDK Build Tools | 36.0.0 | 36.0.0 |
  | NDK | N/A | 28.2.13676358 |
  | JDK | 17 | 17 |

  "The maximum API level that Android Gradle plugin 9.5 supports is API level 37." Source: https://developer.android.com/build/releases/gradle-plugin
- **Minimum Gradle by AGP version**: 9.5 → 9.6.0, 9.4 → 9.6.0, 9.3 → 9.5.0. Source: https://developer.android.com/build/releases/about-agp
- **Minimum AGP by API level**: API 37.0 needs AGP ≥ 9.1.1. API 36.1 needs ≥ 8.13.0. API 33 needs ≥ 7.2. Source: https://developer.android.com/build/releases/about-agp
- **JDK 21 works.** AGP lists JDK 17 as the *minimum*, not a maximum. Gradle supports running on Java 21 since Gradle 8.5 and requires "a JVM between 17 and 27" for 9.x. Source: https://docs.gradle.org/current/userguide/compatibility.html. Nothing in the AGP 9.5 notes forbids JDK 21. I found no explicit sentence saying "AGP 9.5 is tested on JDK 21", so that exact claim is **UNVERIFIED**, but the combination is within every documented range.
- SDK packages available now, from Google's SDK repository manifest https://dl.google.com/android/repository/repository2-3.xml: `platforms;android-37.0`, `android-37.1`, `android-37.2` (all on the stable channel-0), `build-tools;37.0.0`, `platform-tools` 37.0.1, `cmdline-tools;latest` = 23.0.

## 2. Kotlin

- **Latest stable is Kotlin 2.4.21** (released 2026-10-08, a bug-fix release). Previous: 2.4.20 (2026-09-07), 2.4.10 (2026-07-14), 2.4.0 (2026-06-03). Source: https://kotlinlang.org/docs/releases.html
- **KGP ↔ AGP compatibility** (from Kotlin's table): KGP 2.4.20–2.4.21 is fully supported with Gradle 7.6.3–9.7.0 and AGP 8.5.2–**9.3.1**. Kotlin also says: "You can also use Gradle and AGP versions up to the latest releases, but if you do, keep in mind that you might encounter deprecation warnings or some new features might not work." Source: https://kotlinlang.org/docs/gradle-configure-project.html
  - So AGP 9.5.0 is newer than the highest AGP that KGP 2.4.21 officially lists. This is expected to work, but JetBrains does not formally guarantee it. Android's own AGP setup page uses Kotlin `2.4.10` alongside AGP `9.5.0` in its example: https://developer.android.com/build/releases/about-agp
- **AGP has built-in Kotlin support (confirmed).** From the AGP 9.0 release notes: "Android Gradle plugin 9.0 introduces built-in Kotlin support and enables it by default. That means you no longer have to apply the `org.jetbrains.kotlin.android` (or `kotlin-android`) plugin in your build files to compile Kotlin source files." Source: https://developer.android.com/build/releases/agp-9-0-0-release-notes
  - The same notes: "Android Gradle plugin 9.0 now has a runtime dependency on Kotlin Gradle plugin (KGP) 2.2.10 … if you use a KGP version lower than 2.2.10, Gradle will automatically upgrade your KGP version." To use a **newer** KGP, such as 2.4.21, add it to the top-level `buildscript { dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:KGP_VERSION") } }`. Same source.
  - If you apply `kotlin-android` with AGP 9, you get the error "The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0". You can opt out with `android.builtInKotlin=false` plus `android.newDsl=false`, but that "won't be able to [be disabled] in AGP 10.0". `android.kotlinOptions {}` moves to `kotlin { compilerOptions {} }`, and `jvmTarget` defaults to `android.compileOptions.targetCompatibility`. `kapt` is incompatible, so use KSP. Source: https://developer.android.com/build/migrate-to-built-in-kotlin
  - The **bundled KGP version in AGP 9.5** is not stated on the 9.5 page. **UNVERIFIED** (the 9.0 notes say 2.2.10).

## 3. Jetpack Compose

- **Latest stable BOM is `2026.09.00`.** Google Maven lists `…2026.06.01, 2026.08.00, 2026.09.00` as the newest entries. Sources: https://dl.google.com/android/maven2/androidx/compose/compose-bom/maven-metadata.xml and https://developer.android.com/develop/ui/compose/bom/bom-mapping
  - Its POM maps `compose.ui`/`runtime` → **1.12.1** and `material3` → **1.4.0**. Source: https://dl.google.com/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom. The release-notes date for compose-ui 1.12.1 is 2026-09-09: https://developer.android.com/jetpack/androidx/releases/compose-ui
- **Compose compiler setup.** "The Compose Compiler Gradle Plugin is only available from Kotlin 2.0+." The plugin id is `org.jetbrains.kotlin.plugin.compose`, and its version must match your Kotlin version. Source: https://developer.android.com/develop/ui/compose/compiler
  - Version catalog: `compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }`. In the root build: `alias(libs.plugins.compose.compiler) apply false`. In the app module: `alias(libs.plugins.compose.compiler)`. With AGP 9 built-in Kotlin, **omit** the `org.jetbrains.kotlin.android` line that the Compose page's example still shows. That page does not discuss built-in Kotlin. The combination is consistent with the AGP 9 migration guide, but no single page shows both together, so it is **UNVERIFIED** as an official snippet.
  - "starting with Compose 1.12.0, projects are required to use `compileSdk 37` and Android Gradle Plugin (AGP) 9." Source: https://developer.android.com/develop/ui/compose/compiler
  - Compose 1.13 (currently beta) "Libraries now target Kotlin 2.2 and require kotlinc 2.2" and moves compileSdk to 37.1. Source: https://developer.android.com/jetpack/androidx/releases/compose-ui
- **Compose minSdk is 23.** Compose animation/foundation/material/runtime/ui moved their default minSdk from API 21 to 23 in 1.10.0-alpha01 ([compose-foundation notes](https://developer.android.com/jetpack/androidx/releases/compose-foundation)). I checked the published AARs directly: `ui-android-1.12.1.aar` and `runtime-android-1.12.1.aar` declare `minSdkVersion="23"`, and `material3-android-1.4.0.aar` declares `21` (from https://dl.google.com/android/maven2/androidx/compose/…). AndroidX says many libraries will move to minSdk 24 in new releases from July 2026 ([all-channel notes](https://developer.android.com/jetpack/androidx/versions/all-channel)). None of this matters for this app, which uses minSdk 33.

## 4. API levels / targetSdk

- **Google Play requirement:** "Starting August 31, 2026 … New apps and app updates must target Android 16 (API level 36) or higher". Existing apps must target ≥ 35 to stay visible to new users. Extensions run to 2026-11-01. Source: https://developer.android.com/google/play/requirements/target-sdk
- **Latest stable platform is Android 17 = API 37**, released 2026-06-16 ("Today we're releasing Android 17 … Android 17 (API level 37)"). Source: https://android-developers.googleblog.com/2026/06/Android-17.html. The SDK repository also has stable-channel platform packages for 37.1 and 37.2 (QPRs): https://dl.google.com/android/repository/repository2-3.xml
- **Android 13 = API 33.** Source: https://developer.android.com/tools/releases/platforms
- Recommendation: `minSdk 33`, `compileSdk 37`, `targetSdk 36`. Targeting 37 adds Android 17 behaviour changes when the app runs on an Android 17 device, including local network access blocked by default without a new permission. That matters if the bridge talks to LaunchBox over the LAN. These changes do not apply on the Android 13 target device. Source: https://developer.android.com/about/versions/17/behavior-changes-17
- Because targetSdk is ≥ 30, package visibility rules apply. To resolve or query `app.gamenative`, the bridge must declare `<queries><package android:name="app.gamenative"/></queries>`, or an intent `<queries>` for the `app.gamenative.LAUNCH_GAME` action. Source: https://developer.android.com/training/package-visibility

## 5. SDK command-line tools on CachyOS

- **Official download.** The Studio page lists `commandlinetools-linux-15859902_latest.zip` (181.8 MB, SHA-256 `4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583`): https://developer.android.com/studio#command-line-tools-only. The URL `https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip` returns HTTP 200.
  - Google's SDK repository manifest maps that zip to **cmdline-tools 22.0**. `cmdline-tools;latest` there is **23.0** (`commandlinetools-linux-16111833_latest.zip`). The Studio download page therefore lags by one version. Source: https://dl.google.com/android/repository/repository2-3.xml
- **Layout.** Extract to `$ANDROID_HOME/cmdline-tools/latest/{bin,lib,NOTICE.txt,source.properties}`. Then run `sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"` and `sdkmanager --licenses`. Note: the sdkmanager page now says `sdkmanager` is **deprecated**, replaced by the Android CLI (`android sdk install|list|update|remove`). Source: https://developer.android.com/tools/sdkmanager
- **Arch / AUR packages** (from the [AUR RPC](https://aur.archlinux.org/rpc/v5/info?arg[]=android-sdk-cmdline-tools-latest) and [Arch package search](https://archlinux.org/packages/search/json/?name=android-tools)):
  - AUR `android-sdk-cmdline-tools-latest` **23.0-1**. It installs to **`/opt/android-sdk/cmdline-tools/latest`** and sources `commandlinetools-linux-16111833_latest.zip` ([PKGBUILD](https://aur.archlinux.org/cgit/aur.git/plain/PKGBUILD?h=android-sdk-cmdline-tools-latest)).
  - Other AUR packages: `android-sdk-platform-tools` 37.0.1-1, `android-sdk-build-tools` r37.0.0-2, `android-platform` 37.0_r02-1, and `android-sdk` 26.1.1-2 (legacy tools).
  - The official `extra` repo has `android-tools` 37.0.0.p1 (adb/fastboot only, not an SDK) and `waydroid` 1.6.3. No official-repo package provides the SDK cmdline-tools.
  - Trade-off: the AUR packages use root-owned `/opt/android-sdk`, so sdkmanager needs a writable group or sudo. A user-owned SDK in `~/Android/Sdk` avoids that.
- **ANDROID_HOME convention.** `ANDROID_HOME` "Sets the path to the SDK installation directory". `ANDROID_SDK_ROOT` is deprecated. Source: https://developer.android.com/tools/variables. Android Studio's Linux default is "`$HOME/Android/Sdk`": https://developer.android.com/studio/emulator_archive. Suggested PATH (current layout): `$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools`. The variables page still lists the old `tools`/`tools/bin` directories. My guess is that it's stale, but this is **UNVERIFIED**.

## 6. GameNative v1.3.0 external launch contract

Source files at tag `v1.3.0`:
- https://raw.githubusercontent.com/utkarshdalal/GameNative/v1.3.0/app/src/main/java/app/gamenative/utils/IntentLaunchManager.kt
- https://raw.githubusercontent.com/utkarshdalal/GameNative/v1.3.0/app/src/main/AndroidManifest.xml
- https://raw.githubusercontent.com/utkarshdalal/GameNative/v1.3.0/app/src/main/java/app/gamenative/MainActivity.kt
- https://raw.githubusercontent.com/utkarshdalal/GameNative/v1.3.0/app/src/main/java/app/gamenative/data/LibraryItem.kt (the `GameSource` enum lives here; there is no `GameSource.kt`)
- https://raw.githubusercontent.com/utkarshdalal/GameNative/v1.3.0/app/build.gradle.kts

### Constants

```kotlin
private const val EXTRA_APP_ID = "app_id"
private const val EXTRA_GAME_SOURCE = "game_source"
private const val EXTRA_CONTAINER_CONFIG = "container_config"
private const val ACTION_LAUNCH_GAME = "app.gamenative.LAUNCH_GAME"
private const val ACTION_VIEW = "android.intent.action.VIEW"
private const val URI_SCHEME = "gamenative"
private const val URI_HOST = "run"
private const val MAX_CONFIG_JSON_SIZE = 50000 // 50KB limit to prevent memory exhaustion
```

### Intent action path (`app.gamenative.LAUNCH_GAME`)

```kotlin
gameId = intent.getIntExtra(EXTRA_APP_ID, -1)
gameSource = intent.getStringExtra(EXTRA_GAME_SOURCE)?.uppercase(java.util.Locale.ROOT)
    ?.takeIf { source -> GameSource.entries.any { it.name == source } }
    ?: GameSource.STEAM.name
containerConfig = intent.getStringExtra(EXTRA_CONTAINER_CONFIG)?.let { json -> ... parseContainerConfig(json) ... }
```

- **`app_id` must be an `Int` extra** (`getIntExtra`). If it is a Long or String extra, `getIntExtra` returns the default `-1`, and the launch is rejected by `if (gameId <= 0) { … return null }`. From Kotlin, use `putExtra("app_id", id.toInt())`. From adb, use `--ei app_id N`.
- **`game_source` is an optional String** and matching is case-insensitive (`uppercase(Locale.ROOT)`). On this path, a **missing or unrecognised value silently falls back to `STEAM`**.
- **`container_config` is an optional String containing JSON**, at most 50,000 characters. Parse errors are logged and ignored, so the launch continues without overrides. Accepted keys: `name, screenSize, envVars, graphicsDriver, graphicsDriverVersion, dxwrapper, dxwrapperConfig, audioDriver, wincomponents, drives, execArgs, executablePath, installPath, showFPS, launchRealSteam, launchBionicSteam, cpuList, cpuListWoW64, wow64Mode, startupSelection, box86Version, box64Version, box86Preset, box64Preset, desktopTheme, csmt, videoPciDeviceID, offScreenRenderingMode, strictShaderMath, videoMemorySize, mouseWarpOverride, sdlControllerAPI, enableXInput, enableDInput, dinputMapperType, disableMouseInput, suspendPolicy, shaderBackend, useGLSL`. Booleans are read with `getBoolean`, `startupSelection`, `videoPciDeviceID` and `dinputMapperType` with `getInt`, and everything else with `getString`. `dxwrapperConfig` gets the prefix `"version="`. The override is temporary and held in memory only (`saveToDisk = false`).
- The internal app id becomes `"${gameSource}_$gameId"`, for example `GOG_1207658924`.

### Deep-link path (`gamenative://run`)

```kotlin
gameId = data.getQueryParameter("appid")?.toIntOrNull() ?: -1
gameSource = data.getQueryParameter("gamesource")?.uppercase(java.util.Locale.ROOT)
containerConfig = null
```

- The query parameters are **`appid`** and **`gamesource`**, with no underscores, unlike the extras. On this path `gamesource` has **no STEAM default**: if it is missing or invalid, it fails `isValidGameSource` and the launch is rejected. Container config is not supported here.

### Accepted `game_source` values (`GameSource` enum, LibraryItem.kt)

```kotlin
enum class GameSource {
    STEAM,
    CUSTOM_GAME,
    GOG,
    EPIC,
    AMAZON
    // Add other platforms here..
}
```

Matching is case-insensitive because the input is upper-cased before comparison with `it.name`.

### Manifest

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTop"
    ...>
    ...
    <intent-filter>
        <action android:name="app.gamenative.LAUNCH_GAME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="gamenative" android:host="run" />
    </intent-filter>
```

- `MainActivity` is **exported** and uses `singleTop`. While running, new launches arrive through `onNewIntent` and are emitted immediately. On a cold start, they are stored as a pending request.
- Other `VIEW` schemes on MainActivity: `home://pluvia`, `nxm:`, and `gamenative://discord-linked`. These are unrelated to game launch.
- `applicationId = "app.gamenative"`. The `release-gold` build type adds `applicationIdSuffix = ".gold"`, which makes the package `app.gamenative.gold`. The action string stays `app.gamenative.LAUNCH_GAME` (hard-coded in the manifest and in `IntentLaunchManager`). But MainActivity's "launch failed" snackbar compares against `"${BuildConfig.APPLICATION_ID}.LAUNCH_GAME"`, so on gold builds a failed parse is silent. The bridge should set the target package explicitly: `app.gamenative`, or `.gold` if the user has that build.
- Recents re-delivery is ignored (`FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`).
- Flavors: `modern`/`modernXr` use minSdk 29 and targetSdk 36. `legacy`/`legacyXr` use targetSdk 28. Base `minSdk = 26`, `compileSdk = 36`.

### Example

```
adb shell am start -a app.gamenative.LAUNCH_GAME -p app.gamenative \
  --ei app_id 1207658924 --es game_source gog
adb shell am start -a android.intent.action.VIEW -d 'gamenative://run?appid=1207658924&gamesource=GOG'
```

**Caveat (UNVERIFIED):** `app_id` is parsed as an `Int`. GOG and Steam numeric IDs fit in 32 bits. I did not check how GameNative keys EPIC and AMAZON games internally, since their store IDs are typically non-numeric strings. Check `EpicService`/`AmazonService` before relying on intent launch for those sources.

## 7. Waydroid binder on a recent kernel

- Waydroid needs Android **binder IPC** and a **Wayland** session ("Waydroid only works in a Wayland session manager"). Source: https://wiki.archlinux.org/title/Waydroid (read via `?action=raw`). Upstream install docs only mention the Wayland requirement: https://docs.waydro.id/usage/install-on-desktops
- Arch wiki: the stock `linux` and `linux-zen` kernels ship binder, now as `rust_binder`. The wiki carries a "Remove" flag saying specific modules are "no longer needed". For kernels without binder, install AUR `binder_linux-dkms` and run `modprobe binder-linux devices=binder,hwbinder,vndbinder`. The wiki also suggests the kernel parameter `ibt=off` on 5.18+ to avoid an oops when mounting binderfs. If you build your own kernel, it needs the binder/binderfs options plus IPv6, `CONFIG_NF_CT_NETLINK=y`, `CONFIG_PSI=y`, and loop devices. Source: same Arch wiki page.
- binderfs: a `binder` filesystem instance is mounted with `mount -t binder binder /dev/binderfs`. A fresh instance contains only `binder-control`, which creates devices through the `BINDER_CTL_ADD` ioctl. Source: https://docs.kernel.org/admin-guide/binderfs.html. Waydroid's container setup mounts binderfs itself. I'm inferring that from the Arch wiki's `/dev/binderfs` troubleshooting link, and it is **UNVERIFIED** against the Waydroid source.
- **This host (read-only check):** kernel `7.3.1-1-cachyos` has `CONFIG_ANDROID_BINDER_IPC=y`, `CONFIG_ANDROID_BINDERFS=y`, and `CONFIG_ANDROID_BINDER_DEVICES="binder,hwbinder,vndbinder"` (from `/proc/config.gz`). `binder` is listed in `/proc/filesystems`. Binder is built into the kernel, so no DKMS module or `modprobe` is needed. `/dev/binderfs` is not mounted yet, which is expected before Waydroid starts. `waydroid` 1.6.3 is in Arch `extra`.

## Addendum (2026-11-02): GameNative Shortcut export format

Read directly from GameNative v1.3.0 source (`app/src/main/java/app/gamenative/sync/FrontendSyncManager.kt`, plus each `*AppScreen.kt`'s `getExportFileExtension()`): https://github.com/utkarshdalal/GameNative/blob/v1.3.0/app/src/main/java/app/gamenative/sync/FrontendSyncManager.kt

| GameSource | Extension |
|---|---|
| STEAM | `.steam` |
| EPIC | `.epic` |
| GOG | `.gog` |
| AMAZON | `.amazon` |
| CUSTOM_GAME | `.pcgame` |

- Every export file is written with `writeText(appId.toString(), Charsets.UTF_8)`: GameNative's Int ID for the game, with no newline.
- Epic and Amazon IDs are GameNative's internal Int IDs (`epicGameDao` row `id`, Amazon `appId`), not store catalogue strings. This resolves the earlier UNVERIFIED caveat: every Local Shortcut's ID fits the Int `app_id` the launch intent requires.
- For GOG, a non-numeric store ID is written as `0` (`it.id.toIntOrNull() ?: 0`). A `0` Shortcut means GameNative couldn't map the game.
- Custom games are listed from the same table as Steam apps, but exported with `.pcgame` and launched with `game_source=CUSTOM_GAME`. On-device Dispatch behaviour for Custom is UNVERIFIED.
