import com.orz.reark.core.backend.BytecodeToIRConverter
import com.orz.reark.core.ir.Module
import com.orz.reark.core.pass.PassManager
import com.orz.reark.core.pass.transform.AggressiveDeadCodeElimination
import com.orz.reark.core.pass.transform.AlgebraicSimplification
import com.orz.reark.core.pass.transform.ConstantFolding
import com.orz.reark.core.pass.transform.ConstantPropagation
import com.orz.reark.core.pass.transform.SimplifyCFG
import me.yricky.oh.abcd.AbcBuf
import me.yricky.oh.abcd.cfm.AbcClass
import me.yricky.oh.abcd.decompiler.ToJs
import me.yricky.oh.common.wrapAsLEByteBuf
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object TestAbc {

    class ByteArrayBuilder(initialCapacity: Int = 1024) {
        private val outputStream = ByteArrayOutputStream(initialCapacity)

        fun append(data: ByteArray) {
            outputStream.write(data)
        }

        fun append(data: Byte) {
            outputStream.write(data.toInt())
        }

        fun appendAll(arrays: List<ByteArray>) {
            arrays.forEach { outputStream.write(it) }
        }

        fun toByteArray(): ByteArray = outputStream.toByteArray()

        fun size(): Int = outputStream.size()

        fun reset() {
            outputStream.reset()
        }
    }

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

    @JvmStatic
    fun main(args: Array<String>){
            val file = File("/home/orz/data/unitTest/test.abc")
        val mmap = FileChannel.open(file.toPath()).map(FileChannel.MapMode.READ_ONLY,0,file.length())
        val abc = AbcBuf("", wrapAsLEByteBuf(mmap.order(ByteOrder.LITTLE_ENDIAN)))

        abc.classes.forEach { off, item ->
            if(item is AbcClass){
                println("[C]" + item.name)

                item.methods.forEach {
                    val builder = ByteArrayBuilder()
                    println("\t [M] " + it.name)
                    var off = 0;
                    it.codeItem?.asm?.list?.forEach { asmItem ->
                        val opbs = asmItem.opUnits.map { it.toByte() }.toByteArray()
                        print("0x" + off.toHexString() + "  ")
                        off += asmItem.opUnits.size
                        print(asmItem.opUnits)
                        println("  " + asmItem)
                        builder.append(opbs)
                    }
                    val result = builder.toByteArray()
                    println(result.contentToString())
                    fullOptimizationPipeline(result)
                }

            }
        }
    }

    fun fullOptimizationPipeline(bytecode: ByteArray) {
        println("\n" + "=".repeat(70))
        println("Full Optimization Pipeline")
        println("=".repeat(70))

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
}