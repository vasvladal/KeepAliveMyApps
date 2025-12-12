plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.keepalivemyapps"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.keepalivemyapps"
        minSdk = 29
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true  // Enable BuildConfig generation
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.7"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST"
            )
            merges += setOf("META-INF/LICENSE")
        }
    }
}

// Global dependency resolution strategy to handle annotation conflicts
configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:26.0.2")
    }
    exclude(group = "org.jetbrains", module = "annotations-java5")
    exclude(group = "com.google.guava", module = "listenablefuture")
}

dependencies {
    // AndroidX
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
// Compose BOM and libraries
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    implementation(libs.constraintlayout)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Coroutines
    implementation(libs.coroutines.android)

    // Lifecycle components
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.service)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.androidx.concurrent.futures)
    implementation(libs.guava)

    implementation(libs.work.runtime.ktx)
}