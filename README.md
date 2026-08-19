# BlackOut

A minimalist, OLED-friendly clock and battery monitor app designed for Android devices, with special support for dual-screen handhelds like the **Ayn Thor**.

## Features

- **Pure OLED Black:** The background is `#000000` to minimize power consumption and prevent burn-in on OLED screens.
- **Minimalist UI:** Shows the current time and battery percentage in a clean, high-contrast design.
- **Settings Menu:** Accessible via a gear icon when the UI is visible.
- **Custom Triggers:**
    - **Battery Threshold:** Automatically show the UI when battery falls below a set percentage.
    - **Time Trigger:** Automatically show the UI at a specific time (stays active for 1 hour).
- **Low Battery Alert:** The time and battery status pulse red when the battery is below 15%.
- **Smart Inactivity Timer:** The UI automatically fades out after 10 seconds of inactivity to keep the screen "blacked out." A simple tap brings it back.
- **Dual-Screen Support:** Specifically optimized for the Ayn Thor. If launched on the primary screen, it automatically reroutes itself to the secondary (bottom) screen.
- **Splash Screen:** Professional, minimalist startup experience.
- **Lightweight:** Highly optimized with a tiny footprint (~2.5MB).

## Installation

### GitHub Releases
You can download the latest installable APK from the [Releases](https://github.com/YOUR_USERNAME/blackout/releases) section.

### Obtainium (Recommended)
This app is compatible with [Obtainium](https://github.com/ImranR98/Obtainium). To get automatic updates:
1. Open Obtainium.
2. Click **Add App**.
3. Paste the URL of this GitHub repository: `https://github.com/YOUR_USERNAME/blackout`
4. Click **Add**.

## How to Build

1. Clone the repository.
2. Open in Android Studio.
3. Build the `release` variant.

## License

MIT License - feel free to use and modify!
