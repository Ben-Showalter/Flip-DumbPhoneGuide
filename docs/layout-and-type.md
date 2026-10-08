# Screen, layout and type

Each entry gives the **rule** first, then *why* it exists.

## Size
- **Design for 240×320 px at mdpi (2.4–2.8″).** Some models are 320×240 landscape (see
  [devices.md](devices.md)).
- **Typical screen chrome:** a 24dp title bar, about 270dp of content and a 26dp soft-key bar.
- **Size grid cells and rail slots from the available pixels, not fixed dp**, and keep
  per-cell padding small (20dp, not 36dp).
- **Use `minHeight` for rows, never a fixed height.** Give status and flash lines a fixed height,
  for example 20dp, so the layout doesn't jump, and clear them after about 2.5s.
- **Hide anything that isn't needed while browsing.** For example, collapse a compose box to GONE
  while the user scrolls through messages. *Why:* it was hiding short messages.
- **Full-screen apps hide the status bar, so show the battery level yourself** using the sticky
  `ACTION_BATTERY_CHANGED` broadcast.

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
