pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("plugins")
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
            version("springBoot", "3.5.4")
            version("vaadin", "24.8.0")

            // BOMs
            library("spring.boot.bom", "org.springframework.boot", "spring-boot-dependencies").versionRef("springBoot")
            library("vaadin.bom", "com.vaadin", "vaadin-bom").versionRef("vaadin")
            library("junit.bom", "org.junit", "junit-bom").version("5.10.0")
            library("mockito.bom", "org.mockito", "mockito-bom").version("5.12.0")

            // Libraries
            library("logback", "ch.qos.logback", "logback-classic").version("1.5.18")
            library("commonsLanguage", "com.oliveryasuna", "commons-language").version("6.1.0")
            library("apacheCommons.lang", "org.apache.commons", "commons-lang3").version("3.18.0")
            library("apacheCommons.collections", "org.apache.commons", "commons-collections4").version("4.5.0")
            library("apacheCommons.text", "org.apache.commons", "commons-text").version("1.13.1")
            library("apacheCommons.io", "commons-io", "commons-io").version("2.19.0")
            library("jetbrainsAnnotations", "org.jetbrains", "annotations").version("26.0.2")

            // Plugins
            plugin("versions", "com.github.ben-manes.versions").version("0.52.0")
            plugin("springBoot", "org.springframework.boot").versionRef("springBoot")
            plugin("vaadin", "com.vaadin").versionRef("vaadin")
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

rootProject.name = "vaadin-addons"

include(":react-renderer")
project(":react-renderer").projectDir = file("addons/react-renderer/addon")

include(":react-renderer-demo")
project(":react-renderer-demo").projectDir = file("addons/react-renderer/demo")
