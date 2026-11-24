# ADB commands

Commands for setting up a sideloaded app on a flip phone from a computer. Use them when the
phone's own Settings can't reach a toggle with the D-pad, or when you set up several phones.
Replace `<pkg>` with the app's package name, e.g. `com.turbovoice.app`, and `<Service>` with
the accessibility service's class name.

Each entry gives the **rule** first, then *why* it exists.

## Before you start
- **Run every `adb …` line from the computer's terminal, not inside `adb shell`.** *Why:* inside
  `adb shell` you're on the phone, which has no `adb`, so every line fails with
  `adb: inaccessible or not found`. Inside `adb shell`, drop the `adb shell` prefix instead.
- **Write out the package name; don't use a variable like `$PKG`.** *Why:* a variable set in
  another terminal, or any `$VAR` on Windows, is empty. `appops set --uid $PKG X allow` then
  reaches the phone as `appops set --uid X allow` and fails with `Unknown operation string: allow`.
- **Paste one command per line, or join them with `;` on one line.** *Why:* a multi-line paste
  can carry Windows carriage returns that stick to the last word. Our best explanation for
  `Unknown permission: android.permission.RECORD_AUDIO`, a permission every phone has.
- **Quote paths with spaces twice:** `adb shell "ls -la '/sdcard/Turbo Key/'"`. *Why:* `adb shell`
  re-joins its arguments and the phone's shell splits them again, so one level of quotes is
  lost and `/sdcard/Turbo` and `Key/` become two paths.
- **Windows vs Mac/Linux quoting:** wrap a command that contains `$(…)` in **double** quotes on
  Windows and **single** quotes on Mac/Linux. *Why:* a Mac/Linux shell expands `$` inside double
  quotes on the computer before the phone sees it; Windows passes it through.
- **`/sdcard` is internal storage**, the same folder as `/storage/emulated/0` and
  "Internal storage" in a file manager. A removable card is `/storage/XXXX-XXXX/`.
- **ADB AppControl (Windows GUI) shows runtime permissions only.** Special access
  (All files access, Draw over other apps, Install unknown apps, legacy storage) isn't in its
  permission list. Type those `appops` commands in its console tab.

## Find out what you're dealing with
```
adb shell getprop ro.build.version.release          # Android version: decides which storage route applies
adb shell pm list packages | grep <part-of-name>    # is the app installed, and under what name
adb shell dumpsys package <pkg> | grep granted      # runtime permissions and their state
adb shell appops get <pkg>                          # special-access modes
```

## Install and update
```
adb install -r app.apk        # install or update, keeping data
adb install -r -g app.apk     # same, and grant every runtime permission in the manifest
```
An update must be signed with the same key as the installed app; otherwise uninstall first
(this clears the app's settings). See [distribution.md](distribution.md).

## Runtime permissions (`pm grant`)
```
adb shell pm grant <pkg> android.permission.RECORD_AUDIO
adb shell pm grant <pkg> android.permission.RECEIVE_SMS
adb shell pm grant <pkg> android.permission.READ_CONTACTS
adb shell pm grant <pkg> android.permission.ACCESS_FINE_LOCATION
adb shell pm grant <pkg> android.permission.READ_EXTERNAL_STORAGE    # Android 10 and older
adb shell pm grant <pkg> android.permission.WRITE_EXTERNAL_STORAGE   # Android 10 and older
```
Only permissions the app declares can be granted.

## Special access (`appops`)
```
adb shell appops set <pkg> SYSTEM_ALERT_WINDOW allow                 # Draw over other apps
adb shell appops set <pkg> REQUEST_INSTALL_PACKAGES allow            # Install unknown apps (self-update)
adb shell appops set --uid <pkg> MANAGE_EXTERNAL_STORAGE allow       # All files access, Android 11+ only
adb shell appops set <pkg> ACCESS_RESTRICTED_SETTINGS allow          # Android 13+ only, lets a sideloaded app's accessibility switch turn on
```
- **`Unknown operation string: <OP>` means the phone's Android doesn't have that op.** It's
  harmless; that op doesn't apply there. *Why:* `MANAGE_EXTERNAL_STORAGE` returned exactly this
  on the E4811, which runs Android 10.

## Storage on Android 10 (E4811)
On Android 10, an app that targets API 29+ and lacks `requestLegacyExternalStorage="true"` is
in scoped storage: READ/WRITE_EXTERNAL_STORAGE show as granted, yet reading a root-level folder
such as `/sdcard/Turbo Key/` fails silently. The real fix is the manifest flag (see
[platform.md](platform.md#manifest)). Without changing the app, this works (confirmed on the
E4811 with TurboVoice):
```
adb shell pm grant <pkg> android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant <pkg> android.permission.WRITE_EXTERNAL_STORAGE
adb shell appops set --uid <pkg> LEGACY_STORAGE allow
adb shell appops get <pkg> LEGACY_STORAGE
adb shell am force-stop <pkg>
```
- **Set `LEGACY_STORAGE` with `--uid`.** *Why:* `appops get` prints two lines,
  `Uid mode: LEGACY_STORAGE: ignore` and `LEGACY_STORAGE: allow`. The uid mode wins, so setting
  only the package mode changes nothing. Both lines must say `allow`.
- **Force-stop the app afterwards.** *Why:* the storage mode is fixed when the process starts,
  and an app with an accessibility service is already running.
- **Grant the storage permissions before setting `LEGACY_STORAGE`, and expect to re-run it after
  a reboot.** *Why:* Android 10 recomputes the uid mode from the manifest, probably on reboot and
  on permission changes.
- **Re-enable the accessibility service after a force-stop** (next section).

## Accessibility service
Add the service to the enabled list without turning off anyone else's. One line, Windows:
```
adb shell "S=<pkg>/<pkg>.<Service>; C=$(settings get secure enabled_accessibility_services); case $C in *$S*) N=$C;; null|'') N=$S;; *) N=$C:$S;; esac; settings put secure enabled_accessibility_services $N; settings put secure accessibility_enabled 1"
```
Mac/Linux: the same command wrapped in single quotes instead of double quotes.

Check it:
```
adb shell settings get secure enabled_accessibility_services
adb shell "dumpsys accessibility | grep -i <pkg>"
```
- **Never `settings put secure enabled_accessibility_services <one service>` on a shared phone.**
  *Why:* it replaces the whole colon-separated list and turns off every other app's service
  (a launcher's notification service, for example).
- **`am force-stop` turns the app's accessibility service off.** *Why:* seen on the E4811; the
  app's checklist went from ✓ to ✗ for accessibility right after a force-stop.
- If the setting is saved but the service doesn't start, toggle it off and on in
  Settings → Accessibility.

## Battery
```
adb shell dumpsys deviceidle whitelist +<pkg>
```
Keeps Android from idling the app's background service.

## Files (API key files)
```
adb shell "mkdir -p '/sdcard/Turbo Key'"
adb push groq_api_key.txt "/sdcard/Turbo Key/groq_api_key.txt"
adb shell "ls -la '/sdcard/Turbo Key/'"
adb shell "head -c 4 '/sdcard/Turbo Key/groq_api_key.txt' | od -c"
```
- **Check the name, size and first bytes, not the whole key.** *Why:* the file can be
  `groq_api_key.txt.txt` (Windows hides extensions), empty (the app created it), or start with
  a UTF-8 BOM (`357 273 277` in `od`) from Notepad, which `trim()` doesn't remove. Printing the
  whole file puts the secret in a chat or log.

## Logs
```
adb logcat -s <Tag1> <Tag2>
```
Start it, then reproduce the problem on the phone.

## Errors and what they mean

| Error | Cause | Fix |
|---|---|---|
| `adb: inaccessible or not found` | You ran `adb shell …` inside `adb shell` | Drop the `adb shell` prefix, or `exit` first |
| `Unknown operation string: allow` | The package-name variable was empty | Write the package name out |
| `Unknown operation string: <OP>` | This Android version doesn't have that op | Skip it; check `getprop ro.build.version.release` |
| `Unknown permission: android.permission.<NAME>` for a standard permission | Most likely a stray carriage return from a multi-line paste | Paste one line at a time |
| `ls: /sdcard/Turbo: No such file or directory` | Path with a space lost its quotes | Quote twice: `"ls '/sdcard/Turbo Key/'"` |
| `Uid mode: LEGACY_STORAGE: ignore` | The uid mode overrides the package mode | `appops set --uid <pkg> LEGACY_STORAGE allow`, then force-stop |

## Worked example: TurboVoice on an E4811 (Android 10)
```
adb shell pm grant com.turbovoice.app android.permission.RECORD_AUDIO
adb shell pm grant com.turbovoice.app android.permission.RECEIVE_SMS
adb shell pm grant com.turbovoice.app android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.turbovoice.app android.permission.WRITE_EXTERNAL_STORAGE
adb shell appops set com.turbovoice.app SYSTEM_ALERT_WINDOW allow
adb shell appops set com.turbovoice.app REQUEST_INSTALL_PACKAGES allow
adb shell appops set --uid com.turbovoice.app LEGACY_STORAGE allow
adb shell dumpsys deviceidle whitelist +com.turbovoice.app
adb shell am force-stop com.turbovoice.app
adb shell "S=com.turbovoice.app/com.turbovoice.app.TurboVoiceAccessibilityService; C=$(settings get secure enabled_accessibility_services); case $C in *$S*) N=$C;; null|'') N=$S;; *) N=$C:$S;; esac; settings put secure enabled_accessibility_services $N; settings put secure accessibility_enabled 1"
```
Then open the app and check its permission checklist.
