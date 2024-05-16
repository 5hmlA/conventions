package knife.asm

import com.android.build.api.instrumentation.*
import com.android.tools.r8.internal.re
import knife.TransformWorker
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor


abstract class SurgeryInstrumentationParameters : InstrumentationParameters {

    @get:Input
    abstract val buildType: Property<String>

    @get:Input
    abstract val flavorName: Property<String>

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    abstract val classVisitor: Property<ClassVisitor>

    @get:Input
    abstract val instrumentChecker: Property<(ClassData)->Boolean>
}

//必须是抽象类，会自动实现部分方法
abstract class SurgeryAsmClassVisitorFactory : AsmClassVisitorFactory<SurgeryInstrumentationParameters> {

//    gradle会自动生成实现
//    override val parameters: Property<SurgeryInstrumentationParameters>
//        get() = TODO("Not yet implemented")

    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor {
        return parameters.get().classVisitor.get()
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return parameters.get().instrumentChecker.get()(classData)
    }
}