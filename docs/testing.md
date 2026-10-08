# Testing and diagnostics

Each entry gives the **rule** first, then *why* it exists.

## Emulator
- **Test on a 240×320 mdpi emulator (the "2.7in QVGA" profile) with touch disabled.** API 24 is
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
- **Check every direction-dependent behaviour on the device** (map pan signs, D-pad layout).
  *Why:* the docs were wrong for MapLibre `scrollBy`.

## Errors for users
- **Show every failure on screen in plain language, with the next step.** For example:
  - "Couldn't load data: network error"
  - "Could not get a GPS fix - try again outdoors or enter location manually"
