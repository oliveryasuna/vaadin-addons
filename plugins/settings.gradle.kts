rootProject.name = "plugins"

include(":vaadin-addon-gradle-plugin")

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
            // BOMs
            library("junit.bom", "org.junit", "junit-bom").version("5.10.0")
            library("mockito.bom", "org.mockito", "mockito-bom").version("5.12.0")

            // Plugins
            plugin("kotlin", "org.jetbrains.kotlin.jvm").version("2.2.0")
            plugin("detekt", "io.gitlab.arturbosch.detekt").version("1.23.8")
            plugin("versions", "com.github.ben-manes.versions").version("0.52.0")
            plugin("pluginPublish", "com.gradle.plugin-publish").version("1.3.1")
        }
    }
}

plugins {
    id("com.gradle.develocity") version "4.1"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        publishing.onlyIf { !System.getenv("CI").isNullOrEmpty() && it.buildResult.failures.isNotEmpty() }

        capture {
            resourceUsage = true
            buildLogging = false
            testLogging = false
        }

        obfuscation {
            username { _ -> "__username_redacted__" }
            hostname { _ -> "__hostname_redacted__" }
            ipAddresses { addresses -> addresses.map { _ -> "__ip_redacted__"} }
            externalProcessName { _ -> "__command_redacted__" }
        }

        value("Project Name", rootProject.name)
        value("Build Branch", System.getenv("GITHUB_REF")?.replace("refs/heads/", "") ?: "N/A")
        value("Build Commit", System.getenv("GITHUB_SHA") ?: "N/A")

        tag(System.getProperty("os.name"))
        val workflowLink = System.getenv("GITHUB_ACTIONS")?.let { "https://github.com/${System.getenv("GITHUB_REPOSITORY")}/actions/runs/${System.getenv("GITHUB_RUN_ID")}" } ?: "N/A"
        if(workflowLink != "N/A") {
            tag("CI")
            value("Workflow Link", workflowLink)
        } else {
            tag("Local")
        }
    }
}
