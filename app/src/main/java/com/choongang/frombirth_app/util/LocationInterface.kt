package com.choongang.frombirth_app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LocationInterface(private val context: Context, private val webView: WebView) {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // JavaScript로 위치 정보를 전달하는 함수
    @SuppressLint("MissingPermission")
    @JavascriptInterface
    fun sendCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                // JavaScript 함수 호출
                val jsCode = "javascript:window.setLocation($latitude, $longitude);"
                webView.post {
                    webView.evaluateJavascript(jsCode, null)
                }
            } ?: run {
                // 위치 정보를 가져올 수 없는 경우
                val jsCode = "javascript:window.showError('Location not available');"
                webView.post {
                    webView.evaluateJavascript(jsCode, null)
                }
            }
        }
    }

    // Android WebView 여부를 확인하는 메서드
    @JavascriptInterface
    fun isAndroidWebView(): Boolean {
        return true
    }

}
