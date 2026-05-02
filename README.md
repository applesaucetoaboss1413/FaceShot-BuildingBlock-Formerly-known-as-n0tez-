# FaceShot BuildingBlock

## Icon Integration System

This project includes a custom icon integration system for both the application launcher icon and the floating bubble widget.

### Updating Icons

To update the icons, replace the source images in `.github/workflows/`:

1.  **Floating Bubble Icon**: Replace `Floating Overlay Image.jpeg` with your new image.
2.  **Launcher Icon**: Replace `Primary ApkImage.jpeg` with your new image.

Then run the generation script:

```bash
python generate_icons.py
```

This script will:
- Generate optimized PNGs for all Android densities (mdpi to xxxhdpi).
- Create adaptive launcher icons (foreground + background).
- Generate iOS icon assets in `distribution/ios/AppIcon.appiconset`.

### Features

- **Floating Bubble**:
  - Auto-hides (dims to 30% opacity) after 5 seconds of inactivity.
  - Reappears (100% opacity) on touch/drag.
  - Supports drag-and-drop repositioning.
  - Circular crop applied automatically during generation.

- **Launcher Icon**:
  - Adaptive icon support (API 26+).
  - Legacy icon support for older Android versions.

## Canonical App Layout

- Native Android app: `app/`
- Canonical Expo shell: `apps/faceshot-expo/`
- Deprecated duplicate Expo surface: `rn/` has been removed from the working tree and should not be restored

## Android Setup

For local Android builds, create a machine-local `local.properties` at the repository root that points to your installed Android SDK. Keep this file uncommitted.

Examples:

```properties
# Windows
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk

# macOS
sdk.dir=/Users/<you>/Library/Android/sdk

# Linux
sdk.dir=/home/<you>/Android/Sdk
```

For CI, set `ANDROID_SDK_ROOT` in the runner environment and generate `local.properties` during setup so the build does not depend on any developer-specific path.

Android green-path verification from the repository root:

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-daemon
```

## Expo Shell

The canonical Expo Router shell lives in `apps/faceshot-expo/`.

Install dependencies:

```powershell
npm --prefix .\apps\faceshot-expo ci
```

Expo green-path verification:

```powershell
npm --prefix .\apps\faceshot-expo run verify
```

Start the Expo shell:

```powershell
npm --prefix .\apps\faceshot-expo start
```
