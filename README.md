# NinjaVu

Premium Android viewer for [BingeBang](https://bingebang.st/) — movies, series, calendar, explore, and My Space in one installable app. Works on phones and Android TV with the same remote and keyboard controls.

NinjaVu does not host video. It is an unofficial client shell that opens the public BingeBang site inside a tuned player, with a light native chrome, server-friendly WebView settings, downloads, subtitle file picking, picture-in-picture, and a domain fallback to the official gateway at [bingebang.club](https://bingebang.club/).

## Install the release APK

1. Download `NinjaVu-1.1.0.apk` from the [Releases](https://github.com/HumbleKidd/NinjaVu/releases) page after the 1.1.0 build is published, or rebuild with the script below.
2. On the phone or TV, open the APK from Files or a browser download. On Android TV, sideload with Send Files to TV or `adb install -r`.
3. If Android blocks it, allow install from that app (Settings → Apps → Special access → Install unknown apps).
4. Open **NinjaVu**. First launch shows a short splash, then the live catalogue. Press OK or Enter on the splash to skip it.

Package: `com.ninjavu.app`  
Version: 1.1.0 (versionCode 2)  
Min Android: 7.0 (API 24)  
Target: Android 14 (API 34)

Updates install over 1.0.0 because the release keystore is unchanged.

## Remote, keyboard, and Android TV

NinjaVu is a living-room app as well as a phone app. A Bluetooth remote, USB keyboard, or the Android TV D-pad drives the whole screen.

- Arrow keys and D-pad move a green focus ring across posters, buttons, links, and player controls
- OK, Enter, or Center selects the focused item
- Back closes a player or overlay, then goes back a page. Press Back twice on Home to leave
- Down from the top bar enters the catalogue. Up from the bottom tabs enters the catalogue
- Menu opens the more list. Search opens search
- Play/Pause toggles video. Rewind and fast-forward seek 30 seconds. Left and right seek 10 seconds while a fullscreen player is open
- The same controls work on a phone with a keyboard or remote. Touch still works

The app appears in the Android TV launcher (Leanback). Touchscreen is not required.

## What the app does

- Home, Movies, Series, Explore, and My Space as native tabs
- Trending, TV calendar, and Help from the more menu
- Native search that drives the site search box
- HD / 4K playback with fullscreen video and landscape while a title is playing
- Picture-in-picture
- One-tap refresh, desktop site mode, cache clear
- Download listener (movies and episodes the site already exposes)
- Subtitle file picker (OpenSubtitles upload on the site)
- Official domain switch: `bingebang.st` and gateway `bingebang.club`
- Offline retry screen instead of a blank WebView
- Cookies kept so optional BingeBang login, watchlist, and resume still work

## Rebuild

From a machine with the Android SDK command-line tools:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
bash tools/build-apk.sh
```

The signed APK is written to `dist/NinjaVu-1.1.0.apk`.

The release keystore lives in `keystore/ninjavu-release.jks`.  
Alias: `ninjavu`  
Store password: `NinjaVuRelease2026`  
Key password: `NinjaVuRelease2026`

Keep that keystore if you want updates to install over this build. Rotate it before any Play Store upload.

## Notes

BingeBang says it does not host files. It looks up streams and lets you switch servers in the player settings if one fails. If `bingebang.st` moves, open the gateway from the more menu — `bingebang.club` is their official domain list.
