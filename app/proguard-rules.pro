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

# --- App Models ---
-keep class com.akshar.data.model.** { *; }

# --- App Specific Data Classes ---
# Ensures Room models don't get obfuscated
-keepclassmembers class com.akshar.** {
    @androidx.room.Entity *;
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
}
