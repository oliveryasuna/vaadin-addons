package com.oliveryasuna.gradle.vaadinaddon

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip

class VaadinAddonPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("vaadinAddon", VaadinAddonExtension::class.java)
        val config = EffectiveConfiguration.get(project)

        project.pluginManager.apply("maven-publish")

        project.afterEvaluate {
            check(config.title.isPresent) { "vaadinAddon.title must be configured" }

            val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
            configureTasks(project, config, javaExtension)
        }
    }

    private fun configureTasks(
        project: Project,
        config: EffectiveConfiguration,
        javaExtension: JavaPluginExtension,
    ) {
        val title = config.title.get()
        val author = config.author.get()

        val generateMavenProperties = project.tasks.register("generateMavenProperties")
        val jarTask = project.tasks.named("jar", Jar::class.java)
        val sourcesJarTask = project.tasks.register("sourcesJar", Jar::class.java)
        val javadocJarTask = project.tasks.register("javadocJar", Jar::class.java)
        val generateVaadinManifest = project.tasks.register("generateVaadinManifest")
        val vaadinZipTask = project.tasks.register("vaadinZip", Zip::class.java)

        generateMavenProperties.configure { t ->
            t.group = "vaadin-addon"

            val mavenPropertiesFile = project.layout.buildDirectory.file("tmp/pom.properties")

            t.outputs.file(mavenPropertiesFile)

            t.doLast {
                mavenPropertiesFile.get().asFile.parentFile.mkdirs()
                mavenPropertiesFile.get().asFile.writeText(
                    """
                    groupId=${config.groupId.get()}
                    artifactId=${config.artifactId.get()}
                    version=${config.version.get()}

                    """.trimIndent(),
                )
            }
        }

        jarTask.configure { t ->
            // Set up manifest
            t.manifest { m ->
                m.attributes(
                    mapOf(
                        "Manifest-Version" to "1.0",
                        // TODO: Do not hardcode version.
                        "Created-By" to "Vaadin Addon Plugin 1.0.0",
                        "Build-Jdk-Spec" to javaExtension.sourceCompatibility.toString(),
                        "Implementation-Vendor" to author,
                        "Implementation-Title" to title,
                        "Implementation-Version" to project.version,
                        "Vaadin-Package-Version" to "1",
                    ),
                )
            }

            // Include Maven stuff
            t.into("META-INF/maven/${config.groupId.get()}/${config.artifactId.get()}") { f ->
                f.from(generateMavenProperties)
            }
            t.into("META-INF/maven/${config.groupId.get()}/${config.artifactId.get()}") { f ->
                f.from(project.tasks.named("generatePomFileForMavenPublication"))
                f.rename(".*", "pom.xml")
            }

            // Exclude unnecessary files
            t.exclude("META-INF/VAADIN/config/flow-build-info.json")
            t.exclude("META-INF/VAADIN/webapp/**/*")
        }

        sourcesJarTask.configure { t ->
            t.group = "vaadin-addon"

            t.from(javaExtension.sourceSets.getByName("main").allSource)

            t.archiveClassifier.set("sources")
        }

        javadocJarTask.configure { t ->
            t.group = "vaadin-addon"

            t.from(project.tasks.named("javadoc"))

            t.archiveClassifier.set("javadoc")
        }

        generateVaadinManifest.configure { t ->
            t.group = "vaadin-addon"

            t.dependsOn(jarTask)

            val manifestFile = project.layout.buildDirectory.file("tmp/MANIFEST.MF")

            t.outputs.file(manifestFile)

            t.doLast {
                manifestFile.get().asFile.parentFile.mkdirs()
                manifestFile.get().asFile.writeText(
                    """
                    Manifest-Version: 1.0
                    Vaadin-Package-Version: 1
                    Vaadin-Addon: ${jarTask.get().archiveBaseName.get()}-${jarTask.get().archiveVersion.get()}.jar
                    Implementation-Vendor: $author
                    Implementation-Title: $title
                    Implementation-Version: ${project.version}

                    """.trimIndent(),
                )
            }
        }

        vaadinZipTask.configure { t ->
            t.group = "vaadin-addon"

            t.dependsOn(jarTask, sourcesJarTask, javadocJarTask, generateVaadinManifest)

            t.archiveVersion.set(project.version.toString())

            t.from(jarTask.get().outputs)
            t.from(sourcesJarTask.get().outputs)
            t.from(javadocJarTask.get().outputs)
            t.from(project.layout.buildDirectory.file("tmp/MANIFEST.MF")) { f ->
                f.into("META-INF")
            }
            t.from(config.files)

            t.destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
        }
    }

}
