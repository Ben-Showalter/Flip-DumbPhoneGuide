# Keys

Each entry gives the **rule** first, then *why* it exists.

## Keycode table (Kyocera E4810/E4811 unless noted)

| Physical key | Keycode(s) delivered | Notes |
|---|---|---|
| Left soft key | `KEYCODE_SOFT_LEFT` | House rule: unused unless the app's owner asks for a shortcut |
| Right soft key | `KEYCODE_SOFT_RIGHT` | House rule: **Options** (Kyocera). The system T9 keyboard may swallow it while typing. On the DuraXV its DOWN can be lost, so only an UP arrives |
| Menu / Options key (Sonim and others) | `KEYCODE_MENU` | House rule: **Options**, labelled on the left |
| OK / D-pad center | `KEYCODE_DPAD_CENTER` **or** `KEYCODE_ENTER` | Always match both |
| D-pad | `KEYCODE_DPAD_UP/DOWN/LEFT/RIGHT` | |
| Clear | `KEYCODE_BACK` (not DEL) | Treat as backspace in text fields |
| Back (where separate) | `KEYCODE_BACK` | Never swap it with the soft keys |
| Call / Send | `KEYCODE_CALL` | Good as "Send"; let it through to `super` when unhandled |
| End | `KEYCODE_ENDCALL`, but on Kyocera it may never arrive as that (see [End on Kyocera](#end-on-kyocera)) | Acts as Home; leave it alone outside call screens |
| Mic / Assistant | `KEYCODE_F4` (134) when an app has focus, raw `287` (`KEYCODE_SPEAKER_IN`, which has no constant) when none does. The E4610 sends 287 (scan 171) even when an app has focus | Match both |
| Outer SOS | no usable keycode; scan code `763` (E4810) | Match by `scanCode`. Press only, no hold |
| Outer END | scan code `764` (E4810), `172` (E4610) | Match by `scanCode`. Press only, no hold |
| Outer Speaker | scan code `765` (E4810), `213` (E4610) | Match by `scanCode`. Press only, no hold |
| PTT | scan code `766` (E4810), `231` (E4610) | Match by `scanCode`. Press only, no hold |
| Bluetooth headset button | `KEYCODE_MEDIA_PLAY_PAUSE` or `KEYCODE_HEADSETHOOK` | Rarely `MEDIA_PAUSE`; see below |
| Camera | `KEYCODE_F3` (133) on the E4810; `288` on the E4610 (named `KEYCODE_SOFT_LEFT_LOWER` there, so match by `KeyEvent.keyCodeToString`); `KEYCODE_CAMERA` (27) elsewhere | Match all three. Usable by apps (e.g. take a picture while writing) |
| Volume Up/Down | `KEYCODE_VOLUME_UP/DOWN` | Volume Up opens a popup that steals focus |
| `*` | `KEYCODE_STAR` or `KEYCODE_NUMPAD_MULTIPLY` (Sonim) | Match both |
| `#` | `KEYCODE_POUND` | |
| 0-9 | `KEYCODE_0..9` | |

If a key isn't in this table, show its code on screen (see [testing.md](testing.md)), then add
it here.

## Soft keys and Options
Unless the person asking for the app says otherwise, design every app to work on phones that
don't have two soft keys.

- **One Options key.** On Kyocera it's the **right soft key**. On Sonim and other phones with a
  Menu key, it's the **Menu key** (`KEYCODE_MENU`), labelled on the **left**. Both always open
  Options. *Why:* many keypad phones have only one usable soft key or a dedicated Menu key;
  a layout that needs two soft keys strands those users.
- **The other soft key does nothing by default.** Put every action in the Options menu, on OK,
  on the D-pad or on number keys. Add a second-key shortcut only when the app's owner asks for
  one, and keep that action in the Options menu too. *Why:* phones with only a Menu key can't
  press it. For "Refresh", auto-refresh instead (see [network-battery.md](network-battery.md)).
- **Put the soft-key handling in a shared base activity; each screen overrides hooks.**
  *Why:* the layout stays the same everywhere, and new screens get it for free.
  See `templates/FlipBaseActivity.kt`.
- **Consume the whole MENU press in `dispatchKeyEvent`: its DOWN, its UP and its repeats.**
  *Why:* AppCompat opens its own (empty) options panel on MENU UP, covering the menu your DOWN
  just opened. On a Sonim XP3800, MENU looked like it did nothing. A plain framework `Activity`
  doesn't grab MENU at all.
- **The first Menu press ever asks once which side to show "Options" on:**
  "Your phone has an Options key. Show the "Options" label on the left side of the screen
  instead?"
  - Mark the prompt as seen *before* showing it, so it never repeats.
  - Yes moves the label, and the Options soft-key mapping, to the left. No leaves it right.
  - Re-hide the system soft-key bar when the dialog closes.
- **No "Options key" setting in Settings.** Instead, switch the label back to the right only
  when a `KEYCODE_SOFT_RIGHT` press is actually seen. *Why:* the phone itself tells you which
  key it has; a setting is one more thing to get wrong.
- **The footer shows a single "Options" label** at the end that matches the current side.
- **Right soft key with no DOWN:** on the DuraXV the OS can swallow the right soft key's DOWN,
  so only the UP arrives (seen on one screen; may depend on the situation). Treat a DOWN-less
  UP of `KEYCODE_SOFT_RIGHT` as a tap. For hold-to-act on that key, use tap to start, tap to
  stop. This is the one exception to "act on an UP only if this window saw its DOWN".
- **Digit aliases where digits aren't typed:** on screens with no text field or T9 search, `7`
  can act as the left soft key and `9` as the right. *Why:* some D-pad phones' soft keys send
  odd codes or none. Drop the aliases while digits are being typed.
- **Translate raw keycodes into logical keys in one place.**
  *Why:* when a new phone sends different codes, you fix one table.
- **Never swap or remap BACK along with the soft keys.**
  *Why:* on phones with a separate Back key, a swap that included BACK turned Back into Options.
- **The system T9 keyboard can swallow `KEYCODE_SOFT_RIGHT` while a text field is focused.**
  *Why:* seen on Kyocera. The only workaround is an accessibility service that filters key
  events, and it isn't reliable. Plan text screens so Options can be reached another way.
- **On Kyocera, neither soft key is reliable while a text field is focused.** The system T9
  keyboard uses the **left** soft key for its T9/abc/123 menu and can swallow the right one.
  Give text screens a focusable "More options…" row (for example at the end of a form) that
  opens the same menu with OK. *Why:* a sign-in screen that starts with focus in a text box had
  no way to reach Options.

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
- **Launchers: hold `#` toggles ring/vibrate**, as the stock Kyocera Home does; a tap still
  opens the dialer with "#". Switch `AudioManager.ringerMode` between `RINGER_MODE_NORMAL` and
  `RINGER_MODE_VIBRATE`, with a toast ("Phone set to Vibrate only"). *Why:* that's what the
  stock shortcut does (its logcat: `internal_ringer_mode_changed vibrate`), not Do Not Disturb.
  Normal <-> vibrate needs no permission; Do Not Disturb would need the "Do Not Disturb access"
  grant, and that list stays empty for sideloaded apps on the E4810/E4811.
  Remember the ringer mode before the first hold, and let the next hold restore it (ring at its
  earlier volume), as stock Kyocera Home does.
- **Hold OK on a selected row for a second action** (React, Copy…). Time the hold, open the row
  on release, and only act on an OK whose DOWN arrived in that mode.
- **Digits that open the dialer act on key UP.**
  *Why:* if you open it on DOWN, the UP reaches the dialer and the first digit is typed twice.
- **Typing a number from Home: keep every digit and reopen the dialer with the whole number.**
  Collect digits typed on Home in a buffer (cleared in `onResume`). On each digit, open
  `ACTION_DIAL` with `tel:<whole buffer>`. In `onPause`, send any digit still waiting out its
  tap debounce, rather than cancelling it.
  *Why:* the dialer takes a few hundred ms to come up. Keys typed meanwhile still reach Home, so
  opening the dialer with only the latest digit lost digits for a fast typist. Clearing pending
  callbacks in `onPause` dropped the digits typed just after the first one.
- **Don't cancel a pending same-digit tap on every re-press.** Only a re-press within about
  60ms of the release is the keypad splitting one hold in two; anything later is a real second
  press ("55").
  *Why:* cancelling on any re-press merged fast doubles into one digit.
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
- **Accessibility key filters follow the same pairing:** consume the UP of every DOWN you
  consumed. *Why:* a service that consumed DOWN and passed UP left an orphaned UP ("Cancelling
  event due to no window focus").
- **Keep a "known keys" list out of the unknown-key toast:** BACK, CALL, ENDCALL, HOME, POWER,
  volume, CAMERA, FOCUS, headset buttons, the mic code 287 and the outer-button scan codes.
  *Why:* otherwise ordinary system keys pop "unhandled key" messages.

## Other hardware keys
- **CALL = Send** in compose screens, and **Dial** on a contact list. Unhandled CALL/END fall
  through to `super`.
- **Mic key:** `setOf(KeyEvent.KEYCODE_F4, 287)`. Start on DOWN with `repeatCount == 0` and stop
  on UP.
- **The Mic key is the best default for a global hold-to-talk trigger.** Nothing else on the
  phone reacts to it, so it can be consumed on DOWN with no debounce. A second action can use a
  double tap (two taps within ~400ms); remember a short first tap and let the second press
  swallow its own release.
- **Don't use Volume Up as a global trigger.**
  *Why:* the volume popup takes window focus and the focused text field is lost.
- **Volume keys can be reused in a full-screen app with no text field**, e.g. for brightness
  steps.
- **Never swallow volume keys by accident.** Let them through before any catch-all
  `else -> return true` in `dispatchKeyEvent`. While your own audio plays, call
  `setVolumeControlStream(AudioManager.STREAM_MUSIC)` and reset it when playback stops.
  *Why:* a catch-all consume broke the volume keys.
- **The camera key reaches apps.** Match `KEYCODE_F3` (133, E4810), the E4610's 288 (by name,
  `KEYCODE_SOFT_LEFT_LOWER`) and `KEYCODE_CAMERA`. A messaging app used it to take a picture
  while writing; a launcher let the owner assign it to an app.
- **Let users choose a trigger key with a full-screen capture Activity, not a Dialog.**
  *Why:* raw key capture in Dialog windows behaves differently across OEM builds.
- **If two installed apps both hook the same hardware key**, one must ignore it while the other
  is in the foreground.
- **A key-capture screen leaves Back free to cancel** and captures every other key.

## End on Kyocera
Unconfirmed on the E4810; first seen on the E4610. **Outside call screens, leave END alone.**
On a call screen, don't count on `KEYCODE_ENDCALL`. END may instead:
- arrive as HOME, seen only as `onUserLeaveHint`;
- arrive as BACK, told apart from Clear by **scan code 107** (`KEY_END`);
- destroy the window with no key callback at all.

So on a call screen:
- Hang up in `onUserLeaveHint`, unless your app itself opened something (mark those launches by
  overriding `startActivityForResult`).
- As a backstop, hang up in `onDestroy` when your own `finish()` didn't close the screen.
- Treat scan code 107 as END whatever keycode it arrives with. Never hang up on plain BACK.
- Always keep an on-screen End action.

## Calls
- **During a call, send DTMF from the digit keys on DOWN**, never from a text field. *Why:* an
  in-call screen that sent tones only from a text field couldn't take a voicemail PIN.
- **Make the D-pad a ring of in-call controls** (▲ Speaker, ◀ Mute, ▶ Hold/Swap, ▼ Call options,
  OK = End). Claim those keys, DOWN and UP, so focus never moves. Back leaves the screen and
  never hangs up.

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
- **For text-to-speech, render with `TextToSpeech.synthesizeToFile` and play the file from your
  own process (`MediaPlayer`, `USAGE_MEDIA`).** *Why:* on Android 8+ media buttons go to the
  session of the app actually playing audio. Spoken with `tts.speak`, the audio belongs to the
  speech engine's process, so your session is never chosen. An accessibility service that
  filters key events doesn't help: Bluetooth media buttons never pass through it (Flip
  Launcher's headset button didn't stop its readout until it played the audio itself).
- **To let any phone button stop a readout,** stop on the key's DOWN in an accessibility key
  filter (and your own `dispatchKeyEvent`) but don't consume it, so the key still does its
  normal job; let volume keys through without stopping, so the user can still adjust the
  volume. *Why:* users expect the press to work as well; swallowing it made every stop cost an
  extra press.
