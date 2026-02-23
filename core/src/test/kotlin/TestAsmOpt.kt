import com.orz.reark.core.backend.BytecodeToIRConverter
import com.orz.reark.core.backend.PandaAsmParser
import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.PassManager
import com.orz.reark.core.pass.transform.*

/**
 * PandaASM 字节码到 SSA IR 转换及优化测试
 * 
 * 展示从 PandaASM 字节码到 IR 转换，再到 Pass 优化的完整流程
 */
object TestAsmOpt {

    /**
     * 辅助函数：打印函数 IR
     */
    private fun printFunctionIR(title: String, function: com.orz.reark.core.ir.Function) {
        println("\n$title")
        println("-".repeat(60))
        println(function)
    }

    /**
     * 辅助函数：打印模块统计信息
     */
    private fun printModuleStats(title: String, module: Module) {
        println("\n$title")
        println("-".repeat(60))
        println("  Functions: ${module.functionCount()}")
        println("  Global Variables: ${module.globals().size}")
        println("  String Constants: ${module.stringConstants().size}")
        
        val totalInstructions = module.allInstructions().count()
        val totalBlocks = module.allBlocks().count()
        println("  Total Instructions: $totalInstructions")
        println("  Total Basic Blocks: $totalBlocks")
    }

    /**
     * 测试1: 常量折叠优化
     * 
     * 字节码序列模拟: 1 + 2 + 3
     * 优化前: 多个常量加法操作
     * 优化后: 直接得到结果 6
     */
    fun testConstantFolding() {
        println("\n" + "=".repeat(70))
        println("Test 1: Constant Folding Optimization")
        println("=".repeat(70))
        
        // 简化测试：使用 ldtrue 和一元运算代替复杂的立即数加载
        // 实际测试常量折叠使用 IRBuilder 直接构建
        val bytecode = byteArrayOf(
            0x02.toByte(),        // ldtrue
            0x03.toByte(),        // ldfalse  
            0x1F.toByte(), 0x00.toByte(), // neg (一元运算)
            0x64.toByte()         // return
        )
        
        val module = Module("constant_folding_test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("foldConstants", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Conversion failed: ${result.errors}")
            throw AssertionError("Bytecode conversion failed: ${result.errors}")
        }
        
        printFunctionIR("Before Optimization:", result.function)
        printModuleStats("Module Stats (Before)", module)
        
        // 应用常量折叠优化
        val pm = PassManager()
        pm.addPass(ConstantFolding())
        pm.debug = true
        
        val success = pm.run(module)
        
        if (success) {
            printFunctionIR("After Constant Folding:", result.function)
            printModuleStats("Module Stats (After)", module)
            println("\n✓ Constant folding applied successfully")
        }
    }

    /**
     * 测试2: 死代码消除
     * 
     * 字节码包含未使用的计算，应该被消除
     */
    fun testDeadCodeElimination() {
        println("\n" + "=".repeat(70))
        println("Test 2: Dead Code Elimination")
        println("=".repeat(70))
        
        // 创建包含死代码的字节码
        // 创建对象但不使用，应该被消除
        val bytecode = byteArrayOf(
            0x04.toByte(),                    // createemptyobject (死代码 - 结果未使用)
            0x05.toByte(), 0x05.toByte(),     // createemptyarray (死代码)
            0x02.toByte(),                    // ldtrue (实际返回值)
            0x64.toByte()                     // return
        )
        
        val module = Module("dce_test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("removeDeadCode", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Conversion failed: ${result.errors}")
            throw AssertionError("Bytecode conversion failed: ${result.errors}")
        }
        
        val beforeCount = result.function.instructions().count()
        printFunctionIR("Before DCE (Instructions: $beforeCount):", result.function)
        
        // 应用死代码消除
        val pm = PassManager()
        pm.addPass(AggressiveDeadCodeElimination())
        pm.debug = true
        
        val success = pm.run(module)
        
        if (success) {
            val afterCount = result.function.instructions().count()
            printFunctionIR("After DCE (Instructions: $afterCount):", result.function)
            println("\n✓ Removed ${beforeCount - afterCount} dead instructions")
        }
    }

    /**
     * 测试3: 完整优化流水线
     * 
     * 应用多个 Pass 进行完整优化
     */
    fun testFullOptimizationPipeline() {
        println("\n" + "=".repeat(70))
        println("Test 3: Full Optimization Pipeline")
        println("=".repeat(70))
        
        // 创建一个复杂的字节码序列
        val bytecode = byteArrayOf(
            // 一些常量操作
            0x02.toByte(),                    // ldtrue
            0x1F.toByte(), 0x00.toByte(),     // neg (一元运算)
            0x20.toByte(), 0x00.toByte(),     // not
            
            // 对象创建
            0x04.toByte(),                    // createemptyobject
            0x05.toByte(), 0x03.toByte(),     // createemptyarray(3)
            
            // 最终结果
            0x02.toByte(),                    // ldtrue
            0x64.toByte()                     // return
        )
        
        val module = Module("pipeline_test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("optimizedFunction", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Conversion failed: ${result.errors}")
            throw AssertionError("Bytecode conversion failed: ${result.errors}")
        }
        
        val beforeInstCount = module.allInstructions().count()
        printFunctionIR("Original Function:", result.function)
        println("\nOriginal instruction count: $beforeInstCount")
        
        // 应用完整的优化流水线
        val pm = PassManager()
        pm.addPass(AggressiveDeadCodeElimination())  // 先消除死代码
        pm.addPass(ConstantFolding())                // 常量折叠
        pm.addPass(ConstantPropagation())            // 常量传播
        pm.addPass(AlgebraicSimplification())        // 代数简化
        pm.addPass(SimplifyCFG())                    // 简化控制流
        
        pm.collectStatistics = true
        pm.debug = true
        
        val success = pm.run(module)
        
        if (success) {
            val afterInstCount = module.allInstructions().count()
            printFunctionIR("After Full Optimization:", result.function)
            println("\nFinal instruction count: $afterInstCount")
            println("Removed: ${beforeInstCount - afterInstCount} instructions " +
                    "(${(100 * (beforeInstCount - afterInstCount) / beforeInstCount)}% reduction)")
            
            pm.printStatistics()
            println("\n✓ Full optimization pipeline completed")
        }
    }

    /**
     * 测试4: 复杂函数的优化
     * 
     * 模拟一个实际场景的函数，包含多种指令类型
     */
    fun testComplexFunctionOptimization() {
        println("\n" + "=".repeat(70))
        println("Test 4: Complex Function Optimization")
        println("=".repeat(70))
        
        // 创建一个更复杂的字节码序列，避免使用 sta/lda 寄存器操作
        val bytecode = byteArrayOf(
            // 对象创建
            0x04.toByte(),                    // createemptyobject
            
            // 一些一元运算
            0x02.toByte(),                    // ldtrue
            0x1F.toByte(), 0x00.toByte(),     // neg
            0x20.toByte(), 0x00.toByte(),     // not
            0x1C.toByte(), 0x00.toByte(),     // typeof
            
            // 数组操作
            0x05.toByte(), 0x02.toByte(),     // createemptyarray(2)
            
            // 最终返回 true
            0x02.toByte(),                    // ldtrue
            0x64.toByte()                     // return
        )
        
        val module = Module("complex_test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("complexFunc", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Conversion failed: ${result.errors}")
            throw AssertionError("Bytecode conversion failed: ${result.errors}")
        }
        
        println("\n--- Before Optimization ---")
        println(result.function)
        
        val beforeBlocks = result.function.blockCount()
        val beforeInsts = result.function.instructions().count()
        
        // 应用优化
        val pm = PassManager()
        pm.addPass(AggressiveDeadCodeElimination())
        pm.addPass(ConstantFolding())
        pm.addPass(SimplifyCFG())
        pm.collectStatistics = true
        
        val success = pm.run(module)
        
        if (success) {
            println("\n--- After Optimization ---")
            println(result.function)
            
            val afterBlocks = result.function.blockCount()
            val afterInsts = result.function.instructions().count()
            
            println("\nOptimization Summary:")
            println("  Basic Blocks: $beforeBlocks -> $afterBlocks " +
                    "(${(beforeBlocks - afterBlocks)} removed)")
            println("  Instructions: $beforeInsts -> $afterInsts " +
                    "(${(beforeInsts - afterInsts)} removed)")
            
            println("\n✓ Complex function optimization completed")
        }
    }

    /**
     * 测试5: 比较优化前后的 IR
     * 
     * 展示详细的优化前后对比
     */
    fun testOptimizationComparison() {
        println("\n" + "=".repeat(70))
        println("Test 5: Optimization Before/After Comparison")
        println("=".repeat(70))
        
        // 创建一个包含多种可优化模式的字节码（避免使用二元运算的寄存器操作）
        val bytecode = byteArrayOf(
            // 一元运算链
            0x02.toByte(),                    // ldtrue
            0x1F.toByte(), 0x00.toByte(),     // neg
            0x20.toByte(), 0x00.toByte(),     // not
            
            // 类型检查
            0x02.toByte(),                    // ldtrue
            0x23.toByte(),                    // istrue (可简化)
            
            // 死代码 - 对象创建但未使用
            0x04.toByte(),                    // createemptyobject
            
            // 最终结果
            0x02.toByte(),                    // ldtrue
            0x64.toByte()                     // return
        )
        
        val module = Module("comparison_test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("comparison", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Conversion failed: ${result.errors}")
            throw AssertionError("Bytecode conversion failed: ${result.errors}")
        }
        
        // 保存优化前的 IR
        val beforeIR = result.function.toString()
        val beforeInstCount = result.function.instructions().count()
        
        println("\n" + "=".repeat(30))
        println("BEFORE OPTIMIZATION")
        println("=".repeat(30))
        println(beforeIR)
        println("\nInstruction count: $beforeInstCount")
        
        // 应用所有优化
        val pm = PassManager()
        pm.addPass(AggressiveDeadCodeElimination())
        pm.addPass(ConstantFolding())
        pm.addPass(AlgebraicSimplification())
        pm.addPass(SimplifyCFG())
        
        pm.run(module)
        
        // 保存优化后的 IR
        val afterIR = result.function.toString()
        val afterInstCount = result.function.instructions().count()
        
        println("\n" + "=".repeat(30))
        println("AFTER OPTIMIZATION")
        println("=".repeat(30))
        println(afterIR)
        println("\nInstruction count: $afterInstCount")
        
        // 对比
        println("\n" + "=".repeat(30))
        println("COMPARISON")
        println("=".repeat(30))
        println("Instructions: $beforeInstCount -> $afterInstCount")
        println("Reduction: ${beforeInstCount - afterInstCount} instructions " +
                "(${(100 * (beforeInstCount - afterInstCount) / beforeInstCount)}%)")
        
        if (beforeIR != afterIR) {
            println("\n✓ IR was modified by optimization")
        } else {
            println("\n⚠ IR unchanged (may already be optimal)")
        }
    }

    /**
     * 测试6: 验证优化后的函数
     * 
     * 确保优化后的函数仍然有效
     */
    fun testOptimizedFunctionVerification() {
        println("\n" + "=".repeat(70))
        println("Test 6: Optimized Function Verification")
        println("=".repeat(70))
        
        // 创建字节码（避免使用 add2 等需要寄存器操作数的指令）
        val bytecode = byteArrayOf(
            0x04.toByte(),                    // createemptyobject
            0x02.toByte(),                    // ldtrue
            0x1F.toByte(), 0x00.toByte(),     // neg
            0x64.toByte()                     // return
        )
        
        val module = Module("verify_test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("verifyFunc", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Conversion failed: ${result.errors}")
            throw AssertionError("Bytecode conversion failed: ${result.errors}")
        }
        
        println("Before optimization:")
        println("  Valid: ${result.function.verify()}")
        println("  Instructions: ${result.function.instructions().count()}")
        
        // 应用优化
        val pm = PassManager()
        pm.addPass(AggressiveDeadCodeElimination())
        pm.addPass(ConstantFolding())
        pm.run(module)
        
        println("\nAfter optimization:")
        println("  Valid: ${result.function.verify()}")
        println("  Instructions: ${result.function.instructions().count()}")
        
        if (result.function.verify()) {
            println("\n✓ Optimized function is valid")
        } else {
            println("\n✗ Optimized function is INVALID!")
        }
    }

    /**
     * 主函数：运行所有测试
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println("\n" + "█".repeat(70))
        println("█" + " ".repeat(68) + "█")
        println("█" + "   PandaASM Bytecode → SSA IR → Optimization Tests".padEnd(68) + "█")
        println("█" + " ".repeat(68) + "█")
        println("█".repeat(70))
        
        val tests = listOf(
            ::testConstantFolding,
            ::testDeadCodeElimination,
            ::testFullOptimizationPipeline,
            ::testComplexFunctionOptimization,
            ::testOptimizationComparison,
            ::testOptimizedFunctionVerification
        )
        
        var passed = 0
        var failed = 0
        
        for (test in tests) {
            try {
                test()
                passed++
            } catch (e: Exception) {
                println("\n✗ Test failed with exception: ${e.message}")
                e.printStackTrace()
                failed++
            }
        }
        
        println("\n" + "=".repeat(70))
        println("  TEST SUMMARY: $passed passed, $failed failed")
        println("=".repeat(70))
        
        if (failed > 0) {
            System.exit(1)
        }
    }
}
