-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**

-keepclassmembers @kotlinx.serialization.Serializable class com.mihraya.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mihraya.app.**$$serializer { *; }

-keepclassmembers class * extends androidx.glance.appwidget.action.ActionCallback {
    <init>();
}

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
