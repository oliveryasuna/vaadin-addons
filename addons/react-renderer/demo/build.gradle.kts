plugins {
    id("java")
    alias(libs.plugins.springBoot)
    alias(libs.plugins.vaadin)
}

vaadin {
    frontendHotdeploy = false
    optimizeBundle = false
//    reactEnable = false
}

dependencies {
    // BOMs
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.vaadin.bom))

    // Vaadin
    implementation("com.vaadin", "vaadin-core")
    implementation("com.vaadin", "vaadin-spring-boot-starter")

    // Project
    implementation(project(":react-renderer"))
}

configurations.all {
    exclude("com.vaadin", "collaboration-engine")
    exclude("com.vaadin", "copilot")
//    exclude("com.vaadin", "flow-react")
    exclude("com.vaadin", "hilla-dev")
    exclude("com.vaadin", "vaadin-material-theme")
    exclude("com.vaadin.servletdetector", "throw-if-servlet3")
}
