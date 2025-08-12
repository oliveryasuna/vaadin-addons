package com.oliveryasuna.gradle.vaadinaddon

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

abstract class VaadinAddonExtension @Inject constructor(project: Project) {

    abstract val groupId: Property<String>

    abstract val artifactId: Property<String>

    abstract val version: Property<String>

    abstract val title: Property<String>

    abstract val author: Property<String>

    val files: ConfigurableFileCollection = project.objects.fileCollection()

    companion object {

        fun get(project: Project): VaadinAddonExtension =
            project.extensions.getByType(VaadinAddonExtension::class.java)

    }

}

class EffectiveConfiguration(
    project: Project,
    extension: VaadinAddonExtension,
) {

    val groupId: Provider<String> = extension.groupId
        .convention(project.provider { project.group.toString() })

    val artifactId: Provider<String> = extension.artifactId
        .convention(project.provider { project.name })

    val version: Provider<String> = extension.version
        .convention(project.provider { project.version.toString() })

    val title: Provider<String> =
        extension.title
            .convention(project.provider { project.name })

    val author: Provider<String> = extension.author

    val files: ConfigurableFileCollection = extension.files
        .convention(project.objects.fileCollection().from(
            project.file("README.md"),
            project.file("LICENSE")
        ))

    companion object {

        fun get(project: Project): EffectiveConfiguration =
            EffectiveConfiguration(project, VaadinAddonExtension.get(project))

    }

}
