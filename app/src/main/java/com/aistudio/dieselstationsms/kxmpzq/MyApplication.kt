package com.aistudio.dieselstationsms.kxmpzq

import android.app.Application
import android.util.Log

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // معالج استثناءات عام لمنع الانهيار غير المتوقع وتسجيل السبب
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MyApplication", "Uncaught exception in thread ${thread.name}", throwable)
            // يمكن إعادة تشغيل التطبيق أو إظهار رسالة، لكننا نكتفي بالتسجيل
        }
    }
}