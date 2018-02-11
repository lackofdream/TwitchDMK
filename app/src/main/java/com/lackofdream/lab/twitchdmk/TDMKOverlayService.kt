package com.lackofdream.lab.twitchdmk

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_HIDE_OVERLAY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SEND_DANMAKU
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SET_TRANSPARENCY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SHOW_OVERLAY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_SELF
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_TEXT
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_DANMAKU_TRANSPARENCY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_OVERLAY_ENABLED
import com.lackofdream.lab.twitchdmk.TDMKUtils.Companion.getWindowLayoutType
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.ui.widget.DanmakuView
import java.util.*


class TDMKOverlayService : Service() {

    companion object {
        var dmk_service_on = false
    }

    private lateinit var mView: View
    private lateinit var mInflater: LayoutInflater
    private lateinit var mManager: WindowManager
    private lateinit var mParams: WindowManager.LayoutParams
    private lateinit var mDanmakuContext: DanmakuContext
    private lateinit var mDanmakuView: DanmakuView
    private lateinit var prefs: SharedPreferences

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(applicationContext)) {
            Log.e("OVERLAY", "Cannot get overlay permission")
            stopSelf()
        } else if (!dmk_service_on) {
            stopSelf()
        } else if (intent != null && intent.action != null) {
            when (intent.action) {
                ACTION_SHOW_OVERLAY -> {
                    mView.visibility = View.VISIBLE
                }
                ACTION_HIDE_OVERLAY -> {
                    mView.visibility = View.GONE
                }
                ACTION_SEND_DANMAKU -> {
                    val text = intent.getStringExtra(EXTRA_DANMAKU_TEXT)
                    val self = intent.getBooleanExtra(EXTRA_DANMAKU_SELF, false)
                    addDanmaku(text, self)
                }
                ACTION_SET_TRANSPARENCY -> {
                    mDanmakuContext.setDanmakuTransparency(prefs.getInt(PREF_DANMAKU_TRANSPARENCY, 255).toFloat() / 255f)
                    addDanmaku("透明度已变更", true)
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("UseSparseArrays")
    private fun initDanmakuConfig() {
        val overlappingEnablePair = HashMap<Int, Boolean>()
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true)
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true)

        mDanmakuView = mView.findViewById(R.id.danmaku)
        mDanmakuContext = DanmakuContext.create()
                .setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3f * (resources.displayMetrics.density - 0.6f))
                .setDuplicateMergingEnabled(false)
                .setScrollSpeedFactor(1.2f)
                .setScaleTextSize(1.2f)
                .preventOverlapping(overlappingEnablePair)
                .setDanmakuTransparency(prefs.getInt(PREF_DANMAKU_TRANSPARENCY, 255).toFloat() / 255f)
        mDanmakuView.setCallback(object : master.flame.danmaku.controller.DrawHandler.Callback {
            override fun drawingFinished() {
            }

            override fun danmakuShown(danmaku: BaseDanmaku?) {
            }

            override fun prepared() {
                mDanmakuView.start()
            }

            override fun updateTimer(timer: DanmakuTimer?) {
            }
        })

        mDanmakuView.prepare(object : BaseDanmakuParser() {
            override fun parse(): IDanmakus {
                return Danmakus()
            }
        }, mDanmakuContext)

        Timer().schedule(object : TimerTask() {
            override fun run() {
                addDanmaku("弹幕图层准备就绪", true)
            }
        }, 200)
    }

    @SuppressLint("InflateParams")
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(applicationContext)) {
            Log.e("OVERLAY", "Cannot get overlay permission")
            stopSelf()
            return
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!prefs.getBoolean(PREF_OVERLAY_ENABLED, false)) {
            stopSelf()
            return
        }
        dmk_service_on = true
        mParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                getWindowLayoutType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT)
        mParams.gravity = Gravity.TOP or Gravity.START
        mManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mView = mInflater.inflate(R.layout.danmaku_overlay, null)

        mManager.addView(mView, mParams)
        initDanmakuConfig()
    }


    private fun addDanmaku(text: CharSequence, self: Boolean = false) {
        val danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL) ?: return

        danmaku.text = text
        danmaku.padding = 3
        danmaku.priority = if (self) 1 else 0
        danmaku.isLive = true
        danmaku.time = mDanmakuView.currentTime
        danmaku.textSize = 28f * (resources.displayMetrics.density - 0.6f)
        danmaku.textColor = Color.WHITE
        danmaku.textShadowColor = Color.BLACK
        if (self) danmaku.borderColor = Color.GREEN
        mDanmakuView.addDanmaku(danmaku)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        dmk_service_on = false
        try {

            mDanmakuView.release()
            mManager.removeView(mView)
        } catch (e: UninitializedPropertyAccessException) {

        }
        super.onDestroy()
    }
}
