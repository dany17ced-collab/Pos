# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

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

# iText 7: los proveedores de BouncyCastle (firma digital de PDF) son
# opcionales y no se usan en esta app (solo generamos PDFs simples, sin
# firma criptografica). Se le indica a R8 que ignore estas clases ausentes
# en vez de fallar el build por no encontrarlas.
-dontwarn com.itextpdf.bouncycastle.**
-dontwarn com.itextpdf.bouncycastlefips.**
-dontwarn org.bouncycastle.**
-dontwarn org.bouncycastlefips.**
