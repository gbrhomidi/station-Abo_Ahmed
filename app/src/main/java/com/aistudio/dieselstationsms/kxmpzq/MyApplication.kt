package com.aistudio.dieselstationsms.kxmpzq

import android.app.Application
import android.os.Environment
import java.io.File
import java.util.Date

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // حفظ الملف في مجلد التنزيلات العام
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val logFile = File(downloadsDir, "crash_log.txt")
                
                logFile.appendText("${Date()}: ${throwable.stackTraceToString()}\n\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
