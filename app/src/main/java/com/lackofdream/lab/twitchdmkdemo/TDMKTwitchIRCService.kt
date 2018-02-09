package com.lackofdream.lab.twitchdmkdemo

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.ACTION_SEND_DANMAKU
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.EXTRA_DANMAKU_TEXT
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.PREF_IRC_CHANNEL
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.PREF_IRC_ENABLED
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.PREF_IRC_TOKEN
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.PREF_IRC_USERNAME
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.hooks.events.MessageEvent
import java.util.*

class TDMKTwitchIRCService : Service() {

    private lateinit var ircConfiguration: Configuration
    private lateinit var ircBot: PircBotX
    private lateinit var prefs: SharedPreferences

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getString(PREF_IRC_USERNAME, "").isEmpty()) {
            stopSelf()
            return
        }

        ircConfiguration = Configuration.Builder()
                .setAutoNickChange(false)
                .setAutoReconnect(true)
                .setAutoReconnectDelay(500)
                .setAutoReconnectAttempts(5)
                .setOnJoinWhoEnabled(false)
                .setCapEnabled(true)
                .addCapHandlers(listOf(EnableCapHandler("twitch.tv/membership"), EnableCapHandler("twitch.tv/tags"), EnableCapHandler("twitch.tv/commands")))
                .addServer("irc.chat.twitch.tv", 6667)
                .setName(prefs.getString(PREF_IRC_USERNAME, ""))
                .setServerPassword(prefs.getString(PREF_IRC_TOKEN, ""))
                .addAutoJoinChannel("#${prefs.getString(PREF_IRC_CHANNEL, "")}")
                .addListener({ e ->
                    Log.i("TDMK-IRC-V", e.toString())
                    if (e is MessageEvent) {
                        Log.i("TDMK-IRC", e.toString())
                        val intent = Intent(this, TDMKOverlayService::class.java).setAction(ACTION_SEND_DANMAKU).putExtra(EXTRA_DANMAKU_TEXT, e.message)
                        startService(intent)
                    }
                })
                .buildConfiguration()

        ircBot = PircBotX(ircConfiguration)

        val timer = Timer()
        timer.schedule(RunBot(), 0)

    }

    internal inner class RunBot : TimerTask() {
        override fun run() {
            ircBot.startBot()
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
        if (!prefs.getBoolean(PREF_IRC_ENABLED, false)) {
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        val timer = Timer()
        timer.schedule(StopBot(), 0)
        super.onDestroy()
    }
}
