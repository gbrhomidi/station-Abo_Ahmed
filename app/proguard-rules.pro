# ═══════════════════════════════════════════════════════════════
#  محطة أبو أحمد - قواعد ProGuard (إصدار آمن ومُحكم - مصحح)
#  آخر تحديث: 2026-07-01
# ═══════════════════════════════════════════════════════════════

# ─── نقطة الدخول الرئيسية ───
-keep public class com.aistudio.dieselstationsms.kxmpzq.MyApplication {
    public <init>();
}

# ─── المكونات الأساسية (مطلوبة من النظام) ───
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ─── النشاط الرئيسي ───
-keep class com.aistudio.dieselstationsms.kxmpzq.MainActivity {
    public <init>();
    public void onCreate(android.os.Bundle);
}

# ─── واجهة JavaScript لـ WebView ───
-keepclassmembers class com.aistudio.dieselstationsms.kxmpzq.MainActivity$WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}

# ─── الفئات الرئيسية للتطبيق ───
-keep class com.aistudio.dieselstationsms.kxmpzq.DatabaseHelper {
    public <init>(android.content.Context);
    public *** get*(...);
    public *** set*(...);
}
-keep class com.aistudio.dieselstationsms.kxmpzq.SMSService {
    public <init>();
}
-keep class com.aistudio.dieselstationsms.kxmpzq.SmsReceiver {
    public <init>();
}
-keep class com.aistudio.dieselstationsms.kxmpzq.BackupWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─── Moshi - Serialization ───
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.Json <methods>;
}
-keepnames @com.squareup.moshi.JsonClass class *

# ─── Retrofit / OkHttp ───
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions, *Annotation*
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ─── Room ───
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { @androidx.room.PrimaryKey <fields>; }
-keepclassmembers @androidx.room.Entity class * {
    <init>(...);
}
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-dontwarn androidx.room.paging.**

# ─── NanoHTTPD ───
-keep class fi.iki.elonen.NanoHTTPD { *; }
-keep class fi.iki.elonen.NanoHTTPD$* { *; }
-dontwarn fi.iki.elonen.**

# ─── WorkManager ───
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─── Biometric ───
-keep class androidx.biometric.BiometricPrompt { *; }
-keep class androidx.biometric.BiometricPrompt$PromptInfo { *; }
-keep class androidx.biometric.BiometricPrompt$AuthenticationCallback { *; }

# ─── Compose ───
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}
-dontwarn androidx.compose.**

# ─── Compose Navigation ───
-keepclassmembers class * {
    @androidx.navigation.NavType <fields>;
}

# ─── Kotlin Coroutines ───
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ─── Kotlin Serialization ───
-keepclassmembers class kotlinx.serialization.json.** { *; }
-dontwarn kotlinx.serialization.**

# ─── Security Crypto ───
-keep class androidx.security.crypto.EncryptedSharedPreferences { *; }
-keep class androidx.security.crypto.MasterKey { *; }
-dontwarn androidx.security.crypto.**

# ─── RootBeer ───
-keep class com.scottyab.rootbeer.** { *; }
-dontwarn com.scottyab.rootbeer.**

# ─── إزالة السجلات في الإنتاج ───
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# ─── تحسينات الأداء ───
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# ─── معلومات الأخطاء ───
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ─── كتم التحذيرات ───
-dontnote
-dontwarn android.support.**
-dontwarn androidx.**

# ─── تشويش إضافي ───
-repackageclasses 'a'
-flattenpackagehierarchy

# ─── أمان إضافي ───
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify

# ─── نهاية الملف ───
