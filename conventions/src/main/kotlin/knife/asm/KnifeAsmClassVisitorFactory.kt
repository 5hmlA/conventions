package knife.asm

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import knife.asm.visitors.ChangeInvokeOwnerMethodVisitor
import knife.asm.visitors.EmptyInitFunctionVisitor
import knife.asm.visitors.EmptyMethodVisitor
import knife.asm.visitors.RemoveInvokeMethodVisitor
import knife.asm.visitors.TraceMethodVisitor
import knife.asm.visitors.TryCatchMethodVisitor
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import wing.green
import wing.lookDown
import wing.lookup

abstract class KnifeInstrumentationParameters : InstrumentationParameters {

    @get:Input
    abstract val buildType: Property<String>

    @get:Input
    abstract val flavorName: Property<String>

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    @get:Optional
    //Map<类,Map<方法，修改> //这些类的哪些方法要做什么修改
    abstract val classConfigs: MapProperty<String, Map<String, List<ModifyConfig>>>

    @get:Input
    @get:Optional
    abstract val targetClasses: SetProperty<String>
}

//必须是抽象类，会自动实现部分方法
abstract class KnifeAsmClassVisitorFactory :
    AsmClassVisitorFactory<KnifeInstrumentationParameters> {

//    gradle会自动生成实现
//    override val parameters: Property<SurgeryInstrumentationParameters>
//        get() =

    /**
     * isInstrumentable 返回true之后才执行，且在同线程
     */
    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        //classContext.currentClassData.className 是正常的完整类名 .分割
        //inernalClass 是asm的类名 /分割
        return KnifeClassMethodVisitor(
            parameters.get().classConfigs.get()[classContext.currentClassData.className]!!,
            instrumentationContext.apiVersion.get(),
            nextClassVisitor
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return parameters.get().targetClasses.get().contains(classData.className)
    }
}

//https://www.kingkk.com/2020/08/ASM%E5%8E%86%E9%99%A9%E8%AE%B0/
/**
 * @param methodConfigs 当前类要处理的所有方法 <方法名, 具体方法，处理方式>
 *
 * 对某个类执行ASM处理
 */
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
        asmLog(0, "KnifeClassMethodVisitor >> ${internalClass.lookDown} ")
        super.visit(version, access, name, signature, superName, interfaces)
    }


    private fun doEmptyMethodVisitor(
        isInit: Boolean,
        access: Int,
        apiVersion: Int,
        name: String,
        descriptor: String,
        visitMethod: MethodVisitor,
    ) = if (isInit) {
        EmptyInitFunctionVisitor(apiVersion, name, descriptor, visitMethod)
    } else {
        EmptyMethodVisitor(apiVersion, access, name, descriptor, visitMethod)
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
    ): MethodVisitor {
        val visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (access.isMethodIgnore()) {
            //抽象方法不处理
            return visitMethod
        }
        //methodConfigs<key:方法名，value:多种处理方式>
        //通配符匹配到这个方法要处理，具体的处理动作
        val wildcardActions: List<Action> = methodConfigs["*"]?.map { it.methodAction }.orEmpty()
        //方法名+descriptor完整匹配这个方法要处理，具体的处理动作
        val fullMatchActions: List<Action> = methodConfigs[name]?.filter { modifyConfig ->
            modifyConfig.targetMethod.descriptor == "*" || modifyConfig.targetMethod.descriptor == descriptor
        }?.map { it.methodAction }.orEmpty()

        val actions = wildcardActions.plus(fullMatchActions)

        if (actions.isEmpty()) {
            return visitMethod
        }

        val isInit = name.isInitMethod()
        val fullMethodName = "$internalClass#$name"

        val emptyBodyActions = actions.filterIsInstance<Action.EmptyBody>()
        if (emptyBodyActions.isNotEmpty()) {
            asmLog(msg = "KnifeClassMethodVisitor: need empty -> in [$internalClass]: method=[${name}], descriptor=[${descriptor}] ".green)
            return doEmptyMethodVisitor(isInit, access, apiVersion, fullMethodName, descriptor, visitMethod)
        }

        //trace
        //try catch
        //change invoke
        //remove invoke
        var methodVisitor: MethodVisitor = visitMethod

        val traceActions = actions.filterIsInstance<Action.TraceBody>()
        if (traceActions.isNotEmpty()) {
            asmLog(msg = "KnifeClassMethodVisitor: need trace [$name] body -> in [$internalClass]: method=[${name}], descriptor=[${descriptor}] ".green)
            methodVisitor = TraceMethodVisitor(apiVersion, methodVisitor, fullMethodName)
        }

        val tryCatchActions = actions.filterIsInstance<Action.TryCatchBody>()
        if (tryCatchActions.isNotEmpty()) {
            ////这个方法内部的处理：要修改调用方
            asmLog(msg = "KnifeClassMethodVisitor: need try catch [$name] body -> in [$internalClass]: method=[${name}], descriptor=[${descriptor}] ".green)
            methodVisitor = TryCatchMethodVisitor(apiVersion, methodVisitor, descriptor)
        }

        val changeInvokeActions = actions.filterIsInstance<Action.ChangeInvoke>()
        if (changeInvokeActions.isNotEmpty()) {
            asmLog(msg = "KnifeClassMethodVisitor: need change invoke -> in [$internalClass]: method=[${name}], descriptor=[${descriptor}] $changeInvokeActions".green)
            //这个方法内部的处理：要修改调用方
            methodVisitor = ChangeInvokeOwnerMethodVisitor(
                fullMethodName,
                changeInvokeActions,
                apiVersion,
                visitMethod
            )
        }

        val removeInvokeActions = actions.filterIsInstance<Action.RemoveInvoke>()
//        println("removeInvokeActions >>  $removeInvokeActions".purple)
        if (removeInvokeActions.isNotEmpty()) {
            asmLog(msg = "KnifeClassMethodVisitor: need remove invoke -> in [$internalClass]: method=[${name}], descriptor=[${descriptor}] $removeInvokeActions".green)
            //这个方法内部的处理：要移除某行调用
            methodVisitor = RemoveInvokeMethodVisitor(
                fullMethodName,
                removeInvokeActions,
                apiVersion,
                methodVisitor
            )
        }
        return methodVisitor
    }

    override fun visitEnd() {
        super.visitEnd()
        asmLog(0, "KnifeClassMethodVisitor >> ${internalClass.lookup} ")
    }
}
