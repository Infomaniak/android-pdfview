plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kapt)
}

val libMinSdk: Int by rootProject.extra
val libCompileSdk: Int by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra
val libGroupId: String by rootProject.extra
val libVersionName: String by rootProject.extra
val libArtifactId: String by rootProject.extra

group = libGroupId
version = libVersionName

android {
    namespace = "com.infomaniak.lib.pdfview"

    defaultConfig {
        minSdk = libMinSdk
        compileSdk = libCompileSdk
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)

    api(libs.pdfium)
}

fun getPropertyValue(propertyName: String): String? {
    if (project.hasProperty(propertyName)) return project.property(propertyName) as String
    return System.getenv(propertyName)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.findByName("release")!!)
                groupId = libGroupId
                artifactId = libArtifactId
                version = libVersionName
                pom {
                    name.set("android-pdfview")
                    description.set("Android PDF viewer, with animations, gestures, zoom and double tap support")
                    url.set("https://github.com/Infomaniak/android-pdfview")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/Infomaniak/android-pdfview.git")
                        developerConnection.set("scm:git:ssh://github.com/Infomaniak/android-pdfview.git")
                        url.set("https://github.com/Infomaniak/android-pdfview")
                    }
                    developers {
                        developer {
                            id.set("Infomaniak")
                            name.set("Infomaniak Development Team")
                            email.set("mobile+libraries@infomaniak-dev.ch")
                            url.set("https://www.infomaniak.com/")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = "reposilite"
                url = uri(
                    if (libVersionName.endsWith("SNAPSHOT")) {
                        "https://maven.infomaniak.app/snapshots"
                    } else {
                        "https://maven.infomaniak.app/releases"
                    }
                )
                credentials {
                    username = getPropertyValue("reposiliteUsername")
                    password = getPropertyValue("reposilitePassword")
                }
            }
        }
    }
}

val gpgKeyId = providers.gradleProperty("GPG_key_id")
    .orElse(providers.environmentVariable("GPG_key_id"))
    .orNull
    ?.takeIf { it.isNotBlank() }
val gpgPrivateKey = providers.gradleProperty("GPG_private_key")
    .orElse(providers.environmentVariable("GPG_private_key"))
    .orNull
    ?.takeIf { it.isNotBlank() }
val gpgPassword = providers.gradleProperty("GPG_private_password")
    .orElse(providers.environmentVariable("GPG_private_password"))
    .orNull
    ?.takeIf { it.isNotBlank() }

signing {
    val isPublishTask = gradle.startParameter.taskNames.any {
        it.contains("ToReposiliteRepository") || it.contains("CentralPortal") || it.contains("CentralSnapshots")
    }
    if (gpgKeyId != null && gpgPrivateKey != null && gpgPassword != null) {
        useInMemoryPgpKeys(gpgKeyId, gpgPrivateKey.replace('#', '\n'), gpgPassword)
        sign(publishing.publications)
    } else if (isPublishTask) {
        error("Missing signing secrets: GPG_key_id, GPG_private_key, GPG_private_password")
    }
}
