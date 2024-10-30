package com.choongang.frombirth_app

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.choongang.frombirth_app.receiver.AlarmReceiver
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashView: View

    private val splashMinimumTime = 2000L // 최소 2초

    private var isPageLoaded = false
    private var isTimerFinished = false

    //알림 권한 런쳐
    private val notifyPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if(isGranted) {
            // 알림 보내기
            setLocalNotification(this)
        } else {
            Toast.makeText(this, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            // 알림 권한이 거부된 경우 처리
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 알람 실행
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else { // 기기버전이 낮으면, 알림권한이 필요없음
            // 알림 메서드를 호출
            setLocalNotification(this)
        }

        // 배터리 최적화 해제 요청
        if (!isIgnoringBatteryOptimizations(this)) {
            requestIgnoreBatteryOptimizations(this)
        }


        // 상태 바를 투명하게 설정
        window.statusBarColor = resources.getColor(android.R.color.transparent)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

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

                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoaded = true
                hideSplashIfReady()
            }
        }

        webView.webChromeClient = WebChromeClient()

        // 웹 페이지 로드 (React SPA URL) -- 각자 IP 수정
        webView.loadUrl("http://172.30.1.82:5173/login")

        // 최소 스플래시 시간 타이머 시작
        Handler(Looper.getMainLooper()).postDelayed({
            isTimerFinished = true
            hideSplashIfReady()
        }, splashMinimumTime)

        // 뒤로 가기 동작 설정
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish() // 앱 종료
                }
            }
        })
    }

    // 스플래시 화면 숨기기
    private fun hideSplashIfReady() {
        if (isPageLoaded && isTimerFinished) {
            splashView.visibility = View.GONE
        }
    }

    //로컬 알림 기능
    companion object {
        fun setLocalNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calendar 설정(테스트 할 때는 아래의 값을 수정하기)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            // 현재 시간이 지나간 경우 다음 주 월요일로 설정
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }

            // 반복 알람 설정 (주간 반복)
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7, // 7일 간격
                pendingIntent
            )
        }
    }

    // 백그라운드 관련 배터리 최적화 확인
    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }
    // 배터리 최적화 해제 요청
    private fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            startActivity(intent)
        }
    }

}