# Testing and diagnostics

Each entry gives the **rule** first, then *why* it exists.

## Emulator
- **Test on a 240×320 low-density emulator (the "2.7in QVGA" profile; the E4810 is about
  0.75 density) with touch disabled.** API 24 is
  a good baseline. If you tested only on a phone-sized emulator, you did not test.
- **Run screenshot tests at 240×320 at both the normal and the largest font size.**
- **Drive the emulator from the keyboard only.** If a screen needs a tap, it's a bug.

## On the real phone
- **Build a throwaway key-logger first when you get a new phone model.** *Why:* keycodes can't be
  learned on an emulator.
- **Show any unhandled keycode on screen in every app**, for example
  `flash("KEY $code")` or `Toast "Unhandled key: ${KeyEvent.keyCodeToString(code)}"`.
  Keep it in release builds.
- **Log every key with a fixed tag** (for example `<App>Keys`) so you can filter it with
  `adb logcat -s`. Never log personal data or message content.
- **Don't send `adb shell input keyevent` to a phone someone is actively using.**
  *Why:* key-remapping apps such as "Button Mapper" make synthetic keys drop or fire twice. Use
  the emulator for clean signals.
- **For focus checks, don't trust `uiautomator dump`.** *Why:* it can catch focus halfway
  through an update. Prefer `screencap` after 0.5–0.8s, or `dumpsys window | grep mCurrentFocus`.
- **On Windows, run `adb` from Android Studio's Terminal** (PowerShell) after
  `$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"`. In the Logcat tab, filter
  with `tag:<App>Keys`. *Why:* `adb` isn't on the path there by default.
- **Test an OEM API with a debug-only probe receiver**, triggered by
  `adb shell am broadcast -n <package>/.Probe --ez <test> true`.
  - Put the step number in what the screen shows (for example `3 title test`) and log one line
    per step, so the user can report "step 3 was blank".
  - Keep it in the debug source set only.
  - *Why:* it turned build-and-guess rounds on the outer display into one run per question.
- **Check every direction-dependent behaviour on the device** (map pan signs, D-pad layout).
  *Why:* the docs were wrong for MapLibre `scrollBy`.

- **Add an in-app key log** (Settings › Key log) of the last ~8 presses: name, keycode, scan code
  and whether the screen or an accessibility service saw it. Never record which digit was
  pressed (it may be a PIN). *Why:* owners can report what a key sends without adb.
- **Add "Save bug report"** that copies the app's own logcat (`logcat --pid … -T <last time>`)
  into a file in Download, with key events and no message text. Restart the copy with backoff:
  the stream keeps breaking on this phone ("read: unexpected EOF!").
- **Android Studio's Logcat shows only the debugged app's process,** so OEM processes are hidden;
  use the adb CLI unfiltered. In PowerShell `&` backgrounding is a syntax error: run
  `adb logcat -c`, trigger the problem, then `adb logcat -d`.
- **Post outer-display test notifications with the flip closed.** Some renderers only react then.
- **Test the release build on the phone.** Debug builds that skip TLS verification hide
  certificate problems.
- **Never test emergency calling by dialling 911.** Arrange a test call through the police
  non-emergency line, or dial 933 (it reads back the address on file and isn't an emergency
  number to Android). With no SIM, phones often still reach a tower for emergency calls only.

## Errors for users
- **Show every failure on screen in plain language, with the next step.** For example:
  - "Couldn't load data: network error"
  - "Could not get a GPS fix - try again outdoors or enter location manually"
