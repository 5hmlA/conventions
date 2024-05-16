package knife

import com.android.build.api.instrumentation.ClassData
import com.android.build.api.variant.Variant
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.objectweb.asm.ClassVisitor

interface KnifeExtension : ExtensionAware {
    //    fun onArtifactBuilt2(action: (String) -> Unit)
//    fun onArtifactBuilt2(action: (String) -> Unit)
    fun onVariants(action: (Variant) -> Unit)

}


abstract class KnifeExtensionImpl : KnifeExtension {
    var onVariants: ((Variant) -> Unit)? = null
    override fun onVariants(action: (Variant) -> Unit) {
        onVariants = action
    }
}

//怀孕,生成子extension
fun KnifeExtensionImpl.pregnant(): VariantActionImpl {
    return extensions.create("onArtifactBuilt", VariantActionImpl::class.java)
}


interface VariantAction {
    fun onArtifactBuilt(action: (String) -> Unit)

    fun registerAsmTransform(worker: TransformWorker.() -> (ClassData) -> Boolean)
}

abstract class VariantActionImpl : VariantAction {

    var doListenArtifact: () -> Unit = {}
    var listenArtifact: ((String) -> Unit)? = null

    var doAsmTransform: () -> Unit = {}
    var transformWorker: (TransformWorker.() -> (ClassData) -> Boolean)? = null

    override fun onArtifactBuilt(action: (String) -> Unit) {
        listenArtifact = action
        //外部build.gradle中回调回来,就执行plugin中的listenArtifact逻辑,创建task监听文件
        doListenArtifact.invoke()
    }

    override fun registerAsmTransform(worker: TransformWorker.() -> (ClassData) -> Boolean) {
        transformWorker = worker
        doAsmTransform.invoke()
    }
}

class TransformWorker(var classVisitor: ClassVisitor? = null)

fun VariantAction.tt() {
    onArtifactBuilt {

    }

//    registerAsmTransform {
//        classVisitor = ""
//
//        { classData ->
//            true
//        }
//
//    }
}

class AsmTransformWorker {

}