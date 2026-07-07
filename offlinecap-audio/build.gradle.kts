plugins {
    id("offlinecap.android.library")
    id("offlinecap.publish")
}

android {
    namespace = "io.github.alirezajavan.offlinecap.audio"
}

dependencies {
    api(project(":offlinecap-core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit.launcher)

    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

mavenPublishing {
    pom {
        name.set("OfflineCap Audio")
        description.set("Audio extraction and processing for OfflineCap using MediaCodec")
    }
}
