# Phonograph

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE.txt)

English | [简体中文](README_ZH.md)

Phonograph is a material-designed Android music player based on the
original [kabouzeid/Phonograph](https://github.com/kabouzeid/Phonograph).
This branch keeps the Java app architecture while modernizing the build,
Android platform behavior, playback stack, and library sources.

![Screenshots](./art/art.jpg?raw=true)

## Highlights

- Android 16 / API 36 ready: `compileSdk 36`, `targetSdk 36`, `minSdk 23`,
  Java 17, AGP 9.2.1, and Gradle 9.4.1.
- Local music library with songs, albums, artists, genres, playlists, folders,
  queue controls, widgets, media notification controls, and lock screen controls.
- Subsonic/OpenSubsonic/Navidrome V1 support: add and manage servers, switch
  between the local library and servers from the navigation drawer, sync remote
  metadata into a local SQLite cache, browse remote songs/albums/artists/genres/
  playlists, and stream music.
- Remote playback quality controls: use original files by default, optionally
  request server transcoding with configurable format and bitrate.
- Playback fallback: Android native playback is tried first; Media3/FFmpeg
  playback is used as a fallback for unsupported formats such as ALAC when the
  container can be parsed by Media3.
- Subsonic album art cache: remote cover art is cached on demand in the app
  cache directory, with manual pre-cache and cleanup actions in server
  management.
- Lyrics: embedded lyrics and sidecar `.lrc`/`.txt` files are preferred; online
  matching and manual search are available through LRCLIB, NetEase Cloud Music,
  Kugou, and an experimental QQ Music provider. Apple Music is not used as a
  lyric source because the official API does not provide lyric text.
- Flat now playing screen only. Legacy appearance choices, billing, Pro unlocks,
  and donation flows have been removed; former Pro features are available by
  default.

## Current Scope

Subsonic support currently focuses on browsing and streaming. Offline downloads,
remote playlist editing, rating/favorite/scrobble, and "now playing" server
status synchronization are intentionally out of scope for the V1 implementation.

The app does not cache music files. It caches Subsonic metadata, remote cover
art, and selected/matched lyrics in app-private storage.

## Build

Requirements:

- JDK 17
- Android SDK platform 36
- Android Studio that supports AGP 9.2.1, or the included Gradle wrapper

Debug build:

```sh
./gradlew :app:assembleDebug
```

Lint:

```sh
./gradlew :app:lintDebug
```

Release builds use the app's release signing configuration. Provide the expected
signing properties or adjust `app/build.gradle` for your local signing setup
before running:

```sh
./gradlew :app:assembleRelease
```

## Permissions and Privacy

- Local library browsing requires Android media permissions on modern Android
  versions.
- Notification permission is requested separately and denial should not block
  playback.
- Remote libraries can be browsed and played without granting local media
  permissions.
- Subsonic/OpenSubsonic/Navidrome playback sends requests to the configured
  server. HTTP self-hosted servers are allowed, but HTTPS is recommended.
- Online lyric matching sends song metadata such as title, artist, album, and
  duration to third-party lyric providers.

## License

This project is licensed under the GNU General Public License v3.0. See
[LICENSE.txt](LICENSE.txt).
