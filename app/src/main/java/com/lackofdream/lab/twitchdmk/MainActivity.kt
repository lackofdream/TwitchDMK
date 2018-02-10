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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SEND_DANMAKU
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_TEXT
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_CHANNEL
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_ENABLED
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_TOKEN
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_USERNAME
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_OVERLAY_ENABLED
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.REQUEST_IRC_TOKEN
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.RESULT_IRC_TOKEN
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.RESULT_IRC_USERNAME

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var overlayBtn: ToggleButton
    private lateinit var sendDanmakuBtn: Button
    private lateinit var authBtn: Button
    private lateinit var ircBtn: ToggleButton
    private lateinit var ircToken: EditText
    private lateinit var ircName: EditText
    private lateinit var ircChannel: EditText

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
            val danmakuIntent = Intent(applicationContext, TDMKOverlayService::class.java)
            danmakuIntent.putExtra(EXTRA_DANMAKU_TEXT, "対潜部隊との訓練もいいけど、大事な実戦にも出てみたい！　ねぇ、提督？　聞いてる？　むー、聞いてなーい！")
            danmakuIntent.action = ACTION_SEND_DANMAKU
            startService(danmakuIntent)
        })

        authBtn = findViewById(R.id.authBtn)
        authBtn.setOnClickListener({ _ ->
            startActivityForResult(Intent(applicationContext, TDMKTwitchAuthActivity::class.java), REQUEST_IRC_TOKEN)
        })
        authBtn.setOnLongClickListener({_->
            sendDanmakuBtn.visibility = View.VISIBLE
            true
        })

        ircToken = findViewById(R.id.ircToken)
        ircName = findViewById(R.id.ircName)
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
        setEditText()
    }
}
