buildscript {

    extra.apply {
        set("libMinSdk", 21)
        set("libCompileSdk", 35)
        set("libTargetSdk", 35)
        set("libVersionName", "3.2.13")
        set("javaVersion", JavaVersion.VERSION_17)
        set("kotlinVersion", "2.0.21")
    }

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.agp)
    }
}

apply(plugin = "maven-publish")

allprojects {
    repositories {
        google()
        //mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
