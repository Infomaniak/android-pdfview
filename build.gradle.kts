/*
 * Infomaniak PDF Viewer - Android
 * Copyright (C) 2026 Infomaniak Network SA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

buildscript {
    // Appending -SNAPSHOT is triggered by passing -Psnapshot to Gradle (used by the snapshot
    // publishing workflow), so the version doesn't need to be typed in by hand.
    val baseVersionName = "3.2.18"

    extra.apply {
        set("libMinSdk", 23)
        set("libCompileSdk", 37)
        set("libGroupId", "com.infomaniak.pdfview")
        set("libVersionName", if (project.hasProperty("snapshot")) "$baseVersionName-SNAPSHOT" else baseVersionName)
        set("libArtifactId", "android-pdfview")
        set("javaVersion", JavaVersion.VERSION_17)
    }

    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("maven-publish")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.nmcp.aggregation)
}

// Aggregates the published android-pdf-viewer module into a single Maven Central deployment.
nmcpAggregation {
    centralPortal {
        username = providers.gradleProperty("ossrhUsername")
            .orElse(providers.environmentVariable("ossrhUsername"))
            .orNull
        password = providers.gradleProperty("ossrhPassword")
            .orElse(providers.environmentVariable("ossrhPassword"))
            .orNull
        publishingType = "AUTOMATIC"
    }
}

dependencies {
    nmcpAggregation(project(":android-pdf-viewer"))
}

allprojects {
    repositories {
        google()
        // mavenLocal()
        mavenCentral()
        maven {
            name = "infomaniakReposiliteRepository"
            url = uri("https://maven.infomaniak.app/releases")
            content { includeGroup("com.infomaniak.pdfiumandroid") }
        }
        maven {
            name = "infomaniakReposiliteRepositorySnapshots"
            url = uri("https://maven.infomaniak.app/snapshots")
            content { includeGroup("com.infomaniak.pdfiumandroid") }
        }
    }
}
