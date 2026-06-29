package com.aistudio.dieselstationsms.kxmpzq

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aistudio.dieselstationsms.kxmpzq.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val ALL_PERMISSIONS = 103
        private const val SERVICE_START_DELAY_MS = 1500L
        private const val WEBVIEW_LOAD_DELAY_MS = 2500L
        private const val WEBVIEW_RETRY_INTERVAL_MS = 3000L
        private const val MAX_WEBVIEW_RETRIES = 5
    }

    private lateinit var webView: WebView
    private var geminiApiKey: String = ""
    private var serverReady = false
    private var webViewRetryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable WebView debugging in debug builds only
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        enableEdgeToEdge()

        // Load API key from .env file
        try {
            geminiApiKey = loadEnvKey("GEMINI_API_KEY")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading env key", e)
        }

        // Request all required permissions including RECEIVE_SMS and READ_SMS
        // FIXED: Added RECEIVE_SMS and READ_SMS which were missing
        requestAllPermissions()

        // Start the SMS service with proper delay
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                startService(Intent(this, SMSService::class.java))
                Log.d(TAG, "SMSService started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting SMSService", e)
            }
        }, SERVICE_START_DELAY_MS)

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
            Log.w(TAG, "Could not load .env key $key: ${e.message}")
            ""
        }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,      // FIXED: Was missing!
            Manifest.permission.READ_SMS,           // FIXED: Was missing!
            Manifest.permission.CAMERA
        )

        // Add storage permissions for older devices (Android 9 and below)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Request only permissions that are not already granted
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
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
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    // FIXED: Removed allowFileAccess for security
                    // FIXED: Removed javaScriptCanOpenWindowsAutomatically for security
                    // FIXED: Removed mediaPlaybackRequiresUserGesture = false for security

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            serverReady = true
                            webViewRetryCount = 0
                            Log.d(TAG, "WebView page finished loading: $url")
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            Log.w(TAG, "WebView error $errorCode: $description on $failingUrl")
                            serverReady = false
                            if (webViewRetryCount < MAX_WEBVIEW_RETRIES) {
                                webViewRetryCount++
                                Handler(Looper.getMainLooper()).postDelayed({
                                    Log.d(TAG, "Retrying WebView load (attempt $webViewRetryCount/$MAX_WEBVIEW_RETRIES)")
                                    loadUrl("http://127.0.0.1:8080/")
                                }, WEBVIEW_RETRY_INTERVAL_MS)
                            } else {
                                Log.e(TAG, "Max WebView retries reached. Server may not be running.")
                                // Show error page or fallback UI
                                evaluateJavascript(
                                    "document.body.innerHTML = '<h1 style=\'text-align:center;padding:50px;\'>⚠️ تعذر الاتصال بالخادم المحلي<br><small>يرجى إعادة تشغيل التطبيق</small></h1>'",
                                    null
                                )
                            }
                        }
                    }
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(
                        WebAppInterface(context, this@MainActivity),
                        "AndroidInterface"
                    )

                    // Load WebView with delay to ensure server is ready
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadUrl("http://127.0.0.1:8080/")
                    }, WEBVIEW_LOAD_DELAY_MS)
                }
            }
        )
    }

    // FIXED: Biometric prompt using AndroidX Biometric library
    // For minSdk=24, we need androidx.biometric which supports API 14+
    // Note: Add dependency: implementation("androidx.biometric:biometric:1.1.0")
    fun showBiometricPrompt(onSuccess: () -> Unit, onError: (String) -> Unit) {
        // For Android 9+ (API 28+), use BiometricPrompt
        // For older devices, show error (or use fingerprint manager if needed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = androidx.biometric.BiometricPrompt(
                    this,
                    executor,
                    object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: androidx.biometric.BiometricPrompt.AuthenticationResult
                        ) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onError("failed")
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            onError(errString.toString())
                        }
                    }
                )

                val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.biometric_prompt_title))
                    .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                    .setNegativeButtonText(getString(R.string.biometric_cancel))
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Biometric error", e)
                onError("unsupported")
            }
        } else {
            onError("unsupported")
        }
    }

    inner class WebAppInterface(
        private val context: Context,
        private val activity: MainActivity
    ) {

        // FIXED: Proper biometric auth with callback bridge
        // Uses a pending callback pattern to avoid race conditions
        private var pendingBiometricCallback: ((Boolean, String) -> Unit)? = null

        @JavascriptInterface
        fun requestBiometricAuth(): String {
            // Store callback and trigger biometric
            activity.runOnUiThread {
                activity.showBiometricPrompt(
                    onSuccess = {
                        val result = JSONObject().apply {
                            put("success", true)
                            put("message", "authenticated")
                        }
                        safeEvaluateJs("window.onBiometricResult && window.onBiometricResult(${result.toString()})\n")
                    },
                    onError = { error ->
                        val result = JSONObject().apply {
                            put("success", false)
                            put("error", error)
                        }
                        safeEvaluateJs("window.onBiometricResult && window.onBiometricResult(${result.toString()})\n")
                    }
                )
            }
            return "requested"
        }

        // Helper to safely evaluate JS on WebView
        private fun safeEvaluateJs(script: String) {
            try {
                if (::webView.isInitialized) {
                    webView.evaluateJavascript(script, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to evaluate JS: ${e.message}")
            }
        }

        @JavascriptInterface
        fun getGeminiApiKey(): String = geminiApiKey

        @JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ALL_PERMISSIONS) {
            val denied = permissions.zip(grantResults.toList())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }
            if (denied.isNotEmpty()) {
                Log.w(TAG, "Denied permissions: $denied")
                Toast.makeText(
                    this,
                    "بعض الأذونات مرفوضة. قد لا تعمل بعض الميزات.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        try {
            if (::webView.isInitialized) {
                webView.stopLoading()
                webView.destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying WebView", e)
        }
        super.onDestroy()
    }
}
