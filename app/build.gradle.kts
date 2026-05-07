@file:Suppress("UseTomlInstead")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
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

    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST"
            )
        }
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
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("com.sun.mail:android-mail:1.6.8") // Used for Email
    implementation("com.sun.mail:android-activation:1.6.8") // Used for Email
    implementation("com.kizitonwose.calendar:compose:2.10.1") // Calendar Library
    implementation("com.patrykandpatrick.vico:compose-m3:3.1.0")
    implementation(libs.androidx.material3) // For Graph
    implementation("org.osmdroid:osmdroid-android:6.1.20") // For Map
    implementation("com.google.accompanist:accompanist-permissions:0.37.3") // For BLE Use
    implementation("com.google.android.gms:play-services-location:21.3.0") // For Phone GPS
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}