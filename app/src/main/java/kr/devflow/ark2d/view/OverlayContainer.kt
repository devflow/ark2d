package kr.devflow.ark2d.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

class OverlayContainer @JvmOverloads constructor(
    context: Context, attributeSet: AttributeSet? = null, def: Int = 0
) : FrameLayout(context, attributeSet, def), View.OnTouchListener {

    private var downRawX: Int = 0
    private var downRawY: Int = 0
    private var lastX: Int = 0
    private var lastY: Int = 0
    private var callbacks: Callbacks? = null
    private val gestureDetector: GestureDetector
    private var isResizing = false
    private var resizeDirection = 0 // 0-left 1-right
    var resizeThreshold = 45
    var isResizable = false

    init {
        setOnTouchListener(this)
        gestureDetector = GestureDetector(context, GestureListener())
    }

    fun setCallbacks(callbacks: Callbacks) {
        this.callbacks = callbacks
    }

    private fun isResizeControlArea(x: Int, y: Int): Boolean {
        // resize control areas = left-bottom, right-bottom... and... nop.
        val w = layoutParams.width
        val h = layoutParams.height

        //first check y at bottom
        if (y in (h - resizeThreshold) until h) {
            when (x) {
                in 0 until resizeThreshold -> { // left
                    resizeDirection = 0
                    return true // left direction need to move x coordinate with -offset
                }
                in (w - resizeThreshold) until w -> { // right
                    resizeDirection = 1
                    return true
                }
            }
        }

        return false
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(motionEvent)

        val action = motionEvent.action
        val x = motionEvent.rawX.toInt()
        val y = motionEvent.rawY.toInt()

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = x
                downRawY = y
                lastX = x
                lastY = y
                isResizing = isResizable
                        && isResizeControlArea(motionEvent.x.toInt(), motionEvent.y.toInt())

                if (isResizing) callbacks?.onResizeStart()

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val nx = (x - lastX)
                val ny = (y - lastY)
                lastX = x
                lastY = y

                if (isResizing) {
                    callbacks?.onResize(nx, ny, resizeDirection)
                } else {
                    callbacks?.onDrag(nx, ny)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isResizing) callbacks?.onResizeEnd()
                isResizing = false
                callbacks?.onDragEnd(x, y)
                return true
            }
            else -> {
                return super.onTouchEvent(motionEvent)
            }
        }
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            callbacks?.onSingleClick()
            return true
        }

        override fun onLongPress(e: MotionEvent?) {
            callbacks?.onLongPress()
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            callbacks?.onDoubleClick()
            return true
        }

    }

    interface Callbacks {
        fun onResizeStart()
        fun onResizeEnd()
        fun onResize(dx: Int, dy: Int, direction: Int)
        fun onDrag(dx: Int, dy: Int)
        fun onDragEnd(dx: Int, dy: Int)
        fun onDragStart(dx: Int, dy: Int)
        fun onDoubleClick()
        fun onSingleClick()
        fun onLongPress()
    }
}