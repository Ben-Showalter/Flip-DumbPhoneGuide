# Devices

These specs haven't been checked on the hardware. Confirm a spec before you rely on it, and add
new phones here.

| Device | Android | Screen | SoC / RAM | Notes |
|---|---|---|---|---|
| Kyocera DuraXV Extreme **E4810** | AOSP-based | 2.6″ 240×320 | Snapdragon 215 / 2 GB | Clear key sends `KEYCODE_BACK`. Mic key is `F4`/287. Has an outer display (sub-LCD). Outer buttons are scan codes 763-766. Notification Access never binds (see [platform.md](platform.md)) |
| Kyocera **E4811** | 10 | 240×320 | Helio A22 | IME exception loop (`getShwoingNowFlag`), so use NoImeEditText. Storage permission bug on API 29+. Notification Access never binds |
| Kyocera **E4610** (DuraXE Epic) | | 2.4″ 320×240 landscape | | Android 7. Slow SMS provider (see [network-battery.md](network-battery.md)). Mic key sends 287 (scan 171) even when an app has focus. Outer buttons are scan codes 172/213/231. `content://speed_dial` stays empty |
| Sonim XP-series (XP5700 tested) | 5.1 (API 22) | small | | Needs `minSdk 22`. The `*` key may arrive as `KEYCODE_NUMPAD_MULTIPLY` |

The Kyocera models in use run Android 7, 9 and 10, all confirmed on real devices.

## Kyocera package names you may need
- Home screen: `jp.kyocera.kyocerahome.HomeScreenActivity`. The app grid is `MainMenuGridActivity`.
- Outer display: `jp.kyocera.sublcd.SubLcdManager`, `SubLcdNotificationExtender` (reflection only).
- The stock "MESSAGE" home shortcut is hard-wired to `com.android.mms`.
- Kyocera Home's menus: `jp.kyocera.kyocerahome/.MediaCenterMenuActivity`, `.ToolsMenuActivity`
  and `.QuickSettingsActivity`.
- Call log: `com.android.dialer/.calllog.CallLogActivityKc` (E4610) or
  `.app.calllog.CallLogActivityKc` (newer models). Try both.
- Speed Dial screen: `com.android.dialer/.speeddial.SpeedDialActivity` (E4610) or
  `.app.speeddial.SpeedDialActivity`. On the E4610 its entries aren't in `content://speed_dial`,
  so apps can't read them.
- Outer-button settings: `com.android.settings/.afp.PttSettings`
  (`kyocera.intent.action.PTT_SETTINGS`); only applies under Kyocera Home.

## Adding a device
1. Install a key-logger, or any app with an unhandled-key toast, and press every key. Fill in
   the keycode table in [keys.md](keys.md).
2. Record the Android version, screen size and orientation, and any OEM package names.
3. Note any crash that only happens on this device, along with its logcat signature.
