import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import com.oliveryasuna.gradle.vaadinaddon.VaadinAddonExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.component.SoftwareComponentContainer

fun Project.prop(key: String): String = (System.getenv(key) ?: findProperty(key).toString())

sealed class ProjectType {
    data class Addon(
        val title: String
    ) : ProjectType()

    object Demo : ProjectType()
}

var Project.projectType: ProjectType?
    get() {
        val type = findProperty("projectType") as String? ?: return null
        return when(type) {
            "addon" -> ProjectType.Addon(
                title = findProperty("projectTitle") as String? ?: ""
            )
            "demo" -> ProjectType.Demo
            else -> null
        }
    }
    set(value) {
        when(value) {
            is ProjectType.Addon -> {
                val extraProperties = extensions.getByType(ExtraPropertiesExtension::class.java)
                extraProperties["projectType"] = "addon"
                extraProperties["projectTitle"] = value.title

                val vaadinAddon = extensions.getByType(VaadinAddonExtension::class.java)
                vaadinAddon.title.set(value.title)
                vaadinAddon.author.set("Oliver Yasuna")
                vaadinAddon.files.from(rootProject.file("LICENSE"))

                val publishing = extensions.getByType(PublishingExtension::class.java)
                publishing.publications.create("maven", MavenPublication::class.java) {
                    groupId = vaadinAddon.groupId.get()
                    artifactId = vaadinAddon.artifactId.get()
                    version = vaadinAddon.version.get()

                    from(components.getByName("java"))

                    pom {
                        name.set(vaadinAddon.title)
                        description.set(project.description)
                        url.set("https://github.com/oliveryasuna/vaadin-addons")
                    }
                }
            }
            is ProjectType.Demo -> {
                val extraProperties = extensions.getByType(ExtraPropertiesExtension::class.java)
                extraProperties["projectType"] = "demo"
            }
            null -> {
                // NO-OP
            }
        }
    }

fun Project.withProjectType(type: Class<out ProjectType>, action: Project.() -> Unit) {
    if(projectType != null && type.isInstance(projectType)) {
        action()
    }
}

inline fun <reified T : ProjectType> Project.withProjectType(action: Project.(T) -> Unit) {
    val currentType = projectType
    if(currentType is T) {
        action(currentType)
    }
}
