# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ==================== Moshi + Kotlin 反射 ====================
# Kotlin 元数据（Moshi 反射必需）
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Deprecated
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# Kotlin 反射支持
-keep class kotlin.reflect.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Moshi Kotlin 反射适配器
-keep class com.squareup.moshi.kotlin.reflect.KotlinJsonAdapter { *; }

# 所有 DTO 数据类
-keep class cn.szu.blankxiao.panoramaview.data.model.** { *; }
-keep class cn.szu.blankxiao.panoramaview.model.** { *; }
-keep class cn.szu.blankxiao.panoramaview.api.** { *; }
-keep class cn.szu.blankxiao.panorama.data.model.** { *; }
-keep class cn.szu.blankxiao.panorama.model.** { *; }
-keep class cn.szu.blankxiao.simple.market.panorama.**.dto.** { *; }

# 保留数据类的字段和构造函数（供反射使用）
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    <init>(...);
}

# ==================== OkHttp / Retrofit ====================
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Retrofit 接口
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# ==================== Kotlin 协程 ====================
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ==================== AndroidX / Compose ====================
-keep class androidx.** { *; }
-dontwarn androidx.**

# ==================== ViewModel / LiveData ====================
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# ==================== 本地异常 ====================
# 保留异常类名（便于日志追踪）
-keep public class * extends java.lang.Exception

# ==================== Firebase ====================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
