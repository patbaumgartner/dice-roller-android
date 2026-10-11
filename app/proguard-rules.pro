-keepattributes AnnotationDefault,RuntimeVisibleAnnotations
-keepclassmembers class com.patbaumgartner.android.diceroller.** {
    *** Companion;
}
-keepclasseswithmembers class com.patbaumgartner.android.diceroller.** {
    kotlinx.serialization.KSerializer serializer(...);
}
