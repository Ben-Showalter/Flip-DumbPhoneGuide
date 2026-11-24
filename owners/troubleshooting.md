# Troubleshooting

Find what you see in the left column; try the fix on the right.

## Using the phone

| What happens | Try this |
|---|---|
| A key does nothing | Press **Down** once so something is highlighted. If an app shows a message like "KEY 287", tell the app's maker which key you pressed |
| The word "Options" is on the wrong side | Press the key you actually use for Options once; the app moves the label. On a Menu-key phone, answer **Yes** to "Show Options on the left?" |
| A white bar with key names is back at the bottom | It's the phone's own key bar. It shows while you type (the keyboard uses it). Press **Back** or **Down** to leave the text box and it hides again |
| An app stops working after a few hours | Set its battery setting to **Don't optimize** ([how](permissions-and-settings.md#battery-run-in-the-background)) |
| An app reacting to keys stopped after an update | Switch its accessibility service off and on once ([how](permissions-and-settings.md#accessibility-for-apps-that-react-to-keys-or-read-the-screen)) |
| **E4811:** an app's accessibility switch won't stay on | It can't be turned on from Settings on this phone. Use a computer ([how](computer-setup.md#turn-on-an-apps-accessibility-service)) |
| Notifications don't show on a home-screen app (DuraXV) | Notification access doesn't work on these phones; turn on the app's **accessibility** option instead |
| Picture or group texts never arrive | The texting app is missing the "receive picture messages" (MMS) permission. Allow it in **Settings** › **Apps** › the app › **Permissions** |
| Pictures you send never arrive at someone on another carrier | The picture is too large; keep it under about 600 KB (good apps shrink it for you) |
| "Could not get a GPS fix" | Go outdoors, check location is on, or type your town in the app's settings |
| An app says it can't find its key file, but the file is there (Android 10 / E4811) | The app needs a fix from its maker, or the storage command in the [ADB reference](../docs/adb.md#storage-on-android-10-e4811) |
| "Permission denied (os error 13)" after reinstalling an app | Clear the app's storage: **Settings** › **Apps** › the app › **Storage** › **Clear storage**, or uninstall and install again |
| The first press of a voice key does nothing | That press answered the microphone question. Press it again |
| Push-to-talk on the right soft key won't hold | Tap to start, tap again to stop |

## Install problems

| Message | What it means | Fix |
|---|---|---|
| "App not installed" / "a different copy is installed" / `UPDATE_INCOMPATIBLE` | Your copy was built differently | Export the app's data if you can, uninstall it, then install again |
| "Newer version already installed" / `VERSION_DOWNGRADE` | The phone has a newer version | Nothing to do, or uninstall first to go back |
| "Not enough space" / `INSUFFICIENT_STORAGE` | The phone is full | Delete photos, files or apps you don't use |
| "Needs a newer Android" / `OLDER_SDK` | The app doesn't support this phone | Ask the maker; it can't be installed |
| "Not built for this processor" / `NO_MATCHING_ABIS` | Wrong version of the file | Download the version for your phone |
| "Blocked by the phone" / `USER_RESTRICTED` | Installing over USB is switched off | Turn on **Install via USB** in Developer options |
| "There are accounts on the phone" (management apps) | Device-management apps can't be installed while an account is signed in | Remove accounts in **Settings** › **Accounts**, then try again |

## Connecting to a computer

| What happens | Try this |
|---|---|
| The web page says the browser can't connect to phones | Use Chrome or Edge on a computer |
| "Another program is using the phone" | Close Android Studio, phone managers and other browser tabs, unplug and plug in again. Try another cable or port |
| The phone isn't listed (Windows) | Install the Google USB Driver (or the phone maker's driver) |
| The phone never asks "Allow USB debugging?" | Open the flip and unlock the phone. In Developer options choose **Revoke USB debugging authorizations**, then plug in again |

## Emergency calls
**Never call 911 to test a phone or an app.** To check the address your carrier has on file,
dial **933**. Even with no SIM card, phones can usually still reach emergency services.
