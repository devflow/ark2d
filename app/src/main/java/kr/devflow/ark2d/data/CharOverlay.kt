package kr.devflow.ark2d.data

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.SeekBar
import kotlinx.android.synthetic.main.char_overlay.view.*
import kotlinx.android.synthetic.main.config_overlay.view.*
import kr.devflow.ark2d.OverlayWrapper
import kr.devflow.ark2d.R
import kr.devflow.ark2d.util.Pref
import kr.devflow.ark2d.view.OverlayContainer
import kotlin.math.max
import kotlin.math.min

class CharOverlay constructor(
    val overlayId: String,
    val context: Context,
    private val winManager: WindowManager,
    private val charIPC: CharIPC
) {
    lateinit var characterOverlay: OverlayWrapper
    lateinit var configOverlay: OverlayWrapper

    var characterData = CharData("build_char_002_amiya", "당끼")

    fun create(defaultCharacter: CharData? = null) {
        val savedScale = Pref.readPref(context, "chara_scale", "10").toInt()

        hide()

        characterOverlay = OverlayWrapper(context, winManager, calcCharaScale(savedScale), 0)
        characterOverlay.isOverflowAllowed = true
        characterOverlay.isResizable = true
        characterOverlay.createView(R.layout.char_overlay)
        characterOverlay.touchCallback = charCallback
        characterOverlay.apply {
            baseView.webView.webViewClient = CustomClient()
            baseView.webView.webChromeClient = (object : WebChromeClient() {})

            baseView.webView.loadUrl(
                String.format(
                    "file:///android_asset/index.html?c=%s",
                    defaultCharacter?.id
                )
            )
        }

        characterOverlay.show()

        configOverlay = OverlayWrapper(context, winManager, Point(240, 120), 1)
        configOverlay.createView(R.layout.config_overlay)
        configOverlay.apply {
            baseView.btnCloseConfigOverlay.setOnClickListener { toggleConfigOverlay() }
            baseView.sbCharaSize.max = 100

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                baseView.sbCharaSize.min = 5
            }

            baseView.sbCharaSize.progress = savedScale
            baseView.sbCharaSize.setOnSeekBarChangeListener(sizeSeekBarListener)
            baseView.tvCharaSlot.setOnClickListener {
                charIPC.requestChangeChar(
                    this@CharOverlay,
                    characterData.id
                )
            }
        }
    }

    private fun toggleConfigOverlay() {
        if (configOverlay.isShown()) {
            characterOverlay.isResizable = false
            characterOverlay.baseView.setBackgroundColor(Color.TRANSPARENT)
            configOverlay.hide()
            charIPC.changeConfigState(false)
        } else {
            characterOverlay.isResizable = true
            characterOverlay.baseView.setBackgroundResource(R.drawable.border_background)
            configOverlay.show()
            charIPC.changeConfigState(true)
        }
    }

    fun hide() {
        if (::characterOverlay.isInitialized) {
            characterOverlay.hide()
        }
        if (::configOverlay.isInitialized) {
            configOverlay.hide()
        }
    }

    fun screenSizeChanged() {
        if (::characterOverlay.isInitialized) {
            characterOverlay.screenSizeChanged()
        }
        if (::configOverlay.isInitialized) {
            characterOverlay.screenSizeChanged()
        }
    }

    fun changeCharacter(cd: CharData) {
        characterData = cd
        configOverlay.baseView.tvCharaSlot.text = "캐릭터 : ${cd.name}"
        jsChangeChar(cd.id)
    }

    private fun jsChangeChar(charId: String) {
        characterOverlay.baseView.webView.evaluateJavascript(
            String.format(
                "changeChar(\"%s\")",
                charId
            ), null
        )
    }

    private fun calcCharaScale(bigScale: Int): Point {
        return Point(
            (80 * (bigScale / 10.0)).toInt(),
            (140 * (bigScale / 10.0)).toInt()
        )
    }

    private fun changeCharaScale(bigScale: Int) {
        characterOverlay.changeScale(calcCharaScale(bigScale))
        Thread {
            Pref.savePref(context, "chara_scale", bigScale.toString())
        }.run()
    }

    private val charCallback = object : OverlayContainer.Callbacks {
        override fun onResizeStart() {
            //characterOverlay.baseView.setBackgroundResource(R.drawable.border_background)
        }

        override fun onResizeEnd() {
            //characterOverlay.baseView.setBackgroundColor(Color.TRANSPARENT)
        }

        override fun onSingleClick() {
            characterOverlay.baseView.webView.evaluateJavascript("flipX()", null)
        }

        override fun onResize(dx: Int, dy: Int, direction: Int) {
        }

        override fun onDrag(dx: Int, dy: Int) {
        }

        override fun onDragEnd(dx: Int, dy: Int) {
        }

        override fun onDragStart(dx: Int, dy: Int) {
        }

        override fun onDoubleClick() {
            characterOverlay.baseView.webView.evaluateJavascript("ChangeAnimation()", null)
        }

        override fun onLongPress() {
            toggleConfigOverlay()
        }

    }

    private val sizeSeekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val chunkProgress = min(max(progress, 5), 100) // for > Android Oreo
            changeCharaScale(chunkProgress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    interface CharIPC {
        fun changeConfigState(isOpen: Boolean)
        fun requestChangeChar(caller: CharOverlay, currentCharId: String)
    }

    class CustomClient : WebViewClient() {
        override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean {
            return true
        }
    }
}