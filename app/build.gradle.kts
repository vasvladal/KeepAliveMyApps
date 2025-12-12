import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

fun getKeystoreFile(path: String): File {
    val file = file(path)
    if (file.exists()) return file
    val rootFile = rootProject.file(path)
    if (rootFile.exists()) return rootFile
    throw RuntimeException("Keystore file not found: $path")
}

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    compileSdk = project.libs.versions.app.build.compileSDKVersion.get().toInt()

    // ADD THIS LINE - Fixes the namespace error
    namespace = "com.example.keepalivemyapps"

    defaultConfig {
        applicationId = "com.example.keepalivemyapps"  // Make sure this matches your package
        minSdk = project.libs.versions.app.build.minimumSDK.get().toInt()
        targetSdk = project.libs.versions.app.build.targetSDK.get().toInt()
        versionName = project.property("VERSION_NAME").toString()
        versionCode = project.property("VERSION_CODE").toString().toInt()
        vectorDrawables.useSupportLibrary = true
        setProperty("archivesBaseName", "keepalivemyapps-$versionCode")
    }

    signingConfigs {
        create("release") {
            // Try to load from keystore.properties first
            if (keystorePropertiesFile.exists()) {
                try {
                    keyAlias = keystoreProperties.getProperty("keyAlias") ?: ""
                    keyPassword = keystoreProperties.getProperty("keyPassword") ?: ""
                    val storeFilePath = keystoreProperties.getProperty("storeFile") ?: ""
                    storeFile = getKeystoreFile(storeFilePath)
                    storePassword = keystoreProperties.getProperty("storePassword") ?: ""
                } catch (e: Exception) {
                    logger.error("Failed to load from keystore.properties: ${e.message}")
                }
            }
            // Fall back to environment variables
            else {
                val envAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").orNull
                val envKeyPass = providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull
                val envStoreFile = providers.environmentVariable("SIGNING_STORE_FILE").orNull
                val envStorePass = providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull

                if (envAlias != null && envKeyPass != null && envStoreFile != null && envStorePass != null) {
                    keyAlias = envAlias
                    keyPassword = envKeyPass
                    storeFile = getKeystoreFile(envStoreFile)
                    storePassword = envStorePass
                } else {
                    logger.warn("No signing configuration found. Build will be unsigned.")
                }
            }

            // Enable signature schemes (compatible with all AGP versions)
            @Suppress("UnstableApiUsage")
            if (this is com.android.build.gradle.internal.dsl.SigningConfig) {
                this.enableV1Signing = true
                this.enableV2Signing = true

                // Try to enable v3 if available
                try {
                    this.javaClass.getMethod("setEnableV3Signing", Boolean::class.java)
                        .invoke(this, true)
                } catch (e: NoSuchMethodException) {
                    logger.info("v3 signing not available in this AGP version")
                }

                // Try to enable v4 if available
                try {
                    this.javaClass.getMethod("setEnableV4Signing", Boolean::class.java)
                        .invoke(this, true)
                } catch (e: NoSuchMethodException) {
                    logger.info("v4 signing not available in this AGP version")
                }
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Set signing config if it has valid data
            signingConfig = signingConfigs.getByName("release").takeIf { config ->
                config.keyAlias?.isNotBlank() == true &&
                        config.keyPassword?.isNotBlank() == true &&
                        config.storeFile?.exists() == true &&
                        config.storePassword?.isNotBlank() == true
            }

            if (signingConfig == null) {
                logger.warn("Release build will be unsigned - invalid signing configuration")
            }
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }

    compileOptions {
        val currentJavaVersionFromLibs = JavaVersion.valueOf(libs.versions.app.build.javaVersion.get())
        sourceCompatibility = currentJavaVersionFromLibs
        targetCompatibility = currentJavaVersionFromLibs
    }

    kotlinOptions {
        jvmTarget = project.libs.versions.app.build.kotlinJVMTarget.get()
    }

    dependenciesInfo {
        includeInApk = false
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = false
    }

    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(
            JvmTarget.fromTarget(project.libs.versions.app.build.kotlinJVMTarget.get())
        )
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = true
        warningsAsErrors = false
        baseline = file("lint-baseline.xml")
        lintConfig = rootProject.file("lint.xml")
    }

    bundle {
        language {
            enableSplit = false
        }
    }
}

detekt {
    baseline = file("detekt-baseline.xml")
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
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
}