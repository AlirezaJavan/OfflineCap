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

            group = providers.gradleProperty("GROUP").get()
            version = providers.gradleProperty("VERSION_NAME").get()

            extensions.configure<MavenPublishBaseExtension> {
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
