# Setting Up Android Emulator in VS Code

This guide explains how to set up an Android Emulator so you can build and run Android apps directly inside Visual Studio Code on macOS.

## Prerequisites

1. **Install Java (JDK 17 recommended):**
   The easiest way on macOS is via Homebrew:
   ```bash
   brew install --cask temurin
   ```

2. **Install Android Studio (Recommended for SDK/Emulator setup):**
   While you can install command-line tools only, installing Android Studio provides a GUI that makes downloading SDKs and setting up emulators much easier.
   ```bash
   brew install --cask android-studio
   ```
   *Launch Android Studio once and follow the initial setup wizard to download the default Android SDK.*

## Setting Up the Emulator (AVD)

1. Open **Android Studio**.
2. Click on the **More Actions** menu (or the three dots) and select **Virtual Device Manager**.
3. Click **Create Device**.
4. Choose a hardware profile (e.g., Pixel 7) and click **Next**.
5. Download a System Image (e.g., API Level 34 or the latest recommended) and click **Next**.
6. Give the emulator a name and click **Finish**.
7. You can now close Android Studio if you wish; the emulator is set up.

## Configuring VS Code

1. Open Visual Studio Code.
2. Install the **Android** extension (e.g., `mathieumuller.vscode-android` or similar, depending on your workflow. For generic Gradle projects, the **Extension Pack for Java** is helpful).
3. If using Flutter or React Native, you would install their respective extensions, but for standard native Android, the main requirement is running the emulator and triggering gradle builds.
4. **Running the Emulator from VS Code / Terminal:**
   You can start the emulator via terminal:
   ```bash
   # Add Android tools to your PATH in ~/.zshrc or ~/.bash_profile
   export ANDROID_HOME=$HOME/Library/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

   # Start the emulator
   emulator -avd <Your_AVD_Name>
   ```

## Running the App

1. With the emulator running, open the terminal in your project directory.
2. Run the build and install command:
   ```bash
   make build
   # After building, install and run:
   adb install app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n fyi.teddy.android/.MainActivity
   ```
