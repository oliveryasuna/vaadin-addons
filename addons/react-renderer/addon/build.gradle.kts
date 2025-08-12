plugins {
    id("java-library")
    alias(libs.plugins.vaadin)
    id("com.oliveryasuna.vaadin.addon")
}

val vaadinOptimizeBundle = providers.gradleProperty("vaadin.optimizeBundle").orNull?.toBoolean() ?: true

group = "org.vaadin.addons.oliveryasuna"
description = "A React renderer for Vaadin Flow."

vaadinAddon {
    title = "React Renderer"
    author = "Oliver Yasuna"
    files.from(rootProject.file("LICENSE"))
}

publishing {
    publications {
        create("maven", MavenPublication::class.java) {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                name = vaadinAddon.title
                description = "A React renderer for Vaadin Flow."
                url = "https://github.com/oliveryasuna/vaadin-addons"
            }
        }
    }
}

vaadin {}

dependencies {
    implementation(platform(libs.vaadin.bom))

    implementation("com.vaadin", "flow-server")
    implementation("com.vaadin", "flow-client")
    implementation("com.vaadin", "flow-data")
    implementation("com.vaadin", "vaadin-renderer-flow")
}

gradle.taskGraph.whenReady {
    vaadin {
        optimizeBundle = vaadinOptimizeBundle
    }
}
