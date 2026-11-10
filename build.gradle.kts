plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android) apply false
    id("maven-publish")
}

android {
    namespace = "com.umirtech.analyticskit"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 25
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(platform(libs.firebase.bom))
    api(libs.firebase.analytics.ktx)

    testImplementation(libs.junit)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.umirtech"
            artifactId = "analyticskit"
            version = "1.0.0"
            afterEvaluate { from(components["release"]) }
        }
    }
}
