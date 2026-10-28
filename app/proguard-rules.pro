# kotlinx.serialization keeps its generated serializers via bundled rules.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.thumbnailstudio.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.thumbnailstudio.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.thumbnailstudio.app.**$$serializer { *; }
