package com.lackofdream.lab.twitchdmk

import android.app.Activity
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
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SET_FONT_SIZE
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SET_SPEED
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SET_TRANSPARENCY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_SELF
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_TEXT
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_DANMAKU_FONT_SIZE
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_DANMAKU_SPEED
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_DANMAKU_TRANSPARENCY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_ENABLE_REPEAT
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_CHANNEL
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_ENABLED
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_TOKEN
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_USERNAME
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_OVERLAY_ENABLED
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.REQUEST_IRC_TOKEN
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.RESULT_IRC_TOKEN
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.RESULT_IRC_USERNAME
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var sendDanmakuBtn: Button
    private lateinit var serviceBtn: ToggleButton
    private lateinit var ircChannel: EditText
    private lateinit var transparencyBar: SeekBar
    private lateinit var fontSizeBar: SeekBar
    private lateinit var speedBar: SeekBar
    private lateinit var repeatSwitch: Switch
    private lateinit var repeatView: View
    private lateinit var authBtn: Button
    private lateinit var ircToken: EditText
    private lateinit var ircName: EditText


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_IRC_TOKEN -> {
                if (resultCode != Activity.RESULT_OK || data == null) {
                    return
                }
                prefs.edit().putString(PREF_IRC_TOKEN, "oauth:${data.getStringExtra(RESULT_IRC_TOKEN)}").apply()
                prefs.edit().putString(PREF_IRC_USERNAME, data.getStringExtra(RESULT_IRC_USERNAME)).apply()
                ircToken.setText(prefs.getString(PREF_IRC_TOKEN, ""))
                ircName.setText(prefs.getString(PREF_IRC_USERNAME, ""))
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        sendDanmakuBtn = findViewById(R.id.sendDanmaku)
        sendDanmakuBtn.setOnClickListener { _ ->
            val danmakuIntent = Intent(applicationContext, TDMKOverlayService::class.java).putExtra(EXTRA_DANMAKU_TEXT, "対潜部隊との訓練もいいけど、大事な実戦にも出てみたい！　ねぇ、提督？　聞いてる？　むー、聞いてなーい！").setAction(ACTION_SEND_DANMAKU).putExtra(EXTRA_DANMAKU_SELF, true)
            startService(danmakuIntent)
        }

        findViewById<TextView>(R.id.easterEgg).setOnLongClickListener { _ ->
            sendDanmakuBtn.visibility = View.VISIBLE
            true
        }

        ircChannel = findViewById(R.id.ircChannel)


        serviceBtn = findViewById(R.id.serviceBtn)
        serviceBtn.setOnCheckedChangeListener { _, isChecked ->
            val ircIntent = Intent(applicationContext, TDMKTwitchIRCService::class.java)
            val overlayIntent = Intent(applicationContext, TDMKOverlayService::class.java)
            if (isChecked) {
                if (ircChannel.text.isEmpty())
                    return@setOnCheckedChangeListener
                prefs.edit().putBoolean(PREF_IRC_ENABLED, true).apply()
                prefs.edit().putBoolean(PREF_OVERLAY_ENABLED, true).apply()
                prefs.edit().putString(PREF_IRC_CHANNEL, ircChannel.text.toString().toLowerCase()).apply()
                startService(ircIntent)
                startService(overlayIntent)
                ircChannel.isEnabled = false
                repeatSwitch.isEnabled = false
                authBtn.isEnabled = false
            } else {
                prefs.edit().putBoolean(PREF_OVERLAY_ENABLED, false).apply()
                prefs.edit().putBoolean(PREF_IRC_ENABLED, false).apply()
                stopService(overlayIntent)
                stopService(ircIntent)
                ircChannel.isEnabled = true
                repeatSwitch.isEnabled = true
                authBtn.isEnabled = true
            }
        }

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

        fontSizeBar = findViewById(R.id.seekBar2)
        fontSizeBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            private var timer = Timer()

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefs.edit().putFloat(PREF_DANMAKU_FONT_SIZE, progress.toFloat()/10+0.5f).apply()
                    val intent = Intent(applicationContext, TDMKOverlayService::class.java).setAction(ACTION_SET_FONT_SIZE)
                    timer.cancel()
                    timer = Timer()
                    timer.schedule(object: TimerTask() {
                        override fun run() {
                            startService(intent)
                        }
                    }, 200)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        speedBar = findViewById(R.id.seekBar3)
        speedBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            private var timer = Timer()

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefs.edit().putFloat(PREF_DANMAKU_SPEED, 1.5f - progress.toFloat()/10).apply()
                    val intent = Intent(applicationContext, TDMKOverlayService::class.java).setAction(ACTION_SET_SPEED)
                    timer.cancel()
                    timer = Timer()
                    timer.schedule(object: TimerTask() {
                        override fun run() {
                            startService(intent)
                        }
                    }, 200)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        repeatSwitch = findViewById(R.id.repeatSwitch)
        repeatView = findViewById(R.id.repeatModeView)
        authBtn = findViewById(R.id.authBtn)
        ircToken = findViewById(R.id.ircToken)
        ircName = findViewById(R.id.ircName)
        repeatSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prefs.edit().putBoolean(PREF_ENABLE_REPEAT, true).apply()
                repeatView.visibility = View.VISIBLE
            } else {
                prefs.edit().putBoolean(PREF_ENABLE_REPEAT, false).apply()
                repeatView.visibility = View.GONE
            }
        }
        authBtn.setOnClickListener { _ ->
            startActivityForResult(Intent(applicationContext, TDMKTwitchAuthActivity::class.java), REQUEST_IRC_TOKEN)
        }
    }

    private fun setEditText() {
        ircToken.setText(prefs.getString(PREF_IRC_TOKEN, ""))
        ircName.setText(prefs.getString(PREF_IRC_USERNAME, ""))
        if (ircChannel.text.isEmpty())
            ircChannel.setText(prefs.getString(PREF_IRC_CHANNEL, ""))
    }

    override fun onResume() {
        super.onResume()
        title = "老鼠弹幕"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(applicationContext)) {
            findViewById<TextView>(R.id.overlayWarn).visibility = View.VISIBLE
            prefs.edit().putBoolean(PREF_OVERLAY_ENABLED, false).apply()
            serviceBtn.isEnabled = false
            repeatSwitch.isEnabled = false
            return
        } else {
            findViewById<TextView>(R.id.overlayWarn).visibility = View.GONE
            serviceBtn.isEnabled = true
            repeatSwitch.isEnabled = true
        }
        val serviceEnabled = prefs.getBoolean(PREF_IRC_ENABLED, false)
        serviceBtn.isChecked = serviceEnabled
        authBtn.isEnabled = !serviceEnabled
        repeatSwitch.isEnabled = !serviceEnabled
        transparencyBar.progress = prefs.getInt(PREF_DANMAKU_TRANSPARENCY, 255)
        repeatSwitch.isChecked = prefs.getBoolean(PREF_ENABLE_REPEAT, false)
        setEditText()
    }
}
