package kr.devflow.ark2d

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var isAllowedOverlay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                isAllowedOverlay = false
                startActivityForResult(intent, 1)
            } else {
                isAllowedOverlay = true
            }
        } else {
            isAllowedOverlay = true
        }

        button.setOnClickListener {
            it.isEnabled = false
            startArkFriends()
        }
    }

    private fun startArkFriends() {
        if (!isAllowedOverlay) {
            Toast.makeText(this, "권한허용이 안됨. 앱 종료 후 다시 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val floatingService = Intent(applicationContext, Ark2dOverlayService::class.java)
        floatingService.action = "start"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(floatingService)
        } else {
            startService(floatingService)
        }

        finish()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if (Settings.canDrawOverlays(this)) {
                isAllowedOverlay = true
            }
        }
    }
}
