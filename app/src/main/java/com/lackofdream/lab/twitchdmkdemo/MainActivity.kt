package com.lackofdream.lab.twitchdmkdemo

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.ToggleButton
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.ACTION_SEND_DANMAKU
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.EXTRA_DANMAKU_TEXT
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.PREF_IRC_CHANNEL
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.PREF_IRC_ENABLED
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.PREF_IRC_TOKEN
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.PREF_IRC_USERNAME
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.PREF_OVERLAY_ENABLED

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var overlayBtn: ToggleButton
    private lateinit var sendDanmakuBtn: Button
    private lateinit var authBtn: Button
    private lateinit var ircBtn: ToggleButton
    private lateinit var ircToken: EditText
    private lateinit var ircName: EditText
    private lateinit var ircChannel: EditText

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
            val uri = Uri.parse("https://twitchapps.com/tmi/")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        })

        ircToken = findViewById(R.id.ircToken)
        ircName = findViewById(R.id.ircName)
        ircChannel = findViewById(R.id.ircChannel)


        ircBtn = findViewById(R.id.ircBtn)
        ircBtn.setOnCheckedChangeListener({ _, isChecked ->
            val intent = Intent(applicationContext, TDMKTwitchIRCService::class.java)
            if (isChecked) {
                prefs.edit().putBoolean(PREF_IRC_ENABLED, true).apply()
                if (ircToken.text.isNotEmpty())
                    prefs.edit().putString(PREF_IRC_TOKEN, ircToken.text.toString()).apply()
                if (ircName.text.isNotEmpty())
                    prefs.edit().putString(PREF_IRC_USERNAME, ircName.text.toString()).apply()
                if (ircChannel.text.isNotEmpty())
                    prefs.edit().putString(PREF_IRC_CHANNEL, ircChannel.text.toString()).apply()
                ircToken.isEnabled = false
                ircName.isEnabled = false
                ircChannel.isEnabled = false
                startService(intent)

            } else {
                prefs.edit().putBoolean(PREF_IRC_ENABLED, false).apply()
                ircToken.isEnabled = true
                ircName.isEnabled = true
                ircChannel.isEnabled = true
                stopService(intent)
            }
        })


    }

    private fun setEditText() {
        if (ircName.text.isEmpty())
            ircName.setText(prefs.getString(PREF_IRC_USERNAME, ""))
        if (ircChannel.text.isEmpty())
            ircChannel.setText(prefs.getString(PREF_IRC_CHANNEL, ""))
        if (ircToken.text.isEmpty())
            ircToken.setText(prefs.getString(PREF_IRC_TOKEN, ""))

    }

    override fun onResume() {
        super.onResume()
        overlayBtn.isChecked = prefs.getBoolean(PREF_OVERLAY_ENABLED, false)
        ircBtn.isChecked = prefs.getBoolean(PREF_IRC_ENABLED, false)
        ircToken.isEnabled = !prefs.getBoolean(PREF_IRC_ENABLED, false)
        setEditText()
    }
}
