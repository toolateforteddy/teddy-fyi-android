# teddy-fyi-android
Making an Android App

## Project Structure
- `app/`: Main application module.
- `Makefile`: Commands to build, install, and run the app.
- `setup.md`: Guide for setting up the environment.

## Getting Started
1. Ensure you have the Android SDK installed.
2. Run `make build` to build the project.
3. Run `make install` to install on a connected device/emulator.
4. Run `make run` to launch the app.

## Development Notes
### Database Migrations
Destructive migration is **disabled** to protect local data. Any changes to the Room database schema (in `AppDatabase.kt`) that require a version bump **must** be accompanied by a migration script using `.addMigrations(...)` in the `getDatabase` builder.
