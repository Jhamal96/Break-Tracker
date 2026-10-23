# Break Tracker — Android APK with native Picture-in-Picture

The website/PWA stays separate and unchanged by Android-specific code. The APK loads the existing Cloudflare-hosted Break Tracker in a WebView and displays a native Android countdown over the Home screen during an active break.

## Picture-in-Picture: 14 countdown designs

The native APK now has **14 designs**, matching the names and visual themes in the website's browser PiP picker:

Digital, Clock Dial, Speedometer, Minimal, Neon Pulse, Aurora Glass, Retro Flip, Racing HUD, Cosmic Orbit, Sunset Glow, Pixel Arcade, Zen Garden, Ocean Depths, and Blueprint.

### Choose a design
1. Open the installed Android APK, tap the **Settings (gear)** icon, then choose **◉ PiP Styles & Previews**. Select from 14 visual sample cards.
2. Select a design. Your selection is saved locally on the phone.
3. Start a break in the MA/ZIP or Kitt tracker, then press Home to enter native PiP on supported phones.
4. **While PiP is active, tap the floating window** to reveal Android's system PiP controls. Use **Previous style** and **Next style** to cycle through designs without expanding the app. Android controls the PiP menu, so an arbitrary in-window dropdown cannot be made clickable in every Android version.
5. Tap or expand the floating window to return to the app for the complete 14-style list.

The countdown is drawn natively using Android Canvas, so switching styles does not depend on the browser PiP API. Time and progress update once a second. A warning color appears when three minutes or less remain. Native PiP continues to require a phone that supports Picture-in-Picture and that has it enabled for Break Tracker.

## Download the APK from GitHub

Every successful [Android APK build](https://github.com/Jhamal96/Break-Tracker/actions/workflows/build-android-apk.yml) uploads a ZIP artifact and is configured to attach `Break-Tracker-Android.apk` to a [GitHub Release](https://github.com/Jhamal96/Break-Tracker/releases).

1. Open the latest successful workflow or Release.
2. Under **Assets**, download `Break-Tracker-Android.apk` (or unzip the workflow artifact and install `app-debug.apk`).
3. Install it on Android, granting permission to install from this source if prompted.

**Note about updates:** These GitHub builds are *debug* APKs. GitHub runners may generate a fresh debug signing key for a later build. If Android reports a conflicting package signature, the app cannot be installed over the previous APK. **Back up any Kitt data before uninstalling** because uninstalling normally erases the app's local WebView storage. Do not uninstall merely to change PiP style; the 14 designs are included in one APK build.

## Build on a PC or Mac

1. Install [Android Studio](https://developer.android.com/studio) with JDK 17, and Android SDK 35.
2. Open the `android/` directory as a Gradle project.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. The output is `android/app/build/outputs/apk/debug/app-debug.apk`.

Android Studio's local Gradle setup or the GitHub Actions workflow can compile it. The repository does not include a Gradle wrapper JAR.

## Important limitations

- The APK's native PiP UI is separate from Document Picture-in-Picture in desktop browsers. A browser's style menu does not automatically propagate to native Android; the APK saves its own selection.
- The native timer receives active break information through the website's `AndroidBreakTracker` bridge. The website must be deployed to Cloudflare with that integration; GitHub HTML commits alone do not establish a live deployment.
- Android can pause or kill background app processes. The 3-minute notification and 1-second UI updates are best effort, not a foreground-service-backed alarm.
- Native Kitt data still lives in the APK's WebView local storage, independently of the Samsung Internet PWA. MA/ZIP uses its existing Supabase setup.
- The WebView is intended to load only `https://tracker.ajdapp.org/`. External top-level links open in an external browser instead.

## New domain and layout (v1.2)

- The Android app now opens **https://tracker.ajdapp.org/**, the Cloudflare custom domain connected to the existing Break Tracker Worker.
- **Active Break Tracker** appears before **Shift Setup**, including an APK-side layout safeguard if the hosted HTML is temporarily older.
- The native **PiP Styles & Previews** option is added to the existing website Settings menu inside the APK. The earlier floating blue style-picker button has been removed. The 14-option native preview gallery lets you choose a style before entering PiP.
- When updating the same installed app, a one-time on-device migration attempts to preserve Kitt records from the old `break-tracker.jhamal16.workers.dev` WebView localStorage origin. Existing records under the new domain take precedence for matching Kitt numbers, and a private local backup of any recovered legacy records is retained.
- **Keep your old APK installed until you've confirmed your data in the updated app.** GitHub's ephemeral debug signing keys can prevent upgrading in place; uninstalling the old app may erase local Kitt records and defeat migration. If Android rejects the update due to signatures, back up Kitt data before removing the old version.
