plugins {
    id("offlinecap.kotlin.jvm")
    id("offlinecap.publish")
}

dependencies {
    api(project(":offlinecap-core"))

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
