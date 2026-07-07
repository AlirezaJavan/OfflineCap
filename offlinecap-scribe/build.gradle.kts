plugins {
    id("offlinecap.android.library")
    id("offlinecap.publish")
    idea
}

idea {
    module {
        // Vendored whisper.cpp sources (879 files, ~39 MB): never edited here,
        // but IDE indexing/clangd churns through them on every sync otherwise.
        excludeDirs.add(file("src/main/cpp/whisper"))
    }
}

android {
    namespace = "io.github.alirezajavan.offlinecap.scribe"

    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                cppFlags("-O3", "-fexceptions", "-frtti")
                // Debug variants otherwise compile whisper.cpp with -O0, making
                // transcription 5-10x slower; inference must always be optimized.
                arguments(
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DGGML_NATIVE=OFF",
                    "-DWHISPER_BUILD_EXAMPLES=OFF",
                    "-DWHISPER_BUILD_TESTS=OFF",
                )
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }
}

dependencies {
    api(project(":offlinecap-core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.launcher)

    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

mavenPublishing {
    pom {
        name.set("OfflineCap Scribe")
        description.set("Speech-to-text transcription for OfflineCap using whisper.cpp")
    }
}
