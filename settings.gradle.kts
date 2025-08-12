rootProject.name = "vaadin-addons"

include("addon")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.vaadin.com/vaadin-addons") }
    }

    versionCatalogs {
        create("libs") {
            // Versions
            version("vaadin", "24.8.0")

            // BOMs
            library("vaadin.bom", "com.vaadin", "vaadin-bom").versionRef("vaadin")
            library("junit.bom", "org.junit", "junit-bom").version("5.10.0")
            library("mockito.bom", "org.mockito", "mockito-bom").version("5.12.0")

            // Libraries
            library("logback", "ch.qos.logback", "logback-classic").version("1.5.18")
            library("jetbrainsAnnotations", "org.jetbrains", "annotations").version("26.0.2")

            // Plugins
            plugin("vaadin", "com.vaadin").versionRef("vaadin")
        }
    }
}
