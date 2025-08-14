plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
}

gradlePlugin {
    plugins {
        create("com.oliveryasuna.vaadin.addon") {
            id = "com.oliveryasuna.vaadin.addon"
            implementationClass = "com.oliveryasuna.gradle.vaadinaddon.VaadinAddonPlugin"
            version = project.version.toString()
            description = "Gradle plugin for Vaadin add-ons."
            displayName = "Vaadin Add-on Gradle Plugin"
            tags = listOf("vaadin", "vaadin-flow", "gradle", "java", "groovy", "kotlin")
        }
    }

    website = "https://github.com/oliveryasuna/vaadin-addons"
    vcsUrl = "https://github.com/oliveryasuna/vaadin-addons"
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(layout.buildDirectory.dir("generated/kotlin"))
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
}

tasks {
    val generateVersionFileTask = register("generateVersionFile") {
        val outputDir = layout.buildDirectory.dir("generated/kotlin/com/oliveryasuna/gradle/vaadinaddon")
        val outputFile = outputDir.get().asFile.resolve("PluginVersion.kt")

        inputs.property("version", project.version)
        outputs.file(outputFile)

        doLast {
            outputDir.get().asFile.mkdirs()
            outputFile.writeText("""
                package com.oliveryasuna.gradle.vaadinaddon

                internal const val PLUGIN_VERSION: String = "${project.version}"

                """.trimIndent()
            )
        }
    }

    compileKotlin {
        dependsOn(generateVersionFileTask)
    }
    
    withType<Jar> {
        if(name == "sourcesJar") {
            dependsOn(generateVersionFileTask)
        }
    }

    named("check").configure {
        this.setDependsOn(
            this.dependsOn.filterNot {
                it is TaskProvider<*> && it.name == "detekt"
            } + named("detektMain"),
        )
    }
}
