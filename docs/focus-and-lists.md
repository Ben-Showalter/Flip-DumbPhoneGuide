# Focus, lists and menus

The phone has no touchscreen, so the focus highlight is the only cursor the user has.
Each entry gives the **rule** first, then *why* it exists.

## Focus highlight
- **Use one shared, clearly visible focus drawable across the whole app.**
  *Why:* the system's default focus indicator was too hard to see on these phones.
- **ListView: `android:listSelector="@drawable/list_row_selector"` plus
  `android:drawSelectorOnTop="true"`, with a translucent fill and a 2dp accent stroke.**
  *Why:* the list draws the selector at the selected item's bounds itself, which is more reliable
  than relying on the row view's state. See `templates/list_row_selector.xml`.
- **RecyclerView rows: a tinted background plus a 5–6dp accent bar.**
  *Why:* a 3dp bar was barely visible, and a bar alone got lost on light themes.
- **Buttons and fields: a `<selector>` where `state_focused` uses a filled box with a bright
  2dp stroke and the default state uses a dim 1dp stroke. Set `stateListAnimator="@null"` on
  Buttons.**
- **Cover `state_focused`, `state_selected` and `state_pressed`.**

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
- **Chat-style lists use `stackFromEnd = true`.**

## Menus and dialogs
- **Use `AlertDialog.Builder.setItems(...)` for menus.** It works with the D-pad as is.
- **Custom menu sheets:** anchor them to the bottom, use about 5 focusable rows, let Back close
  them, start focus on row 0, and cap their height at 60%. Hide the keyboard first, or the
  dialog draws behind it.
- **Never show a dialog that can only be dismissed by touch.** That leaves the user stuck.
- **Don't use screen pinning (`startLockTask`).** Its confirmation dialog can't be reached
  with the D-pad.
- **Avoid WebView UIs.** They're designed for touch and use an old system WebView.
