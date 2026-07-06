package io.github.alirezajavan.offlinecap.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
    explicitApi: Boolean = true
) {
    commonExtension.apply {
        compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()

        defaultConfig.apply {
            minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
        }

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    configureKotlin<KotlinAndroidProjectExtension>(explicitApi)
}

internal fun Project.configureKotlinJvm(explicitApi: Boolean = true) {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configureKotlin<KotlinJvmProjectExtension>(explicitApi)
}

private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin(
    explicitApi: Boolean = true
) = configure<T> {
    if (explicitApi) {
        explicitApi()
    }

    when (this) {
        is KotlinAndroidProjectExtension -> {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }
        is KotlinJvmProjectExtension -> {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
