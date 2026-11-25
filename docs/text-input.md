# Text input

Each entry gives the **rule** first, then *why* it exists.

## Choosing an approach

| Field type | Approach |
|---|---|
| Numbers, phone, port, IP | Plain `EditText`, `inputType="number"`/`"phone"`, `windowSoftInputMode="stateHidden"` |
| Short free text, where the system T9 is acceptable | Plain `EditText` with the system IME |
| Main free-text entry, where you control T9 | `NoImeEditText` plus your own T9 controller |

Never mix two approaches in one field.

## Plain EditText
- **Numeric fields work fine.**
- **IP fields: `inputType="number"`, `digits="0123456789."`, and a key listener that makes `*`
  insert `.`. Consume both its DOWN and its UP. Say so in the label: `SERVER IP (* = dot)`.**
  *Why:* the keypad has no dot key in numeric mode.
- **Use `imeOptions="actionNext"`** so OK moves to the next field.
- **Put the cursor at the end of prefilled text:** `setSelection(text.length)`.
- **Back on a settings form saves automatically.** *Why:* there's no confirm dialog to navigate.
- **Use `windowSoftInputMode="stateAlwaysHidden"`** on screens with keypad entry.
- **Before API 29, recolor the cursor by reflection on `mCursorDrawableRes`.**
  *Why:* `setTextCursorDrawable` only exists from API 29.
- **While a text field is focused, the system T9 keyboard can swallow `KEYCODE_SOFT_RIGHT`**
  (see [keys.md](keys.md)).

- **On Kyocera the system T9 also keeps D-pad UP/DOWN while a field is focused,** so a form's
  Save row can't be reached. In `dispatchKeyEvent`, before the IME sees the key, move focus out
  with `focusSearch` on DOWN at the field's last line or UP at its first line, and consume the
  matching UP. Also consume MENU while typing so nothing opens over the T9.
- **Read the clipboard only after your window has regained focus** (Android 10+). *Why:* a Paste
  chosen from an Options dialog read an empty clipboard while the dialog was still closing.
- **Strip invisible characters before matching text** (U+2068/2069 direction marks, zero-width
  spaces), but keep the emoji joiner. *Why:* some messaging apps wrap quoted text in them.
- **Enter dates and times with a keypad picker, not Android's pickers.** Digits fill fields that
  advance by themselves, ◀▶ move between fields, ▲▼ change one, Clear deletes or cancels. For
  AM/PM, `2` = AM and `7` = PM (the letters on those keys).

## NoImeEditText (your own T9)
Template: [`templates/NoImeEditText.kt`](../templates/NoImeEditText.kt).
- **`onCheckIsTextEditor() = false` and `onCreateInputConnection() = null`.**
  *Why:* `setShowSoftInputOnFocus(false)` and `stateAlwaysHidden` still start an input-method
  session. On the E4811 that session caused a `getShwoingNowFlag` NoSuchElementException loop
  inside InputMethodManager; the window lost focus and soft-key presses were dropped.
- **Draw the cursor yourself in the view's `ViewOverlay`, and keep its position in your own
  field.** *Why:* with no input-method session, the native cursor gets out of sync in a
  different way on Android 7, 9 and 10, and `selectionStart` can't be trusted.
- **Override `bringPointIntoView()` to return false, and scroll to the cursor in `onDraw` with a
  2dp margin.** *Why:* two scroll mechanisms were fighting, and a cursor sitting exactly on the
  bottom edge got clipped.
- **Don't subtract `scrollX`/`scrollY` when drawing in the overlay.** *Why:* the canvas has
  already been translated. Subtracting again caused the "cursor stuck at line 5" bug.
- **Use a `ready` flag in the constructor.** *Why:* TextView's constructor calls overridden
  setters before the subclass's fields exist, which crashed on the device.

## T9 conventions

| Key | Action |
|---|---|
| `2`–`9` | Build a T9 word |
| Hold a digit 700ms | Type the digit itself (use your own timer and ignore OS repeats) |
| `1` | Punctuation picker, browsed with LEFT/RIGHT. Preselects `?` after question words, `.` otherwise |
| `#` | Space |
| `*` (on UP) | Cycle case: t9word → T9Word → T9WORD → emoji |
| Left soft key | Typing-mode menu (T9 / abc / 123). Its label shows the current mode |
| D-pad while composing a word | Move the suggestion highlight |
| D-pad otherwise | Move the cursor. UP from the top line leaves the field |
| Clear (`KEYCODE_BACK`) | Backspace. Deletes emoji surrogate pairs whole. Exits when the field is empty |

- **Suggestion bar:** page it (12 per page) and keep it left-aligned so it doesn't reflow,
  3 lines at most. Highlight with a `ReplacementSpan` whose `getSize` returns the bare glyph
  width.
- **Always offer the raw digits as a fallback suggestion, but don't highlight it by default.**
- **The dictionary (about 100k words) is a trie, parsed in `Application.onCreate` on a
  background thread and cached.** *Why:* parsing it on first use stalled the main thread for
  about 3s.
- **Recipient fields can reuse T9 to match contact names.**

- **Send commits the highlighted T9 suggestion (or open punctuation pick) first.** *Why:*
  otherwise the word being typed was dropped.
- **Rank T9 guesses by how often each word was picked for those keys, then by frequency,** not
  "last picked first". Halve counts past ~200 so habits can change. *Why:* with last-picked,
  "me" and "of" (both 6-3) kept swapping.
- **Hold `#` for a new line** using the same hold timer as digits; ignore OS repeats so holding
  `#` doesn't type a row of spaces. Turn it off in one-line fields.
- **Filter emoji with `Paint.hasGlyph`** rather than only a hand-made list, and show newer emoji
  the font can't draw as an older close match.
- **Don't invalidate a custom cursor overlay on every `onDraw`.** Redraw only when the scroll
  position changed. *Why:* it redrew 60 times a second, draining the battery.

## Kyocera fields, for keyboards

Rules for an input method (keyboard app), learned building TurboType on the E4810/E4811.

- **Kyocera "Done" fields (`jp.kyocera.*`, `IME_ACTION_DONE`) close only when the keyboard
  sends the private command `Finish_IME`,** then the Done action, the way the stock iWnn keyboard
  does (TT9 issue #423).
- **Only single-line Done fields need the full-screen (extract) editor.** Mail's "Enter address"
  doesn't draw its own text, so it needs it. *Why:* Kyocera **Notepad** (`jp.kyocera.memo`) is
  a multi-line Done field (`inputType=0x24001`: text, multi-line, sentence caps;
  `imeOptions=0x40000006`: Done plus `IME_FLAG_NO_ENTER_ACTION`) that draws its own text, and
  the full-screen editor covered the note, so typing seemed to do nothing (2026-11-25,
  E4810 and E4811).
- **Kyocera fields name their soft-key labels in `privateImeOptions`,** for example Notepad's
  `KC_IME_CSK_LABEL:Save,KC_IME_RSK_LABEL:Cancel`: OK is "Save" and the right soft key "Cancel"
  while the stock keyboard is in use.
- **Notepad's note list (`MemoActivity`) reports `inputType=0x0`.** A keyboard should leave
  such a screen alone (pass every key through).

## Secrets and setup text
- **Never make the user type an API key on the keypad.**
  - Read it from a text file such as `Internal storage/<AppName>/api_key.txt`. Create the
    folder on resume, and tell the user it may only appear after a reboot.
  - Strip a leading UTF-8 BOM (`\uFEFF`) as well as whitespace. *Why:* Windows Notepad can
    save one, and Kotlin's `trim()` keeps it, so the key is silently wrong.
  - On Android 10 the app also needs `requestLegacyExternalStorage` to read that folder (see
    [platform.md](platform.md#manifest)).
  - Or accept an SMS from a trusted number, `<APP>_SETUP:<base64 key>`, through a receiver
    protected by `BROADCAST_SMS`.

- **Clean a key read from a file:** strip a BOM and zero-width characters, surrounding quotes
  and a pasted `NAME=` prefix, and take the first non-empty line. On a 401, show where the key
  came from and its last 4 characters. *Why:* stray characters and stale fallback keys caused
  401s that were hard to diagnose on the phone.
- **Files under `/sdcard/Android/data/<pkg>/files/` need no permission** and are the easiest drop
  point for setup files on Android 10+. Open the app once first so the folder exists.

## Setup codes (QR code or link)

First designed for a SIP calling app with several accounts per phone. The page itself is
tested in a browser; **the phone side (camera scanning, saving to Download, moving the file
off the phone) isn't confirmed on hardware yet.** Update this section when it is.

- **Make the code on a computer with `templates/setup-code/`**: one offline HTML file, edited
  per app through a `CONFIG` block. It shows a QR code and a link, and can copy, save or print.
  *Why:* a server name plus a password is dozens of T9 presses with no room for typos, and the
  person setting phones up usually has a computer.
- **Format: `<scheme>:add?v=1&key=value&…`, every value URL-encoded, empty fields left out.**
  Parse it in **one** place, and keep accepting `v=1` after any change. *Why:* the same text
  works as a link, as a QR code and pasted from the clipboard, and printed codes stay in use for
  years.
- **The page works offline and sends nothing anywhere.** The QR library is inlined; the page
  keeps nothing after it closes and warns that the code contains a secret. *Why:* it handles
  passwords, and admins can trust a file they can open with the network unplugged.
- **In the app, offer three ways in: Type it in, Paste setup link, Scan QR setup code.**
  *Why:* not every phone has a usable camera, and some users get the link by text or email.
- **Show the decoded settings with the password hidden, and save only on "Save".** *Why:* a
  wrong or old code shouldn't silently replace working settings.
- **Ask "Do you have a QR setup code ready?" before opening the camera.** "No, help me set it
  up" explains how to make one. *Why:* otherwise the user lands in a camera view with nothing
  to point it at.
- **Ship the page inside the app** (`assets/`) with a **Save setup page to phone** button that
  writes it to the public Download folder, and tell the user to move it to a computer **over
  USB, Bluetooth or SD card**, open it and fill it in. Offer "Save to SD card" and "Send by
  Bluetooth" (`ACTION_SEND` with a `FileProvider` URI) in Options. *Why:* the user may have no
  admin, no website and no other way to get the file. Saving: `MediaStore.Downloads` on API 29+
  (no permission); below that `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion="28"`. Replace an
  existing file of the same name so the on-screen path stays true.
- **Decode QR codes with ZXing core (Apache-2.0)** in your own camera screen. Never Google ML
  Kit (needs Play services) or a third-party scanner app. Reach the scanner from a menu item:
  don't rely on the camera key alone (see [keys.md](keys.md)).
- **Clear the clipboard after reading a pasted code, and never log the code or any field from
  it.**
- **Flip-phone message apps often only make `http(s)` links tappable,** so a `<scheme>:` link
  usually has to be copied and pasted. If tapping matters, host the same page and also produce
  `https://<host>/<app>#add?…`: everything after `#` never reaches the web server.
- **Confirmation screens for setup codes show one dot per real password character,** offer Show
  password, and warn when the password starts or ends with a space.
