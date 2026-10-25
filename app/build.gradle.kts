@file:Suppress("UnstableApiUsage")
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization") version "2.1.0"
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
}

// local.properties (archivo local, NO se sube al repo). Se lee una sola vez.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Firma de release OPCIONAL: solo si existe keystore.properties (archivo local, NO se sube al repo).
// En F-Droid ese archivo no existe, así que el APK sale sin firmar y F-Droid lo firma con su clave.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Solo para pruebas locales: firma el release con la clave debug si lo activas en local.properties
val signReleaseWithDebug = localProps.getProperty("signing.useDebug") == "true"

// Clave opcional de Audius. Orden: local.properties (audius.apiKey) -> variable de entorno
// AUDIUS_API_KEY -> vacía. Sin clave (como en F-Droid) la app compila igual.
val audiusApiKey: String =
    (localProps.getProperty("audius.apiKey") ?: System.getenv("AUDIUS_API_KEY") ?: "")
        .trim()
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

android {
    namespace = "com.betsy.music"
    //noinspection GradleDependency
    compileSdk = 36

    defaultConfig {
        applicationId = "com.betsy.music"
        minSdk = 24
        targetSdk = 35
        versionCode = 126
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "AUDIUS_API_KEY", "\"$audiusApiKey\"")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 1) keystore.properties presente -> tu clave de release
            // 2) signing.useDebug=true en local.properties -> clave debug (solo pruebas locales)
            // 3) ninguno (F-Droid) -> APK sin firmar; F-Droid lo firma con su propia clave
            signingConfig = when {
                keystoreProps.isNotEmpty() -> signingConfigs.getByName("release")
                signReleaseWithDebug -> signingConfigs.getByName("debug")
                else -> null
            }
        }

        debug {

        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        jvmTarget = "21"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    lint {
        disable += "MissingTranslation"
    }

    // F-Droid: no incluir metadatos de dependencias dentro del APK
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)

    implementation(libs.activity)
    implementation(libs.navigation)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)

    implementation(libs.material3)
    implementation(libs.palette)
    implementation(projects.materialColorUtilities)

    implementation(libs.coil)
    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)
    implementation(libs.squigglyslider)

    implementation(libs.room.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.material.ripple)
    implementation(libs.material.icons.extended)
    implementation(libs.graphics.shapes)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.ui.unit)
    implementation(libs.androidx.ui.geometry)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    implementation(projects.audius)
    implementation(projects.lrclib)
    implementation(project(":internetarchive"))

    implementation(libs.ktor.client.core)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.timber)
}