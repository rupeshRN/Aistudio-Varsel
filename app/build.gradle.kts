plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.varsel.expensetracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.varsel.expensetracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    /*
     * -------------------------------------------------------------------------
     * RELEASE SIGNING
     *
     * The release keystore is supplied by GitHub Actions through environment
     * variables. The actual keystore file is created temporarily on the
     * GitHub Actions runner and is NEVER committed to the repository.
     *
     * Environment variables expected:
     *
     * VARSEL_KEYSTORE_FILE
     * VARSEL_KEYSTORE_PASSWORD
     * VARSEL_KEY_ALIAS
     * VARSEL_KEY_PASSWORD
     *
     * This allows Gradle to sign BOTH:
     *   - APK
     *   - AAB
     *
     * directly during assembleRelease / bundleRelease.
     * -------------------------------------------------------------------------
     */

    val keystoreFile = System.getenv("VARSEL_KEYSTORE_FILE")
    val keystorePassword = System.getenv("VARSEL_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("VARSEL_KEY_ALIAS")
    val keyPassword = System.getenv("VARSEL_KEY_PASSWORD")
    val hasReleaseSigning = !keystoreFile.isNullOrBlank() &&
            !keystorePassword.isNullOrBlank() &&
            !keyAlias.isNullOrBlank() &&
            !keyPassword.isNullOrBlank() &&
            file(keystoreFile).exists()

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreFile!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false

            /*
             * Release builds produced by CI will be signed by release signing config
             * when environment variables and keystore are present.
             */
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"

        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    // Jetpack Compose (BOM Managed)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Offline Encrypted Local Database (Room)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Offline Scanning & Extraction
    implementation(libs.mlkit.text.recognition)
    implementation(libs.pdfbox.android)

    // Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation("junit:junit:4.13.2")

    // SQLCipher for encrypted Room DB
    implementation(libs.sqlcipher.android)

    // Navigation & Hilt Integration for Compose
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Compose Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended")
}
