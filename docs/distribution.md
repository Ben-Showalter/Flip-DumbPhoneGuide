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
  - Install with `Intent.ACTION_INSTALL_PACKAGE`, a FileProvider `content://` URI and
    `FLAG_GRANT_READ_URI_PERMISSION`. On Android 8+ the installer asks for "Install unknown apps"
    the first time and then carries on.
  - Read `https://api.github.com/repos/<owner>/<repo>/releases/latest` without logging in, so the
    repo is public. Releases must be neither drafts nor pre-releases, tagged `v<versionName>`.
  - Send a `User-Agent` header (GitHub rejects requests without one). A 404 means "no releases
    yet", not an error. Parse tags and version names tolerantly (`".1.0.3"` parsed as 0).
  - Check when the main screen resumes, about once a week; no scheduler. Record the check time
    before the request, retry after a day when offline, and check anyway if the saved time is in
    the future (the clock was turned back).
  - Never show the update prompt over another prompt or the camera. Say Wi-Fi is preferred,
    flag mobile data, and show the size in MB.
  - Run the download on its own single-worker loader, so an unrelated newer load can't
    supersede and drop it.
  - Keep `versionName` equal to the release tag. *Why:* a release reporting a higher
    `versionName` than its tag hid later updates.
  - The update must use the same signing key, so a debug build can't update to a release build.
  - Splitting the APK per ABI (armeabi-v7a, arm64-v8a) can more than halve the download. Put
    the ABI in each file name so an installer can pick the one matching `ro.product.cpu.abilist`.
    GitHub's web upload is limited to 25 MB per file.
- **Moving a phone from a debug build to a release build needs one uninstall.** Tell users what
  that loses, and offer an in-app export/import (or `adb pull`/`push`) first. Set
  `allowBackup="false"` when the app stores secrets.
- **Never commit APKs, zipped APKs, `local.properties` or `.gradle/` output** to the repo.
- **In user-facing text, say "navigation keys", not "D-pad".**
- **The README must:**
  - name the phones the app was tested on;
  - list the data sources;
  - include a key-map table;
  - give exact D-pad steps for each permission toggle, such as "select, hit down key, then hit
    up key, then hit OK";
  - give the adb command for each permission too, with the package name written out (see
    [adb.md](adb.md)). *Why:* some toggles can't be reached with the D-pad on every model,
    and setting up several phones is faster over adb.
- **Never make the user type a key or secret on the keypad.** Use a text file in storage, an
  SMS setup message, or a setup code (QR code or link) made with `templates/setup-code/`
  (see [text-input.md](text-input.md)).
