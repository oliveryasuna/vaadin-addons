plugins {
    id("java-library")
    id("com.oliveryasuna.vaadin.addon")
}

//group = "org.vaadin.addons.oliveryasuna"
description = "A React renderer for Vaadin Flow"
projectType = ProjectType.Addon("React Renderer")

dependencies {
    // Vaadin
    implementation("com.vaadin", "flow-client")
    implementation("com.vaadin", "flow-data")
    implementation("com.vaadin", "vaadin-renderer-flow")
}

configurations.all {
    exclude("com.vaadin", "signals")
    exclude("com.vaadin.servletdetector", "throw-if-servlet3")
}
