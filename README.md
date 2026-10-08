# Flip-DumbPhoneGuide

House rules and hard-won quirks for building Android apps on keypad flip phones such as the
Kyocera DuraXV Extreme E4810 and Sonim XP-series phones. These phones have no touchscreen and
no Google Play services, and their screens are 240×320.

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
| `docs/layout-and-type.md` | Screen size, font sizes, rendering quirks, flip config changes |
| `docs/platform.md` | SDK levels, manifest, no-GMS replacements, OEM/accessibility quirks |
| `docs/network-battery.md` | Refresh throttling, caching, threads, Wi-Fi binding, audio |
| `docs/distribution.md` | Sideloading, releases, signing, README expectations |
| `docs/testing.md` | QVGA emulator, unknown-key toast, on-device testing |
| `docs/devices.md` | Known phones and their quirks |
| `templates/` | `FlipBaseActivity.kt`, `list_row_selector.xml`, `NoImeEditText.kt`, manifest snippets |

## Contributing
When you find a new quirk, add a one-line rule to `AGENTS.md`. Add the full entry, in the
format *Rule → Why*, to the matching `docs/` file.
