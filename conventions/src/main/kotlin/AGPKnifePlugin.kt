import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.Variant
import knife.KnifeExtension
import knife.TaskListenApk
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class AGPKnifePlugin : AbsAndroidConfig() {
    override fun androidComponentsExtensionConfig(): AndroidComponentsExtensions.(Project, VersionCatalog) -> Unit = { project, versionCatalog ->
        val knifeExtension = project.extensions.create("knife", KnifeExtension::class.java)
        onVariants(selector().withBuildType("release")) { variant: Variant ->
            val taskProvider = project.tasks.register<TaskListenApk>("listenApkFor${variant.name}") {
                apkFolder.set(variant.artifacts.get(SingleArtifact.APK))
                builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            }

            //https://developer.android.google.cn/build/releases/gradle-plugin-api-updates?hl=zh-cn
            variant.artifacts.use(taskProvider).wiredWith {
                it.apkFolder
            }.toListenTo(SingleArtifact.APK)
        }
    }
}