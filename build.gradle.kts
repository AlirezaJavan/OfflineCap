plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.dokka) apply false
}

// Point git at the version-controlled hook directory so the pre-commit ktlint
// check is shared by every contributor. Idempotent; runs on any Gradle invocation.
if (file(".git").exists()) {
    runCatching {
        providers
            .exec { commandLine("git", "config", "core.hooksPath", ".githooks") }
            .result
            .get()
    }
}

tasks.register<Exec>("installGitHooks") {
    group = "git hooks"
    description = "Activates the version-controlled .githooks pre-commit hook."
    commandLine("git", "config", "core.hooksPath", ".githooks")
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt", "**/src/main/cpp/whisper/**")
            ktlint("1.5.0")
            trimTrailingWhitespace()
            leadingTabsToSpaces()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint("1.5.0")
            trimTrailingWhitespace()
            leadingTabsToSpaces()
            endWithNewline()
        }
    }
}
