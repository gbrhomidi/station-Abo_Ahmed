# حفظ أسماء الفئات المستخدمة في الـ Reflection (Moshi, Retrofit, Room)
-keep class com.aistudio.dieselstationsms.kxmpzq.** { *; }

# Moshi
-keep class * extends com.squareup.moshi.JsonAdapter
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# NanoHTTPD
-keep class fi.iki.elonen.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
