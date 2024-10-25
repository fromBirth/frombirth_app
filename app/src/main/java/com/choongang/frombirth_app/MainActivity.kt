package com.choongang.frombirth_app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.webkit.WebChromeClient

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashView: View

    private val splashMinimumTime = 2000L // 최소 2초

    private var isPageLoaded = false
    private var isTimerFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        splashView = findViewById(R.id.splashView)
        webView = findViewById(R.id.webView)

        // WebView 설정
        webView.settings.run {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            domStorageEnabled = true
        }

        // 카카오톡 앱으로 이동하도록 하는 WebViewClient 설정
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView,request: WebResourceRequest): Boolean {

                if (request.url.scheme == "intent") {
                    try {
                        // Intent 생성
                        val intent = Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)

                        // 실행 가능한 앱이 있으면 앱 실행
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                            return true
                        }

                        // Fallback URL이 있으면 현재 웹뷰에 로딩
                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                        if (fallbackUrl != null) {
                            view.loadUrl(fallbackUrl)
                            return true
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 나머지 서비스 로직 구현

                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoaded = true
                hideSplashIfReady()
            }
        }

        webView.webChromeClient = WebChromeClient()

        // 웹 페이지 로드 (React SPA URL)
        webView.loadUrl("http://172.30.1.18:5173/login")

        // 최소 스플래시 시간 타이머 시작
        Handler(Looper.getMainLooper()).postDelayed({
            isTimerFinished = true
            hideSplashIfReady()
        }, splashMinimumTime)
    }

    // 스플래시 화면 숨기기
    private fun hideSplashIfReady() {
        if (isPageLoaded && isTimerFinished) {
            splashView.visibility = View.GONE
        }
    }
}