package knife

import com.android.build.api.variant.Variant
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType

/**
 * 定义功能接口
 */
interface Knife {
    fun onVariants(action: (Variant) -> Unit)
}

/**
 * 定义功能 Extension
 * 具体实现委托给 KnifeImpl
 * plugin里面用到的实际上是 KnifeImpl
 */
abstract class KnifeExtension(private val knifeExtensionImpl: KnifeImpl) :
    Knife, ExtensionAware {
    override fun onVariants(action: (Variant) -> Unit) {
        knifeExtensionImpl.onVariants = action
    }
}

/**
 * 定义功能具体实现
 */
class KnifeImpl : Knife {
    var onVariants: ((Variant) -> Unit)? = null
    override fun onVariants(action: (Variant) -> Unit) {
        onVariants = action
    }

    fun createExtension(project: Project): KnifeExtension {
        return project.extensions.create(
            "knife", KnifeExtension::class.java,
            this
        )
    }
}

interface VariantKnifeAction {
    fun onArtifactBuilt(action: (String) -> Unit)

    fun asmTransform(configs: TransformConfig.() -> Unit)
}

abstract class VariantKnifeActionExtension(private val variantActionImpl: VariantKnifeActionImpl) :
    VariantKnifeAction {
    override fun onArtifactBuilt(action: (String) -> Unit) {
        variantActionImpl.onArtifactBuilt(action)
    }

    override fun asmTransform(configs: TransformConfig.() -> Unit) {
        variantActionImpl.asmTransform(configs)
    }
}

class VariantKnifeActionImpl : VariantKnifeAction {

    var doListenArtifact: () -> Unit = {}
    var listenArtifact: ((String) -> Unit)? = null

    var doAsmTransform: () -> Unit = {}
    var transformConfigs: (TransformConfig.() -> Unit)? = null

    override fun onArtifactBuilt(action: (String) -> Unit) {
        listenArtifact = action
        //外部build.gradle中回调回来,就执行plugin中的listenArtifact逻辑,创建task监听文件
        doListenArtifact.invoke()
    }

    override fun asmTransform(configs: TransformConfig.() -> Unit) {
        transformConfigs = configs
        doAsmTransform.invoke()
    }

    //怀孕,生成子extension
    fun createExtension(knifeExtension: KnifeExtension): VariantKnifeActionExtension {
        return knifeExtension.extensions.create(
            "utility",
            VariantKnifeActionExtension::class.java,
            this
        )
    }
}


fun Project.asmTransform(config: TransformConfig.() -> Unit) =
    extensions.getByType<KnifeExtension>().extensions.getByType<VariantKnifeAction>()
        .asmTransform(config)

fun Project.onArtifactBuilt(listen: (String) -> Unit) =
    extensions.getByType<KnifeExtension>().extensions.getByType<VariantKnifeAction>()
        .onArtifactBuilt(listen)

interface TransformConfig {
    fun configs(vararg configs: String)
}

class TransformConfigImpl : TransformConfig {
    val modifyConfigs = mutableListOf<String>()
    override fun configs(vararg configs: String) {
        modifyConfigs.addAll(configs)
    }
}