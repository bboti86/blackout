# Implementation Plan - Support Second Screen (Ayn Thor)

The goal is to ensure the app starts on the secondary screen (bottom screen) of the Ayn Thor device. If the app is launched on the primary (top) screen, it should automatically move itself to the secondary screen.

## Proposed Changes

### [Component Name]

#### [MODIFY] [AndroidManifest.xml](file:///home/bboti86/Project/blackout/app/src/main/AndroidManifest.xml)
- Add `android:supportsMultiDisplay="true"` and `android:resizeableActivity="true"` to the `.MainActivity` declaration. These flags are essential for an activity to be moved between displays.

#### [MODIFY] [MainActivity.kt](file:///home/bboti86/Project/blackout/app/src/main/java/net/bboti86/blackout/MainActivity.kt)
- Update `onCreate` to check the current display.
- If launched on `Display.DEFAULT_DISPLAY`, search for a secondary display using `DisplayManager`.
- If a secondary display is found, relaunch the activity on that display using `ActivityOptions.setLaunchDisplayId()`.
- Finish the current activity instance to complete the "reroute".

## Verification Plan

### Manual Verification
- Deploy the app to an Ayn Thor device (or a multi-display emulator).
- Launch the app from the primary screen launcher.
- Verify that the app immediately moves to the secondary screen.
- Verify that if launched directly on the secondary screen (if possible), it remains there.
