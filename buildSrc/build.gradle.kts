plugins {
    kotlin("jvm") version "2.2.0"
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(files("../plugins/vaadin-addon-gradle-plugin/build/libs/vaadin-addon-gradle-plugin-1.0.0.jar"))
}
