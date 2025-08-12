plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
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

    // TODO: Set website URL.
    website = ""
    // TODO: Set VCS URL.
    vcsUrl = ""
}

tasks {
    named("check").configure {
        this.setDependsOn(
            this.dependsOn.filterNot {
                it is TaskProvider<*> && it.name == "detekt"
            } + named("detektMain"),
        )
    }
}
