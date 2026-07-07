import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class PublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.vanniktech.maven.publish")
                apply("org.jetbrains.dokka")
            }

            val groupId = providers.gradleProperty("GROUP").get()
            // Each module versions independently: "offlinecap-core" -> VERSION_OFFLINECAP_CORE in
            // gradle.properties. Falls back to the shared VERSION_NAME only for a module that hasn't
            // been given its own entry yet (e.g. right after scaffolding a brand-new module).
            val modulePropertyName = "VERSION_" + name.uppercase().replace("-", "_")
            val moduleVersion =
                providers
                    .gradleProperty(modulePropertyName)
                    .orElse(providers.gradleProperty("VERSION_NAME"))
                    .get()

            group = groupId
            version = moduleVersion

            extensions.configure<MavenPublishBaseExtension> {
                // com.vanniktech.maven.publish auto-applies the shared GROUP/VERSION_NAME to
                // project.group/version on its own; coordinates() is the supported override so our
                // per-module version always wins on the published Maven coordinates regardless of
                // that internal timing.
                coordinates(groupId, name, moduleVersion)
                configure(
                    if (pluginManager.hasPlugin("com.android.library")) {
                        AndroidSingleVariantLibrary(
                            javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"),
                            sourcesJar = SourcesJar.Sources(),
                            variant = "release",
                        )
                    } else {
                        KotlinJvm(
                            javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"),
                            sourcesJar = SourcesJar.Sources(),
                        )
                    },
                )
                publishToMavenCentral(automaticRelease = true)
                signAllPublications()

                // url/licenses/developers/scm are populated automatically from the POM_* keys
                // in gradle.properties by the com.vanniktech.maven.publish plugin itself
                // (MavenPublishPlugin calls pomFromGradleProperties() on apply); only
                // name/description are left for each module to set explicitly.
            }
        }
    }
}
