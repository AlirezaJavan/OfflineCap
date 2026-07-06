plugins {
    id("offlinecap.kotlin.jvm")
    id("offlinecap.publish")
    id("java-test-fixtures")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testFixturesApi(libs.kotlinx.coroutines.core)
}

mavenPublishing {
    pom {
        name.set("OfflineCap Core")
        description.set("Core domain and coordination logic for OfflineCap")
    }
}
