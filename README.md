# Flip-DumbPhoneGuide

House rules and hard-won quirks for building Android apps on keypad flip phones such as the
Kyocera DuraXV Extreme E4810 and Sonim XP-series phones. These phones have no touchscreen and
no Google Play services, and their screens are 240×320.

## For phone owners
Not a programmer? Start with the **[owner's guide](owners/README.md)**: installing apps, turning
on the settings they need, the keys, and fixing common problems, all with no touchscreen.

## For AI agents
Start with **[AGENTS.md](AGENTS.md)**. Its short house rules link to the details in `docs/`.
Copy starter code from `templates/`.

To use the guide in another app's session, give the agent something like:

> Before working, read https://github.com/Ben-Showalter/Flip-DumbPhoneGuide/blob/main/AGENTS.md
> and follow it.

Or clone this repo next to the app's repo.

## Layout

| Path | What's in it |
|---|---|
| `AGENTS.md` | Rules, the new-app checklist, and decided conventions |
| `CLAUDE.md` | Imports AGENTS.md, so Claude Code loads it automatically |
| `docs/keys.md` | Keycode table, soft keys, OK/Back/Clear, number keys, repeats |
| `docs/focus-and-lists.md` | Focus highlight, lists, scrolling, menus |
| `docs/text-input.md` | EditText vs NoImeEditText, T9 conventions, secrets |
| `docs/layout-and-type.md` | Screen size, hiding the system soft-key bar, icons, font sizes, rendering quirks, flip config changes |
| `docs/platform.md` | SDK levels, manifest, no-GMS replacements, notifications, the outer display, SMS/MMS, calls, camera/QR, location, OEM/accessibility quirks |
| `docs/network-battery.md` | Auto-refresh, caching, threads, Wi-Fi binding, maps, audio |
| `docs/distribution.md` | Sideloading, releases, signing, README expectations |
| `docs/testing.md` | QVGA emulator, unknown-key toast, on-device testing |
| `docs/adb.md` | ADB commands for permissions, special access, Android 10 storage, accessibility, key files, and what their errors mean |
| `docs/devices.md` | Known phones and their quirks |
| `owners/` | Plain-language guide for phone owners: keys, installing apps, permissions, using a computer, troubleshooting, per-phone notes |
| `templates/` | `FlipBaseActivity.kt`, `SystemBars.kt` (hides the system soft-key bar), `list_row_selector.xml`, `NoImeEditText.kt`, manifest snippets, `setup-code/` (offline QR-code/link generator for app settings) |

## Contributing
When you find a new quirk, add a one-line rule to `AGENTS.md`. Add the full entry, in the
format *Rule → Why*, to the matching `docs/` file.

## License
MIT. See [LICENSE](LICENSE).
