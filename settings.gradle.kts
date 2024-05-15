import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name


rootProject.name = "gradle-conventions"

val allGradleProject = rootProject.projectDir.toPath().listDirectoryEntries().filter {
    !it.name.startsWith(".") && it.isDirectory() && it.name != "gradle"
}
allGradleProject.forEachIndexed { index, path -> println(">>> ${path.name} --> $index") }

//添加需要学习的项目
includeBuild(allGradleProject[0].absolutePathString())