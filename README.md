# Break Tracker

Break Tracker website and Android app.

## Android app landing page

The app has a dedicated, mobile-friendly **[Android download page](https://tracker.ajdapp.org/download)** in `download.html`. It links directly to the latest GitHub Release asset at `releases/latest/download/Break-Tracker-Android.apk`, with a secondary link to the GitHub Releases page.

**Cloudflare deployment:** The existing `break-tracker` Worker uses static assets. Include `download.html` in the same deployed static-assets directory as `index.html`, `kitt.html`, `icon.svg`, `sw.js`, and `manifest.webmanifest`. Cloudflare Workers' default HTML handling exposes `download.html` at `/download` without an extra routing script. Avoid deploying the repository's entire root as public assets, because it contains the Android source code and GitHub workflow files.

**Release visibility:** The GitHub repository is private. GitHub may require sign-in and repository access for APK downloads. For public downloads without GitHub authentication, publish the release APK through a separate public download host before advertising the landing page to external visitors.

## Download Android APK

**[Download the latest Android APK](https://github.com/Jhamal96/Break-Tracker/releases/latest)**

On the Release page, expand **Assets** and download `Break-Tracker-Android.apk`. This is a debug/test APK. If Android prompts for permission to install apps from your browser or file manager, approve only if you trust this repository.

The APK is automatically published to GitHub Releases after each successful Android build. You can also find build logs and temporary artifacts under [GitHub Actions](https://github.com/Jhamal96/Break-Tracker/actions/workflows/build-android-apk.yml).

The website is still available at https://tracker.ajdapp.org/.
