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
import androidx.lifecycle.lifecycleScope
import com.aistudio.dieselstationsms.kxmpzq.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private var biometricCancellationSignal: CancellationSignal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        enableEdgeToEdge()

        geminiApiKey = loadEnvKey("GEMINI_API_KEY")

        requestAllPermissions()

        // ✅ استخدام lifecycleScope بدلاً من Handler
        lifecycleScope.launch {
            delay(2000)
            startSafeService()
        }

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
            Log.e("MainActivity", "Error loading env key", e)
            ""
        }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,   // ✅ إضافة
            Manifest.permission.READ_SMS,       // ✅ إضافة
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed, ALL_PERMISSIONS)
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun startSafeService() {
        if (hasPermissions()) {
            val intent = Intent(this, SMSService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
        } else {
            // سيتم طلب الأذونات تلقائياً
            Log.w("MainActivity", "Permissions not granted, service delayed")
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
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    // ✅ إزالة allowFileAccess (أمان)
                    // settings.allowFileAccess = true

                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            // محاولة إعادة التحميل بعد 3 ثوانٍ (حتى 3 مرات)
                            retryLoad()
                        }
                        private var retryCount = 0
                        private fun retryLoad() {
                            if (retryCount < 3) {
                                retryCount++
                                lifecycleScope.launch {
                                    delay(3000)
                                    loadUrl("http://127.0.0.1:8080/")
                                }
                            }
                        }
                    }
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(WebAppInterface(context, this@MainActivity), "AndroidInterface")

                    lifecycleScope.launch {
                        delay(3000)
                        loadUrl("http://127.0.0.1:8080/")
                    }
                }
            }
        )
    }

    // ==================== Biometric ====================
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

            biometricCancellationSignal = CancellationSignal()
            biometricCancellationSignal?.setOnCancelListener {
                runOnUiThread { onError("cancelled") }
            }

            biometricPrompt.authenticate(biometricCancellationSignal!!, executor, object : BiometricPrompt.AuthenticationCallback() {
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
                    safeEvaluateJs("window.onBiometricResult && window.onBiometricResult(${result.toString()})")
                },
                onError = { error ->
                    result.put("success", false)
                    safeEvaluateJs("window.onBiometricResult && window.onBiometricResult(${result.toString()})")
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

        private fun safeEvaluateJs(script: String) {
            activity.runOnUiThread {
                try {
                    webView.evaluateJavascript(script, null)
                } catch (e: Exception) {
                    Log.e("WebAppInterface", "Failed to evaluate JS", e)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ALL_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startSafeService()
            } else {
                Toast.makeText(this, "بعض الأذونات مطلوبة لتشغيل التطبيق", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        biometricCancellationSignal?.cancel()
        super.onDestroy()
    }
}
