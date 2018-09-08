package com.lackofdream.lab.twitchdmk

class TDMKConstants {
    companion object {
        const val PREF_OVERLAY_ENABLED = "overlayenabled"
        const val PREF_IRC_ENABLED = "prefIRCEnabled"
        const val PREF_IRC_CHANNEL = "prefIRCChannel"
        const val PREF_DANMAKU_TRANSPARENCY = "prefDanmakuTransparency"
        const val PREF_DANMAKU_FONT_SIZE = "prefDanmakuFontSize"
        const val PREF_ENABLE_REPEAT = "prefEnableRepeat"
        const val PREF_IRC_TOKEN = "prefIRCToken"
        const val PREF_IRC_USERNAME = "prefIRCUsername"

        const val ACTION_SHOW_OVERLAY = "actionShowOverlay"
        const val ACTION_HIDE_OVERLAY = "actionHideOverlay"

        const val ACTION_SEND_DANMAKU = "actionSendDanmaku"
        const val EXTRA_DANMAKU_TEXT = "extraDanmakuText"
        const val EXTRA_DANMAKU_SELF = "extraDanmakuSelf"

        const val ACTION_SET_TRANSPARENCY = "actionSetTransparency"
        const val ACTION_SET_FONT_SIZE = "actionSetFontSize"

        const val ACTION_ENABLE_REPEAT_MODE = "actionEnableRepeatMode"
        const val ACTION_DISABLE_REPEAT_MODE = "actionDisableRepeatMode"

        const val REQUEST_IRC_TOKEN = 0
        const val RESULT_IRC_TOKEN = "resultIRCToken"
        const val RESULT_IRC_USERNAME = "resultIRCUsername"

        const val ACTION_SEND_IRC_MESSAGE = "actionSendIRCMessage"
        const val EXTRA_MESSAGE_TEXT = "extraMessageText"

    }
}