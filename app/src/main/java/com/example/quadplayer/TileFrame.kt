package com.example.quadplayer

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Tile container. It reports every touch / key that lands in the tile, takes over multi-finger
 * gestures so pinch-zoom works even if a finger starts on a button, and is itself focusable so a
 * tile whose controls are hidden can still hold D-pad focus.
 */
class TileFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var touchObserver: ((MotionEvent) -> Unit)? = null
    var keyObserver: ((KeyEvent) -> Boolean)? = null     // return true to consume the key
    var frameFocusGained: (() -> Unit)? = null           // the tile itself (not a child) got focus

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Observe AFTER dispatch: the touch that reveals hidden controls can't press them.
        val handled = super.dispatchTouchEvent(ev)
        touchObserver?.invoke(ev)
        return handled
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.pointerCount > 1) return true
        return super.onInterceptTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (keyObserver?.invoke(event) == true) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (gainFocus) frameFocusGained?.invoke()
    }
}
