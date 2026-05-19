# Core App Features

This document tracks the features of the Teddy FYI Core App.
The Developer will add feature requests to the Requested Features section, and then, upon Prompting,
will look at the Requested Features section, implement each feature, then move that item to the
Current Features List.


## Current Features
- [x] **Todo Manager**: Access to a Todo list manager with local persistence and edit/debug modes.
- [x] **Auth Caching**: User session is saved to SharedPreferences. On app launch, if a valid session exists, the user is automatically navigated to the home screen.
- [x] **Profile Picture**: The user's Google profile picture is displayed on the home screen.
- [x] **Grocery List**: A dedicated Grocery List manager with quantity support and local persistence.
- [x] **Unauthed Mode**: Added a "Skip Auth" option visible only when running on an emulator, allowing development without a Google account.
- [x] **Health Checks for teddy.fyi**: The home screen makes an async request to https://teddy.fyi/ on load and displays a green (success) or red (failure) cloud icon in the bottom right corner.
- [x] **Encrypted Storage**: Ensure sensitive session and authentication tokens are fully migrated to `EncryptedSharedPreferences` for enhanced security.

## Requested Features
- [ ] **Networking Library Migration**: Migrate from `HttpURLConnection` to Retrofit or Ktor for improved maintainability and cleaner network code.

## Planned Features
- [ ] **Deeper Health Checks**: There should be another screen to tap into that can show some version of health checks for all api services at teddy.fyi, and not just the nginx static proxy.
- [ ] **DataStore Migration**: Migrate remaining app-wide settings from `SharedPreferences` to `Jetpack DataStore`.
- [ ] **Modularization**: Transition to a feature-based multi-module Gradle project structure.
- [ ] **Static Analysis Integration**: Integrate Detekt or Ktlint to enforce coding standards.
- - [ ] **Dependency Injection**: Refactor the singleton repository into an injectable class (using Hilt or similar) to improve modularity and testability.

## Bugs
- [x] **Token display**: Removed the Token display from the home screen.
- [x] **Profile picture missing**: Added a default profile icon and improved error handling for image loading on the home screen.
- [x] **App crashes when calling authed endpoint**: Added robust error handling and logging to the network repository to prevent crashes on invalid tokens or server errors.