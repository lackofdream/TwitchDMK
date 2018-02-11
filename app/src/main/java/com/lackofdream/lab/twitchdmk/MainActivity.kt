package com.lackofdream.lab.twitchdmk

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SEND_DANMAKU
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SET_TRANSPARENCY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_SELF
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_TEXT
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_DANMAKU_TRANSPARENCY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_CHANNEL
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_ENABLED
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_OVERLAY_ENABLED
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var overlayBtn: ToggleButton
    private lateinit var sendDanmakuBtn: Button
    private lateinit var ircBtn: ToggleButton
    private lateinit var ircChannel: EditText
    private lateinit var transparencyBar: SeekBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        overlayBtn = findViewById(R.id.overlayBtn)
        overlayBtn.setOnCheckedChangeListener({ _, isChecked ->
            val intent = Intent(applicationContext, TDMKOverlayService::class.java)
            if (isChecked) {
                prefs.edit().putBoolean(PREF_OVERLAY_ENABLED, true).apply()
                startService(intent)
            } else {
                prefs.edit().putBoolean(PREF_OVERLAY_ENABLED, false).apply()
                stopService(intent)
            }
        })

        sendDanmakuBtn = findViewById(R.id.sendDanmaku)
        sendDanmakuBtn.setOnClickListener({ _ ->
            val danmakuIntent = Intent(applicationContext, TDMKOverlayService::class.java).putExtra(EXTRA_DANMAKU_TEXT, "対潜部隊との訓練もいいけど、大事な実戦にも出てみたい！　ねぇ、提督？　聞いてる？　むー、聞いてなーい！").setAction(ACTION_SEND_DANMAKU).putExtra(EXTRA_DANMAKU_SELF, true)
            startService(danmakuIntent)
        })

        findViewById<TextView>(R.id.easterEgg).setOnLongClickListener({ _ ->
            sendDanmakuBtn.visibility = View.VISIBLE
            true
        })

        ircChannel = findViewById(R.id.ircChannel)


        ircBtn = findViewById(R.id.ircBtn)
        ircBtn.setOnCheckedChangeListener({ _, isChecked ->
            val intent = Intent(applicationContext, TDMKTwitchIRCService::class.java)
            if (isChecked) {
                prefs.edit().putBoolean(PREF_IRC_ENABLED, true).apply()
                if (ircChannel.text.isNotEmpty())
                    prefs.edit().putString(PREF_IRC_CHANNEL, ircChannel.text.toString().toLowerCase()).apply()
                ircChannel.isEnabled = false
                startService(intent)

            } else {
                prefs.edit().putBoolean(PREF_IRC_ENABLED, false).apply()
                ircChannel.isEnabled = true
                stopService(intent)
            }
        })

        transparencyBar = findViewById(R.id.seekBar)
        transparencyBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            private var timer = Timer()

            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefs.edit().putInt(PREF_DANMAKU_TRANSPARENCY, progress).apply()
                    val intent = Intent(applicationContext, TDMKOverlayService::class.java).setAction(ACTION_SET_TRANSPARENCY)
                    timer.cancel()
                    timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            startService(intent)
                        }
                    }, 200)

                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

    }

    private fun setEditText() {
        if (ircChannel.text.isEmpty())
            ircChannel.setText(prefs.getString(PREF_IRC_CHANNEL, ""))
    }

    override fun onResume() {
        super.onResume()
        title = "老鼠弹幕"
        overlayBtn.isChecked = prefs.getBoolean(PREF_OVERLAY_ENABLED, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(applicationContext)) {
            findViewById<TextView>(R.id.overlayWarn).visibility = View.VISIBLE
            prefs.edit().putBoolean(PREF_OVERLAY_ENABLED, false).apply()
            overlayBtn.isEnabled = false
        } else {
            findViewById<TextView>(R.id.overlayWarn).visibility = View.GONE
            overlayBtn.isEnabled = true
        }
        ircBtn.isChecked = prefs.getBoolean(PREF_IRC_ENABLED, false)
        transparencyBar.progress = prefs.getInt(PREF_DANMAKU_TRANSPARENCY, 255)
        setEditText()
    }
}
