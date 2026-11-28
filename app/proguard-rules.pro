# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate

# Preserve JNI methods and native implementations
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keep class com.termux.terminal.JNI { *; }
-keep class com.termux.app.TermuxInstaller { *; }
-keep class io.noties.markwon.** { *; }
-keep class androidx.window.** { *; }

# Keep @Serializable Navigation 3 routes & serializer classes under R8 shrinking
-keepattributes *Annotation*,InnerClasses,Signature
-keep class com.termux.app.settings.navigation.** { *; }
-keep class com.termux.app.settings.data.PluginKind** { *; }
-if @kotlinx.serialization.Serializable class com.termux.app.settings.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
