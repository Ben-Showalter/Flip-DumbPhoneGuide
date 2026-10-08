# AGENTS.md: building Android apps for keypad flip phones

Read this before you write or change code in any app for a keypad flip phone.

**Target hardware:** Kyocera DuraXV Extreme **E4810** and its siblings (E4811, E4610), and Sonim
XP-series phones. They run AOSP-based Android 5.1–10, have no touchscreen and no Google Play
services, and are driven only by a D-pad, two soft keys, a 12-key keypad, Call, End, Clear, Back
and a few OEM keys. The screen is about 240×320 px.

Every rule here comes from code that has run on these phones. The `docs/` files explain the
reason for each one. When a rule and your instinct disagree, the rule wins: most of them exist
because the obvious approach broke on the device.

---

## House rules

### 1. Keys and soft keys (most important)
- **Use the same soft-key layout on every screen:**
  - **Left soft key** (`KEYCODE_SOFT_LEFT`) runs the screen's **primary action**, such as
    Refresh or New.
  - **Right soft key** (`KEYCODE_SOFT_RIGHT`) opens **Options/Settings**. On a sub-screen it
    can open that screen's own options, for example a map legend.
  - Also accept `KEYCODE_MENU` as Options. Some devices send MENU for a soft key.
- **Put the key map in one shared base activity** (see `templates/FlipBaseActivity.kt`), not in
  each screen. Screens override hooks such as `onPrimaryKey()`.
- **Show soft-key labels** in a bottom bar matching the physical keys. Labels must be
  `focusable="false"`. If there's no bar, keep a one-line key legend, e.g. `0-9 SPEED  * MODE  # SETUP`.
- **OK key: match both `KEYCODE_DPAD_CENTER` and `KEYCODE_ENTER`.** Devices send either one.
- **CENTER goes in `onKeyDown`**, so a focused list row gets it first ("open this row").
  If a view swallows CENTER (a MapView, for example), intercept it in `dispatchKeyEvent` on
  that screen.
- **D-pad LEFT/RIGHT across screens go in `dispatchKeyEvent`**, ahead of focus handling, so they
  still work when a row has focus.
  - Skip this when `currentFocus is EditText`, because there the arrows move the cursor.
  - Skip it on screens that need the arrows themselves, such as a pannable map.
  - Clamp at the ends; don't wrap.
- **Back:** leave the normal back stack alone. The exception is a screen you reached by a
  sideways jump (`startActivity` + `finish`), which has nothing underneath it. Override Back
  there to go to a sensible parent rather than quitting the app.
- **Clear sends `KEYCODE_BACK`, not `KEYCODE_DEL`** (Kyocera). In a text field, treat BACK/DEL
  as backspace while there is text, and as exit when the cursor is at the start.
- **Number keys are for shortcuts:**
  - On map screens, `*` / `#` zoom out / in and `5` re-centers.
  - Digits select presets or slots.
  - Treat `KEYCODE_STAR` and `KEYCODE_NUMPAD_MULTIPLY` as the same key.
- **Repeats:** run one-shot actions only when `event.repeatCount == 0`. Let movement keys
  (scroll, pan) repeat.
- **Hold actions:** for a long press, either compare `eventTime - downTime`, or call
  `event.startTracking()` and check `isLongPress`. Swallow the matching key-up so the tap action
  doesn't also run.
- **If you consume a key's DOWN, also consume its UP.** Otherwise the stray UP reaches the next
  window or app.
- **When a key launches another app** (for example the dialer), launch it on key **UP**.
  Otherwise the UP reaches that app and types a second digit.
- **Unhandled CALL/END must reach `super`**, so the phone still dials and hangs up.
  **CALL works well as Send** in compose screens.
- **Mic/Assistant key** is `KEYCODE_F4` when an app has focus and raw code `287` when none
  does. Match both.
- **Avoid these keys:**
  - **Camera key:** the OS swallows it.
  - **Volume Up:** it opens a volume popup that steals window focus. You *can* reuse volume keys
    in a full-screen app with no text field (for example, for brightness).
- **Hold-to-act keys:** remember which action started on DOWN and stop that same action on UP.
  Also release all held keys in `onWindowFocusChanged(false)` and `onPause`, because a dialog or
  closing the flip can swallow the UP.

→ Details: [docs/keys.md](docs/keys.md)

### 2. Focus, lists and menus
- **There is no touch, so focus is the only way users see where they are.** Use one shared, very
  visible focus drawable across the whole app:
  - `ListView`: `android:listSelector="@drawable/list_row_selector"` plus
    `android:drawSelectorOnTop="true"`. See `templates/list_row_selector.xml`.
  - `RecyclerView` rows: a tinted background plus a 5–6dp accent bar. A 2–3dp bar is too hard
    to see.
  - Buttons and fields: a `<selector>` with a filled `state_focused` item. Set
    `stateListAnimator="@null"` on Buttons.
- **The list container must not take focus:**
  - Set `isFocusable = false` on the `RecyclerView` itself.
  - Use `descendantFocusability="afterDescendants"`. Rows are focusable.
- **Every interactive view:** `focusable="true"` and `focusableInTouchMode="false"`. Text fields
  are the exception.
- **Give every screen a clear starting focus** with `post { row.requestFocus() }`. Only request it
  when nothing has focus yet, or you will pull focus away while the user scrolls.
- **Don't wrap at the ends of lists.** Wrapping is confusing on a tiny screen.
- **When rows are taller than the screen, scroll in small steps** (about ¾ inch, 120dp) before
  moving focus to the next row. Snap to a whole row when scrolling stops.
- **Menus are `AlertDialog.Builder.setItems(...)`.** They work with the D-pad as is. Never ship a
  dialog that can only be dismissed by touch.
- **Never use screen pinning** (`startLockTask`): its confirmation dialog can't be reached
  with the D-pad.

→ Details: [docs/focus-and-lists.md](docs/focus-and-lists.md)

### 3. Text input
- **For numeric fields, use a plain `EditText`** with `inputType="number"` or `"phone"`, plus
  `windowSoftInputMode="stateHidden"` or `"stateAlwaysHidden"`.
  - IP fields: let `*` type `.` (the keypad has no dot) and mention it in the label.
- **For free text, either:**
  - (a) use a plain `EditText` and let the system T9 keyboard handle it; or
  - (b) use your own T9 with `NoImeEditText` (`templates/NoImeEditText.kt`), whose
    `onCheckIsTextEditor()` returns false.

  Option (b) avoids the E4811 `InputMethodManager` "getShwoingNowFlag" exception loop, which drops
  soft-key presses. Don't half-do it: with no input-method session you also have to draw the cursor
  yourself.
- **The system T9 keyboard can swallow `KEYCODE_SOFT_RIGHT`** while a text field is focused.
  Screens with text entry need another route to Options, such as the left soft-key menu.
- **T9 conventions for a custom T9:**

  | Key | Behaviour |
  |---|---|
  | `#` | space |
  | `*` | cycles case |
  | `1` | punctuation picker |
  | hold a digit (700ms) | types the digit |

  Show the current mode in the left soft-key label.
- **Never make the user type an API key.** Read it from a text file in storage, or accept it in
  an SMS from a trusted number.

→ Details: [docs/text-input.md](docs/text-input.md)

### 4. Screen, layout and type
- **Design for 240×320 at mdpi.** Some phones are 320×240 landscape. Test at that size.
- **Font sizes:** body text at least 16sp, labels 14sp, metadata 11sp minimum. Headers 18sp.
- **One line per label, ellipsized.** Use `minHeight`, not fixed row heights. Give status and
  message lines a fixed height so the layout doesn't jump.
- **Use a dark background with high contrast**, at least 7:1.
- **Bold doesn't render with the stock font.** Use `paint.isFakeBoldText = true`.
- **Some glyphs don't render with the stock font.** ⏯ and ⏸ show as boxes; ■ and ▶ are fine.
  Keep emoji to a curated set.
- **Use Android Views, not Compose.** Views handle D-pad focus well and are lighter on 2 GB phones.
- **Use short animations (≤150ms), alpha only, no ripples.**
- **Opening or closing the flip must not recreate the activity.** Use
  `android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout"`,
  and lock to portrait unless the app supports landscape.

→ Details: [docs/layout-and-type.md](docs/layout-and-type.md)

### 5. Platform and SDK
- **SDK levels:**
  - `minSdk`: 24, or 22 if a Sonim on Android 5.1 must work.
  - `targetSdk`: 28–34. Test permission flows on the real phone.
  - Build with Java/JVM 1.8 and `minifyEnabled false`.
- **Declare the touchscreen optional:**
  `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>`. Do
  the same for GPS, camera and microphone.
- **No Google Play services, ever** (`com.google.android.gms.*`). Replacements:

  | Instead of | Use |
  |---|---|
  | Fused location | `LocationManager` (GPS + NETWORK providers), one-shot fix with a timeout |
  | FCM push | Foreground service + WorkManager fallback (Android 15 caps `dataSync` at about 6h/day) |
  | Play Store | Sideloading (see §7) |

- **Watch your API levels.** `Context.getColor()` is API 23; `getSystemService(SmsManager)` is
  API 31, so use `SmsManager.getDefault()`. Check every API against `minSdk`.
- **Storage permissions:** put `maxSdkVersion` on the legacy ones, and only check them at
  runtime when `SDK_INT <= P`.
- **OEM Settings deep links to a single app's page don't work.** Open the general list screen
  (`ACTION_ACCESSIBILITY_SETTINGS`, `ACTION_USAGE_ACCESS_SETTINGS`, …) and show a toast with the
  manual path.
- **Accessibility services:**
  - Set the flags in code as well as in XML.
  - Keep `onKeyEvent` cheap: it blocks key delivery to every app.
  - Launch activities from a service with `PendingIntent.send()`, because the OEM throttles
    `startActivity()` from the background.

→ Details: [docs/platform.md](docs/platform.md)

### 6. Network, data and battery
- **Throttle manual refreshes to how often the data actually changes**, and show
  "Already up to date - try again in about N min".
- **Cache the last result** and draw it right away when the user comes back to a screen. Don't
  re-fetch just because they navigated.
- **Keep all I/O and database work off the main thread.** Don't refresh from both `onCreate` and
  `onResume`.
- **Warm caches early, carefully.** Start them in `Application.onCreate` on a background
  thread, but only after the required permission is granted. Registering an observer without
  permission kills the process on first install.
- **Wi-Fi with no internet** (for example a device's own hotspot): bind sockets through
  `network.socketFactory` from a `NetworkCallback`, or Android routes the traffic over
  cellular.
- **On animated or tiled screens, keep frame counts low.** Pause timers in `onPause`.

→ Details: [docs/network-battery.md](docs/network-battery.md)

### 7. Diagnostics and distribution
- **Show unknown keys on screen.** For keycodes you don't handle, show a short message such as
  `KEY 287` or `Unhandled key: KEYCODE_F4`. Rugged phones remap buttons, and this is the
  fastest way to learn a new phone.
- **Show every error on screen in plain language**, with a way to retry. The user has no logcat.
- **Distribute APKs by sideloading** (ADB, an email attachment, or a USB copy) through
  GitHub Releases. In the README, explain permission toggles as exact D-pad steps.

→ Details: [docs/distribution.md](docs/distribution.md) · [docs/testing.md](docs/testing.md) ·
devices: [docs/devices.md](docs/devices.md)

---

## New-app checklist
- [ ] `FlipBaseActivity` with soft keys (Left = primary action, Right/MENU = Options), OK =
      CENTER|ENTER, and LEFT/RIGHT screen navigation that skips EditText
- [ ] On-screen soft-key labels or a key legend; labels aren't focusable
- [ ] A shared focus drawable, applied to every list, row, button and field
- [ ] List containers don't take focus; every screen has a starting focus
- [ ] Back works sensibly on screens reached by a sideways jump
- [ ] `touchscreen required=false`, `configChanges` for the flip, no `gms` dependency
- [ ] `minSdk` ≤ 24 and no API calls above it
- [ ] A toast for unhandled keycodes; plain-language errors on screen
- [ ] Refresh throttle and cached last result
- [ ] README: device tested, sideload steps, key map table
- [ ] Tested on a 240×320 mdpi emulator with touch off, then on the real phone

## Conventions decided here
- **Options goes on the right soft key.** Some flip-phone apps put Options on the left and Back
  on the right; don't follow them. Only change this if the right soft key really can't be
  reached on a given screen (see the T9 note in §3).
- **Text entry:** choose the system T9 or a custom T9 per app, as described in §3. Never mix both
  approaches in one field.

## Keeping this guide alive
When you find a new device quirk, keycode or workaround, **add it here in the same change**:
- a one-line rule in this file, plus
- the Rule → Why entry in the matching `docs/` file.

Keep this file short and imperative; put the long explanations in `docs/`.
