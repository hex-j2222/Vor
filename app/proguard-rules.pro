# ── Kotlin ─────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-dontwarn kotlin.**

# ── App entry points ───────────────────────────────────────────
-keep public class com.nebula.editor.NebulaApp
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ── ViewBinding ────────────────────────────────────────────────
-keep class com.nebula.editor.databinding.** { *; }

# ── Room ───────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao    class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Hilt ───────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class *
-dontwarn dagger.**

# ── FFmpegKit ──────────────────────────────────────────────────
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.arthenica.smartexception.** { *; }
-dontwarn com.arthenica.**

# ── Media3 ─────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── WorkManager ────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── YoutubeDL-Android ──────────────────────────────────────────
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# ── Glide ──────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# ── Gson ───────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# ── Data models ────────────────────────────────────────────────
-keep class com.nebula.editor.model.** { *; }
-keep class com.nebula.editor.data.db.entity.** { *; }

# ── SQLCipher ──────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# ── Security ───────────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }

# ── RootBeer ───────────────────────────────────────────────────
-keep class com.scottyab.rootbeer.** { *; }
-dontwarn com.scottyab.**

# ── Lottie ─────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ── Strip logs in release ──────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ── Obfuscation ────────────────────────────────────────────────
-repackageclasses 'n'
-allowaccessmodification
-optimizationpasses 5
-renamesourcefileattribute ''
-keepattributes !SourceFile,!LineNumberTable
-dontwarn **
