package com.choongang.frombirth_app

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Kakao SDK 초기화
        KakaoSdk.init(this, getString(R.string.NATIVE_APP_KEY))
    }
}