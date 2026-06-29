package com.aistudio.dieselstationsms.kxmpzq

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aistudio.dieselstationsms.kxmpzq.ui.theme.MyApplicationTheme
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private val CAMERA_REQUEST = 101
    private val BIOMETRIC_REQUEST = 102
    private val ALL_PERMISSIONS = 103
    private var geminiApiKey: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // تفعيل تصحيح أخطاء الـ WebView في نسخة التطوير فقط
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        
        enableEdgeToEdge()

        try {
            geminiApiKey = loadEnvKey("GEMINI_API_KEY")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading env key", e)
        }

        requestAllPermissions()

        // تأجيل تشغيل الخدمة لضمان استقرار الأذونات والواجهة
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                startService(Intent(this, SMSService::class.java))
            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting SMSService", e)
            }
        }, 2000) 

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WebViewScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun loadEnvKey(key: String): String {
        return try {
            assets.open(".env").use { stream ->
                BufferedReader(InputStreamReader(stream)).useLines { lines ->
                    lines.mapNotNull { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("$key=")) trimmed.substringAfter("=").trim() else null
                    }.firstOrNull() ?: ""
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS, 
            Manifest.permission.CAMERA
        )
        
        // إضافة إذن التخزين للأجهزة القديمة فقط (Android 9 وأقل)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // طلب الأذونات من المستخدم
        val needed = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (needed.isNotEmpty()) {
            requestPermissions(needed, ALL_PERMISSIONS)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun WebViewScreen(modifier: Modifier = Modifier) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.mediaPlaybackRequiresUserGesture = false

                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            Handler(Looper.getMainLooper()).postDelayed({
                                loadUrl("http://127.0.0.1:8080/")
                            }, 3000)
                        }
                    }
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(WebAppInterface(context, this@MainActivity), "AndroidInterface")

                    Handler(Looper.getMainLooper()).postDelayed({
                        loadUrl("http://127.0.0.1:8080/")
                    }, 3000)
                }
            }
        )
    }

    fun showBiometricPrompt(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = Executors.newSingleThreadExecutor()
            val biometricPrompt = BiometricPrompt.Builder(this)
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setNegativeButton(getString(R.string.biometric_cancel), executor) { _, _ ->
                    runOnUiThread { onError("cancelled") }
                }
                .build()

            val cancellationSignal = CancellationSignal()
            cancellationSignal.setOnCancelListener { runOnUiThread { onError("cancelled") } }

            biometricPrompt.authenticate(cancellationSignal, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    runOnUiThread { onSuccess() }
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    runOnUiThread { onError("failed") }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    runOnUiThread { onError(errString?.toString() ?: "error") }
                }
            })
        } else {
            onError("unsupported")
        }
    }

    inner class WebAppInterface(private val context: Context, private val activity: MainActivity) {
        @JavascriptInterface
        fun requestBiometricAuth(): String {
            val result = JSONObject()
            activity.showBiometricPrompt(
                onSuccess = {
                    result.put("success", true)
                    activity.runOnUiThread { webView.evaluateJavascript("window.onBiometricResult && window.onBiometricResult(${result.toString()})", null) }
                },
                onError = { error ->
                    result.put("success", false)
                    activity.runOnUiThread { webView.evaluateJavascript("window.onBiometricResult && window.onBiometricResult(${result.toString()})", null) }
                }
            )
            return "requested"
        }

        @JavascriptInterface
        fun getGeminiApiKey(): String = geminiApiKey
        
        @JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
