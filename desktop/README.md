# Break Tracker — Desktop installers

The Windows and macOS apps wrap the existing production website at https://tracker.ajdapp.org/. They do **not** replace the web app or Android project. App data follows the same server-side account and Supabase rules as the website; local webview storage is independent of browser storage.

- Windows: `Break-Tracker-Windows-Setup.exe` (Windows x64)
- macOS: `Break-Tracker-macOS.dmg` (universal Intel + Apple Silicon)
- Repository workflow: [Build Windows and Mac desktop installers](https://github.com/Jhamal96/Break-Tracker/actions/workflows/desktop.yml)
- Release assets: https://github.com/Jhamal96/Break-Tracker/releases/tag/desktop-latest
- Download website: https://tracker.ajdapp.org/download

GitHub Actions runs when `desktop/**` or the desktop workflow changes or from the **Run workflow** button. Build results appear as workflow artifacts and a stable `desktop-latest` pre-release. Both installers are intentionally **unsigned** for initial testing: Windows SmartScreen and macOS Gatekeeper may show warnings, and macOS notarization requires an Apple Developer account. Do not disable system-wide security features to install.

The repository is currently **private**. Release downloads therefore require an authorized GitHub sign-in; a separate public distribution location is needed before inviting public visitors to download files.

The desktop shell uses Electron with Node integration disabled, context isolation and renderer sandbox enabled; external links open in the system browser. The hosted website must be online for full functionality and will supply UI updates immediately. Build and test an installer before distributing it widely.

## Fixed: floating timer on Intel Macs and Windows

**Desktop 1.1.0** uses an Electron **always-on-top floating window** instead of relying on the browser-only Document Picture-in-Picture API. This specifically addresses the message “This browser could not open system PiP” inside the installed desktop app.

- Open the desktop app, start a break on **MA/ZIP** or **Kitt**, and click the floating timer icon next to Active Break Tracker.
- A compact, movable native window opens over other applications. Drag its top bar to move it, resize its edges, or close it using ×. The remaining time is calculated using the original break end timestamp.
- The **Style** menu inside the floating window has all 14 countdown designs (Digital, Clock Dial, Speedometer, Minimal, Neon Pulse, Aurora Glass, Retro Flip, Racing HUD, Cosmic Orbit, Sunset Glow, Pixel Arcade, Zen Garden, Ocean Depths, Blueprint). The choice is saved for subsequent windows.
- Starting, completing or resetting a break in the source page updates the companion window. Closing only the companion window does not cancel the break.
- Both **macOS Monterey 12 on Intel** and **Windows x64** are targeted. The macOS DMG is built as a universal installer. This native mini-window is NOT Apple's video Picture-in-Picture API; it is a normal Electron topmost window designed for timer use.
- The web/PWA and Android APK continue using their existing PiP implementations. The desktop app loads the hosted page and installs a narrow timer-button hook so an HTML redeployment is not required for this fix.

**Important:** Updating the hosted website alone will not fix an older downloaded Electron app. Install the **newly built** DMG/EXE from the desktop-latest release after a green GitHub Actions run. Older unsigned Mac installers may continue triggering Apple's Gatekeeper warning; use System Preferences → Security & Privacy → General → Open Anyway only when you trust the source. Do not disable macOS security globally.

## Local development

Run `cd desktop && npm install && npm start`. To make an installer on the matching OS, run `npm run dist:win` on Windows or `npm run dist:mac` on macOS.
