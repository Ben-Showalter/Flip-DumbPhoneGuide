# Network, data and battery

Each entry gives the **rule** first, then *why* it exists.

## Refresh and caching
- **Auto-refresh instead of a Refresh key.** Show the cache the moment the screen opens, refetch
  in the background when it's due, recheck about every minute while the screen stays open, and
  show an "Updated h:mm" line: green when fresh, red when stale (e.g. over 20 minutes), with a
  small spinner while fetching. *Why:* it frees the second soft key (see
  [keys.md](keys.md#soft-keys-and-options)) and the user never has to think about refreshing.
- **Throttle any manual refresh to how often the source data actually changes.**
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
  - Prewarm about 10 threads. *Why:* 25 kept the message store busy for minutes on the E4810
    and held up sending.
  - Prewarming 157 threads ran for 90s or more and stalled a send for 39s on an E4610.
  - The SMS/MMS provider costs about 2.7s per query, no matter how much it returns.
- **The OEM SMS provider:**
  - It ignores `ORDER BY date DESC`, so sort the results yourself.
  - Watch the `mms-sms`, `sms` and `mms` URIs, debounced at 500ms.
- **Cache decoded images in an LruCache** of about 20 bitmaps. *Why:* decoding is slow.

- **Parse large downloads from a streamed file, never from one `String`.** These phones have
  2 GB of RAM.
- **Run once-a-day work on Home resume and `SCREEN_ON`;** no scheduler is needed.

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

- **When a library hides its network error, retry the same URL once with Android's own
  networking** and report the failing step: DNS, refused, timeout or TLS.
- **For TLS on Android 7–11,** see [platform.md](platform.md#other-oem-behaviour).

## Maps
- **Draw maps with Canvas from XYZ raster tiles,** not an OpenGL map library: MapLibre GL
  crashed natively on the E4610.
- **Use tile providers that need no key, and check them on the phone.** One provider answered
  "API key required" on the device; Esri ArcGIS Online tiles worked without a key, but their URL
  order is `{z}/{y}/{x}`. Put road and label overlays above weather layers.
- **Size the tile bitmap LruCache at a quarter of `maxMemory`.** While a tile loads, stretch the
  next coarser cached tile in its place, and prefetch every animation frame for the current
  view. *Why:* tiles are 256–384 KB each.
- **Animated layers start stopped on "Now", and OK plays.** Advance the frame *before* the delay;
  delaying first sits on the same frame and looks as if OK did nothing.
- **Make build-time map tokens optional:** read them from `local.properties` into a
  `buildConfigField`, and fall back to a provider that needs no key.

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
- **Safety-critical stop commands over TCP:** after each stop, ask for status; if that fails,
  reconnect and send the stop again. *Why:* a dropped link can swallow a write with no error.
- **Devices with their own Wi-Fi hotspot:** document the default SSID, password and IP, and add a
  "Wi-Fi settings" button that opens `Settings.ACTION_WIFI_SETTINGS`.
