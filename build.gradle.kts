// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false  // ✅ FIXED: was kotlin.android
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    // alias(libs.plugins.secrets) apply false  // Keep commented if removed
}
