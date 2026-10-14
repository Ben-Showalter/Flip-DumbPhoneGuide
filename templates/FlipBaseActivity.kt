package com.example.app // TODO: your package

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * App-wide keypad scheme for flip-phone apps.
 * Every screen extends this. Override the hooks, not onKeyDown.
 *
 *   Left softkey   - onPrimaryKey()   (Refresh / New / the screen's main action)
 *   Right softkey  - onOptionsKey()   (Options / Settings). KEYCODE_MENU too:
 *                    its whole press (DOWN, UP, repeats) is consumed in
 *                    dispatchKeyEvent, because AppCompat opens its own
 *                    options panel on MENU UP over yours.
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
 * The phone's white soft-key label bar (the system navigation bar) is hidden
 * on every screen, in onResume and again whenever the window regains focus
 * (see SystemBars.kt), except while a text field has focus: the T9 keyboard
 * shows its labels there. Your layouts draw their own soft-key bar.
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

    open fun onPrimaryKey() {}
    open fun onOptionsKey() {}
    /** Return true if handled. Default does nothing, so the key falls through. */
    open fun onCenterKey(): Boolean = false

    // Show the system bar while a text field has focus (the T9 keyboard's
    // labels live there); hide it again when focus leaves.
    private val barFocusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
        if (newFocus is EditText) SystemBars.showNavigation(this) else SystemBars.hideNavigation(this)
    }

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
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) onOptionsKey()
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
            KeyEvent.KEYCODE_SOFT_LEFT -> { if (first) onPrimaryKey(); return true }
            KeyEvent.KEYCODE_SOFT_RIGHT -> { if (first) onOptionsKey(); return true }
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
