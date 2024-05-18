package knife.asm

import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import wing.purple
import wing.red
import java.io.Serializable

data class MethodData(
    val fullClass: String,
    val internalClass: String,
    val methodName: String,
    val descriptor: String,
) : Serializable

data class MethodAction(
    val methodData: MethodData,
    val toNewClass: String? = null
) : Serializable

private fun String.isIgnore(): Boolean = this == "*" || this == "?"

private fun String.toMethodData(): MethodData {
    val (clz, method, desc) = this.split("#")
    return MethodData(clz, clz.replace(".", "/"), method, desc)
}

data class ModifyConfig(
    val targetMethod: MethodData,
    val methodAction: MethodAction? = null,
) : Serializable

internal fun String.toModifyConfig(): ModifyConfig {
    // "target.class#method#(I)V=>PrintStream#println#(I)V->dest/clazz"
    if (!contains("=>")) {
        return ModifyConfig(toMethodData())
    }
    val (targetMethodStr, innerMethodStr) = split("=>")
    val targetMethod = targetMethodStr.toMethodData()

    if (innerMethodStr.isEmpty()) {
        return ModifyConfig(targetMethod)
    }
    // PrintStream#println#(I)V->dest/clazz
    if (!innerMethodStr.contains("->")) {
        // PrintStream#println#(I)V
        return ModifyConfig(targetMethod, MethodAction(innerMethodStr.toMethodData()))
    }
    // PrintStream#println#(I)V->dest/clazz
    val (oldInnerMethodStr, toClz) = innerMethodStr.split("->")
    return ModifyConfig(targetMethod, MethodAction(oldInnerMethodStr.toMethodData(), toClz.replace(".","/")))
}


/**
 * 一个 MethodVisitor，用于将目标方法修改为空方法，并返回适当的默认值。
 *
 * @param methodVisitor 父 MethodVisitor 用于委托
 *
 * @param methodDesc 方法的描述符，用于确定返回类型
 */
internal class EmptyMethodVisitor(
    apiVersion: Int,
    private val classMethod: String,
    private val methodDesc: String,
    methodVisitor: MethodVisitor,
) : MethodVisitor(apiVersion, methodVisitor) {

    /**
     * 访问方法的代码开始处。插入指令使方法为空方法。
     */
    override fun visitCode() {
        super.visitCode()
        // 根据方法的返回类型插入相应的返回指令
        println("EmptyMethodVisitor >> [$classMethod] > $methodDesc".purple)
        when (Type.getReturnType(methodDesc).sort) {
            Type.VOID -> {
                // 如果返回类型是 void，插入 RETURN 指令
                mv.visitInsn(Opcodes.RETURN)
            }

            Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> {
                // 对于 boolean、char、byte、short 和 int 类型，插入 ICONST_0 和 IRETURN 指令
                mv.visitInsn(Opcodes.ICONST_0)
                mv.visitInsn(Opcodes.IRETURN)
            }

            Type.FLOAT -> {
                // 对于 float 类型，插入 FCONST_0 和 FRETURN 指令
                mv.visitInsn(Opcodes.FCONST_0)
                mv.visitInsn(Opcodes.FRETURN)
            }

            Type.LONG -> {
                // 对于 long 类型，插入 LCONST_0 和 LRETURN 指令
                mv.visitInsn(Opcodes.LCONST_0)
                mv.visitInsn(Opcodes.LRETURN)
            }

            Type.DOUBLE -> {
                // 对于 double 类型，插入 DCONST_0 和 DRETURN 指令
                mv.visitInsn(Opcodes.DCONST_0)
                mv.visitInsn(Opcodes.DRETURN)
            }

            Type.ARRAY, Type.OBJECT -> {
                //Ljava/lang/String;返回值为String
                if (methodDesc.endsWith("lang/String;")) {
                    // 加载空字符串常量到操作数栈
                    mv.visitLdcInsn("")
                } else {
                    // 对于数组和对象类型，插入 ACONST_NULL 和 ARETURN 指令
                    mv.visitInsn(Opcodes.ACONST_NULL)
                }
                mv.visitInsn(Opcodes.ARETURN)
            }

            else -> throw IllegalArgumentException("不支持的返回类型:$methodDesc")
        }

        // 计算并设置最大堆栈大小和局部变量表的大小
        // 因为方法中可能有返回值的指令，所以需要合理设置堆栈和局部变量的大小
        mv.visitMaxs(1, 1)

        // 标识方法访问的结束
        // 标识方法访问的结束。这个调用是必要的，以完成对方法的访问。如果没有这个调用，方法的定义将不完整，从而导致生成的字节码不正确。
        mv.visitEnd()
    }

}


internal class RemoveInvokeMethodVisitor(
    private val classMethod: String,
    private val methodActions: List<MethodAction>,
    apiVersion: Int,
    nextVisitor: MethodVisitor
) : MethodVisitor(apiVersion, nextVisitor) {

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        val methodAction = methodActions.find {
            val ignoreDescriptor = it.methodData.descriptor.isIgnore()
            val ignoreInternalClass = it.methodData.internalClass.isIgnore()
            if (ignoreDescriptor && ignoreInternalClass) {
                it.methodData.methodName == name
            } else if (ignoreDescriptor) {
                it.methodData.methodName == name && it.methodData.internalClass == owner
            } else if (ignoreInternalClass) {
                it.methodData.methodName == name && it.methodData.descriptor == descriptor
            } else {
                it.methodData.methodName == name && it.methodData.descriptor == descriptor && it.methodData.internalClass == owner
            }
        }
        if (methodAction == null) {
            //没匹配到就不需要移除
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        } else {
            //移除的是方法里面调用的其他方法
            //不执行【super.visitMethodInsn()】那么就是移除方法的调用
            println("RemoveInvokeMethodVisitor >> owner = [${owner}], name = [${name}], descriptor = [${descriptor}], in [$classMethod]".purple)
        }
    }
}


/**
 * 替换方法内部中调用的指定方法的对象
 * fun toBeChange(){
 *      a.method() => b.method()
 * }
 */
internal class ChangeInvokeOwnerMethodVisitor(
    private val classMethod: String,
    private val methodActions: List<MethodAction>,
    apiVersion: Int,
    nextVisitor: MethodVisitor
) : MethodVisitor(apiVersion, nextVisitor) {

    /**
     * 访问方法中的方法调用指令。替换旧的调用对象为新的调用对象。
     *
     * @param opcode 方法调用的操作码（如 INVOKEVIRTUAL, INVOKESTATIC 等）
     *
     * @param owner 方法调用的对象的内部名称
     *
     * @param name 调用的方法的名称
     *
     * @param descriptor 调用的方法的描述符
     *
     * @param isInterface 方法调用的对象是否是接口
     */
    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        val methodAction = methodActions.find {
            val ignoreDescriptor = it.methodData.descriptor.isIgnore()
            val ignoreInternalClass = it.methodData.internalClass.isIgnore()
            if (ignoreDescriptor && ignoreInternalClass) {
                it.methodData.methodName == name
            } else if (ignoreDescriptor) {
                it.methodData.methodName == name && owner.contains(it.methodData.internalClass)
            } else if (ignoreInternalClass) {
                it.methodData.methodName == name && it.methodData.descriptor == descriptor
            } else {
                it.methodData.methodName == name && it.methodData.descriptor == descriptor && owner.contains(it.methodData.internalClass)
            }
        }
        if (methodAction == null) {
            //没匹配到就不需要移除
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        } else {
            println("ChangeInvokeOwnerMethodVisitor >> owner = [${owner}], name = [${name}], descriptor = [${descriptor}], to [${methodAction.toNewClass}], in [$classMethod]".purple)
            // 替换为新的类的静态方法调用
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                methodAction.toNewClass,
                name,
                descriptor,
                isInterface
            )
        }
    }

    //    https://jack-zheng.github.io/hexo/2020/09/07/ASM-quick-guide/
    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        //检测 lambda 表达式
        super.visitInvokeDynamicInsn(
            name,
            descriptor,
            bootstrapMethodHandle,
            *bootstrapMethodArguments
        )
    }
}