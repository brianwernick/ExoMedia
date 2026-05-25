package release

import org.gradle.api.Project
import java.io.File
import java.util.Properties

object ReleaseInfo {
    private const val KEY_ARTIFACT_ID = "ARTIFACT_ID"
    private const val KEY_GROUP_ID = "GROUP_ID"

    private const val KEY_VERSION_MAJOR = "VERSION_MAJOR"
    private const val KEY_VERSION_MINOR = "VERSION_MINOR"
    private const val KEY_VERSION_PATCH = "VERSION_PATCH"

    fun getLibraryInfo(project: Project): LibraryInfo {
        val properties = readProperties(project)

        val version = LibraryInfo.Version(
            major = properties[KEY_VERSION_MAJOR].toString().toInt(),
            minor = properties[KEY_VERSION_MINOR].toString().toInt(),
            patch = properties[KEY_VERSION_PATCH].toString().toInt()
        )

        return LibraryInfo(
            artifactId = properties[KEY_ARTIFACT_ID].toString(),
            groupId = properties[KEY_GROUP_ID].toString(),
            version = version
        )
    }

    private fun readProperties(project: Project): Properties {
        val file = File("${project.rootDir}/buildSrc/libraryInfo.properties")

        return Properties().apply {
            load(file.inputStream())
        }
    }
}