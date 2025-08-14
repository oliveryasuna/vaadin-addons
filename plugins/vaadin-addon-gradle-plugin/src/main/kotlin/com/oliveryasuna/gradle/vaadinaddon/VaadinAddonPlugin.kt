package com.oliveryasuna.gradle.vaadinaddon

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
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

            ConfigureTasks(project, config, javaExtension).apply()
        }
    }

}

private class ConfigureTasks(
    private val project: Project,
    private val config: EffectiveConfiguration,
    private val javaExtension: JavaPluginExtension
) {

    fun apply() {
        val generateMavenProperties = project.tasks.register("generateMavenProperties")
        val jarTask = project.tasks.named("jar", Jar::class.java)
        val sourcesJarTask = project.tasks.register("sourcesJar", Jar::class.java)
        val javadocJarTask = project.tasks.register("javadocJar", Jar::class.java)
        val generateVaadinManifest = project.tasks.register("generateVaadinManifest")
        val vaadinZipTask = project.tasks.register("vaadinZip", Zip::class.java)

        configureGenerateMavenPropertiesTask(generateMavenProperties)
        configureJarTask(jarTask, generateMavenProperties)
        configureSourcesJarTask(sourcesJarTask)
        configureJavadocJarTask(javadocJarTask)
        configureGenerateVaadinManifestTask(generateVaadinManifest, jarTask)
        configureVaadinZipTask(vaadinZipTask, jarTask, sourcesJarTask, javadocJarTask, generateVaadinManifest)
    }

    private fun configureGenerateMavenPropertiesTask(
        generateMavenProperties: TaskProvider<Task>
    ) {
        generateMavenProperties.configure { t ->
            t.group = "vaadin-addon"

            t.inputs.property("groupId", config.groupId)
            t.inputs.property("artifactId", config.artifactId)
            t.inputs.property("version", config.version)

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
    }

    private fun configureJarTask(
        jarTask: TaskProvider<Jar>,
        generateMavenProperties: TaskProvider<Task>
    ) {
        jarTask.configure { t ->
            // Set up manifest
            t.manifest { m ->
                m.attributes(
                    mapOf(
                        "Manifest-Version" to "1.0",
                        // TODO: Do not hardcode version.
                        "Created-By" to "Vaadin Addon Plugin 1.0.0",
                        "Build-Jdk-Spec" to javaExtension.sourceCompatibility.toString(),
                        "Implementation-Vendor" to config.author.get(),
                        "Implementation-Title" to config.title.get(),
                        "Implementation-Version" to config.version.get(),
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
    }

    private fun configureSourcesJarTask(
        sourcesJarTask: TaskProvider<Jar>
    ) {
        sourcesJarTask.configure { t ->
            t.group = "vaadin-addon"

            t.from(javaExtension.sourceSets.getByName("main").allSource)

            t.archiveClassifier.set("sources")
        }
    }

    private fun configureJavadocJarTask(
        javadocJarTask: TaskProvider<Jar>
    ) {
        javadocJarTask.configure { t ->
            t.group = "vaadin-addon"

            t.from(project.tasks.named("javadoc"))

            t.archiveClassifier.set("javadoc")
        }
    }

    private fun configureGenerateVaadinManifestTask(
        generateVaadinManifest: TaskProvider<Task>,
        jarTask: TaskProvider<Jar>
    ) {
        generateVaadinManifest.configure { t ->
            t.group = "vaadin-addon"

            t.dependsOn(jarTask)
            t.inputs.property("title", config.title)
            t.inputs.property("version", config.version)
            t.inputs.property("author", config.author)

            val manifestFile = project.layout.buildDirectory.file("tmp/MANIFEST.MF")

            t.outputs.file(manifestFile)

            t.doLast {
                manifestFile.get().asFile.parentFile.mkdirs()
                manifestFile.get().asFile.writeText(
                    """
                    Manifest-Version: 1.0
                    Vaadin-Package-Version: 1
                    Vaadin-Addon: ${jarTask.get().archiveBaseName.get()}-${jarTask.get().archiveVersion.get()}.jar
                    Implementation-Vendor: ${config.author.get()}
                    Implementation-Title: ${config.title.get()}
                    Implementation-Version: ${config.version.get()}

                    """.trimIndent(),
                )
            }
        }
    }

    private fun configureVaadinZipTask(
        vaadinZipTask: TaskProvider<Zip>,
        jarTask: TaskProvider<Jar>,
        sourcesJarTask: TaskProvider<Jar>,
        javadocJarTask: TaskProvider<Jar>,
        generateVaadinManifest: TaskProvider<Task>
    ) {
        vaadinZipTask.configure { t ->
            t.group = "vaadin-addon"

            t.dependsOn(jarTask, sourcesJarTask, javadocJarTask, generateVaadinManifest)
            t.inputs.property("version", config.version)
            t.inputs.property("files", config.files)

            t.from(jarTask)
            t.from(sourcesJarTask)
            t.from(javadocJarTask)
            t.into("META-INF") { i ->
                i.from(generateVaadinManifest)
            }
            // TODO: INDEX.LIST
            t.from(config.files)

            t.archiveVersion.set(config.version.get())
            t.destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
        }
    }

}
