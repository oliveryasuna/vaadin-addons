import org.gradle.api.Plugin
import org.gradle.api.Project

class VaadinAddonPlugin : Plugin<Project> {
    override fun apply(project: Project) {
//        val libs = project.extensions.getByName("libs") as org.gradle.accessors.dm.LibrariesForLibs
//
//        project.dependencies.add("implementation", project.dependencies.platform(libs.vaadin.bom))
//        project.dependencies.add("implementation", "com.vaadin:flow-server")
        println("Hello, World!")
    }
}

open class VaadinAddonExtension {
}
