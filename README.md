# BlackOut

A minimalist, OLED-friendly clock and battery monitor app designed for Android devices, with special support for dual-screen handhelds like the **Ayn Thor**.

## Features

- **Pure OLED Black:** The background is `#000000` to minimize power consumption and prevent burn-in on OLED screens.
- **Minimalist UI:** Shows only the current time and battery percentage in a clean, high-contrast design.
- **Smart Inactivity Timer:** The UI automatically fades out after 10 seconds of inactivity to keep the screen "blacked out." A simple tap brings it back.
- **Dual-Screen Support:** Specifically optimized for the Ayn Thor. If launched on the primary screen, it automatically reroutes itself to the secondary (bottom) screen.
- **Splash Screen:** Professional, minimalist startup experience.
- **Lightweight:** Highly optimized with a tiny footprint (~2.2MB).

## Installation

### GitHub Releases
You can download the latest installable APK from the [Releases](https://github.com/YOUR_USERNAME/blackout/releases) section.

### Obtainium (Recommended)
This app is compatible with [Obtainium](https://github.com/ImranR98/Obtainium). To get automatic updates:
1. Open Obtainium.
2. Click **Add App**.
3. Paste the URL of this GitHub repository: `https://github.com/YOUR_USERNAME/blackout`
4. Click **Add**.

To install via ADB:
```bash
adb install app-release.apk
```

## How to Build

1. Clone the repository.
2. Open in Android Studio.
3. Build the `release` variant.

## License

MIT License - feel free to use and modify!
