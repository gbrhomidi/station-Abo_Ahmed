package com.aistudio.dieselstationsms.kxmpzq

import android.app.Application
import android.util.Log
import java.io.File
import java.util.Date

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // الاحتفاظ بالمعالج الأصلي للنظام لضمان عدم تعطل وظائف النظام الأساسية
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // ننشئ ملف سجل الأخطاء في المسار الخاص بالتطبيق
                val logFile = File(getExternalFilesDir(null), "crash_log.txt")
                
                // كتابة تفاصيل الخطأ مع التاريخ
                val errorDetails = "${Date()}: \n${throwable.stackTraceToString()}\n\n"
                logFile.appendText(errorDetails)
                
            } catch (e: Exception) {
                // في حال فشلت الكتابة للملف لأي سبب، نطبع خطأ داخلي
                Log.e("MyApplication", "Failed to write crash log", e)
            }

            // تمرير الخطأ للمعالج الافتراضي للنظام حتى لا يتجمد التطبيق للأبد
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
