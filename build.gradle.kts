buildscript {
    dependencies {
        classpath(libs.maven.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

tasks.register("clean", Delete::class){
    delete(rootProject.buildDir)
}