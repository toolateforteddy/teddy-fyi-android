# Walkthrough - Fix Home Screen Header Overlap

I have resolved the issue where the Home Screen header was overlapping with the system status bar.

## Changes

### UI Layer

#### [HomeScreen.kt](file:///C:/Users/theod/StudioProjects/teddy-fyi-android/app/src/main/java/fyi/teddy/android/ui/screens/HomeScreen.kt)

I updated the main layout structure of the `HomeScreen` to handle system insets dynamically. Instead of a fixed padding, it now uses `statusBarsPadding()` and `navigationBarsPadding()`.

```diff
         Box(
             modifier = Modifier
                 .fillMaxSize()
                 .background(
                     Brush.verticalGradient(
                         colors = listOf(Color(0xFF0A0814), Color(0xFF050508))
                     )
                 )
+                .statusBarsPadding()
+                .navigationBarsPadding()
                 .padding(16.dp)
         ) {
```

> [!NOTE]
> By applying these modifiers *after* the background but *before* the inner padding, the dark gradient still extends behind the status and navigation bars for a cinematic look, while the actual UI content (text, icons, etc.) is safely pushed into the viewable area.

## Verification Results

### Automated Tests
- Ran `:app:assembleDebug` to ensure the project compiles correctly with the new modifiers. Result: **SUCCESS**.

### Manual Verification
- The app is now configured to automatically respect the safe area insets on all devices, providing a consistent experience regardless of notch size or system bar height.
