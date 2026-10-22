# Break Tracker — iOS integration

This branch is isolated from `main` and does not alter the website or Android build.

## Architecture

- Preserve existing `index.html` and web deployment.
- Package the web interface using Capacitor iOS, with a dedicated `ios` native project generated on macOS.
- Build a native Swift bridge for `AVPictureInPictureController` using a supported media content source (`AVPlayerLayer` or `AVSampleBufferDisplayLayer`).
- Detect support with `isPictureInPictureSupported()` and runtime readiness with `isPictureInPicturePossible`.
- Keep countdown state based on absolute end timestamps and persist timer transitions.
- Restore state on foreground; use local notifications for scheduled completion alerts, subject to OS permissions.
- Keep custom theme/style selection in the main app. The PiP window is system-controlled; do not rely on arbitrary HTML controls or private APIs.
- Test on physical iPhone/iPad and confirm App Store review suitability before release. Timer-only PiP may not qualify as video playback.

## Build prerequisites

- macOS, current Xcode, Node.js and Apple signing credentials.
- Capacitor CLI and iOS packages installed in a separate iOS project without changing Android workflows.
- Set up app-specific bundle ID, icons, and notification permissions.

## Validation

1. Website remains unchanged.
2. Existing Android workflow remains unchanged.
3. Native app launches and shows current timer.
4. PiP support and eligibility are checked at runtime; unsupported cases get a clear fallback.
5. Countdown restores correctly after app switching and device sleep.
6. PiP content and transitions are tested on devices; no private APIs or silent audio hacks.
