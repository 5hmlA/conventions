import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.plugins.PluginManager
import org.gradle.internal.impldep.org.eclipse.jgit.lib.ObjectChecker.encoding
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import kotlin.jvm.optionals.getOrNull

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
                //如果根build.gradle没在plugins中apply的话这里无法依赖，之后补充自动依赖
                apply("kotlin-android")
//                apply("org.jetbrains.kotlin.android")
                apply("kotlin-parcelize")
                //</editor-fold>
                pluginConfigs()()
            }
            val catalog = vlibs
            val catalogWings = vWings
            androidComponents?.apply {
                finalizeDsl { android ->
                    with(android) {
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
//                          isCoreLibraryDesugaringEnabled = true
                        }
                        //</editor-fold>
                        androidExtensionConfig()(target, catalog)
                    }
                }
                androidComponentsExtensionConfig()(target, catalog)
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
                catalogWings?.findBundle("android-project")?.getOrNull()?.let { androidProject ->
                    log("implementation(android-project)")
                    add("implementation", androidProject)
                } ?: run {
                    log("implementation(androidx...appcompat)")
                    add("implementation", catalog.findLibrary("androidx-navigation-ui-ktx").get())
                    add("implementation", catalog.findLibrary("androidx-navigation-fragment-ktx").get())
                    add("implementation", catalog.findLibrary("lifecycle-livedata-ktx").get())
                    add("implementation", catalog.findLibrary("lifecycle-viewmodel-ktx").get())
                    add("implementation", catalog.findLibrary("google-material").get())
                    add("implementation", catalog.findLibrary("androidx-appcompat").get())
                    add("implementation", catalog.findLibrary("androidx-core-ktx").get())
                    add("implementation", catalog.findLibrary("androidx-constraintlayout").get())
                }
                catalogWings?.findBundle("sparkj")?.ifPresent { sparkj ->
                    log("implementation(sparkj)")
                    add("implementation", sparkj)
                }
                catalogWings?.findBundle("android-view")?.ifPresent { sparkj ->
                    log("implementation(android-view)")
                    add("implementation", sparkj)
                }

                catalog.findBundle("koin-bom").ifPresent { koinBom ->
                    log("implementation(koin-bom)")
                    add("implementation", platform(koinBom))
                    add("implementation", catalog.findBundle("koin").get())
                }

                catalog.findBundle("okhttp-bom").ifPresent { okhttpBom ->
                    log("implementation(okhttp-bom)")
                    add("implementation", platform(okhttpBom))
                    add("implementation", catalog.findBundle("okhttp").get())
                }
                catalog.findBundle("ktor").ifPresent { ktor ->
                    log("implementation(ktor)")
                    add("implementation", ktor)
                }
                catalog.findLibrary("test-junit").ifPresent { jUnit ->
                    add("testImplementation", jUnit)
                }
                catalog.findBundle("androidx-benchmark").ifPresent { androidxBenchmark ->
//                    包括 androidx-test-ext-junit , androidx-test-espresso-core
                    add("androidTestImplementation", androidxBenchmark)
                }
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