package com.lackofdream.lab.twitchdmkdemo

import android.os.Build
import android.view.WindowManager

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
    }
}