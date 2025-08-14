package com.oliveryasuna.gradle.vaadinaddon

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.io.FileOutputStream

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

    @Suppress("LongMethod")
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
                        "Created-By" to "Vaadin Addon Plugin $PLUGIN_VERSION",
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

            // Generate INDEX.LIST and add to JAR
            t.doLast {
                val jarFile = t.archiveFile.get().asFile
                val tempJarFile = project.layout.buildDirectory.file("tmp/temp-${jarFile.name}").get().asFile
                tempJarFile.parentFile.mkdirs()
                
                // Create INDEX.LIST content
                val indexContent = JarFile(jarFile).use { jar ->
                    val allDirectories = mutableSetOf<String>()
                    
                    jar.entries().asSequence()
                        .filterNot { it.isDirectory }
                        .map { it.name }
                        .filter { it.contains("/") }
                        .forEach { path ->
                            // Add all parent directories
                            var currentPath = path
                            while (currentPath.contains("/")) {
                                currentPath = currentPath.substringBeforeLast("/")
                                allDirectories.add(currentPath)
                            }
                        }
                    
                    val packages = allDirectories
                        .sorted()
                        .joinToString("\n")
                    "JarIndex-Version: 1.0\n\n${jarFile.name}\n$packages\n"
                }
                
                // Create new JAR with INDEX.LIST
                JarOutputStream(FileOutputStream(tempJarFile)).use { newJar ->
                    // Add INDEX.LIST first
                    val indexEntry = ZipEntry("META-INF/INDEX.LIST")
                    newJar.putNextEntry(indexEntry)
                    newJar.write(indexContent.toByteArray())
                    newJar.closeEntry()
                    
                    // Copy existing entries
                    JarFile(jarFile).use { oldJar ->
                        oldJar.entries().asSequence().forEach { entry ->
                            if (!entry.isDirectory) {
                                newJar.putNextEntry(ZipEntry(entry.name))
                                oldJar.getInputStream(entry).copyTo(newJar)
                                newJar.closeEntry()
                            }
                        }
                    }
                }
                
                // Replace original JAR
                jarFile.delete()
                tempJarFile.renameTo(jarFile)
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
            t.from(config.files)

            t.archiveVersion.set(config.version.get())
            t.destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
        }
    }

}
