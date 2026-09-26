plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tuempresa.possystem"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tuempresa.possystem"
        minSdk = 26 // Android 8.0 - necesario para Bluetooth SPP estable y CameraX
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Variables para Supabase (se inyectan desde local.properties o gradle.properties,
        // NUNCA se hardcodean ni se suben a git)
        buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_ANON_KEY") ?: ""}\"")
    }

    signingConfigs {
        create("release") {
            // Se completa vía -P (gradle.properties o parámetros -P en CI, ver
            // .github/workflows/android-build.yml). Si no están presentes,
            // simplemente no se aplica y el build de release queda sin firmar.
            val keystorePath = project.findProperty("RELEASE_STORE_FILE") as String?
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            // PRUEBA DE DIAGNÓSTICO TEMPORAL: isDebuggable = true para
            // descartar si el toque perdido en TextField es causado por
            // protecciones de MIUI/Android que tratan distinto a las apps
            // no-debuggables (release normal es debuggable=false). Si con
            // esto el bug desaparece, confirma la causa y luego se revierte
            // este flag a false (una app de producción real NUNCA debe
            // quedar debuggable=true) buscando la protección específica de
            // MIUI que lo cause, en vez de dejar esto puesto.
            isDebuggable = true
            // Minify/R8 desactivado temporalmente: causaba fallos intermitentes
            // de foco/click en los TextField de precio y stock por talla
            // (ver reglas agregadas en proguard-rules.pro por si se reactiva
            // más adelante). Costo: el APK pesa más, nada más.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Firma release activada: usa el keystore inyectado por CI
            // (o por tu local.properties si compilas localmente).
            signingConfig = signingConfigs.getByName("release")
        }
        create("diagnostico") {
            // BUILD TEMPORAL SOLO PARA AISLAR LA CAUSA: es una copia exacta
            // de "debug" (mismos flags, mismo applicationIdSuffix para poder
            // instalarla junto a las otras dos sin conflicto) pero FIRMADA
            // con el keystore de release en vez de con el keystore de debug
            // automático de Android Studio/CI.
            //
            // Si esta build (misma config que debug, pero firmada distinto)
            // FALLA igual que "release" -> la causa es 100% la firma/el
            // keystore, no ningún flag de build.
            // Si esta build FUNCIONA bien (como debug) -> la firma queda
            // descartada del todo, y el o los flags que le quedan a
            // "release" y no tiene esta build (minify ya en false,
            // isDebuggable ya en true... o sea NINGUNO distinto salvo la
            // firma) confirmarían aun más que es la firma.
            initWith(getByName("debug"))
            applicationIdSuffix = ".diagnostico"
            signingConfig = signingConfigs.getByName("release")
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    // ---------- Core & Compose ----------
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2025.01.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ---------- Room (offline-first) ----------
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ---------- Supabase (Postgrest, Realtime, Storage, Auth) ----------
    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.5"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-core:2.3.12")

    // Serialización (requerida por el cliente de Supabase)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ---------- WorkManager (sincronización en background) ----------
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // ---------- ML Kit (escaneo de código de barras) ----------
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ---------- Generación de códigos QR (boleta: folio de venta y redes sociales) ----------
    implementation("com.google.zxing:core:3.5.3")

    // ---------- Permisos en Compose ----------
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // ---------- Generación de PDF (comprobantes y reportes) ----------
    implementation("com.itextpdf:itext7-core:8.0.5")

    // Nota: los reportes en Excel (.xlsx) se generan a mano en
    // domain/reportes/ExportadorExcel.kt escribiendo directamente el formato
    // OOXML (zip + XML) con java.util.zip, sin Apache POI. POI depende de
    // java.awt y de varios jars nativos de escritorio que no existen en
    // Android y causan errores de compilación/runtime difíciles de resolver
    // de forma confiable (ver https://github.com/centic9/poi-on-android).

    // ---------- Coil (carga de imágenes de productos) ----------
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ---------- Testing ----------
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.01.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
