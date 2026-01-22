# ============================================================================
# HomeRepairs - ProGuard Rules
# ============================================================================
# These rules ensure proper code shrinking and obfuscation while maintaining
# functionality of Firebase, Retrofit, Glide, and other third-party libraries.
# ============================================================================

# ============================================================================
# General Android Rules
# ============================================================================

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# Firebase Rules
# ============================================================================

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keepclassmembers class com.google.firebase.auth.** { *; }

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }
-keepclassmembers class com.google.firebase.storage.** { *; }

# Firebase Messaging (FCM)
-keep class com.google.firebase.messaging.** { *; }
-keepclassmembers class com.google.firebase.messaging.** { *; }

# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-keepclassmembers class com.google.firebase.analytics.** { *; }

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-keepclassmembers class com.google.firebase.crashlytics.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# ============================================================================
# Google Play Services
# ============================================================================

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.location.** { *; }

# ============================================================================
# Model Classes (Data Models)
# ============================================================================

# Keep all model classes for Firebase serialization
-keep class com.example.homerepairs.models.** { *; }
-keepclassmembers class com.example.homerepairs.models.** { *; }

# Specifically keep model constructors and fields
-keepclassmembers class com.example.homerepairs.models.* {
    public <init>();
    public <init>(...);
    public *;
}

# ============================================================================
# Retrofit & OkHttp
# ============================================================================

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn javax.annotation.**

# OkHttp Platform used only on JVM and when Conscrypt dependency is available.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================================================================
# Gson
# ============================================================================

-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================================================
# Glide
# ============================================================================

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# Keep our custom Glide module
-keep class com.example.homerepairs.HomeRepairsGlideModule

# ============================================================================
# Lottie Animations
# ============================================================================

-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ============================================================================
# CircleImageView
# ============================================================================

-keep class de.hdodenhof.circleimageview.** { *; }
-dontwarn de.hdodenhof.circleimageview.**

# ============================================================================
# Facebook SDK
# ============================================================================

-keep class com.facebook.** { *; }
-keepclassmembers class com.facebook.** { *; }
-dontwarn com.facebook.**

# ============================================================================
# AndroidX & Material Components
# ============================================================================

-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ============================================================================
# ViewBinding
# ============================================================================

-keep class * implements androidx.viewbinding.ViewBinding {
    public static * inflate(android.view.LayoutInflater);
    public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static * bind(android.view.View);
}

# ============================================================================
# Lifecycle Components
# ============================================================================

-keep class androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ============================================================================
# Application-Specific Rules
# ============================================================================

# Keep BaseActivity and all Activities
-keep class com.example.homerepairs.BaseActivity { *; }
-keep class * extends com.example.homerepairs.BaseActivity { *; }
-keep class * extends androidx.appcompat.app.AppCompatActivity { *; }

# Keep all adapters
-keep class com.example.homerepairs.adapters.** { *; }

# Keep all services
-keep class com.example.homerepairs.services.** { *; }

# Keep ViewModels
-keep class com.example.homerepairs.viewmodel.** { *; }

# Keep utils
-keep class com.example.homerepairs.utils.** { *; }

# ============================================================================
# Debugging (Comment out for production)
# ============================================================================

# Print mapping to file for debugging
-printmapping mapping.txt

# Print seeds (kept classes)
-printseeds seeds.txt

# Print usage (removed classes)
-printusage usage.txt

# ============================================================================
# Optimization Settings
# ============================================================================

# Optimize code
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization is turned on by default. Dex does not like code run
# through the ProGuard optimize and preverify steps (and performs some
# of these optimizations on its own).
-dontpreverify

# Note that if you want to enable optimization, you cannot use the
# -dontoptimize flag and you should remove the -dontpreverify flag as well.
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ============================================================================
# End of ProGuard Rules
# ============================================================================
