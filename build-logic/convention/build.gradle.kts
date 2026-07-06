plugins {
    `kotlin-dsl`
}

group = "io.github.alirezajavan.offlinecap.buildlogic"

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.compose.compiler.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.maven.publish.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kotlinJvm") {
            id = "offlinecap.kotlin.jvm"
            implementationClass = "KotlinJvmConventionPlugin"
        }
        register("androidLibrary") {
            id = "offlinecap.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "offlinecap.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "offlinecap.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("publish") {
            id = "offlinecap.publish"
            implementationClass = "PublishConventionPlugin"
        }
    }
}
