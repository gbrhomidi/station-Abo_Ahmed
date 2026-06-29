package com.aistudio.dieselstationsms.kxmpzq

import android.app.Application
import android.util.Log
import java.io.File
import java.util.Date

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Setup global crash handler - FIXED: uses cacheDir instead of external storage
        // This prevents SecurityException on Android 10+ (API 29+)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // FIXED: Use cacheDir instead of ExternalStoragePublicDirectory
                // cacheDir is always accessible without runtime permissions
                val crashDir = File(cacheDir, "crashes")
                if (!crashDir.exists()) {
                    crashDir.mkdirs()
                }
                val logFile = File(crashDir, "crash_log.txt")
                logFile.appendText("${Date()}: ${throwable.stackTraceToString()}\n\n")
                Log.e("CrashHandler", "Crash logged to: ${logFile.absolutePath}", throwable)
            } catch (e: Exception) {
                // If crash handler itself fails, log to system log at minimum
                Log.e("CrashHandler", "Failed to write crash log: ${e.message}", e)
            }
            // Always call default handler so the app still crashes properly
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
