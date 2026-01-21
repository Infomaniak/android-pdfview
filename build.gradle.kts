buildscript {

    extra.apply {
        set("libMinSdk", 21)
        set("libCompileSdk", 36)
        set("libVersionName", "3.2.15")
        set("javaVersion", JavaVersion.VERSION_17)
    }

    repositories {
        google()
        mavenCentral()
    }
}

apply(plugin = "maven-publish")

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

allprojects {
    repositories {
        google()
        //mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
