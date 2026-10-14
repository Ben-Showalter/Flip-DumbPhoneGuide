# Platform, SDK and OEM quirks

Each entry gives the **rule** first, then *why* it exists.

## SDK levels and build

| Setting | Recommended | Notes |
|---|---|---|
| `minSdk` | 24 (22 for Android 5.1 Sonims) | Covers Android 7–10 Kyoceras |
| `targetSdk` | 28–35 | Higher targets mean more runtime permission prompts; test them on the device |
| `compileSdk` | 34+ | |
| Java/JVM | 1.8 | |
| `minifyEnabled` | false | |

- **Check every API call against `minSdk`.**

  | API | Requires | Use instead |
  |---|---|---|
  | `Context.getColor()` | API 23 | `resources.getColor()` |
  | `getSystemService(SmsManager::class.java)` | API 31 | `SmsManager.getDefault()`; the other call crashed Send |
  | `setTextCursorDrawable` | API 29 | reflection (see [text-input.md](text-input.md)) |

- **Use the fewest dependencies you can.** A framework-only app (plain `Activity`, `java.net`)
  keeps the APK tiny and works on old firmware.
- **If you use okhttp, stay on 4.12.x.** *Why:* avoid the 5.x split artifacts.
- **If you need `java.time` below API 26, use core-library desugaring.**

## Manifest

See [`templates/AndroidManifest-snippets.xml`](../templates/AndroidManifest-snippets.xml).

- **`<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>`**,
  and the same for `location.gps`, `camera` and `microphone` when the app uses them.
- **Use `launchMode="singleTask"` on the launcher activity.** *Why:* launching from a hardware
  key or the home icon then lands on the main screen rather than whatever screen was left on top.
- **Add `configChanges` for opening and closing the flip** (see [layout-and-type.md](layout-and-type.md)).
- **Legacy storage permissions:**
  - Set `maxSdkVersion` on them.
  - Only check `READ/WRITE_EXTERNAL_STORAGE` at runtime when `SDK_INT <= P`. *Why:* checking
    them on API 29+ fails every time; on an E4811 the app never got past the check.
  - Use `MANAGE_EXTERNAL_STORAGE` for root-level folders.
- **Declare `MODIFY_AUDIO_SETTINGS` for a Bluetooth headset microphone (SCO).** *Why:* without
  it, `setMode` and `startBluetoothSco` fail silently.
- **Launchers: use a `<queries>` block for MAIN/LAUNCHER and DIAL rather than
  `QUERY_ALL_PACKAGES`**, and the `HOME` + `DEFAULT` categories with `singleTask` and
  `stateNotNeeded`.

## No Google Play services
- **Never depend on `com.google.android.gms.*`.**

  | Need | Replacement |
  |---|---|
  | Location | Plain `LocationManager`, with GPS then NETWORK providers. Get a one-shot fix with a 20s timeout. On failure, fall back to the last saved location, and show a toast telling the user to set it manually |
  | Push (FCM) | A `dataSync` foreground service (`FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`). Some carrier builds refuse the battery-optimization exemption. Android 15 caps `dataSync` at about 6h per 24h: in `onTimeout()`, hand off to a 15-minute WorkManager job |
  | Speech to text | A cloud Whisper-style API. On-device models were too heavy for these CPUs |
  | Store | Sideloading (see [distribution.md](distribution.md)) |

- **Location permission flow:** forward `onRequestPermissionsResult` from the base activity to
  the helper. When the saved location came from GPS, refresh it before every data refresh.
- **Repeating alerts that must fire with the app closed use AlarmManager.**

## OEM settings and permissions (Kyocera)
- **Deep links to a single app's Settings page don't work.** Open the general list screen:
  `ACTION_MANAGE_DEFAULT_APPS_SETTINGS`, `ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION`,
  `ACTION_ACCESSIBILITY_SETTINGS`, `ACTION_USAGE_ACCESS_SETTINGS` or
  `Settings.ACTION_WIFI_SETTINGS`. Wrap the call in try/catch and show a toast with the manual
  path.
- **On Android 13+, sideloaded apps may hit "restricted settings"** when the user tries to turn
  on accessibility.

## Accessibility services (global key hooks)
- **Set the service flags in code as well as in the XML config.** *Why:* the OEM build doesn't
  reliably honour the XML.
- **`onKeyEvent` blocks key delivery to every app.** Run expensive checks only for the one key
  you care about. *Why:* checking on every key made typing lag.
- **Export the service** (`exported="true"`). *Why:* that's the configuration known to work on
  this hardware.
- **Launch activities from the service with `PendingIntent.send()`, not `startActivity()`.**
  *Why:* the OEM delays the latter by several seconds ("Activity start request … stopped").
- **Detecting the foreground app is unreliable.**
  - Window events don't fire when the user returns Home.
  - `rootInActiveWindow` is null.
  - UsageStats lags by about 1s.
  - `getRunningTasks` sometimes reports Home while another app is in front.

  What works: `getRunningTasks()` first, then let UsageStats veto a "Home" result, with no age
  limit on the veto (an age limit let the hook fire inside other apps).
  Kyocera home is `jp.kyocera.kyocerahome.HomeScreenActivity`.
- **To filter key events, set `canRequestFilterKeyEvents="true"` and
  `flagRequestFilterKeyEvents` in the XML, and `FLAG_REQUEST_FILTER_KEY_EVENTS` in code.** Keep
  `onKeyEvent` to one volatile read when idle. *Why:* it's the only hook that sees every button,
  including Bluetooth headset keys, in any app and with the flip closed.
- **After an update adds a capability to the service, the user must switch it off and on.**
  *Why:* Android grants capabilities such as key filtering only when the service is enabled.
- **Inserting text into another app:** find the focused editable node across all windows,
  paste via the clipboard (`ACTION_PASTE`), then restore the clipboard. On this OEM an empty
  field reports its hint as its text, so don't rely on `isShowingHintText`.
- **Feedback without a touchscreen:** a 40ms vibration plus a non-touchable
  `TYPE_ACCESSIBILITY_OVERLAY` (API 26+). Below 26, use `TYPE_PHONE`, and only after
  `canDrawOverlays()`; otherwise it throws BadTokenException.

## Notifications
- **Notification Access is non-functional on the Kyocera E4810/E4811 (Android 9+).**
  `Settings.Secure` and `cmd notification allow_listener` appear to succeed and the component
  shows in `enabled_notification_listeners`, but `dumpsys notification` shows it never binds;
  only Kyocera's own listeners do. *Why:* a platform restriction on the newer models; the same
  code works on the E4610 (Android 7). Don't re-diagnose it.
- **Fallback: an accessibility service's `TYPE_NOTIFICATION_STATE_CHANGED` events.**
  - There's no list of active notifications and no removal event, so you can't build a full
    notification list from it.
  - Store the events in arrival order and pick the newest by `postTime`, never by list
    position. *Why:* the listener's list is newest-first and the event store is oldest-first;
    code shared between them showed the oldest message.

## Opening the app
- **A launcher tap resumes whatever screen was last open.** *Why:* the existing task is brought
  forward and no new intent is delivered, so `onNewIntent` doesn't run. Seen on the E4610.
- **If the app should always open on its home screen, decide in `onResume`.** Go home only when
  the user left the app themselves (`onUserLeaveHint`: Home or End) and came back, and not when:
  - returning from a screen your app opened (a picker, a settings page, a share sheet). Override
    `startActivityForResult(intent, requestCode, options)` to mark it: every launch goes through
    it, including fragment and `ActivityResult` launches;
  - the screen was off when the activity stopped (`PowerManager.isInteractive` is false in
    `onStop`). *Why:* closing the flip stops the activity too;
  - a notification opened the app (its extras arrive in `onNewIntent`, before `onResume`);
  - a call is on screen.
- **After process death, ignore the launch intent's extras.** *Why:* with saved state, `intent`
  is the one the activity was first started with, so an old notification tap would reopen its
  screen.
- **Tell an in-process recreate from a restart after process death with a static flag.**
  *Why:* both arrive with `savedInstanceState`; a theme change should keep its screen, a restart
  should go home.

## Resolving apps
- **Treat a `resolveActivity()` result in package `android` as unresolved.**
  *Why:* when several apps handle an intent and none is the default, it returns Android's
  chooser, so "the Gallery app" turned out to be the chooser.
- **Look for a category's app by known packages first, then all handlers, then label.**
- **To show an OEM activity in an app list, use an `<activity-alias>` of a small trampoline
  activity** that launches it and finishes. Catch `ActivityNotFoundException` and show a toast.

## Startup prompts
- **At startup, check the access the app needs (default Home, accessibility, …) and ask for
  one missing item at a time, at most once per start.** Each prompt opens the general settings
  screen and shows a toast with the D-pad path. *Why:* several dialogs at once stack up and
  can't all be answered; the per-app deep links don't work (see above).

## Outer display and LED (Kyocera)
- **The outer display (sub-LCD) is reachable by reflection** on
  `jp.kyocera.sublcd.SubLcdManager` / `SubLcdNotificationExtender`.
  - Every post needs a duration, or the screen stays on.
  - Nothing persists.
  - Wrap all of it in `try/catch (Throwable)`.
- **The message card template (`0x010900af`, `k_sublcd_template_5`) draws only an icon and one
  line of text.** Use `setIcon(resId)` and `setText(...)`. These are *not* drawn:
  - `setText2` and `setTitle`;
  - a second line after `"\n"` in `setText` (only the first line shows);
  - `setRemoteViews(...)`: the cover stays blank (status bar only) whatever the views or
    colours, with or without `setFullScreen(true)`.

  *Why:* each was tried on the DuraXV (MatChat). Don't spend another round on them.
- **The icon is a resource id only, and should be square.**
  - A runtime `Bitmap` or `Icon`, handed to any extender setter or to
    `Notification.Builder.setLargeIcon`, is ignored.
  - The slot is square and scales the image to fit, so a wide image draws small. Of 24 px
    `drawable-mdpi`, 48 px and 96 px `drawable-nodpi`, **96×96 `drawable-nodpi` drew best**.
  - Black on transparent reads well. App images work: that's how TurboText shows its own
    message symbol.
- **Measure how many characters fit beside the icon** with a still "ruler" card
  (`0123456789ABCDEFGHIJ`). *Why:* about 14 fit beside a small icon but only 9 beside a 96 px
  one; frames wider than that lose their ends.
- **There's no marquee, so scroll by reposting.** Post the same card id every ~400 ms with a
  sliding window of the text:
  - hold the start and the end for a few frames;
  - never wrap the end back into the start (it reads as broken text);
  - run whole passes only, and stretch the card's duration to fit, so it never stops partway
    through the message;
  - moving 2 characters per step reads well.
- **To re-show a card when the cover turns on**, register
  `SubLcdManager.registerCallback(ISubLcdCallback)`. It calls `onScreenStateChanged(1)` when the
  flip closes or an outside button wakes the cover.
  - Extending `ISubLcdCallback.Stub` needs a compile-only stub of the AIDL interface.
  - `ACTION_SCREEN_OFF` (flip closed) is a fallback.
  - Debounce it: your own post also wakes the cover, so an undebounced re-show loops.
- **Never call `SubLcdManager.notifyIconToAnnunciatorTray`.** *Why:* it crashed SystemUI on the
  DuraXV.
- **When testing card variants, give each test its own card id.** *Why:* a blank
  `setRemoteViews` card blanked the next card posted on the same id, which made a working
  approach look broken.
- **Ask the user whether a card should show the time.** The card covers the outer display's own
  clock while it shows. If they want it, put the time at the front of the one text line, for
  example `1:05 PM · Ann: hi`.
- **Notification LED:** enable it on the channel, plus `setLights(BLUE, 1, 0)`.
  Notification IDs must match the IDs you cancel, or icons stay forever.
- **Incoming calls over a closed flip:** use `USE_FULL_SCREEN_INTENT` + `setFullScreenIntent`.
