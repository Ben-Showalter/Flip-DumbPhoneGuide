# Keys

Each entry gives the **rule** first, then *why* it exists.

## Keycode table (Kyocera E4810/E4811 unless noted)

| Physical key | Keycode(s) delivered | Notes |
|---|---|---|
| Left soft key | `KEYCODE_SOFT_LEFT` (some devices: `KEYCODE_MENU`) | House rule: primary action |
| Right soft key | `KEYCODE_SOFT_RIGHT` | House rule: Options. The system T9 keyboard may swallow it while typing |
| OK / D-pad center | `KEYCODE_DPAD_CENTER` **or** `KEYCODE_ENTER` | Always match both |
| D-pad | `KEYCODE_DPAD_UP/DOWN/LEFT/RIGHT` | |
| Clear | `KEYCODE_BACK` (not DEL) | Treat as backspace in text fields |
| Back (where separate) | `KEYCODE_BACK` | Never swap it with the soft keys |
| Call / Send | `KEYCODE_CALL` | Good as "Send"; let it through to `super` when unhandled |
| End | `KEYCODE_ENDCALL` | Acts as Home; leave it alone |
| Mic / Assistant | `KEYCODE_F4` (134) when an app has focus, raw `287` (`KEYCODE_SPEAKER_IN`, which has no constant) when none does | Match both |
| Camera | swallowed by the OS | Unavailable to apps |
| Volume Up/Down | `KEYCODE_VOLUME_UP/DOWN` | Volume Up opens a popup that steals focus |
| `*` | `KEYCODE_STAR` or `KEYCODE_NUMPAD_MULTIPLY` (Sonim) | Match both |
| `#` | `KEYCODE_POUND` | |
| 0-9 | `KEYCODE_0..9` | |

If a key isn't in this table, show its code on screen (see [testing.md](testing.md)), then add
it here.

## Soft keys and Options
- **Left soft key = the screen's primary action. Right soft key = Options/Settings.**
  *Why:* when every app works the same way, users don't have to relearn the keys.
- **Put the soft-key handling in a shared base activity; each screen overrides hooks.**
  *Why:* the layout stays the same everywhere, and new screens get it for free.
  See `templates/FlipBaseActivity.kt`.
- **Accept `KEYCODE_MENU` as a soft key.**
  *Why:* the soft keys don't send `SOFT_LEFT`/`SOFT_RIGHT` on every device.
- **Translate raw keycodes into logical keys in one place.**
  *Why:* when a new phone sends different codes, you fix one table.
- **Never swap or remap BACK along with the soft keys.**
  *Why:* on phones with a separate Back key, a swap that included BACK turned Back into Options.
- **The system T9 keyboard can swallow `KEYCODE_SOFT_RIGHT` while a text field is focused.**
  *Why:* seen on Kyocera. The only workaround is an accessibility service that filters key
  events, and it isn't reliable. Plan text screens so Options can be reached another way.

## OK / CENTER
- **Match `DPAD_CENTER` and `ENTER`.** *Why:* devices send either one for the same key.
- **Handle the app-wide CENTER action in `onKeyDown`, not `dispatchKeyEvent`.**
  *Why:* Android sends the key to the focused child view first, so a focused list row can run
  its own "open" action.
- **If a view keeps swallowing CENTER, intercept it in `dispatchKeyEvent` before `super`.**
  *Why:* MapLibre's `MapView` takes focus even with `focusable="false"`, and its default click
  handling ate OK.

## D-pad
- **Handle LEFT/RIGHT screen switching in `dispatchKeyEvent`, and skip it when
  `currentFocus is EditText`.**
  *Why:* it works even while a row or button holds focus, and in a text field the arrows still
  move the cursor.
- **Lay screens out in a row and clamp at the ends; don't wrap.**
  From a screen outside the row, LEFT goes to the first main screen and RIGHT to the last.
  Show direction hints in the header (e.g. "◀ Groups", "Broadcasts ▶").
- **Screens that need the arrows for themselves opt out of switching.** On a map, for example,
  the arrows pan.
- **Check the pan direction on the device.** MapLibre's `scrollBy` signs were the opposite of
  what the docs say.
- **Movement keys may repeat and speed up the longer they're held**: 1, then 2, 3 and 4 cells
  per repeat.
- **Override Android's geometric focus search where it picks the wrong target.**

## Back and Clear
- **Keep the normal back stack.**
- **A screen you reached with `startActivity` + `finish` has nothing beneath it.** Override
  `onBackPressed` to go to its logical parent.
- **Clear sends `KEYCODE_BACK`.** In a text field: backspace while there's text; exit (and
  save a draft) when the cursor is at the start.
- **Kiosk/lock mode:** swallow Back, and enable a disabled-by-default HOME `activity-alias` so
  Home/End returns to the app. *Why:* screen pinning's confirmation dialog can't be reached
  with the D-pad. See `templates/AndroidManifest-snippets.xml`.

## Number keys, `*` and `#`
- **On map screens:** `*` zoom out, `#` zoom in, `5` re-center.
- **Digits select presets; `*` cycles a mode.**
- **Treat `KEYCODE_STAR` and `KEYCODE_NUMPAD_MULTIPLY` as the same key.**
- **Digits that open the dialer act on key UP.**
  *Why:* if you open it on DOWN, the UP reaches the dialer and the first digit is typed twice.
- **Long press:** use one of these.
  - Compare `event.eventTime - event.downTime >= HOLD_MS` on repeat events, with a `handled`
    flag that stops the UP counting as a tap.
  - Or call `event.startTracking()` on the first DOWN and check `isLongPress`.

  *Why:* `isLongPress` alone doesn't fire reliably on some devices.

## Repeats and pairing
- **Fire on the first press only:** `val first = down && event.repeatCount == 0`.
- **When you consume a DOWN, consume its UP too.**
  *Why:* a stray UP reaches the next window ("Cancelling event due to no window focus").
- **Hold to act, release to stop:** store keycode → action on DOWN, and stop that same action on
  UP. *Why:* the stop has to match the start even if a setting changed mid-press.
- **Release all held keys in `onWindowFocusChanged(false)`, in `onPause`, and before opening
  another screen.** *Why:* a dialog or closing the flip swallows the UP.
- **Cancel T9 hold timers on the next key's DOWN, not just on UP.**
  *Why:* when typing fast, one key's DOWN arrives before the previous key's UP, and stray digits
  appeared ("4hogs").

## Other hardware keys
- **CALL = Send** in compose screens, and **Dial** on a contact list. Unhandled CALL/END fall
  through to `super`.
- **Mic key:** `setOf(KeyEvent.KEYCODE_F4, 287)`. Start on DOWN with `repeatCount == 0` and stop
  on UP.
- **Don't use Volume Up as a global trigger.**
  *Why:* the volume popup takes window focus and the focused text field is lost.
- **Volume keys can be reused in a full-screen app with no text field**, e.g. for brightness
  steps.
- **The camera key is unavailable**, so put camera actions in the Options menu.
- **Let users choose a trigger key with a full-screen capture Activity, not a Dialog.**
  *Why:* raw key capture in Dialog windows behaves differently across OEM builds.
- **If two installed apps both hook the same hardware key**, one must ignore it while the other
  is in the foreground.
