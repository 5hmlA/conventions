import org.gradle.internal.impldep.org.bouncycastle.asn1.x500.style.RFC4519Style.name
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "gradle-conventions"

val allGradleProject = rootProject.projectDir.toPath().listDirectoryEntries().filter {
    it.isDirectory() && !it.name.startsWith(".") && it.name != "gradle"
}
allGradleProject.forEachIndexed { index, path ->
    println(">>> ${path.name} --> $index")
    if (path.name == "conventions") {
        includeBuild(path) { name = path.name }
    }
}

//添加需要学习的项目,方便看gradle-recipes源码, 把此setting.gradle.kts放到根目录下,选择要看的模块即可
//includeBuild(allGradleProject[0].absolutePathString()) { name = "conventions" }


include(":app")
//include(":lib-test")