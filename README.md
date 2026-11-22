# Echo

A local music player for Android. It plays the songs already on your phone, with a dark UI, a lime accent and blurred album art everywhere. No server, no account, no internet.

![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-ExoPlayer-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-27-C8F25A)
![Offline](https://img.shields.io/badge/100%25-offline-0E0F11)

## Features

**Playback**
- Plays your local library, scanned from the phone with MediaStore
- Keeps playing in the background with a media notification and lock screen controls
- Queue, shuffle, repeat, play next, and a custom-drawn seek bar

**Library**
- Home with a cover-flow carousel of recent albums and a songs list with All / Favorites filters
- Search across titles and artists
- Browse by album or artist
- Favorites and your own playlists

**Now playing**
- Blurred album-art backdrop with a color tint pulled from the cover
- A circular disc that spins while the music plays
- Floating mini player and a glass pill navigation bar

**Profile**
- Name, bio, profile photo and cover photo
- A pinned anthem and your top 5 tracks, picked from your own songs
- Posts: a photo with a song from your library and a caption
- Library stats and the artists you collect most

Everything you create (profile, playlists, favorites, posts) is stored on the phone only.

## Built with

- Kotlin
- Jetpack Compose and Material 3
- Media3 (ExoPlayer and MediaSessionService)
- Coil for images
- Palette for colors from album art

## Getting started

1. Clone the repo:
   ```
   git clone https://github.com/Sumeet-Y1/Echo.git
   ```
2. Open the folder in Android Studio and let Gradle sync.
3. Connect a phone with USB debugging on, or start an emulator with some music files on it.
4. Run the `app` configuration and allow access to your music when asked.

Requires Android 8.1 (API 27) or newer. Target SDK is 37.

For the smoothest feel, run the **release** build variant. Debug builds are noticeably slower with Compose.

## Project layout

```
app/src/main/java/com/echoflux/echo/
  MainActivity.kt        permissions, navigation, overlays
  data/                  Song, MusicRepository (MediaStore), LibraryStore (local storage)
  playback/              PlaybackService, PlayerViewModel
  ui/                    screens, dock, now playing, profile, shared components
  ui/theme/              colors, typography, theme
```

## Roadmap

- [ ] Custom font
- [ ] Swipe down to close the player
- [ ] Sleep timer
- [ ] Lyrics
- [ ] Settings screen
- [ ] Embedded album art loading for newer Android versions

## About

Made by Sumeet (echoflux) as a first Android app.
