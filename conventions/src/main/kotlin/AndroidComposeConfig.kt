import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions

class AndroidComposeConfig : AndroidConfig() {

    override fun androidExtensionConfig(): AndroidCommonExtension.(Project, VersionCatalog) -> Unit {
        return { _, vlibs ->
            buildFeatures {
                compose = true
            }
            composeOptions {
                //https://developer.android.google.cn/jetpack/androidx/releases/compose-kotlin?hl=zh-cn
                kotlinCompilerExtensionVersion = vlibs.findVersion("androidx-compose-compiler").get().toString()
            }
        }
    }

    override fun dependenciesConfig(): DependencyHandlerScope.(VersionCatalog) -> Unit = { vlibs: VersionCatalog ->
        vlibs.findLibrary("androidx-compose-bom").ifPresent { bom ->
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("implementation", vlibs.findBundle("compose").get())
            add("debugImplementation", vlibs.findLibrary("androidx-compose-ui-tooling-preview").get())
            add("debugImplementation", vlibs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }


    override fun kotlinOptionsConfig(): KotlinCommonToolOptions.(Project) -> Unit = { project ->
        with(project) {
            freeCompilerArgs += buildComposeMetricsParameters()
//                    freeCompilerArgs += stabilityConfiguration()
            freeCompilerArgs += strongSkippingConfiguration()
        }
    }
}

private fun Project.buildComposeMetricsParameters(): List<String> {
    val metricParameters = mutableListOf<String>()
    val enableMetricsProvider = project.providers.gradleProperty("enableComposeCompilerMetrics")
    val relativePath = projectDir.relativeTo(rootDir)
    val buildDir = layout.buildDirectory.get().asFile
    val enableMetrics = (enableMetricsProvider.orNull == "true")
    if (enableMetrics) {
        val metricsFolder = buildDir.resolve("compose-metrics").resolve(relativePath)
        metricParameters.add("-P")
        metricParameters.add(
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + metricsFolder.absolutePath,
        )
    }

    val enableReportsProvider = project.providers.gradleProperty("enableComposeCompilerReports")
    val enableReports = (enableReportsProvider.orNull == "true")
    if (enableReports) {
        val reportsFolder = buildDir.resolve("compose-reports").resolve(relativePath)
        metricParameters.add("-P")
        metricParameters.add(
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" + reportsFolder.absolutePath
        )
    }

    return metricParameters.toList()
}

private fun Project.stabilityConfiguration() = listOf(
    "-P",
    "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=${project.rootDir.absolutePath}/compose_compiler_config.conf",
)

private fun Project.strongSkippingConfiguration() = listOf(
    "-P",
    "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
)

