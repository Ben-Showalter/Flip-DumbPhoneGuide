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
| Mic / Assistant | `KEYCODE_F4` (134) when an app has focus, raw `287` (`KEYCODE_SPEAKER_IN`, which has no constant) when none does. The E4610 sends 287 (scan 171) even when an app has focus | Match both |
| Outer SOS | no usable keycode; scan code `763` (E4810) | Match by `scanCode`. Press only, no hold |
| Outer END | scan code `764` (E4810), `172` (E4610) | Match by `scanCode`. Press only, no hold |
| Outer Speaker | scan code `765` (E4810), `213` (E4610) | Match by `scanCode`. Press only, no hold |
| PTT | scan code `766` (E4810), `231` (E4610) | Match by `scanCode`. Press only, no hold |
| Bluetooth headset button | `KEYCODE_MEDIA_PLAY_PAUSE` or `KEYCODE_HEADSETHOOK` | Rarely `MEDIA_PAUSE`; see below |
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
- **Long press: time the hold yourself.** Post a delayed runnable on the first DOWN
  (`repeatCount == 0`), cancel it on UP, and when it has fired, swallow the UP so it doesn't
  also count as a tap. Comparing `event.eventTime - event.downTime >= HOLD_MS` on repeat events
  also works where repeats arrive.
  *Why:* on the Kyocera keypad, `isLongPress` delivery is unreliable; a held digit fired both
  the tap (dial) and the hold (speed dial) for the same press.
- **Ignore a second UP for a `downTime` you've already handled.**
  *Why:* some presses deliver a spurious extra `ACTION_UP` mid-hold, sharing the real press's
  `downTime`. It looked like a release and dialled twice. Don't clear this state in `onPause`:
  the dial that the first UP launched pauses the activity before the spurious UP arrives.

## Repeats and pairing
- **Fire on the first press only:** `val first = down && event.repeatCount == 0`.
- **When you consume a DOWN, consume its UP too.**
  *Why:* a stray UP reaches the next window ("Cancelling event due to no window focus").
- **Act on an UP only if this window saw its DOWN.** Track pressed keys in a set on DOWN and
  fire on UP only when the key is removed from it (and the UP isn't canceled).
  *Why:* the UP of the press that opened or closed a screen lands in the next window. A launcher
  opened Notices from the UP of the key that had just closed another screen.
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

## Outer buttons (Kyocera)
- **Identify SOS, outer END, outer Speaker and PTT by `event.scanCode`** (see the table above),
  mapped to your own logical keys in one place.
  *Why:* they arrive with no usable `KEYCODE_*`, and the scan codes differ between the E4810
  and E4610.
- **No long press on them.** *Why:* the phone reports only the press, never how long the
  button is held. A hold timer never fired; it was tried and reverted.
- **Act on a single press, but only while your window has focus, the screen is on and the
  keyguard is down.** *Why:* these buttons sit on the outside of the phone and get pressed in a
  pocket. Closing the flip turns the screen off, which covers that case. Don't use the
  `keyboardHidden` configuration to detect a closed flip; it's unreliable on these keypads.
- **Kyocera's own button assignment (`kyocera.intent.action.PTT_SETTINGS`) only works under
  Kyocera Home.** *Why:* it's a Kyocera Home feature, so a replacement launcher must assign the
  buttons itself.

## Bluetooth headset buttons
- **Treat `KEYCODE_MEDIA_PLAY_PAUSE` and `KEYCODE_HEADSETHOOK` as pause.**
  *Why:* headsets rarely send `MEDIA_PAUSE`, and `MediaSession`'s default `onMediaButtonEvent`
  ignores PLAY_PAUSE unless `ACTION_PLAY_PAUSE` is advertised, and never maps HEADSETHOOK.
  Override `onMediaButtonEvent`.
- **On Android 7 set `FLAG_HANDLES_MEDIA_BUTTONS`** (deprecated, a no-op on 26+).
  *Why:* without it the session receives no media buttons on the E4610.
- **For text-to-speech, also catch the buttons in an accessibility service that filters key
  events** (see [platform.md](platform.md)). *Why:* on Android 8+ media buttons go to the app
  that last played audio, and TTS audio is played by the speech engine's process, so your
  session may never be chosen.
- **To let any button stop a readout,** consume the stopping press (DOWN and UP) and let volume
  keys through so the user can still adjust the volume.
