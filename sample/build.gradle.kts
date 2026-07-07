plugins {
    id("offlinecap.android.application")
    id("offlinecap.android.compose")
}

android {
    namespace = "io.github.alirezajavan.offlinecap.sample"

    defaultConfig {
        applicationId = "io.github.alirezajavan.offlinecap.sample"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":offlinecap"))

    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
