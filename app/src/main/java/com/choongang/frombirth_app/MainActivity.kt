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
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.choongang.frombirth_app.auth.TokenManager
import com.choongang.frombirth_app.receiver.AlarmReceiver
import com.choongang.frombirth_app.util.WebAppInterface
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splashView: View

    private val splashMinimumTime = 2000L // 최소 2초

    private var isPageLoaded = false
    private var isTimerFinished = false

    private var frontendUrl = BuildConfig.FRONTEND_URL;

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1

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

        splashView = findViewById(R.id.splashView)
        webView = findViewById(R.id.webView)

        // WebView 설정
        webView.settings.run {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            domStorageEnabled = true
        }

        // 쿠키 설정
        setTokensAsCookies()
        // JavaScript Interface 설정
        webView.addJavascriptInterface(TokenHandler(this), "Android") //토큰
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidFileChooser") //파일 탐색기

        // 카카오톡 앱으로 이동하도록 하는 WebViewClient 설정
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // URL이 변경될 때마다 호출
                val currentUrl = request.url.toString()
                Log.d("WebView", "현재 URL: $currentUrl")
                if (request.url.scheme == "intent") {
                    try {
                        val intent = Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                            return true
                        }
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

        // 안드로이드 파일 탐색기 설정
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback = filePathCallback
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), FILE_CHOOSER_REQUEST_CODE)
                return true
            }
        }

        // 웹 페이지 로드 (React SPA URL) -- 각자 IP 수정
        webView.loadUrl("$frontendUrl/login")

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

    // 안드로이드 파일 선택
    fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), FILE_CHOOSER_REQUEST_CODE)
    }

    // 파일 명 가져오기
    private fun getFileName(uri: Uri): String {
        var name = "uploadedImage.jpg" // 기본 파일명 설정
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    // 파일 타입 가져오기
    private fun getFileMimeType(uri: Uri): String {
        return contentResolver.getType(uri) ?: "application/octet-stream" // 기본 MIME 타입 설정
    }

    // 파일 선택 콜백을 위한 액티비티 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            fileChooserCallback?.onReceiveValue(arrayOf(selectedImageUri ?: Uri.EMPTY))

            selectedImageUri?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                val byteArray = inputStream?.readBytes()
                inputStream?.close()
                val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                // 파일명과 MIME 타입 가져오기
                val fileName = getFileName(uri)
                val mimeType = getFileMimeType(uri)

                // JavaScript 함수로 파일명, MIME 타입, base64 데이터 전달
                webView.evaluateJavascript("javascript:handleBase64Image('$base64String', '$fileName', '$mimeType');", null)
            }
        } else {
            fileChooserCallback?.onReceiveValue(null)
        }
        fileChooserCallback = null
        super.onActivityResult(requestCode, resultCode, data)
    }

    // 저장된 토큰을 쿠키로 설정하는 함수  -- 각자 IP 수정
    private fun setTokensAsCookies() {
        val tokenManager = TokenManager(this)
        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        val domain = frontendUrl

        // 쿠키 매니저 활성화
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        // 액세스 토큰과 리프레시 토큰 쿠키 설정
        accessToken?.let {
            cookieManager.setCookie(domain, "accessToken=$it; path=/")
        }
        refreshToken?.let {
            cookieManager.setCookie(domain, "refreshToken=$it; path=/")
        }

        // 쿠키 강제 동기화
        cookieManager.flush()
    }
    // 스플래시 화면 숨기기
    private fun hideSplashIfReady() {
        if (isPageLoaded && isTimerFinished) {
            splashView.visibility = View.GONE
        }
    }
    // JavaScript Interface 클래스
    class TokenHandler(private val context: Context) {
        private val tokenManager = TokenManager(context)

        @JavascriptInterface
        fun receiveTokens(accessToken: String, refreshToken: String) {
            tokenManager.saveTokens(accessToken, refreshToken)
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

}