# Using a computer (optional)

A computer makes installing apps faster, and it can switch on a few settings the phone's own
menus can't, such as an app's accessibility service on the Kyocera E4811. This uses a feature
called **USB debugging** (ADB).

## 1. Turn on Developer options and USB debugging
1. On the phone, go to **Settings** › **About phone** *(may be under **System**)*.
2. Highlight **Build number** and press **OK** seven times. The phone says "You are now a
   developer".
3. Press **Back** and open **Developer options** *(sometimes under **System**)*.
4. Highlight **USB debugging** and press **OK** to turn it on.
5. If there's an **Install via USB** option, turn that on too.

You can turn USB debugging off again when you're done.

## 2. Connect the phone
1. Open the flip and unlock the phone. The question in the next step only appears on the
   **inside** screen while the phone is unlocked.
2. Plug the phone into the computer with a **data** cable.
3. The phone asks **"Allow USB debugging?"**. Tick **Always allow from this computer** and
   choose **Allow**.

## 3a. Install from a web page (easiest)
Some apps offer an install page that runs in the browser.
- It works in **Chrome** or **Edge** on Windows, Mac, Linux or a Chromebook. Firefox and Safari
  can't talk to phones.
- Close other programs that use the phone (Android Studio, phone managers, other tabs): only
  one program can connect at a time.
- Click **Connect phone**, choose the phone in the browser's pop-up, then choose **Allow** on
  the phone. Then pick the app(s) to install.

## 3b. Install with the ADB command
If you have Android's platform-tools installed, in a terminal:
```
adb install -r app.apk
```
`-r` updates an app that's already installed and keeps its data.

## Turn on an app's accessibility service
Needed on the **Kyocera E4811**, where the switch in Settings doesn't work. The app's
instructions should give this line with its own names filled in; paste it as **one line**:
```
adb shell "S=<app package>/<service name>; C=$(settings get secure enabled_accessibility_services); case $C in *$S*) N=$C;; null|'') N=$S;; *) N=$C:$S;; esac; settings put secure enabled_accessibility_services $N; settings put secure accessibility_enabled 1"
```
On a Mac or Linux, use single quotes `'…'` around the part after `adb shell` instead of double
quotes. This adds the service **without switching off other apps' services**.

Other one-line commands (permissions, default apps, battery, storage on Android 10) are listed
in the developers' [ADB reference](../docs/adb.md). Paste **one command per line**.

## Putting a key or setup file on the phone
Some apps read a password or API key from a small text file so you never type it on the
keypad.
- Make the file in a plain text editor. Name it exactly as the app says; Windows hides
  ".txt", so check it isn't called `name.txt.txt`.
- Put **only** the key in the file, on one line.
- Copy it to the folder the app names (see [Installing apps, Way 1](installing-apps.md)).
  Open the app once first so its folder exists.

## Scanning a setup code (QR code)
Some apps let you set them up by scanning a QR code shown on a computer screen.
1. Choose the app's **Scan** option.
2. Hold the phone about a hand's width from the screen until it buzzes.
3. In the dark, press **OK** to turn on the light.
4. The app shows what it read; check it, then confirm.
5. Close the page on the computer afterwards: it contains your password.
