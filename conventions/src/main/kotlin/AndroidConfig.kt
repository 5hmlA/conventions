import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.buildscript
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import wing.AndroidCommonExtension
import wing.AndroidComponentsExtensions
import wing.androidExtensionComponent
import wing.chinaRepos
import wing.log
import wing.red
import wing.vlibs
import java.io.File

open class AndroidConfig : Plugin<Project> {

    /**
     * ```kotlin
     *     override fun pluginConfigs(): PluginManager.() -> Unit = {
     *         //有需要的话执行父类逻辑
     *         super.pluginConfigs().invoke(this)
     *         //执行自己的逻辑
     *         apply("kotlin-android")
     *     }
     * ```
     */
    open fun pluginConfigs(): PluginManager.() -> Unit = {}

    /**
     * ```kotlin
     *     override fun androidExtensionConfig(): AndroidExtension.(Project, VersionCatalog) -> Unit {
     *         return { project, versionCatalog ->
     *             //有需要的话执行父类逻辑
     *             super.androidExtensionConfig().invoke(this,project,versionCatalog)
     *             //自己特有的逻辑
     *         }
     *     }
     * ```
     */
    open fun androidExtensionConfig(): AndroidCommonExtension.(Project, VersionCatalog) -> Unit = { _, _ -> }

    open fun androidComponentsExtensionConfig(): AndroidComponentsExtensions.(Project, VersionCatalog) -> Unit = { _, _ -> }

    open fun kotlinOptionsConfig(): KotlinCommonCompilerOptions.(Project) -> Unit = {}

    /**
     * ```kotlin
     *     override fun dependenciesConfig(): DependencyHandlerScope.(VersionCatalog) -> Unit = { vlibs: VersionCatalog ->
     *         //有需要的话执行父类逻辑
     *         super.dependenciesConfig().invoke(this, vlibs)
     *         //自己特有的逻辑
     *     }
     * ```
     */
    open fun dependenciesConfig(): DependencyHandlerScope.(VersionCatalog) -> Unit = { _ -> }

    override fun apply(target: Project) {
        var androidConfig: Android = AndroidBase()
        if (target.properties["config.android.room"] == "true") {
            androidConfig = AndroidRoom(androidConfig)
        }
//        val androidConfig = AndroidBase()
        with(target) {
            log("=========================== START【${this@AndroidConfig}】 =========================")
            log("常见构建自定义的即用配方，展示如何使用Android Gradle插件的公共API和DSL:")
            log("https://github.com/android/gradle-recipes")

            buildCacheDir()
            repoConfig()

            with(pluginManager) {
                androidConfig.pluginConfigs()(target)
                pluginConfigs()()
            }
            val catalog = vlibs
            androidExtensionComponent?.apply {
                finalizeDsl { android ->
                    with(android) {
                        androidConfig.androidExtensionConfig()(target, catalog)
                        androidExtensionConfig()(target, catalog)
                    }
                }
                androidConfig.androidComponentsExtensionConfig()(target, catalog)
                androidComponentsExtensionConfig()(target, catalog)
            }

            //https://kotlinlang.org/docs/gradle-compiler-options.html#target-the-jvm
            tasks.withType<KotlinJvmCompile>().configureEach {
                compilerOptions {
                    androidConfig.kotlinOptionsConfig()(target)
                    kotlinOptionsConfig()(target)
                }
            }
//            和上面等效
//            tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
//                compilerOptions {
//                    androidConfig.kotlinOptionsConfig()(target)
//                    kotlinOptionsConfig()(target)
//                }
//            }

            //com.android.build.gradle.internal.scope.MutableTaskContainer
            dependencies {
                androidConfig.dependenciesConfig()(target, catalog)
                dependenciesConfig()(catalog)
            }
            log("=========================== END【${this@AndroidConfig}】 =========================")
//            生成apk地址
//            https://github.com/android/gradle-recipes/blob/agp-8.4/allProjectsApkAction/README.md
//            com.android.build.gradle.internal.variant.VariantPathHelper.getApkLocation
//            com.android.build.gradle.internal.variant.VariantPathHelper.getDefaultApkLocation
//            com.android.build.gradle.tasks.PackageApplication

//            layout.buildDirectory.set(f.absolutePath)
//            修改as生成缓存的地址

//            transform
//            https://github.com/android/gradle-recipes/blob/agp-8.4/transformAllClasses/README.md
        }
    }

    private fun Project.buildCacheDir() {
        log("========= Project.layout ${layout.buildDirectory.javaClass} ${layout.buildDirectory.asFile.get().absolutePath}")
        log("👉 set『build.cache.root.dir=D』can change build cache dir to D:/0buildCache/")
//      log("========= Project.buildDir ${buildDir} =========================")
        val buildDir = properties["build.cache.root.dir"] ?: System.getenv("build.cache.root.dir")
        buildDir?.let {
            //https://github.com/gradle/gradle/issues/20210
            //https://docs.gradle.org/current/userguide/upgrading_version_8.html#deprecations
            layout.buildDirectory.set(File("$it:/0buildCache/${rootProject.name}/${project.name}"))
            log("👉『${project.name}』buildDir is relocated to -> ${project.layout.buildDirectory.asFile.get()} 🥱")
            //buildDir = File("E:/0buildCache/${rootProject.name}/${project.name}")
        }
    }

    private fun Project.repoConfig() {
        buildscript {
            repositories.removeAll { true }
            repositories {
                chinaRepos()
            }
            repositories.forEach {
                log("> Project.buildscript repositories ${it.name} >  =========================")
            }
        }

        repositories.forEach {
            log("> Project.repositories ${it.name} > ${it.javaClass} =========================")
        }
        try {
            repositories {
                chinaRepos()
            }
        } catch (e: Exception) {
            log(
                """
                        ${e.message}\n
                        报错原因是 repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) 导致的
                        修改为如下设置:
                            dependencyResolutionManagement {
                                repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
                            }
                        """.trimIndent().red
            )
        }
        repositories.forEach {
            log("> Project.repositories ${it.name} > ${(it as DefaultMavenArtifactRepository).url} =========================")
        }
    }
}