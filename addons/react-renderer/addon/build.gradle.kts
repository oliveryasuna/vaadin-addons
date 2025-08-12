plugins {
    id("java-library")
    alias(libs.plugins.vaadin)
}

afterEvaluate {
    gradle.taskGraph.whenReady {
        println(gradle.taskGraph.allTasks.joinToString(" - ") { it.name })
        if(gradle.taskGraph.hasTask("vaadinZip")) {
            vaadin {
                productionMode = true
                optimizeBundle = false
            }
        }
    }
}

dependencies {
    implementation(platform(libs.vaadin.bom))

    implementation("com.vaadin", "flow-server")
    implementation("com.vaadin", "flow-client")
    implementation("com.vaadin", "flow-data")
    implementation("com.vaadin", "vaadin-renderer-flow")
}

tasks {
    jar {
        manifest {
            attributes(
                "Manifest-Version" to "1.0",
                "Implementation-Vendor" to "Oliver Yasuna",
                "Implementation-Title" to "React Renderer",
                "Implementation-Version" to project.version,
                "Vaadin-Package-Version" to "1"
            )
        }
        exclude("META-INF/VAADIN/config/flow-build-info.json")
        exclude("META-INF/VAADIN/webapp/**/*")
    }

    val sourcesJar = register<Jar>("sourcesJar") {
        dependsOn("vaadinPrepareFrontend")
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }

    val javadocJar = register<Jar>("javadocJar") {
        from(javadoc)
        archiveClassifier.set("javadoc")
    }

    val createVaadinManifest = register("createVaadinManifest") {
        val manifestFile = layout.buildDirectory.file("tmp/vaadin/MANIFEST.MF")
        outputs.file(manifestFile)
        doLast {
            manifestFile.get().asFile.parentFile.mkdirs()
            manifestFile.get().asFile.writeText("""
                Manifest-Version: 1.0
                Vaadin-Package-Version: 1
                Vaadin-Addon: ${jar.get().archiveBaseName.get()}-${jar.get().archiveVersion.get()}.jar
                Implementation-Vendor: Oliver Yasuna
                Implementation-Title: React Renderer
                Implementation-Version: ${project.version}

            """.trimIndent())
        }
    }

    register<Zip>("vaadinZip") {
        group = "distribution"

        dependsOn(jar, sourcesJar, javadocJar, createVaadinManifest)
        archiveVersion.set(project.version.toString())

        from("../../") {
            include("LICENSE")
        }
        from(".") {
            include("README.md")
        }
        from(jar.get().outputs)
        from(named("sourcesJar").get().outputs)
        from(named("javadocJar").get().outputs)
        from(createVaadinManifest.get().outputs) {
            into("META-INF")
        }

        destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    }
}
