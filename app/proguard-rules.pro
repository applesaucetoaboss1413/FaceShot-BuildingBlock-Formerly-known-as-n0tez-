# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Security library classes
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Keep Material Design classes
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Keep Room database classes
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Kotlin classes
-keep class **.model.** { *; }
-keep class **.entity.** { *; }
-keep class **.database.** { *; }

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}