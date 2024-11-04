package com.choongang.frombirth_app.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    private val prefs: SharedPreferences

    init {
        // EncryptedSharedPreferences 설정
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // 토큰 저장
    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit().apply {
            putString("ACCESS_TOKEN", accessToken)
            putString("REFRESH_TOKEN", refreshToken)
            apply()
        }
    }

    // 액세스 토큰 가져오기
    fun getAccessToken(): String? = prefs.getString("ACCESS_TOKEN", null)

    // 리프레시 토큰 가져오기
    fun getRefreshToken(): String? = prefs.getString("REFRESH_TOKEN", null)

    // 토큰 삭제 (로그아웃 시 사용)
    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}
