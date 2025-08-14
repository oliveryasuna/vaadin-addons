import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java")
    id("idea")
    alias(libs.plugins.versions)
}

allprojects {
    group = prop("project.group")
    version = prop("project.version")

    plugins.apply(rootProject.libs.plugins.versions.get().pluginId)

    tasks {
        dependencyUpdates {
            fun isStable(candidate: ModuleComponentIdentifier): Boolean {
                val containsStableKeyword: Boolean = listOf("RELEASE", "FINAL", "GA")
                    .any { keyword -> candidate.version.uppercase().contains(keyword) }

                if(containsStableKeyword) {
                    return true
                }

                val regex: Regex = if(candidate.displayName.startsWith("com.google.guava:guava")) {
                    "^[0-9,.v-]+(-r)?(-jre)$".toRegex();
                } else {
                    "^[0-9,.v-]+(-r)?$".toRegex();
                }

                return regex.matches(candidate.version)
            }

            gradleReleaseChannel = "current"
            revision = "release"
            checkConstraints = true
            resolutionStrategy {
                componentSelection {
                    all {
                        if(isStable(candidate).not()) {
                            reject("Unstable version.")
                        }
                    }
                }
            }
        }
    }
}

subprojects {
    // Java conventions
    pluginManager.withPlugin("java") {
        plugins.apply("idea")

        java {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        idea {
            module {
                isDownloadJavadoc = true
                isDownloadSources = true
            }
        }

        dependencies {
            // BOMs
            testImplementation(platform(libs.junit.bom))
            testImplementation(platform(libs.mockito.bom))

            // Testing
            testImplementation("org.junit.jupiter:junit-jupiter-api")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
        }

        // Apply BOMs to custom configurations.
        afterEvaluate {
            configurations.forEach { config ->
//                if(config.name.endsWith("Implementation") && config.name != "implementation" && config.name != "testImplementation") {
//                    dependencies.add(config.name, project.dependencies.platform(libs.vaadin))
//                }

                if(config.name.endsWith("TestImplementation") && config.name != "testImplementation") {
                    dependencies.add(config.name, project.dependencies.platform(libs.junit))
                    dependencies.add(config.name, project.dependencies.platform(libs.mockito))
                }
            }
        }

        tasks {
            compileJava {
                options.encoding = "UTF-8"
                options.release = 17
                options.isFork = true
                options.isIncremental = true
            }

            javadoc {
                options.encoding = "UTF-8"
            }

            test {
                useJUnitPlatform()

                testLogging {
                    exceptionFormat = TestExceptionFormat.FULL
                    showStandardStreams = true
                    showStackTraces = true

                    events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
                }

                reports {
                    junitXml.outputLocation = project.layout.buildDirectory.dir("reports/tests")
                }
            }
        }
    }

    // Vaadin addon conventions
    pluginManager.withPlugin("com.oliveryasuna.vaadin.addon") {
        group = "org.vaadin.addons.oliveryasuna"
    }
    afterEvaluate {
        withProjectType<ProjectType.Addon> {
            // Add Vaadin BOM.
            dependencies.add(configurations.implementation.name, project.dependencies.platform(libs.vaadin.bom))
        }
    }
}
