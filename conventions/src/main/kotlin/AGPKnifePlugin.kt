import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.Variant
import knife.*
import knife.asm.SurgeryAsmClassVisitorFactory
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.register
import wing.AndroidComponentsExtensions
import wing.isAndroidApplication
import wing.log

//https://developer.android.google.cn/build/extend-agp?hl=zh-cn
class AGPKnifePlugin : AbsAndroidConfig() {
    override fun androidComponentsExtensionConfig(): AndroidComponentsExtensions.(Project, VersionCatalog) -> Unit = { project, versionCatalog ->
        val knifeExtension = project.extensions.create("knife", KnifeExtensionImpl::class.java)
        val variantAction = knifeExtension.pregnant()
        project.log("knife -> knifeExtension:${knifeExtension.onVariants}")

        /**
         * plugin中的onVariants{}会优先执行 ,所以app中建议用 beforeVariants{}遍历和配置
         */
        onVariants(selector().all()) { variant: Variant ->
            knifeExtension.onVariants?.let {
                project.log("knife -> onVariant:${variant.name}")
                variantAction.doListenArtifact = {
                    //存在listenArtifact的时候才创建task
                    tryListenArtifact(variantAction, project, variant)
                }
                //transform
                variantAction.doAsmTransform = {
                    //存在listenArtifact的时候才创建task
                    tryAsmTransform(variantAction, project, variant)
                }

                it.invoke(variant)
            }
        }

    }

    private fun tryAsmTransform(variantAction: VariantActionImpl, project: Project, variant: Variant) {
        project.log("knife -> tryAsmTransform:${variant.name}")
        variantAction.transformWorker?.let { worker ->
            val transformWorker = TransformWorker()
            val checker = worker(transformWorker)
            //https://github1s.com/android/gradle-recipes/blob/agp-8.2/asmTransformClasses/build-logic/plugins/src/main/kotlin/CheckAsmTransformationTask.kt
            //https://github1s.com/android/gradle-recipes/blob/agp-8.2/asmTransformClasses/build-logic/plugins/src/main/kotlin/CustomPlugin.kt
            variant.instrumentation.transformClassesWith(
                SurgeryAsmClassVisitorFactory::class.java,
                InstrumentationScope.ALL,
            ) { params ->
                params.buildType.set(variant.buildType)
                params.flavorName.set(variant.flavorName)
                params.variantName.set(variant.name)
                params.instrumentChecker.set(checker)
                params.classVisitor.set(transformWorker.classVisitor)
            }
        }

    }

    private fun tryListenArtifact(variantAction: VariantActionImpl, project: Project, variant: Variant) {
        project.log("knife -> tryListenArtifact:${variant.name}")
        variantAction.listenArtifact?.let {
            val taskProvider = project.tasks.register<TaskListenApk>("listenApkFor${variant.name}") {
                apkFolder.set(variant.artifacts.get(SingleArtifact.APK))
                builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
                listenArtifact = variantAction.listenArtifact
            }
            //https://developer.android.google.cn/build/releases/gradle-plugin-api-updates?hl=zh-cn
            if (project.isAndroidApplication) {
                variant.artifacts.use(taskProvider).wiredWith {
                    it.apkFolder
                }.toListenTo(SingleArtifact.APK)
            } else {
                //https://developer.android.google.cn/build/releases/gradle-plugin-api-updates?hl=zh-cn
                variant.artifacts.use(taskProvider).wiredWith {
                    it.aarFile
                }.toListenTo(SingleArtifact.AAR)
            }
        }
    }
}