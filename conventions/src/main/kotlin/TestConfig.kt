import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifactsLoader
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register

abstract class DisplayApksTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkFolder: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @TaskAction
    fun taskAction() {

        val builtArtifacts = builtArtifactsLoader.get().load(apkFolder.get())
            ?: throw RuntimeException("Cannot load APKs")
        builtArtifacts.elements.forEach {
            println("Got an APK at ${it.outputFile}")
        }
    }
}

//https://medium.com/androiddevelopers/new-apis-in-the-android-gradle-plugin-f5325742e614
//class CustomSettings: Plugin<Settings> {
class TestConfig : Plugin<Project> {
    override fun apply(target: Project) {
        // Registers a callback on the application of the Android Application plugin.
        // This allows the CustomPlugin to work whether it's applied before or after
        // the Android Application plugin.
//        target.plugins.withType(AppPlugin::class.java){
//            //application
//            println("================AppPlugin=============================")
//        }
//        target.plugins.withType(BasePlugin::class.java){
//            //application or library
//            println("================BasePlugin=============================")
//        }

        with(target) {
            log("=========================== START【${this@TestConfig}】 =========================")

            log("常见构建自定义的即用配方，展示如何使用Android Gradle插件的公共API和DSL:")
            log("https://github.com/android/gradle-recipes")

            val projectName = name
//            ApplicationAndroidComponentsExtension -> ApplicationExtension
//            findByType 不存在返回空 getByType 不存在抛异常
            println("$projectName ApplicationExtension ===================== ${extensions.findByType<ApplicationExtension>()}")
            println("$projectName LibraryExtension ========================= ${extensions.findByType<LibraryExtension>()}")
            println("$projectName ApplicationAndroidComponentsExtension ==== ${extensions.findByType<ApplicationAndroidComponentsExtension>()}")
            println("$projectName LibraryAndroidComponentsExtension ======== ${extensions.findByType<LibraryAndroidComponentsExtension>()}")
//            println("$projectName BaseAppModuleExtension =================== ${extensions.findByType<BaseAppModuleExtension>()}")
            println("$projectName getByName android ======================== ${extensions.findByName("android")}")
            println("$projectName getByName android ======================== ${androidExtension?.javaClass}")
            androidComponents?.apply {
                onVariants { variant ->
                    println("variant.buildType = ${variant.buildType}")
//                    println("variant.buildConfigFields = ${variant.buildConfigFields.keySet().get().toStr()}")
//                    println("variant.applicationId = ${variant.applicationId.get()}")
                    println("variant.name = ${variant.name}")
                    val displayApksTaskProvider = tasks.register<DisplayApksTask>("${variant.name}DisplayApks") {
                        group = "display group"
                        apkFolder.set(variant.artifacts.get(SingleArtifact.APK))
                        builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
                    }
                    val apkParentDir = variant.artifacts.get(SingleArtifact.APK)
                    afterEvaluate {
                        apkParentDir.orNull?.let {
                            log(" > afterEvaluate > ${variant.flavorName} apkParentDir = ${apkParentDir.get().asFile.absolutePath}")
                            val builtArtifacts = variant.artifacts.getBuiltArtifactsLoader().load(apkParentDir.get())
                            log(" > afterEvaluate > ${variant.flavorName} builtArtifacts = $builtArtifacts")
                            builtArtifacts?.elements?.forEach {
                                log(" > afterEvaluate > ${variant.flavorName} builtArtifacts apk path = ${it.outputFile}")
                            }
                            apkParentDir.get().asFileTree.forEach {
                                log(" > afterEvaluate > ${variant.flavorName} apkParentDir FileTree > ${it.absolutePath}")
                            }
                        }
                    }
//                    onVariants(selector().all(), {
//                        variant.instrumentation.transformClassesWith(
//                            AsmClassVisitorFactoryImpl.class,
//                                    InstrumentationScope . Project
//                        ) { params ->
//                            params.x = "value"
//                        }
//                        instrumentation.setAsmFramesComputationMode(
//                            COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
//                        )
//                    })
                    log("------variant class--------------${variant.javaClass}")
//                    if (it is ApplicationVariantImpl) {
//                        log("---------ApplicationVariantImpl--------- ${it.name}")
//                    }
                }
            }
            log("=========================== END【${this@TestConfig}】 =========================")
        }
    }
}