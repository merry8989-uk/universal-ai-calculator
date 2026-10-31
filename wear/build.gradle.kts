import java.io.File

// The watch companion.
//
// It shares the calculation engine with the phone through :core, so the two can
// never disagree about an answer. The application id is deliberately the same
// as the phone app: on Wear OS that is what makes the two "one app", and a
// watch is the only kind of device that will install this (the manifest
// requires the watch feature).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.merry8989uk.universalcalculator.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.merry8989uk.universalcalculator"
        minSdk = 26
        targetSdk = 35
        versionCode = 123
        versionName = "2.17.0"
    }

    signingConfigs {
        create("release") {
            val path = System.getenv("KEYSTORE_PATH") ?: "release.p12"
            storeFile = if (path.startsWith("/")) File(path) else rootProject.file(path)
            storeType = System.getenv("KEYSTORE_TYPE") ?: "PKCS12"
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
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
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    debugImplementation(libs.androidx.ui.tooling)
}
