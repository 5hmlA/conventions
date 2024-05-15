package knife

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

abstract class TaskListenApk : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkFolder: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    var listenApk: ((String) -> Unit)? = null

    @TaskAction
    fun taskAction(){
        val builtArtifacts = builtArtifactsLoader.get().load(apkFolder.get())
            ?: throw RuntimeException("Cannot load APKs")
        builtArtifacts.elements.forEach {
            listenApk?.invoke(it.outputFile)
            println("  Got an APK at ${it.outputFile}")
        }

        // The above will only save the artifact themselves. It will not save the
        // metadata associated with them. Depending on our needs we may need to copy it.
        // This is required when transforming such an artifact. We'll do it here for demonstration
        // purpose.
//        builtArtifacts.save(outputDirectory)
    }
}