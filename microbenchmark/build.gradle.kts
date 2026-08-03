import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.androidx.benchmark)
}

android {
    namespace = "com.sonsation.library.benchmark"
    compileSdk = 37

    defaultConfig {
        // HardwareRenderer, which the harness needs to get a real GPU canvas, is API 29.
        minSdk = 29
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        // Clocks cannot be locked on a retail device without root. The benchmark library
        // still detects thermal throttling and repeats until the numbers are stable.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "UNLOCKED"
    }

    // Benchmarks must run against a non-debuggable build; a debuggable one is skewed by
    // the JIT and debug hooks badly enough to make the numbers meaningless.
    testBuildType = "release"

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isDefault = true
            isMinifyEnabled = false
        }
    }

    sourceSets {
        // Shared with the library's correctness tests instead of being duplicated.
        getByName("androidTest").java.srcDir("$rootDir/testharness")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

dependencies {
    androidTestImplementation(project(":library"))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.benchmark.junit4)
}
