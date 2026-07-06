plugins {
    id("offlinecap.android.library")
    id("offlinecap.publish")
}

android {
    namespace = "io.github.alirezajavan.offlinecap"
}

dependencies {
    api(project(":offlinecap-core"))
    api(project(":offlinecap-audio"))
    api(project(":offlinecap-scribe"))
    api(project(":offlinecap-lingua"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testRuntimeOnly(libs.junit.launcher)

    // Test fixtures for integration tests
    testImplementation(testFixtures(project(":offlinecap-core")))
}

mavenPublishing {
    pom {
        name.set("OfflineCap")
        description.set("Full offline video captioning library for Android")
    }
}
