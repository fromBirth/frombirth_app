package com.choongang.frombirth_app.util

import android.app.Activity
import android.webkit.JavascriptInterface
import com.choongang.frombirth_app.MainActivity

class WebAppInterface(private val activity: Activity) {

    @JavascriptInterface
    fun openFileChooser(isVideo: Boolean) { //안드로이드 파일 탐색기 사용
        (activity as MainActivity).openFileChooser(isVideo)
    }
}