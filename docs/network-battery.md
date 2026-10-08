# Network, data and battery

Each entry gives the **rule** first, then *why* it exists.

## Refresh and caching
- **Throttle manual refreshes to how often the source data actually changes.**
  - Store a timestamp per screen in prefs.
  - When a refresh comes too soon, show "Already up to date - try again in about N min".

  For example, weather observations change roughly hourly and radar every 5–10 minutes, so
  10–15 minute and 5 minute throttles respectively are about right.
- **Cache the last result, and draw it the moment the user comes back to a screen.** Don't make
  a network call just because they navigated there. Clear the cache when its key (for example
  the location) changes.
- **Load a small first page fast, then the full set.** For example, show 12 messages, then load
  the whole history.
- **Skip re-rendering when fresh data equals the cache.**

## Threads and startup
- **Keep all network, database and decoding work off the main thread.** Use `lifecycleScope`
  coroutines, or background threads at background priority.
- **Don't run a refresh from both `onCreate` and `onResume`.** *Why:* the duplicate queries
  competed with each other.
- **Warm caches in `Application.onCreate`, but only after the needed permission is granted.**
  *Why:* a content observer registered without permission throws `SecurityException` and kills
  the process on first install.
- **Warm only what you need.**
  - Prewarming 25 threads was fine.
  - Prewarming 157 threads ran for 90s or more and stalled a send for 39s on an E4610.
  - The SMS/MMS provider costs about 2.7s per query, no matter how much it returns.
- **The OEM SMS provider:**
  - It ignores `ORDER BY date DESC`, so sort the results yourself.
  - Watch the `mms-sms`, `sms` and `mms` URIs, debounced at 500ms.
- **Cache decoded images in an LruCache** of about 20 bitmaps. *Why:* decoding is slow.

## Connectivity
- **Wi-Fi without internet** (a device's own access point):
  - Request a `TRANSPORT_WIFI` network with `NET_CAPABILITY_INTERNET` removed.
  - Create sockets via `network.socketFactory`. *Why:* otherwise Android routes the traffic over
    cellular.
  - This works on Android 5.1.
- **Open connections in `onResume` and close them in `onPause`.**
- **Prefer free data sources that need no API key**, and throttle them as a courtesy.
- **Features that need the cloud must say so.** For example, cloud transcription needs a data
  connection at the moment it's used.

## Audio and voice
- **Record AAC at 16 kHz, 32 kbps. Trim silence before uploading**, and fall back to the
  untrimmed file if trimming fails.
- **Bluetooth headset microphone:**
  - Keep SCO connected for about 15s after the last key press, to avoid a 450ms reconnect.
  - Release it in `onPause`.
  - Fall back to the phone's microphone after 2s.

## Battery and lifecycle
- **Stop animation timers and polling in `onPause`, and resume them in `onResume`.**
- **For long-running sync without FCM, see the foreground service notes in
  [platform.md](platform.md).**
- **Volume keys can control screen brightness** in full-screen apps used at night (10 steps,
  0.01–1.0, via `window.attributes.screenBrightness`).
