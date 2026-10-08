# Distribution and setup

Each entry gives the **rule** first, then *why* it exists.

- **These phones have no Play Store. Install APKs by sideloading**:
  - over ADB (`adb install -r app.apk`), WebADB or Android Studio;
  - by emailing the APK or downloading it from cloud storage; or
  - by copying it over USB and enabling "Unknown sources".
- **Attach release APKs to GitHub Releases** instead of committing them to the repo.
- **Keep signing secrets out of git.** Read them from an untracked `keystore.properties`, and
  fall back to an unsigned build when it's missing.
- **In-app self-update from GitHub Releases:**
  - It needs `REQUEST_INSTALL_PACKAGES`. If `canRequestPackageInstalls()` is false, open
    `ACTION_MANAGE_UNKNOWN_APP_SOURCES`.
  - Install with a FileProvider + `ACTION_VIEW`.
  - The update must use the same signing key, so a debug build can't update to a release build.
  - Splitting the APK per ABI (armeabi-v7a, arm64-v8a) can more than halve the download.
- **The README must:**
  - name the phones the app was tested on;
  - list the data sources;
  - include a key-map table;
  - give exact D-pad steps for each permission toggle, such as "select, hit down key, then hit
    up key, then hit OK".
- **Never make the user type a key or secret on the keypad.** Use a text file in storage, or an
  SMS setup message (see [text-input.md](text-input.md)).
