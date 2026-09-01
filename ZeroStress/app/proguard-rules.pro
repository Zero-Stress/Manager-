# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.zerostress.manager.models.** { *; }

# Compose
-dontwarn androidx.compose.**

# ML Kit
-keep class com.google.mlkit.** { *; }
