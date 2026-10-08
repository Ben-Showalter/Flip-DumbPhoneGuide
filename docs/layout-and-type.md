# Screen, layout and type

Each entry gives the **rule** first, then *why* it exists.

## Size
- **Design for 240×320 px at mdpi (2.4–2.8″).** Some models are 320×240 landscape (see
  [devices.md](devices.md)).
- **Typical screen chrome:** a 24dp title bar, about 270dp of content and a 26dp soft-key bar.
  Hide the system's own soft-key bar (see [below](#system-soft-key-bar)) so only yours shows.
- **Size grid cells and rail slots from the available pixels, not fixed dp**, and keep
  per-cell padding small (20dp, not 36dp).
- **Use `minHeight` for rows, never a fixed height.** Give status and flash lines a fixed height,
  for example 20dp, so the layout doesn't jump, and clear them after about 2.5s.
- **Hide anything that isn't needed while browsing.** For example, collapse a compose box to GONE
  while the user scrolls through messages. *Why:* it was hiding short messages.
- **Full-screen apps hide the status bar, so show the battery level yourself** using the sticky
  `ACTION_BATTERY_CHANGED` broadcast.

## System soft-key bar
Confirmed on a real phone (FlipWeather). Starter code: `templates/SystemBars.kt`.

- **Hide the white soft-key label bar the phone draws at the bottom of the screen, on every
  screen.** It is the system *navigation bar*. *Why:* your app's own soft-key bar already shows
  the labels, so the system one wastes about 26dp of a 320px screen and shows labels that may not
  match your keys.
- **Don't offer a setting to show it again.** *Why:* the app's own bar replaces it, so showing
  both just repeats the labels.
- **Apply both methods, on every API level:**
  - Android 11+: `window.insetsController.hide(WindowInsets.Type.navigationBars())`, with
    `systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`.
  - Every API level: `SYSTEM_UI_FLAG_HIDE_NAVIGATION | SYSTEM_UI_FLAG_IMMERSIVE_STICKY` on the
    decor view's `systemUiVisibility`, **and** the same flags in `window.attributes`.

  *Why:* vendor keypad ROMs often honor only the old flags, and flags set in the window
  attributes survive system-initiated clears better than the view flag alone.
- **Apply it again in `onResume()`, in `onWindowFocusChanged(true)`, once more through
  `decor.post {}`, and from an `OnSystemUiVisibilityChangeListener`.** *Why:* the decor isn't
  attached yet early in a launch, and dialogs and focus changes bring the bar back.
- **Leave the system T9 keyboard alone when it brings the bar back while typing** (for example
  in a town-search field). *Why:* the keyboard uses the bar for its word labels. The bar hides
  again when focus returns to your window.
- **Don't draw your window under the bar.** Let the content grow into the freed space.
  *Why:* if a ROM refuses to hide the bar, your own bar then sits above it rather than behind it.
- **On a framework theme (`Theme.DeviceDefault`), check for a leftover strip.** *Why:* some
  vendor builds of that theme keep drawing a bottom strip after a hide that looks successful.
  AppCompat themes don't.
- *Source:* the same approach the vela-dpad maps app uses, through its Yapchik softkey library.

## Type sizes

| Use | Size |
|---|---|
| Screen header | 18sp |
| Big hero value (e.g. a temperature) | 34sp |
| List row title | 16–20sp |
| Body minimum | 16sp |
| Labels | 14sp |
| Secondary/status | 13–15sp |
| Metadata / timestamps | 11sp minimum |
| Soft-key bar | 15–16sp, fixed height, ignores the app's text-size setting |

- **Keep every label on one line** with `singleLine`/`maxLines=1` and `ellipsize="end"`.
- **Write short error and status messages that fit one line.**
- **Offer a text-size setting through theme overlays (Small/Normal/Large) applied before
  `super.onCreate`.** Recreate the activity when the setting changes.

## Rendering quirks
- **There's no real bold in the stock font.** `Typeface.BOLD` changes nothing; use
  `paint.isFakeBoldText = true`.
- **Some glyphs don't render.** ⏯ and ⏸ show as boxes; ■ and ▶ are fine. Keep emoji to a
  curated list.
- **To show unread items, combine a "●" marker, the accent colour and bold** rather than relying
  on colour alone.
- **XML hints can't contain spans.** For inline icons in a hint, build a Spannable in code.

## Colour and theme
- **Use a dark background with high contrast, at least 7:1.** A warm accent such as orange reads
  well; night-use apps can go red-on-black.
- **Use `Theme.AppCompat.NoActionBar`, or framework `android:Theme.Material.NoActionBar` when you
  have no AppCompat dependency.** Draw your own header. *Why:* an action bar wastes rows.

## Motion
- **Animations: at most 150ms, alpha only, no ripples.** Let the user turn them off on slow chips.
- **Animated content:** keep frames few. A radar loop works well with about 7 frames at 500ms.
  Add every frame as its own layer up front and switch each layer's opacity on and off, rather
  than reloading sources. Stop timers in `onPause`.

## Toolkit and configuration
- **Use Views, not Compose.** *Why:* Views have mature D-pad focus handling, and Compose costs
  APK size and CPU on 2 GB Snapdragon 215 / Helio A22 phones.
- **Opening or closing the flip, or a keypad state change, must not recreate the activity.**
  Use `android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout"`
  and `screenOrientation="portrait"` unless you support landscape.
  *Why:* recreating drops held-key state and open sockets.
- **Keep-on display apps:** use `FLAG_KEEP_SCREEN_ON | FLAG_SHOW_WHEN_LOCKED |
  FLAG_DISMISS_KEYGUARD | FLAG_TURN_SCREEN_ON`, so opening the flip comes straight back to the
  app.
