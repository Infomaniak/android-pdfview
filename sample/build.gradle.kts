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
    id("com.android.application")

    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kapt)
}

val libMinSdk: Int by rootProject.extra
val libCompileSdk: Int by rootProject.extra
val libTargetSdk: Int by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra

android {
    namespace = "com.infomaniak.lib.pdfview.sample"

    defaultConfig {
        minSdk = libMinSdk
        compileSdk = libCompileSdk
        targetSdk = libTargetSdk
        versionCode = 3
    }
    buildFeatures { viewBinding = true }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlinOptions { jvmTarget = javaVersion.toString() }
}

dependencies {
    implementation(project(":android-pdf-viewer"))

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.pdfium)

    annotationProcessor(libs.androidannotations)
}
