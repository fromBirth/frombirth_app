package com.choongang.frombirth_app.auth

import android.content.Context
import android.webkit.JavascriptInterface

class TokenHandler(private val context: Context) {

    @JavascriptInterface
    fun receiveTokens(accessToken: String, refreshToken: String) {
        // 토큰을 안전하게 저장
        val tokenManager = TokenManager(context)
        tokenManager.saveTokens(accessToken, refreshToken)
    }
}
