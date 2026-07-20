-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class androidx.datastore.preferences.core.Preferences { *; }
-keepclassmembers class androidx.datastore.preferences.core.Preferences { *; }

-keep class androidx.compose.runtime.Composable {}
-keep class androidx.compose.runtime.State {}
-keep class androidx.compose.runtime.MutableState {}

-allowaccessmodification
-repackageclasses
