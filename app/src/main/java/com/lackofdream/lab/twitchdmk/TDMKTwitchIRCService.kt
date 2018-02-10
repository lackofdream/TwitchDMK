package com.lackofdream.lab.twitchdmk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SEND_DANMAKU
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_TEXT
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_CHANNEL
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_ENABLED
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_TOKEN
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_USERNAME
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.ConnectAttemptFailedEvent
import org.pircbotx.hooks.events.ConnectEvent
import org.pircbotx.hooks.events.DisconnectEvent
import org.pircbotx.hooks.events.MessageEvent
import java.io.IOException
import java.util.*

class TDMKTwitchIRCService : Service() {

    companion object {
        private val NOTIFY_ID = 1
        private val NOTIFY_CHANNEL_ID = "twitch_irc_01"
        var irc_service_on = false
    }

    private lateinit var ircConfiguration: Configuration
    private lateinit var ircBot: PircBotX
    private lateinit var prefs: SharedPreferences
    private lateinit var mNotificationmanager: NotificationManager
    private lateinit var mNotifyBuilder: NotificationCompat.Builder
    private lateinit var mChannel: NotificationChannel
    private val timer: Timer = Timer()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    fun createNotificationBuild(context: Context, channel: String): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(applicationContext, NOTIFY_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(applicationContext)
        }
    }

    fun initNotification() {


        mNotificationmanager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = NotificationChannel(NOTIFY_CHANNEL_ID, "老鼠弹幕", NotificationManager.IMPORTANCE_LOW)
            mChannel.description = "与Twitch聊天服务器的连接状态"
            mChannel.enableLights(false)
            mChannel.enableVibration(false)
            mNotificationmanager.createNotificationChannel(mChannel)
        }

        mNotifyBuilder = createNotificationBuild(applicationContext, NOTIFY_CHANNEL_ID)
                .setContentTitle("老鼠弹幕")
                .setSmallIcon(R.drawable.ic_stat_sort)
                .setAutoCancel(false)
                .setContentIntent(PendingIntent.getActivity(this@TDMKTwitchIRCService, 0, Intent(this@TDMKTwitchIRCService, MainActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT))

        startForeground(NOTIFY_ID, mNotifyBuilder.build())

    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getString(PREF_IRC_USERNAME, "").isEmpty()) {
            stopSelf()
            return
        }

        irc_service_on = true
        initNotification()

        ircConfiguration = Configuration.Builder()
                .setAutoNickChange(false)
                .setAutoReconnect(true)
                .setAutoReconnectDelay(2000)
                .setAutoReconnectAttempts(15)
                .setOnJoinWhoEnabled(false)
                .setCapEnabled(true)
                .addCapHandlers(listOf(EnableCapHandler("twitch.tv/membership"), EnableCapHandler("twitch.tv/tags"), EnableCapHandler("twitch.tv/commands")))
                .addServer("irc.chat.twitch.tv", 6667)
                .setName(prefs.getString(PREF_IRC_USERNAME, ""))
                .setServerPassword(prefs.getString(PREF_IRC_TOKEN, ""))
                .addAutoJoinChannel("#${prefs.getString(PREF_IRC_CHANNEL, "")}")
                .addListener(object : ListenerAdapter() {
                    override fun onDisconnect(event: DisconnectEvent?) {
                        Log.e("TDMK-IRC-LISTENER", "disconnected: ${event?.disconnectException?.message}")
                        mNotificationmanager.notify(NOTIFY_ID, mNotifyBuilder
                                .setContentText("与Twitch聊天室的连接已断开")
                                .setSmallIcon(R.drawable.ic_stat_short_text)
                                .build())
                    }


                    override fun onMessage(event: MessageEvent?) {
                        if (event == null) return
                        val intent = Intent(applicationContext, TDMKOverlayService::class.java).setAction(ACTION_SEND_DANMAKU).putExtra(EXTRA_DANMAKU_TEXT, event.message)
                        startService(intent)
                    }

                    override fun onConnect(event: ConnectEvent?) {
                        Log.i("TDMK-IRC-LISTENER", "connected")
                        mNotificationmanager.notify(NOTIFY_ID, mNotifyBuilder
                                .setContentText("已连接到Twitch聊天室")
                                .setSmallIcon(R.drawable.ic_stat_sort)
                                .build())
                    }

                    override fun onConnectAttemptFailed(event: ConnectAttemptFailedEvent?) {
                        Log.e("TDKM-IRC-LISTENER", "attempt connect failed, remain ${event?.remainingAttempts}")
                        if (event != null && event.remainingAttempts == 0)
                            mNotificationmanager.notify(NOTIFY_ID, mNotifyBuilder
                                    .setContentText("连接Twitch聊天室失败，请重新开启服务以重试")
                                    .setSmallIcon(R.drawable.ic_stat_short_text)
                                    .build())
                        else
                            mNotificationmanager.notify(NOTIFY_ID, mNotifyBuilder
                                    .setContentText("连接Twitch聊天室失败，正在重试...")
                                    .setSmallIcon(R.drawable.ic_stat_short_text)
                                    .build())
                    }
                })
                .buildConfiguration()

        ircBot = PircBotX(ircConfiguration)

        timer.schedule(RunBot(), 0)

    }

    internal inner class RunBot : TimerTask() {
        override fun run() {
            try {
                ircBot.startBot()
            } catch (e: IOException) {
                stopSelf()
            }
        }
    }

    internal inner class StopBot : TimerTask() {
        override fun run() {
            try {
                ircBot.stopBotReconnect()
                try {
                    ircBot.sendIRC().quitServer()
                } catch (_: Exception) {
                }
            } catch (_: UninitializedPropertyAccessException) {

            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefs.getBoolean(PREF_IRC_ENABLED, false) || !irc_service_on) {
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        irc_service_on = false
        val timer = Timer()
        timer.schedule(StopBot(), 0)
        stopForeground(false)
        super.onDestroy()
    }
}
