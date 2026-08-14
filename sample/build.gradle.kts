buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

repositories {
    google()
    //mavenLocal()
    mavenCentral()
}

plugins {
    alias(libs.plugins.android.application)
}

val libMinSdk: Int by rootProject.extra
val libCompileSdk: Int by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra

android {
    namespace = "com.infomaniak.lib.pdfview.sample"

    defaultConfig {
        minSdk = libMinSdk
        compileSdk = libCompileSdk
        targetSdk = libCompileSdk
        versionCode = 3
    }
    buildFeatures { viewBinding = true }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(javaVersion.toString())
    }
}

dependencies {
    implementation(project(":android-pdf-viewer"))

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.pdfium)

}
