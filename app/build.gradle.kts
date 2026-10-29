import java.io.File
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.myplayer.app"
    compileSdk = 36

    val localProps = Properties().apply {
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { load(it) }
        }
        val envFile = rootProject.file(".env")
        if (envFile.exists()) {
            envFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val parts = trimmed.split("=", limit = 2)
                    setProperty(parts[0].trim(), parts[1].trim())
                }
            }
        }
    }

    fun resolveSecret(vararg keys: String): String {
        for (key in keys) {
            val fromEnv = System.getenv(key)
            if (!fromEnv.isNullOrBlank()) return fromEnv.trim().replace("\r", "").replace("\n", "").replace("\"", "").replace("\\", "")
            val fromGradle = project.findProperty(key) as? String
            if (!fromGradle.isNullOrBlank()) return fromGradle.trim().replace("\r", "").replace("\n", "").replace("\"", "").replace("\\", "")
            val fromLocal = localProps.getProperty(key)
            if (!fromLocal.isNullOrBlank()) return fromLocal.trim().replace("\r", "").replace("\n", "").replace("\"", "").replace("\\", "")
            val dotEnv = rootProject.file(".env")
            if (dotEnv.isFile) {
                dotEnv.useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("$key=")) {
                            val v = trimmed.substringAfter("=").trim().replace("\"", "").replace("\\", "")
                            if (v.isNotBlank()) return v
                        }
                    }
                }
            }
        }
        return ""
    }

    defaultConfig {
        applicationId = "com.myplayer.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 18
        versionName = "6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read Play Integrity project number from local.properties (never commit this)
        val playProjectNumber = project.findProperty("play.integrity.projectNumber")?.toString()
            ?: "0" // Replace 0 with your Google Cloud project number linked to Play Console
        buildConfigField("String", "PLAY_INTEGRITY_PROJECT_NUMBER", "\"$playProjectNumber\"")

        // Read Last.fm API key from local.properties or gradle.properties (never hardcode in Kotlin)
        val lastFmApiKey = project.findProperty("lastfm.apiKey")?.toString() ?: ""
        buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmApiKey\"")

        // Embed the NDK native library
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-O3", "-Wall") // no -ffast-math: DSP relies on isfinite()/denormal guards
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        create("release_config") {
            val base64Key = resolveSecret("SIGNING_KEY")
            val storeFilePath = resolveSecret("RELEASE_STORE_FILE")
            val storePasswordProp = resolveSecret("RELEASE_STORE_PASSWORD", "KEY_STORE_PASSWORD")
            val keyAliasProp = resolveSecret("RELEASE_KEY_ALIAS", "ALIAS").ifBlank { "myplayer_release_key" }
            val keyPasswordProp = resolveSecret("RELEASE_KEY_PASSWORD", "KEY_PASSWORD").ifBlank { storePasswordProp }

            val keystoreFile: File? = when {
                base64Key.isNotBlank() -> {
                    try {
                        val decodedBytes = Base64.getDecoder().decode(base64Key.trim())
                        val tempKeystore = layout.buildDirectory.file("signing/release.keystore").get().asFile
                        tempKeystore.parentFile.mkdirs()
                        tempKeystore.writeBytes(decodedBytes)
                        tempKeystore
                    } catch (_: Exception) {
                        null
                    }
                }
                storeFilePath.isNotBlank() -> {
                    val direct = file(storeFilePath)
                    if (direct.exists()) direct else rootProject.file(storeFilePath)
                }
                else -> null
            }

            if (keystoreFile != null && keystoreFile.exists() && storePasswordProp.isNotBlank()) {
                storeFile = keystoreFile
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            // ─── Phase 1: R8 hardening ──────────────────────────────────────
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseConfig = signingConfigs.findByName("release_config")
            signingConfig = if (releaseConfig?.storeFile != null && releaseConfig.storeFile?.exists() == true) {
                releaseConfig
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true   // needed for PLAY_INTEGRITY_PROJECT_NUMBER
        aidl = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    
    lint {
        baseline = file("lint-baseline.xml")
    }

    sourceSets {
        getByName("debug") {
            java.srcDir("build/generated/ksp/debug/kotlin")
            java.srcDir("build/generated/ksp/debug/java")
        }
        getByName("release") {
            java.srcDir("build/generated/ksp/release/kotlin")
            java.srcDir("build/generated/ksp/release/java")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(11)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${project.layout.buildDirectory.get().asFile.absolutePath}/compose_metrics",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.layout.buildDirectory.get().asFile.absolutePath}/compose_metrics"
        )
    }
}

composeCompiler {
    enableStrongSkippingMode = true
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    // Core library desugaring — backports Java APIs (e.g. URLDecoder.decode(String, Charset))
    // to pre-API 33 devices. Required by NewPipeExtractor v0.26+ which calls that API.
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)
    // LastWave's secret weapon for 24-bit FLAC & Atmos decoding:
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.3.1+2")

    // Navigation Compose
    implementation(libs.navigation.compose)

    // Coil
    implementation(libs.coil.compose)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)


    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // NewPipeExtractor
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.3")

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Paging
    implementation(libs.paging.runtime.ktx)
    implementation(libs.paging.compose)

    // ─── Security Dependencies ──────────────────────────────────────────────
    // Play Integrity API (Phase 5)
    implementation(libs.play.integrity)

    // Annotations for security code
    implementation(libs.androidx.annotation)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}