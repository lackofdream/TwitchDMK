package com.lackofdream.lab.twitchdmkdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.RESULT_IRC_TOKEN
import com.lackofdream.lab.twitchdmkdemo.TDMKConstants.Companion.RESULT_IRC_USERNAME

class TDMKTwitchAuthActivity : AppCompatActivity() {

    private lateinit var authWebView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_twitch_auth)

        title = "登录 Twitch 以获取 Token"

        authWebView = findViewById(R.id.authView)

        CookieManager.getInstance().setAcceptCookie(true)
        authWebView.settings.javaScriptEnabled = true

        authWebView.loadUrl("https://twitchapps.com/tmi/")

        authWebView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                if (url != null && url.contains("access_token=")) {
                    val cookie = CookieManager.getInstance().getCookie("https://api.twitch.tv")
                    val username = cookie.split("; ").filter { i -> i.startsWith("login", false) }[0].split("=")[1]
                    val queryString = url.substring(41)
                    val intent = Intent()
                    intent.putExtra(RESULT_IRC_TOKEN, queryString.split("&")[0])
                    intent.putExtra(RESULT_IRC_USERNAME, username)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        }
    }
}
