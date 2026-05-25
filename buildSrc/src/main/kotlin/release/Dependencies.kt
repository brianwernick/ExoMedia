package release

import groovy.util.Node
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.get

object Dependencies {

    fun appendAll(
        node: Node,
        configurations: ConfigurationContainer,
    ) {
        configurations["api"].dependencies.forEach { dependency ->
            append(
                dependency = dependency,
                node = node,
                scope = "compile"
            )
        }

        configurations["implementation"].dependencies.forEach { dependency ->
            append(
                dependency = dependency,
                node = node,
                scope = "runtime"
            )
        }
    }

    private fun append(
        dependency: Dependency,
        node: Node,
        scope: String
    ) {
        if (dependency.name == "unspecified" || dependency.group == null || dependency.version == null) {
            return
        }

        with(node.appendNode("dependency")) {
            appendNode("groupId", dependency.group)
            appendNode("artifactId", dependency.name)
            appendNode("version", dependency.version)
            appendNode("scope", scope)
        }
    }
}