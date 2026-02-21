# N0tez

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

### Testing

Run unit tests to verify icon generation and dimensions:

```bash
./gradlew test
```

## React Native (Expo) Editor Shell

The React Native / Expo Router UI shell lives in `apps/faceshot-expo/` so it stays isolated from the native Android Gradle project.

Run it:

```powershell
cd apps\faceshot-expo
npm install
npm start
```
