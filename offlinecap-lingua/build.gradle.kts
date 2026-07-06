plugins {
    id("offlinecap.android.library")
    id("offlinecap.publish")
}

android {
    namespace = "io.github.alirezajavan.offlinecap.lingua"
}

dependencies {
    api(project(":offlinecap-core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.mlkit.translate)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit.launcher)

    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

mavenPublishing {
    pom {
        name.set("OfflineCap Lingua")
        description.set("On-device translation for OfflineCap using ML Kit")
    }
}
