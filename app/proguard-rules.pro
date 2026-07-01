# ═══════════════════════════════════════════════════════════════
#  محطة أبو أحمد - قواعد ProGuard/ProGuard Rules
#  إصدار محسّن وأكثر صرامة - 2026
# ═══════════════════════════════════════════════════════════════

# ─── الحفاظ على نقطة الدخول الرئيسية ───
-keep public class com.aistudio.dieselstationsms.kxmpzq.MyApplication {
    public <init>();
}

# ─── الحفاظ على الأنشطة والخدمات والمستقبلات ───
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgent
-keep public class * extends android.preference.Preference

# ─── الحفاظ على الأنشطة المحددة ───
-keep class com.aistudio.dieselstationsms.kxmpzq.MainActivity {
    public <init>();
    public void onCreate(android.os.Bundle);
}

# ─── الحفاظ على واجهة JavaScript ───
-keepclassmembers class com.aistudio.dieselstationsms.kxmpzq.MainActivity$WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}

# ─── Moshi - JSON Serialization (محدد بدقة) ───
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.Json <methods>;
}
-keepnames @com.squareup.moshi.JsonClass class *

# ─── Retrofit - Network (محدد بدقة) ───
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

# ─── OkHttp - محدود جداً ───
-keep class okhttp3.OkHttpClient { *; }
-keep class okhttp3.Request { *; }
-keep class okhttp3.Response { *; }
-keep class okhttp3.Interceptor { *; }
-keep class okhttp3.logging.HttpLoggingInterceptor { *; }
-dontwarn okhttp3.internal.**

# ─── Room - Database (محدد بدقة) ───
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { @androidx.room.PrimaryKey <fields>; }
-keepclassmembers @androidx.room.Entity class * {
    <init>(...);
}
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-dontwarn androidx.room.paging.**

# ─── NanoHTTPD - محدود جداً ───
-keep class fi.iki.elonen.NanoHTTPD { *; }
-keep class fi.iki.elonen.NanoHTTPD$* { *; }
-dontwarn fi.iki.elonen.**

# ─── WorkManager ───
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─── Biometric (محدد) ───
-keep class androidx.biometric.BiometricPrompt { *; }
-keep class androidx.biometric.BiometricPrompt$* { *; }

# ─── Compose - قواعد أساسية فقط ───
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}
-dontwarn androidx.compose.**

# ─── Kotlin Coroutines ───
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ─── Kotlin Serialization (إذا استخدمت) ───
-keepclassmembers class kotlinx.serialization.json.** { *; }
-dontwarn kotlinx.serialization.**

# ─── AndroidX Core ───
-keep class androidx.core.content.FileProvider { *; }
-keep class androidx.core.app.NotificationCompat { *; }

# ─── إزالة السجلات في الإنتاج ───
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# ─── إزالة Log من التطبيق نفسه ───
-assumenosideeffects class com.aistudio.dieselstationsms.kxmpzq.** {
    void log*(...);
    void debug*(...);
}

# ─── تحسينات الأداء ───
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# ─── الحفاظ على معلومات الأخطاء ───
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ─── إزالة التعليقات والسجلات ───
-dontnote
-dontwarn android.support.**
-dontwarn androidx.**

# ─── حماية من الهندسة العكسية ───
-repackageclasses 'a'
-flattenpackagehierarchy
-allowaccessmodification

# ─── قواعد إضافية للأمان ───
# منع الاحتفاظ بأسماء الفئات غير الضرورية
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify

# ─── قواعد خاصة بالتطبيق ───
# الحفاظ على DatabaseHelper فقط للأسماء العامة
-keepnames class com.aistudio.dieselstationsms.kxmpzq.DatabaseHelper
-keepclassmembers class com.aistudio.dieselstationsms.kxmpzq.DatabaseHelper {
    public <init>(android.content.Context);
}

# الحفاظ على SMSService و SmsReceiver
-keepnames class com.aistudio.dieselstationsms.kxmpzq.SMSService
-keepnames class com.aistudio.dieselstationsms.kxmpzq.SmsReceiver
-keepnames class com.aistudio.dieselstationsms.kxmpzq.BackupWorker

# ─── قواعد JSON/JSONObject (للاستخدام مع SQLite) ───
-keepclassmembers class org.json.JSONObject {
    <init>(...);
    *** get*(...);
    *** opt*(...);
    *** put*(...);
}
-keepclassmembers class org.json.JSONArray {
    <init>(...);
    *** get*(...);
    *** opt*(...);
    *** put*(...);
}

# ─── قواعد Reflection المحدودة ───
-keepclassmembers class * {
    *** *Callback;
    *** *Listener;
}

# ─── إزالة الأكواد غير المستخدمة ───
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ─── قواعد Android الرسمية ───
-keep public class android.net.http.SslError
-keep public class android.webkit.WebViewClient

# ─── نهاية الملف ───
