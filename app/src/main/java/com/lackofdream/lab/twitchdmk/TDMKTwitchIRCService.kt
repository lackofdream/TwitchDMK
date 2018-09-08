package com.lackofdream.lab.twitchdmk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_DISABLE_REPEAT_MODE
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_ENABLE_REPEAT_MODE
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SEND_DANMAKU
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SEND_IRC_MESSAGE
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_SELF
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_TEXT
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_MESSAGE_TEXT
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

abstract class IRCTask(protected val bot: PircBotX) : AsyncTask<String, Unit, Unit>()

class IRCStartBotTask(bot: PircBotX) : IRCTask(bot) {
    override fun doInBackground(vararg p0: String?) {
        Log.i("TDMK-IRC-ASYNC", "start bot")
        try {
            bot.startBot()
        } catch (e: IOException) {
        }
    }
}

class IRCStopBotTask(bot: PircBotX) : IRCTask(bot) {
    override fun doInBackground(vararg p0: String?) {
        Log.i("TDMK-IRC-ASYNC", "stop bot")
        try {
            bot.stopBotReconnect()
            try {
                bot.sendIRC().quitServer()
            } catch (_: Exception) {
            }
        } catch (_: UninitializedPropertyAccessException) {
        }
    }
}

class SendIRCMessageTask(bot: PircBotX) : IRCTask(bot) {
    override fun doInBackground(vararg param: String?) {
        val channel = param[0]
        val message = param[1]
        try {
            bot.sendIRC().message(channel, message)
        } catch (e: Exception) {
        }
    }
}

class TDMKTwitchIRCService : Service() {

    companion object {
        private const val NOTIFY_ID = 1
        private const val NOTIFY_CHANNEL_ID = "twitch_irc_01"
        var irc_service_on = false
        var repeat_mode_on = false
    }

    private lateinit var ircConfiguration: Configuration
    private lateinit var ircBot: PircBotX
    private lateinit var prefs: SharedPreferences
    private lateinit var mNotificationmanager: NotificationManager
    private lateinit var mNotifyBuilder: NotificationCompat.Builder
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var startBotTask: IRCStartBotTask
    private lateinit var stopBotTask: IRCStopBotTask
    private lateinit var notificationText: CharSequence

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationBuild(context: Context, channel: String): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, channel)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(context)
        }
    }

    private fun initNotification() {


        mNotificationmanager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(NOTIFY_CHANNEL_ID, "老鼠弹幕", NotificationManager.IMPORTANCE_LOW)
            notificationChannel.description = "与Twitch聊天服务器的连接状态"
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            mNotificationmanager.createNotificationChannel(notificationChannel)
        }

        notificationText = "正在连接至Twitch聊天室…"
        mNotifyBuilder = createNotificationBuild(applicationContext, NOTIFY_CHANNEL_ID)
                .setContentTitle("老鼠弹幕")
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_stat_sort)
                .setAutoCancel(true)
                .addAction(0, "打开主界面",PendingIntent.getActivity(
                        this@TDMKTwitchIRCService,
                        0,
                        Intent(this@TDMKTwitchIRCService,
                                MainActivity::class.java),
                        PendingIntent.FLAG_CANCEL_CURRENT))

        startForeground(NOTIFY_ID, mNotifyBuilder.build())

    }

    private fun getRandomLoginName(): String {
        val ret = String.format("%06d", Math.abs(Random().nextInt()) % 1000000)
        Log.i("TDMK-IRC-RANDOM-NAME", ret)
        return "justinfan114514$ret"
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        irc_service_on = true
        initNotification()

        initIRCConfiguration()

        ircBot = PircBotX(ircConfiguration)
        startBotTask = IRCStartBotTask(ircBot)
        stopBotTask = IRCStopBotTask(ircBot)

        startBotTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun setNotificationContentIntent() {
        if (!ircBot.isConnected || !TDMKUtils.canEnableRepeatMode(prefs)) {
            mNotifyBuilder.setContentIntent(PendingIntent.getActivity(applicationContext, 0, Intent(), 0))
        } else {
            if (!repeat_mode_on) {
                mNotifyBuilder
                        .setContentText("$notificationText 点击开启复读模式")
                        .setContentIntent(PendingIntent.getService(this@TDMKTwitchIRCService, 0,
                                Intent(this@TDMKTwitchIRCService,
                                        TDMKOverlayService::class.java)
                                        .setAction(TDMKConstants.ACTION_ENABLE_REPEAT_MODE),
                                PendingIntent.FLAG_CANCEL_CURRENT))
            } else {
                mNotifyBuilder
                        .setContentText("$notificationText 点击关闭复读模式")
                        .setContentIntent(PendingIntent.getService(this@TDMKTwitchIRCService, 0,
                                Intent(this@TDMKTwitchIRCService,
                                        TDMKOverlayService::class.java).
                                        setAction(TDMKConstants.ACTION_DISABLE_REPEAT_MODE),
                                PendingIntent.FLAG_CANCEL_CURRENT))
            }
        }
        mNotificationmanager.notify(NOTIFY_ID, mNotifyBuilder.build())
    }

    private fun initIRCConfiguration() {

        val ircConfigBuilder = Configuration.Builder()
                .setAutoNickChange(false)
                .setAutoReconnect(true)
                .setAutoReconnectDelay(2000)
                .setAutoReconnectAttempts(15)
                .setOnJoinWhoEnabled(false)
                .setCapEnabled(true)
                .addCapHandlers(listOf(EnableCapHandler("twitch.tv/membership"),
                        EnableCapHandler("twitch.tv/tags"),
                        EnableCapHandler("twitch.tv/commands")))
                .addServer("irc.chat.twitch.tv", 6667)
                .addAutoJoinChannel("#${prefs.getString(PREF_IRC_CHANNEL, "")}")
                .addListener(object : ListenerAdapter() {
                    override fun onDisconnect(event: DisconnectEvent?) {
                        Log.e("TDMK-IRC-LISTENER", "disconnected: ${event?.disconnectException?.message}")
                        notificationText = "与Twitch聊天室的连接已断开"
                        mNotificationmanager.notify(NOTIFY_ID, mNotifyBuilder
                                .setContentText(notificationText)
                                .setSmallIcon(R.drawable.ic_stat_short_text)
                                .build())
                        setNotificationContentIntent()
                    }


                    override fun onMessage(event: MessageEvent?) {
                        if (event == null) return
                        var isSelf = false
                        if (event.user?.nick?.equals(prefs.getString(PREF_IRC_USERNAME, "")) == true) {
                            Log.i("TDMK-IRC-MESSAGE", "self message: ${event.message}")
                            isSelf = true
                        }
                        val intent = Intent(applicationContext, TDMKOverlayService::class.java).setAction(ACTION_SEND_DANMAKU).putExtra(EXTRA_DANMAKU_TEXT, event.message).putExtra(EXTRA_DANMAKU_SELF, isSelf)
                        startService(intent)
                    }

                    override fun onConnect(event: ConnectEvent?) {
                        Log.i("TDMK-IRC-LISTENER", "connected")
                        notificationText = "已连接到Twitch聊天室"
                        mNotificationmanager.notify(NOTIFY_ID, mNotifyBuilder
                                .setContentText(notificationText)
                                .setSmallIcon(R.drawable.ic_stat_sort)
                                .build())
                        setNotificationContentIntent()
                    }

                    override fun onConnectAttemptFailed(event: ConnectAttemptFailedEvent?) {
                        Log.e("TDKM-IRC-LISTENER", "attempt connect failed, remain ${event?.remainingAttempts}")
                        if (event != null && event.remainingAttempts == 0) {
                            notificationText = "连接Twitch聊天室失败，请重新开启服务以重试"
                            mNotificationmanager.notify(NOTIFY_ID, mNotifyBuilder
                                    .setContentText(notificationText)
                                    .setSmallIcon(R.drawable.ic_stat_short_text)
                                    .build())
                        } else {
                            notificationText = "连接Twitch聊天室失败，正在重试..."
                            mNotificationmanager.notify(NOTIFY_ID, mNotifyBuilder
                                    .setContentText(notificationText)
                                    .setSmallIcon(R.drawable.ic_stat_short_text)
                                    .build())
                        }
                        setNotificationContentIntent()
                    }
                })
        if (TDMKUtils.canEnableRepeatMode(prefs)) {
            ircConfigBuilder.setName(prefs.getString(PREF_IRC_USERNAME, "")).serverPassword = prefs.getString(PREF_IRC_TOKEN, "")
        } else {
            ircConfigBuilder
                    .setName(getRandomLoginName()).serverPassword = "kappa"
        }
        ircConfiguration = ircConfigBuilder.buildConfiguration()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefs.getBoolean(PREF_IRC_ENABLED, false) || !irc_service_on) {
            stopSelf()
        }
        if (intent == null) return super.onStartCommand(intent, flags, startId)
        when (intent.action) {
            ACTION_SEND_IRC_MESSAGE -> run {
                if (!ircBot.isConnected) return@run

                val channel = "#${prefs.getString(PREF_IRC_CHANNEL, "")}"
                val message = intent.getStringExtra(EXTRA_MESSAGE_TEXT)
                SendIRCMessageTask(ircBot).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, channel, message)
            }
            ACTION_ENABLE_REPEAT_MODE -> {
                repeat_mode_on = true
                setNotificationContentIntent()
            }
            ACTION_DISABLE_REPEAT_MODE -> {
                repeat_mode_on = false
                setNotificationContentIntent()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        irc_service_on = false
        stopBotTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        startBotTask.cancel(true)
        stopForeground(false)
        super.onDestroy()
    }
}
