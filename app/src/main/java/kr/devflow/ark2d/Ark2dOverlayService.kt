package kr.devflow.ark2d

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kr.devflow.ark2d.data.CharData
import kr.devflow.ark2d.data.CharOverlay


class Ark2dOverlayService : Service() {
    inline fun <reified T> Gson.fromJson(json: String) =
        fromJson<T>(json, object : TypeToken<T>() {}.type)

    private lateinit var winManager: WindowManager
    private lateinit var notificationManager: NotificationManager
    private var overlayStacks = HashMap<String, CharOverlay>()
    private var localBinder = LocalBinder()
    private var charList = ArrayList<CharData>()

    override fun onBind(intent: Intent?): IBinder? {
        return localBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        winManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels()

        when (intent?.action) {
            "start" -> {
                init()
            }
            "kill" -> {
                stopSelf()
                return super.onStartCommand(intent, flags, startId)
            }
        }

        return START_STICKY
    }

    private fun init() {
        initCharData()
        createOverlays()
        createKillNotification()
    }

    private fun createOverlays() {
        val co = CharOverlay("0", this, winManager, charIPC)
        co.create()
        overlayStacks["0"] = co
    }

    private fun initCharData() {
        val inStream = assets.open("char_data.json")
        val buffer = ByteArray(inStream.available())
        inStream.read(buffer)
        inStream.close()

        val chars = Gson().fromJson<ArrayList<CharData>>(String(buffer, Charsets.UTF_8))
        charList.clear()
        charList.addAll(chars)
    }

    private fun createKillNotification() {
        val killIntent = Intent(this, Ark2dOverlayService::class.java)
        killIntent.action = "kill"

        val killPIntent = PendingIntent.getService(this, 0, killIntent, 0)

        val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, "ark_friends")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText("명빵친구들 퇴근시키기")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(killPIntent)

        startForeground(1, mBuilder.build())
    }

    @TargetApi(26)
    private fun createNotificationChannels() {
        val recordingNotificationChannel = NotificationChannel(
            "ark_friends",
            "명빵 친구들",
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(recordingNotificationChannel)
    }

    private fun hideAllOverlays() {
        for (overlay in overlayStacks.values) {
            overlay.hide()
        }
    }


    /**
     * replaced with spinner widget. because glitch bug on appcompact spinner with alert overlay
     */
    private fun showSelectCharDialog(selectedCharIndex: Int, callbackTo: CharOverlay) {
        val listItems = ArrayList(charList.map { charData -> charData.name })
        val dialogBuilder = AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert)
        dialogBuilder.setTitle("캐릭터 선택")
        dialogBuilder.setSingleChoiceItems(listItems.toTypedArray(), selectedCharIndex)
        { dialogInterface, i ->
            callbackTo.changeCharacter(charList[i])
            dialogInterface.dismiss()
        }.setNeutralButton("취소") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = dialogBuilder.create()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideAllOverlays()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        for (ov in overlayStacks.values) {
            ov.screenSizeChanged()
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): Ark2dOverlayService = this@Ark2dOverlayService
    }

    private val charIPC = object : CharOverlay.CharIPC {
        override fun changeConfigState(isOpen: Boolean) {

        }

        override fun requestChangeChar(caller: CharOverlay, currentCharId: String) {
            showSelectCharDialog(charList.indexOfFirst { data -> data.id == currentCharId }, caller)
        }
    }
}