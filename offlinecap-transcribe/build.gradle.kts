plugins {
    id("offlinecap.android.library")
    id("offlinecap.publish")
}

android {
    namespace = "io.github.alirezajavan.offlinecap.transcribe"
}

dependencies {
    api(project(":offlinecap-core"))
    api(project(":offlinecap-scribe"))
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

    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

mavenPublishing {
    pom {
        name.set("OfflineCap Transcribe")
        description.set("Audio-to-transcript speech recognition with model download management for OfflineCap")
    }
}
