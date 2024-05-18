package knife.asm

import com.android.build.api.instrumentation.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import wing.green
import wing.purple
import wing.red
import wing.toStr

abstract class SurgeryInstrumentationParameters : InstrumentationParameters {

    @get:Input
    abstract val buildType: Property<String>

    @get:Input
    abstract val flavorName: Property<String>

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    @get:Optional
    abstract val methodConfigs: MapProperty<String, List<ModifyConfig>>//一个方法名->对应多个操作

    @get:Input
    @get:Optional
    abstract val targetClasses: ListProperty<String>
}

//必须是抽象类，会自动实现部分方法
abstract class SurgeryAsmClassVisitorFactory :
    AsmClassVisitorFactory<SurgeryInstrumentationParameters> {

//    gradle会自动生成实现
//    override val parameters: Property<SurgeryInstrumentationParameters>
//        get() =

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        println("xxxxxx ${classContext.currentClassData.className}")
        return KnifeClassMethodVisitor(
            parameters.get().methodConfigs.get(),
            instrumentationContext.apiVersion.get(),
            nextClassVisitor
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return parameters.get().targetClasses.get().contains(classData.className)
    }
}

//https://www.kingkk.com/2020/08/ASM%E5%8E%86%E9%99%A9%E8%AE%B0/
class KnifeClassMethodVisitor(
    private val methodConfigs: Map<String, List<ModifyConfig>>,
    private val apiVersion: Int,
    cv: ClassVisitor,
) : ClassVisitor(apiVersion, cv) {

    private var internalClass = ""

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        internalClass = name
        super.visit(version, access, name, signature, superName, interfaces)
    }


    /**
     * 访问类的方法。如果方法匹配目标方法，则返回一个自定义的 MethodVisitor 来修改它。
     *
     * #### 参数descriptor详解
     * 举例 **```(I)Ljava/lang/String;```**
     * - 参数列表：由圆括号 () 包围。
     *      - (I) 表示该方法有一个参数，其类型为 int。在描述符中，I 表示 int 类型。
     * - 返回类型：在圆括号之后描述。
     *      - Ljava/lang/String; 表示返回类型为 java.lang.String。在描述符中，对象类型以 L 开头，后跟类的完全限定名，最后以分号 ; 结尾。
     *
     *  - **对象类型：** L<classname>; 表示对象类型，其中 classname 是类的完全限定名
     *  - **数组类型：** [<type> 表示数组类型，其中 <type> 可以是基本类型、对象类型或另一种数组类型。例如，[I 表示 int 数组，[[Ljava/lang/String; 表示 String 的二维数组。
     *
     *
     * @param access 方法的访问标志（见 Opcodes）
     *
     * @param name 方法的名称
     *
     * @param descriptor 方法的描述符（见 Type）
     *
     * @param signature 方法的签名（可选，用于泛型）
     *
     * @param exceptions 方法的异常类的内部名称
     *
     * @return 用于访问方法代码的 MethodVisitor
     */
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        val visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)

        val modifyConfigs = methodConfigs[name] ?: return visitMethod
        val matchedModifyConfigs = modifyConfigs.filter { modifyConfig ->
            if (modifyConfig.targetMethod.descriptor == "*" || modifyConfig.targetMethod.descriptor == "?") {
                modifyConfig.targetMethod.internalClass == internalClass
            } else {
                modifyConfig.targetMethod.descriptor == descriptor && modifyConfig.targetMethod.internalClass == internalClass
            }
        }

        if (matchedModifyConfigs.isEmpty()) {
            return visitMethod
        }

        val fullMethodName = "$internalClass#$name"
        println("KnifeClassMethodVisitor >> [$fullMethodName], name = [${name}], descriptor = [${descriptor}], signature = [${signature}], exceptions = [${exceptions}]".purple)
        //方法置空处理
        val emptyMethodConfig = matchedModifyConfigs.find {
            it.methodAction == null
        }
        if (emptyMethodConfig != null) {
            //方法置空的话 后面就不需要处理了，后面都是方法内部的处理
            println("need empty $emptyMethodConfig".green)
            return EmptyMethodVisitor(apiVersion, name, descriptor, visitMethod)
        }

        val changeInvokeMethodActions = modifyConfigs.filter { modifyConfig ->
            modifyConfig.methodAction!!.toNewClass != null
        }.map { it.methodAction!! }

        if (changeInvokeMethodActions.isEmpty()) {
            val removeInvokeMethodActions = modifyConfigs.filter { modifyConfig ->
                modifyConfig.methodAction!!.toNewClass == null
            }.map { it.methodAction!! }

            if (removeInvokeMethodActions.isEmpty()) {
                return visitMethod
            }

            println("need remove $changeInvokeMethodActions".green)
            return RemoveInvokeMethodVisitor(
                fullMethodName,
                removeInvokeMethodActions,
                apiVersion,
                visitMethod
            )
        }

        println("need change $changeInvokeMethodActions".green)

        val changeInvokeOwnerMethodVisitor = ChangeInvokeOwnerMethodVisitor(
            fullMethodName,
            changeInvokeMethodActions,
            apiVersion,
            visitMethod
        )

        val removeInvokeMethodActions = modifyConfigs.filter { modifyConfig ->
            modifyConfig.methodAction!!.toNewClass == null
        }.map { it.methodAction!! }

        if (removeInvokeMethodActions.isEmpty()) {
            return changeInvokeOwnerMethodVisitor
        }


        println("need remove $removeInvokeMethodActions".red)
        return RemoveInvokeMethodVisitor(
            fullMethodName,
            removeInvokeMethodActions,
            apiVersion,
            changeInvokeOwnerMethodVisitor
        )
    }
}
