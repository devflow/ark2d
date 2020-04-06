package kr.devflow.ark2d

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import kr.devflow.ark2d.view.OverlayContainer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class OverlayWrapper(
    private val context: Context,
    private val winManager: WindowManager,
    private var defaultSize: Point,
    private val initialPosition: Int = 0
) :
    OverlayContainer.Callbacks {

    private var isShown = false
    private lateinit var layoutParams: WindowManager.LayoutParams
    lateinit var baseView: View
    lateinit var baseRect: Rect
    var touchCallback: OverlayContainer.Callbacks? = null
    var isOverflowAllowed = false // must change val before createView()
    var isResizable = false // also before createView()

    fun isShown(): Boolean {
        return isShown
    }

    fun createView(layoutId: Int): View {
        updateRect()
        createLayoutParams()

        baseView = LayoutInflater.from(context).inflate(
            layoutId, null, false
        )

        (baseView as OverlayContainer).setCallbacks(this)
        (baseView as OverlayContainer).isResizable = isResizable

        return baseView
    }

    fun screenSizeChanged() {
        updateRect()
        settle()
    }

    fun show() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                winManager.addView(baseView, layoutParams)
            }
        } else {
            winManager.addView(baseView, layoutParams)
        }

        isShown = true
    }

    fun hide() {
        winManager.removeView(baseView)
        isShown = false
    }

    fun moveTo(x: Int, y: Int) {
        val va1 = ValueAnimator.ofInt(layoutParams.x, x)
        va1.interpolator = AccelerateDecelerateInterpolator()
        va1.duration = 500
        va1.addUpdateListener {
            layoutParams.x = it.animatedValue as Int
            redraw()
        }

        val va2 = ValueAnimator.ofInt(layoutParams.y, y)
        va2.interpolator = AccelerateDecelerateInterpolator()
        va2.duration = 500
        va2.addUpdateListener {
            layoutParams.y = it.animatedValue as Int
            redraw()
        }

        va1.start()
        va2.start()
    }

    fun moveBy(dx: Int, dy: Int) {
        layoutParams.x = min(max(layoutParams.x + dx, baseRect.left), baseRect.right)
        layoutParams.y = min(max(layoutParams.y + dy, baseRect.top), baseRect.bottom)
        redraw()
    }

    fun changeScale(newScale: Point) {
        defaultSize = Point(newScale)

        layoutParams.width = dp2px(defaultSize.x)
        layoutParams.height = dp2px(defaultSize.y)

        updateRect()
        settle()
    }

    fun redraw() {
        winManager.updateViewLayout(baseView, layoutParams)
    }

    private fun settle() {
        val x = min(max(layoutParams.x, baseRect.left), baseRect.right)
        val y = min(max(layoutParams.y, baseRect.top), baseRect.bottom)

        moveTo(x, y)
    }

    private fun updateRect() {
        val dm = context.resources.displayMetrics
        val overflowThreshold = Point(0, 0)

        if (isOverflowAllowed) {
            overflowThreshold.set(overlayWidth() / 2, overlayHeight() / 2)
        }

        baseRect =
            Rect(
                -overflowThreshold.x, -overflowThreshold.y,
                dm.widthPixels - overlayWidth() + overflowThreshold.x,
                dm.heightPixels - overlayHeight() + overflowThreshold.y
            )
    }

    private fun overlayWidth(): Int {
        if (::layoutParams.isInitialized) {
            return layoutParams.width
        }

        return dp2px(defaultSize.x)
    }

    private fun overlayHeight(): Int {
        if (::layoutParams.isInitialized) {
            return layoutParams.height
        }

        return dp2px(defaultSize.y)
    }

    @SuppressLint("RtlHardcoded")
    private fun createLayoutParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                PixelFormat.TRANSLUCENT
            )
        } else {
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR),
                PixelFormat.TRANSLUCENT
            )
        }
        layoutParams.windowAnimations = R.style.Popping
        layoutParams.gravity = Gravity.TOP or Gravity.LEFT
        layoutParams.width = dp2px(defaultSize.x)
        layoutParams.height = dp2px(defaultSize.y)

        when (initialPosition) {
            1 -> {
                layoutParams.y = baseRect.bottom - dp2px(defaultSize.y)
                layoutParams.x = baseRect.right - dp2px(defaultSize.x)
            }
            else -> {
                layoutParams.y = baseRect.bottom / 2
                layoutParams.x = baseRect.right - dp2px(defaultSize.x)
            }
        }


    }

    override fun onResizeStart() {
        touchCallback?.onResizeStart()
    }

    override fun onResizeEnd() {
        touchCallback?.onResizeEnd()
    }

    override fun onResize(dx: Int, dy: Int, direction: Int) {
        if (!isResizable) return // double check for changes at run-time

        when (direction) {
            0 -> { // L
                layoutParams.height += dy
                layoutParams.x += dx

                if (dx < 0) {
                    layoutParams.width += abs(dx)
                } else {
                    layoutParams.width += -abs(dx)
                }
            }
            1 -> { // R
                layoutParams.height += dy
                layoutParams.width += dx
            }
        }
        redraw()

        touchCallback?.onResize(dx, dy, direction)
    }

    override fun onDrag(dx: Int, dy: Int) {
        moveBy(dx, dy)
        touchCallback?.onDrag(dx, dy)
    }

    override fun onDragEnd(dx: Int, dy: Int) {
        settle()
        touchCallback?.onDragEnd(dx, dy)
    }

    override fun onDragStart(dx: Int, dy: Int) {
        touchCallback?.onDragEnd(dx, dy)
    }

    override fun onSingleClick() {
        touchCallback?.onSingleClick()
    }

    override fun onDoubleClick() {
        touchCallback?.onDoubleClick()
    }

    override fun onLongPress() {
        touchCallback?.onLongPress()
    }

    // helpers
    private fun dp2px(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

}