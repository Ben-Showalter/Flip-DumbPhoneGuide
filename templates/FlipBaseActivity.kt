package com.example.app // TODO: your package

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * App-wide keypad scheme for flip-phone apps.
 * Every screen extends this. Override the hooks, not onKeyDown.
 *
 * ONE Options key, so the app works on phones without two soft keys:
 *   Right softkey  - Options on Kyocera.
 *   Menu key       - Options on Sonim and other phones with a Menu key
 *                    (KEYCODE_MENU). Its whole press (DOWN, UP, repeats) is
 *                    consumed in dispatchKeyEvent, because AppCompat opens its
 *                    own options panel on MENU UP over yours. The very first
 *                    MENU press ever asks once whether to show the "Options"
 *                    label on the left (promptOptionsSide). There is no
 *                    setting for this: the label moves back to the right only
 *                    when a real KEYCODE_SOFT_RIGHT press is seen.
 *   Left softkey   - does nothing by default. Override onOtherSoftKey() ONLY
 *                    if the app's owner asked for a shortcut, and keep that
 *                    action in the Options menu too.
 *   OK (CENTER or ENTER) - onCenterKey(). Handled at the onKeyDown stage on
 *                    purpose, so a focused list row gets first crack at it.
 *   D-pad LEFT/RIGHT - shift through mainScreens in order, clamped at the
 *                    ends. Intercepted in dispatchKeyEvent (ahead of view
 *                    focus) so it works even while a row/button has focus.
 *                    Skipped while an EditText has focus (arrows move the
 *                    cursor) and on screens where handlesHorizontalKeys
 *                    is true (e.g. a pannable map).
 *   Back           - left alone (normal back stack). A screen reached via
 *                    jump() has nothing beneath it, so override
 *                    onBackPressed there to jump() to a sensible parent.
 *   CALL / END     - always fall through to super so the phone still
 *                    dials/hangs up.
 *
 * Layouts draw their own footer with a single "Options" label whose id is
 * R.id.footerOptions (TODO: match your layout); placeOptionsLabel() moves it
 * to the end that matches the Options side.
 *
 * The phone's white soft-key label bar (the system navigation bar) is hidden
 * on every screen, in onResume and again whenever the window regains focus
 * (see SystemBars.kt), except while a text field has focus: the T9 keyboard
 * shows its labels there.
 *
 * If a view (MapView etc.) swallows CENTER before onKeyDown, override
 * dispatchKeyEvent in that screen and handle CENTER/ENTER before super.
 */
abstract class FlipBaseActivity : AppCompatActivity() {

    /** Screens LEFT/RIGHT shift through, in order. Return the same list from every screen. */
    protected abstract val mainScreens: List<Class<out FlipBaseActivity>>

    /** True on screens that use LEFT/RIGHT themselves. */
    protected open val handlesHorizontalKeys: Boolean = false

    /** Set false in release builds if the toast gets in the way; keep it for new hardware. */
    protected open val showUnhandledKeys: Boolean = true

    /** Open this screen's Options menu (or Settings). */
    abstract fun onOptionsKey()

    /** The non-Options soft key. Empty unless the app's owner asked for a shortcut. */
    open fun onOtherSoftKey() {}

    /** Return true if handled. Default does nothing, so the key falls through. */
    open fun onCenterKey(): Boolean = false

    // Show the system bar while a text field has focus (the T9 keyboard's
    // labels live there); hide it again when focus leaves.
    private val barFocusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
        if (newFocus is EditText) SystemBars.showNavigation(this) else SystemBars.hideNavigation(this)
    }

    // Keys whose DOWN this window saw (see "act on an UP only if this window saw its DOWN").
    private val downSeen = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener(barFocusListener)
    }

    override fun onDestroy() {
        window.decorView.viewTreeObserver.removeOnGlobalFocusChangeListener(barFocusListener)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        SystemBars.hideNavigation(this)
        placeOptionsLabel()
    }

    override fun onPause() {
        downSeen.clear()
        super.onPause()
    }

    // Hide it again after a dialog brought it back (not while typing).
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) SystemBars.hideNavigation(this)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // MENU = Options, and the whole press is ours: AppCompat would open its
        // own options panel on MENU UP, covering the one we just opened.
        if (event.keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                if (OptionsSide.hasSeenMenuKey(this)) onOptionsKey() else promptOptionsSide()
            }
            return true
        }
        if (!handlesHorizontalKeys && event.action == KeyEvent.ACTION_DOWN && currentFocus !is EditText) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> { shift(-1); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { shift(1); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val first = (event?.repeatCount ?: 0) == 0
        when (keyCode) {
            KeyEvent.KEYCODE_SOFT_LEFT, KeyEvent.KEYCODE_SOFT_RIGHT -> {
                if (first) downSeen.add(keyCode)
                return true // act on UP, below
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (first && onCenterKey()) return true
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {}
            else -> if (showUnhandledKeys && first && currentFocus !is EditText) {
                Toast.makeText(this, "Unhandled key: ${KeyEvent.keyCodeToString(keyCode)}", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT || keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
            val sawDown = downSeen.remove(keyCode)
            if (event?.isCanceled == true) return true
            if (keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
                // A real right soft key exists: Options belongs on the right.
                if (OptionsSide.isOnLeft(this)) {
                    OptionsSide.setOnLeft(this, false)
                    placeOptionsLabel()
                }
                // On the DuraXV the right soft key's DOWN can be lost, so a
                // DOWN-less UP still counts as a tap for this key only.
                onOptionsKey()
            } else if (sawDown) {
                if (OptionsSide.isOnLeft(this)) onOptionsKey() else onOtherSoftKey()
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * First MENU press ever: offer to show "Options" on the left. Marked seen
     * up front so it never asks twice. No Settings entry for this.
     */
    private fun promptOptionsSide() {
        OptionsSide.setSeenMenuKey(this)
        AlertDialog.Builder(this)
            .setTitle("Options key found")
            .setMessage("Your phone has an Options key. Show the \"Options\" label on the left side of the screen instead?")
            .setPositiveButton("Yes") { _, _ ->
                OptionsSide.setOnLeft(this, true)
                placeOptionsLabel()
            }
            .setNegativeButton("No", null)
            .setOnDismissListener { SystemBars.hideNavigation(this) }
            .show()
    }

    /** Moves the footer's single "Options" label to the end matching the Options side. */
    private fun placeOptionsLabel() {
        val label = findViewById<View>(R.id.footerOptions) ?: return // TODO: your footer label id
        val footer = label.parent as? ViewGroup ?: return
        val wanted = if (OptionsSide.isOnLeft(this)) 0 else footer.childCount - 1
        if (footer.indexOfChild(label) != wanted) {
            footer.removeView(label)
            footer.addView(label, wanted)
        }
    }

    private fun shift(direction: Int) {
        val screens = mainScreens
        val idx = screens.indexOfFirst { it.isInstance(this) }
        val target = if (idx == -1) {
            // From a screen outside the main row: LEFT -> first, RIGHT -> last.
            if (direction < 0) screens.first() else screens.last()
        } else {
            screens.getOrNull(idx + direction) // null at the ends = clamp, no wrap
        }
        if (target != null) jump(target)
    }

    /** Sideways navigation: replace this screen rather than stacking on it. */
    protected fun jump(cls: Class<out FlipBaseActivity>) {
        startActivity(Intent(this, cls))
        finish()
    }
}

/** Which side the single Options label sits on. Learned from the keys, never a setting. */
object OptionsSide {
    private const val PREFS = "options_side"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun isOnLeft(ctx: Context) = prefs(ctx).getBoolean("on_left", false)
    fun setOnLeft(ctx: Context, left: Boolean) = prefs(ctx).edit().putBoolean("on_left", left).apply()
    fun hasSeenMenuKey(ctx: Context) = prefs(ctx).getBoolean("seen_menu", false)
    fun setSeenMenuKey(ctx: Context) = prefs(ctx).edit().putBoolean("seen_menu", true).apply()
}
