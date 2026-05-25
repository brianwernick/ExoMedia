package util

import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.internal.logging.text.StyledTextOutput.Style
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.kotlin.dsl.support.serviceOf

/**
 * Writes a simplified test result summary to the console, for each configured module,
 * in the format:
 * ```
 * ╔═ module-name ════════════════════════════╗
 * ║ 24 Total, 18 Passed, 6 Failed, 0 Skipped ║
 * ╚══════════════════════════════════════════╝
 * ```
 *
 * To configure this for all modules, add the below code to the root `build.gradle.kts`
 * ```kotlin
 * allprojects {
 *     tasks.withType(Test::class).configureEach {
 *         TestOutput.configure(this)
 *     }
 * }
 * ```
 */
object TestOutput {
    fun <T : AbstractTestTask> configure(task: T) {
        with(task) {
            testLogging {
                outputs.upToDateWhen { false }
                events("failed", "skipped", "standardOut")
                addTestListener(TestResultListener(task.project))
            }
        }
    }

    private fun printModuleResults(
        title: String,
        results: ResultSummary,
        project: Project
    ) {
        val summaryItems = listOf(
            "${results.total} Total",
            "${results.passed} Passed",
            "${results.failed} Failed",
            "${results.skipped} Skipped"
        )

        val summary = summaryItems.joinToString(
            separator = ", ",
            prefix = " ",
            postfix = " "
        )

        val style = when {
            results.failed > 0 -> Style.Failure
            results.skipped > 0 -> Style.Info
            else -> Style.Success
        }

        project.printTestResults(
            title = " $title ",
            summary = summary,
            style = style
        )
    }

    private fun Project.printTestResults(
        title: String,
        summary: String,
        style: Style,
    ) {
        val maxWidth = maxOf(title.length, summary.length)

        val styleFactory = serviceOf<StyledTextOutputFactory>()
        val output = styleFactory.create("test-output").withStyle(style)

        output.apply {
            println("╔═${title}${fill('═', length = maxWidth - title.length - 1)}╗")
            println("║$summary${fill(' ', length = maxWidth - summary.length)}║")
            println("╚${fill('═', length = maxWidth)}╝")
        }
    }

    private fun fill(char: Char, length: Int): String {
        return buildString {
            repeat(length) {
                append(char)
            }
        }
    }

    private data class ResultSummary(
        val passed: Long,
        val failed: Long,
        val skipped: Long,
        val total: Long
    ) {
        companion object {
            fun forResult(result: TestResult): ResultSummary {
                return ResultSummary(
                    passed = result.successfulTestCount,
                    failed = result.failedTestCount,
                    skipped = result.skippedTestCount,
                    total = result.testCount
                )
            }
        }
    }

    private class TestResultListener(private val project: Project) : TestListener {
        override fun beforeSuite(suite: TestDescriptor?) {}
        override fun afterSuite(suite: TestDescriptor?, result: TestResult?) {
            if (suite?.parent == null && result != null) {
                // The displayName is in the format "project '{moduleName}'"
                val nameRegex = """project '([a-zA-Z:]+)'""".toRegex()
                val matchResult = nameRegex.find(project.displayName)
                val moduleName = matchResult?.groupValues?.getOrNull(1) ?: project.displayName

                printModuleResults(
                    title = moduleName,
                    results = ResultSummary.forResult(result),
                    project = project
                )
            }
        }

        override fun beforeTest(testDescriptor: TestDescriptor?) {}
        override fun afterTest(testDescriptor: TestDescriptor?, result: TestResult?) {}
    }
}