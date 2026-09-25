# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Jetpack Compose: evitar que R8 reordene/optimice agresivamente el runtime
# y los composables generados. Sin esto, en release (con isMinifyEnabled=true)
# pueden aparecer fallos intermitentes de foco/click en TextField dentro de
# listas generadas dinámicamente (forEach/items), que nunca se ven en debug
# porque ahí R8 no actúa.
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# Kotlin coroutines y metadata reflexiva (StateFlow, corutinas del ViewModel)
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlinx.coroutines.**

# Kotlinx Serialization (usado por el cliente de Supabase)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ML Kit
-keep class com.google.mlkit.** { *; }
# iTextPDF / BouncyCastle
-dontwarn com.itextpdf.bouncycastle.**
-dontwarn com.itextpdf.bouncycastlefips.**

