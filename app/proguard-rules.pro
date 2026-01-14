# Add project specific ProGuard rules here.

# Gson
# Gson uses generic type information stored in a class file when working with fields. ProGuard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.example.theloop.models.** { *; }

# WorkManager
# The WorkManager library bundles its own consumer Proguard rules, so we don't need to keep its classes.
# However, custom workers created by the application must be kept explicitly as they are instantiated via reflection.
-keep public class com.example.theloop.WidgetUpdateWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ViewModels
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
