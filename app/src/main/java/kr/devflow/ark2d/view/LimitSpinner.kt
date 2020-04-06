package kr.devflow.ark2d.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.util.AttributeSet
import android.view.WindowManager

class LimitSpinner : androidx.appcompat.widget.AppCompatSpinner {
    constructor(context: Context) : super(context)
    constructor(context: Context, mode: Int) : super(context, mode)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, mode: Int) : super(
        context,
        attrs,
        defStyleAttr,
        mode
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        mode: Int,
        pop: Resources.Theme
    ) : super(context, attrs, defStyleAttr, mode, pop)


    override fun getWindowVisibleDisplayFrame(outRect: Rect?) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d = wm.defaultDisplay
        d.getRectSize(outRect)
        outRect?.set(outRect.left, outRect.top, outRect.right, outRect.bottom)
    }
}