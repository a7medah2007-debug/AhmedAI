# Keep Gson Models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.ahmed.companion.brain.models.** { *; }
-keep class com.google.gson.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
