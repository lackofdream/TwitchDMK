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
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_DISABLE_REPEAT_MODE
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_ENABLE_REPEAT_MODE
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_HIDE_OVERLAY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SEND_DANMAKU
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SEND_IRC_MESSAGE
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SET_FONT_SIZE
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SET_SPEED
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SET_TRANSPARENCY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.ACTION_SHOW_OVERLAY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_SELF
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_DANMAKU_TEXT
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.EXTRA_MESSAGE_TEXT
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_DANMAKU_FONT_SIZE
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_DANMAKU_SPEED
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_DANMAKU_TRANSPARENCY
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_OVERLAY_ENABLED
import com.lackofdream.lab.twitchdmk.TDMKUtils.Companion.getWindowLayoutType
import master.flame.danmaku.controller.IDanmakuView
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
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var windowManager: WindowManager
    private lateinit var danmakuContext: DanmakuContext
    private lateinit var danmakuView: DanmakuView
    private lateinit var prefs: SharedPreferences

    private fun getViewLayoutParams(canTouch: Boolean): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                getWindowLayoutType(),
                if (canTouch) WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT)
    }

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
                    danmakuContext.setDanmakuTransparency(prefs.getInt(PREF_DANMAKU_TRANSPARENCY, 255).toFloat() / 255f)
                    addDanmaku("透明度已变更", true)
                }
                ACTION_SET_FONT_SIZE -> {
                    danmakuContext.setScaleTextSize(prefs.getFloat(PREF_DANMAKU_FONT_SIZE, 1.2f))
                    addDanmaku("字体大小已变更", true)
                }
                ACTION_SET_SPEED -> {
                    danmakuContext.setScrollSpeedFactor(prefs.getFloat(PREF_DANMAKU_SPEED, 1.2f))
                    addDanmaku("滚动速度已变更", true)
                }
                ACTION_ENABLE_REPEAT_MODE -> {
                    windowManager.updateViewLayout(mView, getViewLayoutParams(true))
                    startService(Intent(applicationContext, TDMKTwitchIRCService::class.java)
                            .setAction(ACTION_ENABLE_REPEAT_MODE))
                }
                ACTION_DISABLE_REPEAT_MODE -> {
                    windowManager.updateViewLayout(mView, getViewLayoutParams(false))
                    startService(Intent(applicationContext, TDMKTwitchIRCService::class.java)
                            .setAction(ACTION_DISABLE_REPEAT_MODE))
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("UseSparseArrays")
    private fun initDanmakuConfig() {
        val overlappingEnablePair = HashMap<Int, Boolean>()
        overlappingEnablePair[BaseDanmaku.TYPE_SCROLL_RL] = true
        overlappingEnablePair[BaseDanmaku.TYPE_FIX_TOP] = true

        danmakuView = mView.findViewById(R.id.danmaku)
        danmakuContext = DanmakuContext.create()
                .setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3f * (resources.displayMetrics.density - 0.6f))
                .setDuplicateMergingEnabled(false)
                .setScrollSpeedFactor(prefs.getFloat(PREF_DANMAKU_SPEED, 1.2f))
                .setScaleTextSize(prefs.getFloat(PREF_DANMAKU_FONT_SIZE, 1.2f))
                .preventOverlapping(overlappingEnablePair)
                .setDanmakuTransparency(prefs.getInt(PREF_DANMAKU_TRANSPARENCY, 255).toFloat() / 255f)
        danmakuView.onDanmakuClickListener = object : IDanmakuView.OnDanmakuClickListener {
            override fun onDanmakuClick(danmakus: IDanmakus?): Boolean {
                if (danmakus == null) return false
                val danmaku = danmakus.first()
                Log.i("TDMK-DANMAKU-CLICK", danmaku.text.toString())
                addDanmaku(danmaku.text, true)
                startService(Intent(applicationContext, TDMKTwitchIRCService::class.java).putExtra(EXTRA_MESSAGE_TEXT, danmaku.text.toString()).setAction(ACTION_SEND_IRC_MESSAGE))
                return true
            }

            override fun onViewClick(view: IDanmakuView?): Boolean {
                return false
            }

            override fun onDanmakuLongClick(danmakus: IDanmakus?): Boolean {
                return false
            }
        }
        danmakuView.setCallback(object : master.flame.danmaku.controller.DrawHandler.Callback {
            override fun drawingFinished() {
            }

            override fun danmakuShown(danmaku: BaseDanmaku?) {
            }

            override fun prepared() {
                danmakuView.start()
            }

            override fun updateTimer(timer: DanmakuTimer?) {
            }
        })

        danmakuView.prepare(object : BaseDanmakuParser() {
            override fun parse(): IDanmakus {
                return Danmakus()
            }
        }, danmakuContext)

        Timer().schedule(object : TimerTask() {
            override fun run() {
                addDanmaku("弹幕图层准备就绪", true)
            }
        }, 500)
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
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mView = layoutInflater.inflate(R.layout.danmaku_overlay, null)

        windowManager.addView(mView, getViewLayoutParams(false))
        initDanmakuConfig()
    }


    private fun addDanmaku(text: CharSequence, self: Boolean = false) {
        val danmaku = danmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL)
                ?: return

        danmaku.text = text
        danmaku.padding = 3
        danmaku.priority = if (self) 1 else 0
        danmaku.isLive = true
        danmaku.time = danmakuView.currentTime
        danmaku.textSize = 28f * (resources.displayMetrics.density - 0.6f)
        danmaku.textColor = Color.WHITE
        danmaku.textShadowColor = Color.BLACK
        if (self) danmaku.borderColor = Color.GREEN
        danmakuView.addDanmaku(danmaku)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        dmk_service_on = false
        try {
            danmakuView.release()
            windowManager.removeView(mView)
        } catch (e: UninitializedPropertyAccessException) {

        }
        super.onDestroy()
    }
}
