# --- General Android & Kotlin ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn kotlin.Unit

# --- Jetpack Compose ---
-keep class androidx.compose.material3.TextKt { *; }
-keep class androidx.compose.material3.ButtonKt { *; }
-keep class androidx.compose.ui.platform.** { *; }

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Retrofit & OkHttp ---
-keepattributes Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn okhttp3.internal.platform.BouncyCastlePlatform
-dontwarn okhttp3.internal.platform.OpenJSSEPlatform

# --- Moshi (JSON Serialization) ---
# Keep the actual data classes to avoid shrinking fields used by JSON
-keep class com.akshar.data.model.** { *; }
-keep @com.squareup.moshi.JsonClass class *
-keep class * extends com.squareup.moshi.JsonAdapter
-keep class com.squareup.moshi.** { *; }

# --- Firebase & Google Services ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# --- App Specific Data Classes ---
# Ensures your Room/Moshi models don't get obfuscated
-keepclassmembers class com.akshar.** {
    @androidx.room.Entity *;
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @com.squareup.moshi.Json *;
}
