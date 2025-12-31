# LogMeow

Android logcat viewer for Desktop (macOS, Windows, Linux)

## Features

- **Logcat Viewer**: Real-time Android logcat monitoring with filtering
- **Log Bookmark**: Bookmark important log lines for quick navigation
- **Screenshot Capture**: Take device screenshots and save to local directory
- **Screen Recording**: Record device screen to MP4 video
- **DeepLink Execution**: Execute and manage deeplink history
- **Scrcpy Integration**: Launch scrcpy for device mirroring and control

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Cmd+C` / `Ctrl+C` | Copy selected logs to clipboard |
| `Cmd+B` / `Ctrl+B` | Toggle bookmark for selected logs |
| `Click` | Select single log |
| `Shift+Click` | Select range of logs |
| `Cmd+Click` / `Ctrl+Click` | Toggle individual log selection |
| `Right-Click` | Context menu (Add/Remove Bookmark)

## Requirements

- JDK 17 or higher
- Android SDK with ADB installed
- scrcpy (optional, for device mirroring feature)

### ADB Setup

The application automatically searches for ADB in the following locations:

1. `$ANDROID_HOME/platform-tools/adb` or `$ANDROID_SDK_ROOT/platform-tools/adb`
2. `~/Library/Android/sdk/platform-tools/adb` (macOS)
3. `/usr/local/bin/adb`
4. `/opt/homebrew/bin/adb` (macOS with Homebrew)
5. System PATH

**Recommended Setup (macOS):**

Add to your `~/.zshrc` or `~/.bash_profile`:

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

Then restart your terminal or run `source ~/.zshrc`

### Scrcpy Setup (Optional)

For device mirroring and control feature, install scrcpy:

**macOS (Homebrew):**
```bash
brew install scrcpy
```

**Windows:**
Download from [scrcpy releases](https://github.com/Genymobile/scrcpy/releases)

**Linux:**
```bash
sudo apt install scrcpy  # Debian/Ubuntu
```

The application automatically searches for scrcpy in common installation paths.

## Build Guide

### Run Application

```bash
./gradlew run
```

### Build DMG (macOS)

```bash
./gradlew packageDmg
```

Output: `build/compose/binaries/main/dmg/LogMeow-1.0.0.dmg`

### Build MSI (Windows)

```bash
./gradlew packageMsi
```

Output: `build/compose/binaries/main/msi/LogMeow-1.0.0.msi`

### Build DEB (Linux)

```bash
./gradlew packageDeb
```

Output: `build/compose/binaries/main/deb/logmeow_1.0.0-1_amd64.deb`

### Build for Current OS

```bash
./gradlew packageDistributionForCurrentOS
```

### Create Distributable (without installer)

```bash
./gradlew createDistributable
```

Output: `build/compose/binaries/main/app/`

## Development

### Project Structure

```
src/main/kotlin/
├── adb/              # ADB service and data models
├── di/               # Dependency injection
├── ui/               # UI components
│   ├── common/       # Common UI components
│   └── icons/        # Custom icons
└── vm/               # ViewModels
```

### Technology Stack

- Kotlin
- Jetpack Compose for Desktop
- Koin (Dependency Injection)
- Kotlinx Coroutines

## License
Copyright 2026 gaiuszzang (Mincheol Shin)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
