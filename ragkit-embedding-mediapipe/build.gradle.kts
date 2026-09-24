plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    signing
}

android {
    namespace = "com.ragkit.embedding.mediapipe"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(project(":ragkit-core"))
    implementation(libs.mediapipe.tasks.text)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.bundles.unit.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "io.github.gurkan26"
                artifactId = "ragkit-embedding-mediapipe"
                version = "0.1.0"

                pom {
                    name.set("RagKit Embedding MediaPipe")
                    description.set("On-device semantic search SDK for Android - MediaPipe Embedding Module")
                    url.set("https://github.com/Gurkan26/ragkit-android")
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("Gurkan26")
                            name.set("Gürkan Şentürk")
                            email.set("dev@ragkit.org")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/Gurkan26/ragkit-android.git")
                        developerConnection.set("scm:git:ssh://github.com/Gurkan26/ragkit-android.git")
                        url.set("https://github.com/Gurkan26/ragkit-android")
                    }
                }
            }
        }
    }

    signing {
        val signingKeyId = project.findProperty("signing.keyId") as String?
        val signingKey = project.findProperty("signing.key") as String?
        val signingPassword = project.findProperty("signing.password") as String?

        if (!signingKeyId.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
            if (!signingKey.isNullOrBlank()) {
                useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            }
            sign(publishing.publications["release"])
        }
    }
}