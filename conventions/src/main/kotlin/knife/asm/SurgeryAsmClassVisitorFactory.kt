package knife.asm

import com.android.build.api.instrumentation.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

abstract class SurgeryInstrumentationParameters : InstrumentationParameters {

    @get:Input
    abstract val buildType: Property<String>

    @get:Input
    abstract val flavorName: Property<String>

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    @get:Optional
    abstract val classVisitor: Property<ClassVisitor>

    @get:Input
    @get:Optional
    abstract val instrumentChecker: Property<(ClassData) -> Boolean>
}

//必须是抽象类，会自动实现部分方法
abstract class SurgeryAsmClassVisitorFactory : AsmClassVisitorFactory<SurgeryInstrumentationParameters> {

//    gradle会自动生成实现
//    override val parameters: Property<SurgeryInstrumentationParameters>
//        get() =

    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor {
        println("xxxxxxxxxxxxxxxxxxxx ${classContext.toString()}")
        return ClassMethodVisitor(instrumentationContext.apiVersion.get(), nextClassVisitor)
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        println("xxxxxxxxxxxxxxxxxxxx ${classData.className}")
        return parameters.get().instrumentChecker.get()(classData)
    }
}

//https://www.kingkk.com/2020/08/ASM%E5%8E%86%E9%99%A9%E8%AE%B0/
class ClassMethodVisitor(
    val apiVersion: Int,
    cv: ClassVisitor,
) : ClassVisitor(apiVersion, cv) {


    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        println("xxxxxxxxxxxxxxxxxxxx visitMethod> ${name}")
        if (name != "someMethod") {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
        val returnType = Type.getType(descriptor).returnType
        if (returnType.sort >= Type.BOOLEAN && returnType.sort <= Type.DOUBLE) {
            val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
            mv.visitCode()
            when (returnType.sort) {
                Type.BOOLEAN -> {
                    mv.visitInsn(Opcodes.ICONST_0)
                    mv.visitInsn(Opcodes.IRETURN)
                }
                Type.CHAR -> {
                    mv.visitInsn(Opcodes.ICONST_0)
                    mv.visitInsn(Opcodes.IRETURN)
                }
                Type.BYTE -> {
                    mv.visitInsn(Opcodes.ICONST_0)
                    mv.visitInsn(Opcodes.IRETURN)
                }
                Type.SHORT -> {
                    mv.visitInsn(Opcodes.ICONST_0)
                    mv.visitInsn(Opcodes.IRETURN)
                }
                Type.INT -> {
                    mv.visitInsn(Opcodes.ICONST_0)
                    mv.visitInsn(Opcodes.IRETURN)
                }
                Type.LONG -> {
                    mv.visitInsn(Opcodes.LCONST_0)
                    mv.visitInsn(Opcodes.LRETURN)
                }
                Type.FLOAT -> {
                    mv.visitInsn(Opcodes.FCONST_0)
                    mv.visitInsn(Opcodes.FRETURN)
                }
                Type.DOUBLE -> {
                    mv.visitInsn(Opcodes.DCONST_0)
                    mv.visitInsn(Opcodes.DRETURN)
                }
            }
            mv.visitMaxs(1, 1)
            mv.visitEnd()
            return mv
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }
}

class MetohdCodeVisitor(
    apiVersion: Int, nextVisitor: MethodVisitor
) : MethodVisitor(apiVersion, nextVisitor) {
    //    https://cloud.tencent.com/developer/article/1633443
//    内部方法必须按照以下顺序调用（和MethodVisitor接口在Javadoc中指定的一些额外约束）
//    visitAnnotationDefault?
//    ( visitAnnotation | visitParameterAnnotation | visitAttribute )\*
//    ( visitCode
//    ( visitTryCatchBlock | visitLabel | visitFrame | visitXxx Insn | visitLocalVariable | visitLineNumber ) \*
//    visitMaxs )?
//    visitEnd
    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
        //方法内不调opcode用其他类owner的其他方法name
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

//    https://jack-zheng.github.io/hexo/2020/09/07/ASM-quick-guide/
    override fun visitInvokeDynamicInsn(name: String?, descriptor: String?, bootstrapMethodHandle: Handle?, vararg bootstrapMethodArguments: Any?) {
        //检测 lambda 表达式
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }
}