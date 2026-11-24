# Focus, lists and menus

The phone has no touchscreen, so the focus highlight is the only cursor the user has.
Each entry gives the **rule** first, then *why* it exists.

## Focus highlight
- **Use one shared, clearly visible focus drawable across the whole app.**
  *Why:* the system's default focus indicator was too hard to see on these phones.
- **ListView: `android:listSelector="@drawable/list_row_selector"` plus
  `android:drawSelectorOnTop="true"`, with a translucent fill and a 2dp accent stroke.**
  If your focus tint is opaque, draw the selector behind the rows instead
  (`drawSelectorOnTop="false"`). *Why:* a solid tint drawn on top hid menu text.
  *Why:* the list draws the selector at the selected item's bounds itself, which is more reliable
  than relying on the row view's state. See `templates/list_row_selector.xml`.
- **RecyclerView rows: a tinted background plus a 5–6dp accent bar.**
  *Why:* a 3dp bar was barely visible, and a bar alone got lost on light themes.
- **Buttons and fields: a `<selector>` where `state_focused` uses a filled box with a bright
  2dp stroke and the default state uses a dim 1dp stroke. Set `stateListAnimator="@null"` on
  Buttons.**
- **Cover `state_focused`, `state_selected` and `state_pressed`.**
- **Focused app icons: draw a ring around the icon, never a fill behind it.**
  *Why:* a fill in the accent or icon colour washed out icons drawn in that colour.

## Focusability
- **Interactive views: `focusable="true"`, `focusableInTouchMode="false"`.** Text fields are the
  only views that set `focusableInTouchMode="true"`.
- **Soft-key labels and decorative views: `focusable="false"`.** *Why:* otherwise they steal
  D-pad focus.
- **Make the RecyclerView or ListView itself non-focusable, and give it
  `descendantFocusability="afterDescendants"`.**
  *Why:* otherwise, returning to the screen, or swapping a page that removes the focused row,
  puts focus on the whole list for a frame.
- **A MapView can take focus even when the XML says `focusable="false"`.** Plan for that (see
  [keys.md](keys.md), OK/CENTER).
- **Screens that are driven entirely by keys may skip focus.** Handle every key in
  `dispatchKeyEvent` and highlight the on-screen control while its key is held.

- **For long lists, prefer a recycled `ListView` that keeps focus itself and moves its
  *selection*, with non-focusable rows (`itemsCanFocus = false`).** *Why:* on Kyocera every
  focus change runs the IME's failing `getShwoingNowFlag` path, which made focus-per-row lists
  sluggish.
- **Make section headers disabled list items** so the D-pad skips them, and show an empty state
  as a selectable row ("No events") so the screen still has a focus.

## Starting focus and restoring it
- **Every screen has a predictable starting focus, using `post { requestFocus() }` as a fallback.**
- **Only request focus for row 0 when nothing is focused yet.**
  *Why:* requesting it on every rebind pulled focus away while the user was scrolling.
- **If the target row isn't laid out yet, scroll to it first and request focus once it
  attaches.** *Why:* `findViewByPosition` returns null right after a rebind, `requestFocus`
  silently does nothing, and focus jumps to the top.
- **Skip re-rendering when fresh data equals the cached data.**
  *Why:* two full rebinds in a row caused a focus and scroll glitch.
- **After moving between fields, call `view.post { target.requestFocus() }`.**

## Scrolling
- **Don't wrap at the ends of a list.** *Why:* wrapping is disorienting on a tiny screen.
  (A launcher's app grid is the exception.)
- **When rows are taller than the screen, scroll about ¾ inch (120dp) per press until the row is
  fully visible, then move focus.** *Why:* the default `requestRectangleOnScreen` jumped past
  the middle of long items.
- **Read-only lists scroll a fixed 120dp per press.**
- **Snap to a whole row when scrolling stops.** *Why:* holding a key down interrupts scroll
  animations and leaves the top row half hidden.
- **For a settings screen, use a `ScrollView` with `fillViewport="true"` around focusable
  Buttons.** The D-pad moves focus and the ScrollView follows it with no extra code.
- **Chat-style lists: `stackFromEnd = true`, and follow the bottom yourself.**
  *Why:* `stackFromEnd` only places content at the bottom on the first layout. New rows land
  below the screen, and rows that grow after binding (an image loading, receipts appearing)
  push the newest message further down.
  - Keep a "follow bottom" flag. In `onScrolled`, set it to `!rv.canScrollVertically(1)`, but
    ignore calls with `dy == 0`: those are layout passes, not the user scrolling.
  - When following, scroll to the last row in the `submitList(rows) { … }` commit callback, and
    again from a global layout listener whenever the list can still scroll down.
  - With `stackFromEnd`, `scrollToPosition(last)` puts the last row's bottom flush with the
    list's bottom, even when the row is taller than the list.
  - Sending a message always follows again.

- **When stepping through tall RecyclerView rows, measure the current (attached) row**, not the
  next one. *Why:* RecyclerView doesn't keep off-screen neighbours attached.
- **A "scroll within a tall row" step must never loop.** Scroll only while
  `canScrollVertically` is true; otherwise move focus on. *Why:* a row whose edge sat a few
  pixels past the bottom trapped Down.
- **Derive a hand-made list's last index from its rows list**, never from a hard-coded count.
  *Why:* a stale count crashed on Down at the last row.
- **App drawers: restart focus on the first page's centre cell every time the drawer resumes,**
  not only on first open.

## Menus and dialogs
- **Use `AlertDialog.Builder.setItems(...)` for menus.** It works with the D-pad as is.
- **Custom menu sheets:** anchor them to the bottom, use about 5 focusable rows, let Back close
  them, start focus on row 0, and cap their height at 60%. Hide the keyboard first, or the
  dialog draws behind it.
- **Never show a dialog that can only be dismissed by touch.** That leaves the user stuck.
- **Don't use screen pinning (`startLockTask`).** Its confirmation dialog can't be reached
  with the D-pad.
- **Avoid WebView UIs.** They're designed for touch and use an old system WebView.
- **Sound pickers: preview the highlighted sound after a ~250ms pause**, so holding Down doesn't
  play every sound it passes. Each new preview stops the last. Back closes, with no Done button.
