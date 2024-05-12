import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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

    open fun kotlinOptionsConfig(): KotlinCommonToolOptions.(Project) -> Unit = {}

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
        with(target) {
            log("=========================== START【${this@AndroidConfig}】 =========================")
            log("常见构建自定义的即用配方，展示如何使用Android Gradle插件的公共API和DSL:")
            log("https://github.com/android/gradle-recipes")

            buildCacheDir()

            repoConfig()

            with(pluginManager) {
                //<editor-fold desc="android project default plugin">
                apply("kotlin-android")
//                apply("org.jetbrains.kotlin.android")
                apply("kotlin-parcelize")
                //</editor-fold>
                pluginConfigs()()
            }
            val catalog = vlibs
            androidExtension?.apply {
                //<editor-fold desc="android project default config">
                compileSdk = catalog.findVersion("android-compileSdk").get().requiredVersion.toInt()
                defaultConfig {
                    minSdk = catalog.findVersion("android-minSdk").get().requiredVersion.toInt()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }
                buildFeatures {
                    buildConfig = true
                }
                compileOptions {
                    // Up to Java 11 APIs are available through desugaring
                    // https://developer.android.com/studio/write/java11-minimal-support-table
                    sourceCompatibility = JavaVersion.VERSION_18
                    targetCompatibility = JavaVersion.VERSION_18
                    encoding = "UTF-8"
//                    isCoreLibraryDesugaringEnabled = true
                }
                //</editor-fold>
                androidExtensionConfig()(target, catalog)


            }
            tasks.withType<KotlinCompile>().configureEach {
                kotlinOptions {
                    freeCompilerArgs += "-Xcontext-receivers"
                    jvmTarget = "18"
//                    kotlinOptionsPlugin().invoke(this)
                    kotlinOptionsConfig()(target)
                }
            }
            //com.android.build.gradle.internal.scope.MutableTaskContainer
            dependencies {
                //<editor-fold desc="android project default dependencies">
                val koin_bom = vlibs.findLibrary("koin-bom").get()
                add("implementation", platform(koin_bom))
                add("implementation", vlibs.findBundle("koin").get())

                val okhttp_bom = vlibs.findLibrary("okhttp-bom").get()
                add("implementation", platform(okhttp_bom))
                add("implementation", vlibs.findBundle("okhttp").get())

                add("implementation", vlibs.findBundle("android-project").get())
                add("implementation", vlibs.findBundle("sparkj").get())
                add("implementation", vlibs.findBundle("ktor").get())

                add("testImplementation", vlibs.findLibrary("test-junit").get())
                add("debugImplementation", vlibs.findLibrary("androidx-compose-ui-test-manifest").get())
                add("androidTestImplementation", vlibs.findBundle("androidx-benchmark").get())
                //</editor-fold>
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
//            log("========= Project.buildDir ${buildDir} =========================")
        properties["build.cache.root.dir"]?.let {
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