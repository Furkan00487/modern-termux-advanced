# Modern Termux

[![Release](https://img.shields.io/github/v/release/Furkan00487/modern-termux?label=release&color=00E5FF)](https://github.com/Furkan00487/modern-termux/releases/latest)
[![Release Build](https://github.com/Furkan00487/modern-termux/actions/workflows/release_build.yml/badge.svg)](https://github.com/Furkan00487/modern-termux/actions/workflows/release_build.yml)
[![Unit tests](https://github.com/Furkan00487/modern-termux/actions/workflows/run_tests.yml/badge.svg)](https://github.com/Furkan00487/modern-termux/actions/workflows/run_tests.yml)
[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)

🌐 **[English](README.md)** | **[Türkçe](README.tr.md)**

---

**Modern Termux** is an enhanced fork of the [Termux](https://github.com/termux/termux-app) Android terminal emulator and Linux environment. It ships with built-in X11 graphical support, event-driven lifecycle management for zero idle CPU drain, dedicated drawer navigation shortcuts, out-of-the-box audio and microphone support, and signed production release builds.

> This project is a fork of [termux/termux-app](https://github.com/termux/termux-app). The terminal emulation engine, Linux shell environment, and package repository ecosystem originate from the upstream Termux project and its contributors.

---

## Contents

- [Why Modern Termux?](#why-modern-termux)
- [Key Features](#key-features)
- [Quickstart: Graphical Desktop (X11)](#quickstart-graphical-desktop-x11)
- [Audio & Microphone Setup](#audio--microphone-setup)
- [UI & Navigation Drawer Shortcuts](#ui--navigation-drawer-shortcuts)
- [Built-in CLI Utilities](#built-in-cli-utilities)
- [Installation](#installation)
- [Verifying a Download](#verifying-a-download)
- [Upgrading and Uninstalling](#upgrading-and-uninstalling)
- [Building from Source](#building-from-source)
- [Debugging](#debugging)
- [Contributing](#contributing)
- [Upstream Resources](#upstream-resources)
- [License](#license)

---

## Why Modern Termux?

| Capability / Feature | Upstream Termux | Modern Termux |
| :--- | :--- | :--- |
| **X11 Graphical Support** | Requires installing a separate third-party Termux:X11 APK | **Fully integrated `:x11` engine** inside the same app |
| **Status Bar Icon** | Static terminal icon (`>_`) | **Dynamic status icon**: Automatically switches to `X_` in graphical mode and returns to `>_` |
| **Battery & CPU Efficiency** | Wakes CPU every 200 ms via `serverWatchdog` loop | **Event-driven**: Watchdog loop eliminated; 0% background overhead |
| **X11 Navigation & Config** | Manual switching, separate app preference menu | **Drawer quick-switch button** + **Long-press** for instant settings |
| **Keyboard Layouts (XKB)** | Requires external repository setup or manual packaging | **Bundled XKB archive (`xkb.zip`)** auto-extracted on launch |
| **Microphone & Audio** | Manual permission debugging, external script hunting | **Native `RECORD_AUDIO` permission** + `termux-setup-microphone` |
| **UI Theme & Accent** | Default dark grey/black scheme | **Neon Cyan (`#00E5FF`)** accent theme and custom modern icons |
| **Release Signing** | Often distributed as unsigned or public test keys | **Signed in GitHub Actions** with dedicated private release key (v2 & v3) |
| **Welcome Banner (MOTD)** | English default | **Modern Turkish MOTD** synchronized on app upgrade |

---

## Key Features

- **Integrated X11 display server:** The complete Termux:X11 display server is built into the application package. No companion APKs or signature mismatch headaches.
- **Out-of-the-box GPU Hardware Acceleration:** Built-in hardware acceleration preference enabled by default, bridging X11 clients (Firefox, Chromium, 3D games, Blender, MPV) to device GPU via Mesa Zink and Vulkan.
- **Dynamic status bar icon:** The persistent foreground notification icon dynamically reflects current activity: Neon Cyan `X_` while the X11 screen is active, and `>_` when in terminal sessions.
- **Battery-friendly event-driven lifecycle:** The legacy 200 ms `serverWatchdog` polling thread has been completely removed. State changes hook into `onActivityResumed` and `onActivityPaused` with zero continuous CPU wakeups.
- **Audio and microphone ready:** Includes the Android `RECORD_AUDIO` permission out of the box and bundles `termux-setup-microphone` to request runtime audio permissions instantly.
- **Pre-bundled XKB configuration:** Keyboard layouts are packed directly in the app assets and auto-installed into `$PREFIX/share/`, eliminating `xkbcomp` missing errors.
- **Signed single-asset releases:** Release workflows produce an authentic signed APK (`arm64-v8a`) verified with APK Signature Scheme v2 and v3.

---

## Quickstart: Graphical Desktop (X11)

Modern Termux lets you launch full desktop environments (such as XFCE4) directly.

### 1. Install a Desktop Environment

Open Modern Termux and run:

```bash
pkg update
pkg install xfce4 xfce4-terminal
```

### 2. Start the Integrated X11 Server

Launch the display server in the background:

```bash
termux-x11 :0 &
```

### 3. Launch the Desktop Session

Export the display variable and start XFCE:

```bash
export DISPLAY=:0
startxfce4 &
```

### 4. Switch to the Graphical Screen

- Swipe from the left edge of the screen to open the **Navigation Drawer**.
- Tap the **X11 icon** (`X_`) to switch directly to your desktop workspace.

---

## Audio & Microphone Setup

Modern Termux includes native Android microphone support (`RECORD_AUDIO`). You can configure full duplex audio (playback and recording) using PulseAudio and ALSA.

### 1. Grant Microphone Permission

Run the bundled microphone helper script:

```bash
termux-setup-microphone
```

A system dialog will prompt you to grant microphone access to Modern Termux. Tap **Allow** (While using the app).

### 2. Install Audio Packages

```bash
pkg install pulseaudio alsa-utils
```

### 3. Start PulseAudio with OpenSL ES Support

To enable audio playback and recording for both terminal CLI and graphical X11 apps, start PulseAudio with OpenSL ES sink and source modules:

```bash
pulseaudio --start \
    --load="module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1" \
    --load="module-sles-sink" \
    --load="module-sles-source" \
    --exit-idle-time=-1
```

> [!TIP]
> You can add this command to your `~/.bashrc` or X11 startup script (`startxfce4`) to initialize sound automatically whenever you work.

### 4. Test Playback and Recording

- **Test Microphone Recording (ALSA):**
  Record 5 seconds of audio to `test.wav`:
  ```bash
  arecord -D pulse -f cd -d 5 test.wav
  ```
- **Test Audio Playback (ALSA):**
  Play back the recorded audio:
  ```bash
  aplay -D pulse test.wav
  ```
- **PulseAudio Native Tools:**
  You can also record and play using `parec` and `paplay`:
  ```bash
  parec --format=s16le --rate=44100 --channels=2 > mic_sample.raw
  paplay --format=s16le --rate=44100 --channels=2 mic_sample.raw
  ```

---

## UI & Navigation Drawer Shortcuts

The terminal navigation drawer includes dedicated controls for the graphical environment:

- **Open Drawer:** Swipe from the left edge of the terminal or tap the left drawer indicator.
- **X11 Button Tap:** Tapping the Neon Cyan `X_` button immediately brings the X11 desktop activity into the foreground.
- **X11 Button Long-Press:** Long-pressing the `X_` button opens **X11 Preferences** (`LoriePreferences`), allowing you to configure:
  - Display resolution, scaling mode, and DPI.
  - Touch input modes (Trackpad mode, Simulated Touchscreen, or Direct Mouse).
  - Software vs Hardware rendering options.
  - Soft keyboard behaviors and extra key bindings.

---

## Built-in CLI Utilities

Modern Termux installs helper commands directly into `$PREFIX/bin`:

### `termux-x11`
Starts the native Termux:X11 display server daemon using the embedded APK and libraries:
```bash
termux-x11 :0 [options]
```

### `termux-x11-preference`
Inspect and change X11 settings directly from your terminal or shell scripts:
```bash
# List all configurable preference keys and current values:
termux-x11-preference list

# Change preferences (e.g. resolution scale, fullscreen mode):
termux-x11-preference displayScale:100 fullscreen:true
```

### `termux-x11-env`
Launch X11 apps with GPU hardware acceleration (Vulkan/Zink bridge):
```bash
# Launch hardware-accelerated Firefox:
termux-x11-env firefox

# Run general 3D/X11 applications on GPU:
termux-x11-env glxgears
```

### `termux-setup-microphone`
Broadcasts the intent request to prompt the Android system for runtime audio recording permission (`android.permission.RECORD_AUDIO`).

### `termux-motd-refresh`
Refreshes and displays the dynamic MOTD banner with live Battery, RAM, and Storage stats if the **Modern Termux:MOTD** companion app is installed. If the companion app is not installed, it outputs a clean, colored error guiding you to install it.

---

## Installation

Modern Termux requires **Android 7.0 or later** and currently targets **`arm64-v8a`** architecture.

1. Navigate to the [latest release](https://github.com/Furkan00487/modern-termux/releases/latest).
2. Download `termux-app_<version>-apt-android-7-github-release_arm64-v8a.apk` and its corresponding `..._sha256sums` file.
3. [Verify the download integrity](#verifying-a-download), then install the APK on your device.

### Compatibility with Other Termux Builds

Modern Termux is signed with its own dedicated private release key. Android package management prevents installing this build over existing Termux installations signed by F-Droid, upstream GitHub releases, or Google Play (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).

To switch to Modern Termux:
1. [Back up your existing Termux data](https://wiki.termux.com/wiki/Backing_up_Termux) if necessary.
2. Uninstall your current Termux app and any installed Termux plugins.
3. Install Modern Termux APK.

---

## Verifying a Download

Check the SHA-256 checksum from the directory containing both files:

```bash
sha256sum -c termux-app_<version>-apt-android-7-github-release_sha256sums
```

Verify the APK signature with `apksigner` (from the Android SDK build tools or the Termux `apksigner` package):

```bash
apksigner verify --verbose termux-app_<version>-apt-android-7-github-release_arm64-v8a.apk
```

The output will confirm validity under both **Scheme v2** and **Scheme v3**:
```
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

---

## Upgrading and Uninstalling

- **Upgrading:** Installing a newer release over an existing Modern Termux installation upgrades the app in place and preserves all user files and packages. When `versionCode` increases, Modern Termux automatically refreshes helper scripts (`termux-x11`, `termux-setup-microphone`) and updates the MOTD banner.
- **Uninstalling:** Remove Modern Termux from **Android Settings** -> **Apps** -> **Modern Termux** -> **Uninstall**.

---

## Building from Source

```bash
# Clone the repository with its submodules
git clone --recursive https://github.com/Furkan00487/modern-termux.git
cd modern-termux

# Run unit tests
./gradlew testDebugUnitTest

# Build release APK
./gradlew downloadBootstraps assembleRelease
```

Native X11 modules are linked as submodules, so clone with `--recursive`. Release signing in CI uses the `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, and `KEYSTORE_ALIAS` secrets. In local environments without these variables, `assembleRelease` produces an unsigned APK.

---

## Debugging

Modern Termux includes a localized Turkish UI by default. To capture diagnostic logs for troubleshooting:

1. **Open Settings:**
   - Swipe from the left edge of the terminal and tap the gear icon ⚙️ (**Ayarlar**), or
   - Long-press anywhere on the terminal window and choose **Ayarlar** (**Settings**).
2. **Access Log Level:**
   - Tap **Modern Termux**.
   - Tap **Hata Ayıklama** (**Debugging**).
   - Under **Günlük Kaydı (Logging)**, tap **Günlük Düzeyi** (**Log Level**).
3. **Select Verbosity:**
   - Select **Ayrıntılı (Verbose)** or **Hata Ayıklama (Debug)**.
   - Always revert back to **Normal** after testing to avoid performance overhead.

View logs from within Termux:
```bash
logcat -d | grep -i "com.termux"
```

Or from a connected PC via ADB:
```bash
adb logcat -s "termux:*"
```

---

## Contributing

- Keep pull requests focused and run `./gradlew testDebugUnitTest` prior to opening a PR.
- Follow [Conventional Commits](https://www.conventionalcommits.org) (e.g. `feat: add dynamic notification icon`, `fix: handle null service instance`).
- When releasing a new version, bump `versionCode` (+1 integer) and `versionName` (SemVer `major.minor.patch`) in `app/build.gradle` and submodule gradle files.
- Place shared utilities in `termux-shared` to maintain modular separation.

---

## Upstream Resources

- [Termux Wiki](https://wiki.termux.com/wiki/)
- [Termux Packages Repository](https://github.com/termux/termux-packages)
- [Termux File System Layout](https://github.com/termux/termux-packages/wiki/Termux-file-system-layout)
- [Running Commands via RUN_COMMAND Intent](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent)

---

## License

Modern Termux is licensed under the [GNU General Public License v3.0](LICENSE).
- Upstream terminal emulation libraries from [Terminal Emulator for Android](https://github.com/jackpal/Android-Terminal-Emulator) are licensed under Apache 2.0. See `terminal-view` and `terminal-emulator`.
- Additional notices can be found in `termux-shared/LICENSE.md`.
