plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val debugKeystorePath = System.getenv("DEBUG_KEYSTORE_PATH")
val debugKeystorePassword = System.getenv("DEBUG_KEYSTORE_PASSWORD")
val debugKeyAlias = System.getenv("DEBUG_KEY_ALIAS")
val debugKeyPassword = System.getenv("DEBUG_KEY_PASSWORD")

android {
    namespace = "dev.vendora.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.vendora.android"
        minSdk = 26
        targetSdk = 36
        versionCode = ciRunNumber ?: 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (debugKeystorePath != null) {
            create("persistentDebug") {
                storeFile = file(debugKeystorePath)
                storePassword = requireNotNull(debugKeystorePassword) {
                    "DEBUG_KEYSTORE_PASSWORD must be set when DEBUG_KEYSTORE_PATH is set"
                }
                keyAlias = requireNotNull(debugKeyAlias) {
                    "DEBUG_KEY_ALIAS must be set when DEBUG_KEYSTORE_PATH is set"
                }
                keyPassword = requireNotNull(debugKeyPassword) {
                    "DEBUG_KEY_PASSWORD must be set when DEBUG_KEYSTORE_PATH is set"
                }
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (debugKeystorePath != null) {
                signingConfig = signingConfigs.getByName("persistentDebug")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
