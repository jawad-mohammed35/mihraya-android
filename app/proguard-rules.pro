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

# Glance finds a widget's instances by its GlanceAppWidget class name. If R8 merges the widget
# classes into one, updating any widget redraws every placed widget with that widget's content.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
