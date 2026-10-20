# Break Tracker Android (native Picture-in-Picture)

This is a separate Android WebView wrapper for the existing Cloudflare-hosted Break Tracker. The PWA remains intact. The native bridge receives timer start/end events from both MA/ZIP and Kitt and shows a native countdown in Android Picture-in-Picture when you press Home while a break is active.

## Build an APK
1. Install Android Studio (recent version supporting Android Gradle Plugin 8.7.3) and Android SDK 35.
2. Open the **android** directory as a project. Allow Gradle sync; use JDK 17.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Find the debug APK at `android/app/build/outputs/apk/debug/app-debug.apk` after a successful build. Install on an Android device for testing.

The repository does not include a Gradle wrapper JAR. Android Studio can sync with an available Gradle distribution. No APK is committed or claimed built.

## Important
- Native PiP requires a device supporting PiP and enabling it in Android settings. Enter PiP by pressing Home during an active timer.
- The 3-minute warning uses an in-process timer and is not guaranteed if Android stops the app. A foreground service would be needed for robust long-running background notifications.
- This wrapper loads the production URL: https://break-tracker.jhamal16.workers.dev/. Deploy website changes before testing native timer integration.
- Only the production origin should be loaded; do not navigate to untrusted websites with the native JavaScript bridge enabled.
- Kitt still uses browser-local storage, while MA/ZIP uses its existing Supabase setup. No new sync or permissions are introduced.
