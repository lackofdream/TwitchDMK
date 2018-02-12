package com.lackofdream.lab.twitchdmk

import android.content.SharedPreferences
import android.os.Build
import android.view.WindowManager
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_ENABLE_REPEAT
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_TOKEN
import com.lackofdream.lab.twitchdmk.TDMKConstants.Companion.PREF_IRC_USERNAME

class TDMKUtils {
    companion object {
        fun getWindowLayoutType(): Int {
            val windowLayoutType: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                windowLayoutType = WindowManager.LayoutParams.TYPE_PHONE
            }
            return windowLayoutType
        }

        fun canEnableRepeatMode(prefs: SharedPreferences): Boolean {
            return prefs.getBoolean(PREF_ENABLE_REPEAT, false) &&
                    !prefs.getString(PREF_IRC_TOKEN, "").isEmpty() &&
                    !prefs.getString(PREF_IRC_USERNAME, "").isEmpty()
        }
    }
}