import com.orz.reark.core.frontend.AccumulatorLowering
import com.orz.reark.core.frontend.BinaryOp
import com.orz.reark.core.frontend.IRBuilder
import com.orz.reark.core.ir.Module
import com.orz.reark.core.ir.Type
import com.orz.reark.core.ir.anyType
import com.orz.reark.core.ir.i32Type
import com.orz.reark.core.ir.stringType
import com.orz.reark.core.ir.voidType
import com.orz.reark.core.pass.PassManager
import com.orz.reark.core.pass.transform.AlgebraicSimplification
import com.orz.reark.core.pass.transform.ConstantFolding
import com.orz.reark.core.pass.transform.DeadCodeElimination
import com.orz.reark.core.pass.transform.SimplifyCFG

/**
 * SSA IR 使用示例和测试
 */
object Test {

    /**
     * 示例1: 创建一个简单的加法函数
     */
    fun createAddFunction(): Module {
        val module = Module("example")

        // 创建函数: int add(int a, int b)
        val func = module.createFunction("add", i32Type)
        val a = func.addArgument(i32Type, "a")
        val b = func.addArgument(i32Type, "b")

        // 创建入口块
        val entry = func.createBlock("entry")

        // 使用IRBuilder构建IR
        val builder = IRBuilder(entry)
        val sum = builder.createAdd(a, b, "sum")
        builder.createRet(sum)

        return module
    }

    /**
     * 示例2: 创建条件分支
     */
    fun createIfElseFunction(): Module {
        val module = Module("example")

        // 函数: int max(int a, int b)
        val func = module.createFunction("max", i32Type)
        val a = func.addArgument(i32Type, "a")
        val b = func.addArgument(i32Type, "b")

        val builder = IRBuilder()

        // 基本块
        val entry = func.createBlock("entry")
        val thenBlock = func.createBlock("then")
        val elseBlock = func.createBlock("else")
        val mergeBlock = func.createBlock("merge")

        // Entry
        builder.setInsertPoint(entry)
        val cond = builder.createICmpSGT(a, b, "cond")
        builder.createCondBr(cond, thenBlock, elseBlock)

        // Then
        builder.setInsertPoint(thenBlock)
        builder.createBr(mergeBlock)

        // Else
        builder.setInsertPoint(elseBlock)
        builder.createBr(mergeBlock)

        // Merge (PHI节点)
        builder.setInsertPoint(mergeBlock)
        val result = builder.createPhi(i32Type, listOf(a to thenBlock, b to elseBlock), "result")
        builder.createRet(result)

        return module
    }

    /**
     * 示例3: 使用累加器模型转换
     */
    fun createWithAccumulator(): Module {
        val module = Module("example")

        // 函数: int calculate(int x)
        val func = module.createFunction("calculate", i32Type)
        val x = func.addArgument(i32Type, "x")

        val builder = IRBuilder()
        val accLowering = AccumulatorLowering(builder)

        val entry = func.createBlock("entry")
        builder.setInsertPoint(entry)

        // 模拟: acc = x * 2 + 1
        // 对应 pandaASM: lda x; mul2 2; add2 1; return
        accLowering.createLda(x)
        val two = builder.getConstantI32(2)
        accLowering.createBinaryOpWithAcc(BinaryOp.MUL, two)
        val one = builder.getConstantI32(1)
        accLowering.createBinaryOpWithAcc(BinaryOp.ADD, one)
        accLowering.createReturnAcc()

        return module
    }

    /**
     * 示例4: 对象操作
     */
    fun createObjectOperations(): Module {
        val module = Module("example")

        // 函数: any createPerson(string name, int age)
        val func = module.createFunction("createPerson", anyType)
        val name = func.addArgument(stringType, "name")
        val age = func.addArgument(i32Type, "age")

        val builder = IRBuilder()
        val entry = func.createBlock("entry")
        builder.setInsertPoint(entry)

        // 创建空对象
        val obj = builder.createEmptyObject("person")

        // 设置属性
        val nameKey = builder.getConstantString("name")
        builder.createSetProperty(obj, nameKey, name)

        val ageKey = builder.getConstantString("age")
        builder.createSetProperty(obj, ageKey, age)

        builder.createRet(obj)

        return module
    }

    /**
     * 示例5: 函数调用
     */
    fun createFunctionCall(): Module {
        val module = Module("example")

        // 声明外部函数: console.log
        val consoleLog = module.getOrDeclareFunction(
            "console.log",
            voidType,
            listOf(anyType)
        )

        // 创建主函数
        val func = module.createFunction("main", voidType)

        val builder = IRBuilder()
        val entry = func.createBlock("entry")
        builder.setInsertPoint(entry)

        // 调用 console.log("Hello, World!")
        val message = builder.getConstantString("Hello, World!")
        builder.createCall(consoleLog, listOf(message))

        builder.createRetVoid()

        return module
    }

    /**
     * 示例6: 运行优化
     */
    fun runOptimizations(module: Module) {
        println("Before optimization:")
        println(module.printDetailed())

        // 创建PassManager
        val pm = PassManager()

        // 添加优化Pass
        pm.addPass(DeadCodeElimination())
        pm.addPass(ConstantFolding())
        pm.addPass(AlgebraicSimplification())
        pm.addPass(SimplifyCFG())

        pm.debug = true
        pm.collectStatistics = true

        // 运行
        val success = pm.run(module)

        if (success) {
            println("\nAfter optimization:")
            println(module.printDetailed())
            pm.printStatistics()
        } else {
            println("Optimization failed!")
        }
    }

    /**
     * 示例7: 数组操作
     */
    fun createArrayOperations(): Module {
        val module = Module("example")

        // 函数: array createNumbers()
        val func = module.createFunction("createNumbers", Type.ArrayType(i32Type))

        val builder = IRBuilder()
        val entry = func.createBlock("entry")
        builder.setInsertPoint(entry)

        // 创建空数组
        val arr = builder.createEmptyArray(3, "numbers")

        // 设置元素
        val zero = builder.getConstantI32(0)
        val one = builder.getConstantI32(1)
        val two = builder.getConstantI32(2)

        builder.createSetElement(arr, builder.getConstantI32(0), zero)
        builder.createSetElement(arr, builder.getConstantI32(1), one)
        builder.createSetElement(arr, builder.getConstantI32(2), two)

        builder.createRet(arr)

        return module
    }

    /**
     * 示例8: 常量折叠演示
     */
    fun createConstantFoldingExample(): Module {
        val module = Module("example")

        val func = module.createFunction("constantFolding", i32Type)

        val builder = IRBuilder()
        val entry = func.createBlock("entry")
        builder.setInsertPoint(entry)

        // 这些常量运算会被优化
        val a = builder.getConstantI32(10)
        val b = builder.getConstantI32(20)
        val c = builder.createAdd(a, b, "c")        // 30
        val d = builder.createMul(c, builder.getConstantI32(2), "d")  // 60
        val e = builder.createSub(d, builder.getConstantI32(10), "e") // 50

        builder.createRet(e)

        return module
    }

    /**
     * 示例9: 死代码消除演示
     */
    fun createDeadCodeExample(): Module {
        val module = Module("example")

        val func = module.createFunction("deadCode", i32Type)
        val x = func.addArgument(i32Type, "x")

        val builder = IRBuilder()
        val entry = func.createBlock("entry")
        builder.setInsertPoint(entry)

        // 这些计算的结果未被使用，会被消除
        val unused1 = builder.createMul(x, x, "unused1")
        val unused2 = builder.createAdd(unused1, x, "unused2")

        // 只有这个会被保留
        val result = builder.createAdd(x, builder.getConstantI32(1), "result")
        builder.createRet(result)

        return module
    }

    /**
     * 主函数 - 演示所有示例
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println("=".repeat(60))
        println("SSA IR Examples")
        println("=".repeat(60))

        println("\n1. Simple Add Function:")
        println(createAddFunction().printDetailed())

        println("\n2. If-Else Function:")
        println(createIfElseFunction().printDetailed())

        println("\n3. With Accumulator Model:")
        println(createWithAccumulator().printDetailed())

        println("\n4. Object Operations:")
        println(createObjectOperations().printDetailed())

        println("\n5. Function Call:")
        println(createFunctionCall().printDetailed())

        println("\n6. Array Operations:")
        println(createArrayOperations().printDetailed())

        println("\n7. Constant Folding Example:")
        runOptimizations(createConstantFoldingExample())

        println("\n8. Dead Code Elimination Example:")
        runOptimizations(createDeadCodeExample())

        println("\n" + "=".repeat(60))
        println("Examples completed!")
        println("=".repeat(60))
    }
}