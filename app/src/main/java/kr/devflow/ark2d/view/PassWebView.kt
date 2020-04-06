package kr.devflow.ark2d.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView

/**
 * Pass through touch event WebView
 */
@SuppressLint("SetJavaScriptEnabled")
class PassWebView @JvmOverloads constructor(
    context: Context, attributeSet: AttributeSet? = null, def: Int = 0
) : WebView(context, attributeSet, def) {

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isEnabled = false
        isClickable = false
        isFocusable = false
        setWebContentsDebuggingEnabled(true)
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.useWideViewPort = false
        settings.builtInZoomControls = false
        settings.setSupportZoom(false)
        settings.displayZoomControls = false
        settings.loadWithOverviewMode = false
        setLayerType(View.LAYER_TYPE_HARDWARE, null) // for webGL
        settings.javaScriptEnabled = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun performClick(): Boolean {
        return false
    }

}