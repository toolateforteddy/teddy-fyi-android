# Implementation Plan - Fix Home Screen Header Overlap

The Home Screen content is overlapping with the system status bar (time and notification bar). This is likely due to the project targeting a high SDK version (API 36) where edge-to-edge rendering is enforced by default. The user has requested approximately 10px of additional space to resolve this overlap.

## User Review Required

> [!IMPORTANT]
> I will implement the fix using standard Android `WindowInsets` handling (`statusBarsPadding`). This is more robust than hardcoding a fixed pixel value, as it dynamically adjusts to different device configurations (notches, status bar heights, etc.).

## Proposed Changes

### UI Layer

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/ui/screens/HomeScreen.kt)

- Add `statusBarsPadding()` to the main content container to ensure the header is pushed below the status bar.
- Add `navigationBarsPadding()` or ensure the bottom content respects system insets if necessary.
- I will apply these to the inner `Box` that contains the layout, while keeping the background gradient full-screen in the outer `Box`.

## Verification Plan

### Manual Verification
- Deploy the app to a device or emulator.
- Observe the `HomeScreen` to ensure the "Hello, [Name]" header is no longer overlapping with the status bar.
- Verify that the background gradient still extends behind the status bar for a seamless look.
