import com.orz.reark.core.backend.BytecodeToIRConverter
import com.orz.reark.core.ir.Module
import com.orz.reark.core.pass.PassManager
import com.orz.reark.core.pass.transform.AggressiveDeadCodeElimination
import com.orz.reark.core.pass.transform.AlgebraicSimplification
import com.orz.reark.core.pass.transform.ConstantFolding
import com.orz.reark.core.pass.transform.ConstantPropagation
import com.orz.reark.core.pass.transform.RedundantReturnElimination
import com.orz.reark.core.pass.transform.SimplifyCFG
import me.yricky.oh.abcd.AbcBuf
import me.yricky.oh.abcd.cfm.AbcClass
import me.yricky.oh.common.toByteArray
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

    @JvmStatic
    fun main(args: Array<String>){
        val file = File("/home/orz/data/unitTest/test.abc")
        val mmap = FileChannel.open(file.toPath()).map(FileChannel.MapMode.READ_ONLY,0,file.length())
        val abc = AbcBuf("", wrapAsLEByteBuf(mmap.order(ByteOrder.LITTLE_ENDIAN)))
        abc.classes.forEach { off, item ->
            if(item is AbcClass){
                item.methods.forEach { method ->
                    println("[M] ${method.name}")
                    val codeItem = method.codeItem
                    val result = codeItem?.instructions?.toByteArray()

                    if (result != null) {
                        val numArgs = codeItem.numArgs
                        val numVRegs = codeItem.numVRegs
                        val actualParamCount = (numArgs - 3).coerceAtLeast(0)
                        fullOptimizationPipeline(result, actualParamCount, numVRegs, codeItem.numArgs)
                    }
                }
            }
        }
    }

    fun fullOptimizationPipeline(bytecode: ByteArray, paramCount: Int = 0, numVRegs: Int = 0, numArgs: Int = 0) {
        val module = Module("pipeline_test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("optimizedFunction", bytecode, paramCount = paramCount, numVRegs = numVRegs, numArgs = numArgs)

        if (!result.isSuccess) {
            throw AssertionError("Bytecode conversion failed: ${result.errors}")
        }

        val beforeInstCount = module.allInstructions().count()
        printFunctionIR("Original Function:", result.function)
        println("Original instruction count: $beforeInstCount")

        val passes = listOf(
            AggressiveDeadCodeElimination(),
            RedundantReturnElimination(),
            ConstantFolding(),
            ConstantPropagation(),
            AlgebraicSimplification(),
            SimplifyCFG()
        )

        var currentInstCount = beforeInstCount
        for ((index, pass) in passes.withIndex()) {
            val pm = PassManager()
            pm.addPass(pass)
            pm.run(module)

            val newCount = module.allInstructions().count()
            println("Pass ${index + 1} (${pass.name}): $currentInstCount -> $newCount instructions")
            currentInstCount = newCount
        }

        printFunctionIR("After All Optimizations:", result.function)
    }
}
