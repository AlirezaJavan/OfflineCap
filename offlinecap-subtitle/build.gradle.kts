plugins {
    id("offlinecap.kotlin.jvm")
    id("offlinecap.publish")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":offlinecap-core"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testRuntimeOnly(libs.junit.launcher)
}

mavenPublishing {
    pom {
        name.set("OfflineCap Subtitle")
        description.set("Transcript-to-subtitle formatting (SRT/WebVTT) for OfflineCap")
    }
}
