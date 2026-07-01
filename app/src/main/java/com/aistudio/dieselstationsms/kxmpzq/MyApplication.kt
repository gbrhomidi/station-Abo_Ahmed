package com.aistudio.dieselstationsms.kxmpzq

import android.app.Application
import android.os.Build
import android.util.Log
import java.io.File
import java.util.Date

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Global crash handler – uses cacheDir (no permissions needed)
        //    Improved with detailed device info & unique crash file per incident
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Save crash logs in app's private cache directory
                val crashDir = File(cacheDir, "crashes")
                crashDir.mkdirs()

                // Unique file name using timestamp to avoid overwriting previous crashes
                val logFile = File(crashDir, "crash_${System.currentTimeMillis()}.txt")
                logFile.writeText("""
                    Time: ${Date()}
                    Thread: ${thread.name}
                    Exception: ${throwable.stackTraceToString()}
                    Device: ${Build.MANUFACTURER} ${Build.MODEL}
                    Android: ${Build.VERSION.RELEASE}
                """.trimIndent())

                // Optional: send crash report to FirebaseCrashlytics
                // FirebaseCrashlytics.getInstance().recordException(throwable)

            } catch (e: Exception) {
                Log.e("CrashHandler", "Failed to write crash log: ${e.message}", e)
            }

            // Always delegate to the default handler so the system can display the crash dialog / ANR properly
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
