@file:Suppress("UseTomlInstead")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

@Suppress("OldTargetApi")
android {
    namespace = "com.example.safetyvestinator"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }

    compileOptions{
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    }

    defaultConfig {
        applicationId = "com.example.safetyvestinator"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.compose.material:material-icons-extended") // Used for Calendar Chevrons
    implementation("androidx.datastore:datastore-preferences:1.2.1") // Used for Data Persistence
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0") // Used for Data Persistence
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0") // Used for Data Persistence
    implementation("androidx.preference:preference-ktx:1.2.1") // Used for Map
    implementation("com.kizitonwose.calendar:compose:2.10.1") // Calendar Library
    implementation("com.patrykandpatrick.vico:compose-m3:3.1.0")
    implementation(libs.androidx.material3) // For Graph
    implementation("org.osmdroid:osmdroid-android:6.1.20") // For Map
    implementation("com.google.accompanist:accompanist-permissions:0.37.3") // For BLE Use
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}