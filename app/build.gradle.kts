plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.mauricem0ss.fastdroid"
    compileSdk = 36
    // Pinned to what the build image ships; AGP's default may differ and the image's SDK is read-only.
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "io.github.mauricem0ss.fastdroid"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

// Pins the runtime classpaths (what ends up in the APK) in app/gradle.lockfile, so
// builds are reproducible and scanners (osv-scanner, Trivy) can read the exact set.
// Refresh after changing gradle/libs.versions.toml: scripts/gradle.sh lock
configurations.matching { it.name == "debugRuntimeClasspath" || it.name == "releaseRuntimeClasspath" }
    .configureEach { resolutionStrategy.activateDependencyLocking() }
dependencyLocking {
    // Empty compatibility shells, merged into kotlin-stdlib since Kotlin 1.8. They
    // reach the graph only through version constraints, which --write-locks doesn't
    // record but the lock check still rejects.
    ignoredDependencies.add("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
    ignoredDependencies.add("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
