import com.orz.reark.core.backend.BytecodeToIRConverter
import com.orz.reark.core.backend.PandaAsmParser
import com.orz.reark.core.backend.InstructionMapping
import com.orz.reark.core.backend.PandaAsmOpcodes
import com.orz.reark.core.backend.InstructionCategory
import com.orz.reark.core.ir.*

/**
 * PandaASM 字节码到 SSA IR 转换测试
 * 
 * 测试 BytecodeToIRConverter 和 PandaAsmParser 的功能
 */
object TestIR {

    /**
     * 测试1: 解析简单的 PandaASM 字节码
     * 
     * 字节码: ldtrue, returnundefined
     * 对应: 加载 true，返回 undefined
     */
    fun testParseSimpleBytecode() {
        println("\n" + "=".repeat(60))
        println("Test 1: Parse Simple Bytecode")
        println("=".repeat(60))
        
        // 字节码: 0x02 (ldtrue), 0x65 (returnundefined)
        val bytecode = byteArrayOf(0x02.toByte(), 0x65.toByte())
        
        val parser = PandaAsmParser(bytecode)
        val instructions = parser.parseAll()
        
        println("Parsed ${instructions.size} instructions:")
        instructions.forEach { println("  $it") }
        
        assert(instructions.size == 2) { "Expected 2 instructions" }
        assert(instructions[0].opcode == 0x02) { "First instruction should be ldtrue" }
        assert(instructions[1].opcode == 0x65) { "Second instruction should be returnundefined" }
        
        println("✓ Parse test passed")
    }

    /**
     * 测试2: 转换简单函数 - 返回 true
     */
    fun testConvertReturnTrue() {
        println("\n" + "=".repeat(60))
        println("Test 2: Convert Return True Function")
        println("=".repeat(60))
        
        // 字节码:
        // 0x02 (ldtrue) - 加载 true 到累加器
        // 0x64 (return) - 返回累加器的值
        val bytecode = byteArrayOf(0x02.toByte(), 0x64.toByte())
        
        val module = Module("test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("returnTrue", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Errors: ${result.errors}")
            throw AssertionError("Conversion failed")
        }
        
        if (result.warnings.isNotEmpty()) {
            println("Warnings: ${result.warnings}")
        }
        
        println("Generated IR:")
        println(result.function)
        
        assert(result.function.verify()) { "Function verification failed" }
        
        println("✓ Return true test passed")
    }

    /**
     * 测试3: 转换常量加载和返回
     */
    fun testConvertConstants() {
        println("\n" + "=".repeat(60))
        println("Test 3: Convert Constants")
        println("=".repeat(60))
        
        // 字节码: 加载各种常量然后返回
        // 0x00 (ldundefined)
        // 0x01 (ldnull)
        // 0x02 (ldtrue)
        // 0x64 (return)
        val bytecode = byteArrayOf(
            0x00.toByte(),  // ldundefined
            0x01.toByte(),  // ldnull
            0x02.toByte(),  // ldtrue
            0x64.toByte()   // return
        )
        
        val module = Module("test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("constants", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Errors: ${result.errors}")
            throw AssertionError("Conversion failed")
        }
        
        println("Generated IR:")
        println(result.function)
        
        assert(result.function.verify()) { "Function verification failed" }
        
        println("✓ Constants test passed")
    }

    /**
     * 测试4: 转换对象创建和数组创建
     */
    fun testConvertObjectAndArray() {
        println("\n" + "=".repeat(60))
        println("Test 4: Convert Object and Array Creation")
        println("=".repeat(60))
        
        // 创建空对象和空数组
        val bytecode = byteArrayOf(
            0x04.toByte(),        // createemptyobject
            0x05.toByte(), 0x00.toByte(),  // createemptyarray (8-bit imm)
            0x64.toByte()         // return
        )
        
        val module = Module("test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("createObjAndArr", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Errors: ${result.errors}")
            throw AssertionError("Conversion failed")
        }
        
        println("Generated IR:")
        println(result.function)
        
        assert(result.function.verify()) { "Function verification failed" }
        
        println("✓ Object and array creation test passed")
    }

    /**
     * 测试5: 一元运算
     */
    fun testUnaryOperations() {
        println("\n" + "=".repeat(60))
        println("Test 5: Unary Operations")
        println("=".repeat(60))
        
        // neg (0x1f), not (0x20), typeof (0x1c)
        val bytecode = byteArrayOf(
            0x02.toByte(),        // ldtrue (先加载一个值到累加器)
            0x1F.toByte(), 0x00.toByte(),  // neg
            0x20.toByte(), 0x00.toByte(),  // not
            0x1C.toByte(), 0x00.toByte(),  // typeof
            0x64.toByte()         // return
        )
        
        val module = Module("test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("unaryOps", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Errors: ${result.errors}")
            throw AssertionError("Conversion failed")
        }
        
        println("Generated IR:")
        println(result.function)
        
        println("✓ Unary operations test passed")
    }

    /**
     * 测试6: 从十六进制字符串解析字节码
     */
    fun testParseFromHex() {
        println("\n" + "=".repeat(60))
        println("Test 6: Parse from Hex String")
        println("=".repeat(60))
        
        // ldtrue (0x02), ldfalse (0x03), returnundefined (0x65)
        val hexString = "02 03 65"
        
        val parser = PandaAsmParser.fromHex(hexString)
        val instructions = parser.parseAll()
        
        println("Parsed ${instructions.size} instructions from hex:")
        instructions.forEach { println("  $it") }
        
        assert(instructions.size == 3) { "Expected 3 instructions" }
        
        println("✓ Hex parsing test passed")
    }

    /**
     * 测试7: 复杂指令序列
     */
    fun testComplexInstructionSequence() {
        println("\n" + "=".repeat(60))
        println("Test 7: Complex Instruction Sequence")
        println("=".repeat(60))
        
        // 混合多种指令
        val bytecode = byteArrayOf(
            0x00.toByte(),        // ldundefined
            0x01.toByte(),        // ldnull
            0x02.toByte(),        // ldtrue
            0x03.toByte(),        // ldfalse
            0x6A.toByte(),        // ldnan
            0x6B.toByte(),        // ldinfinity
            0x64.toByte()         // return
        )
        
        val module = Module("test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("complex", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Errors: ${result.errors}")
            throw AssertionError("Conversion failed")
        }
        
        println("Generated IR:")
        println(result.function)
        
        assert(result.function.verify()) { "Function verification failed" }
        
        println("✓ Complex sequence test passed")
    }

    /**
     * 测试8: 空字节码处理
     */
    fun testEmptyBytecode() {
        println("\n" + "=".repeat(60))
        println("Test 8: Empty Bytecode Handling")
        println("=".repeat(60))
        
        val bytecode = byteArrayOf()
        
        val module = Module("test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("empty", bytecode, paramCount = 0)
        
        println("Generated IR (should have minimal content):")
        println(result.function)
        
        // 空字节码应该生成一个带有警告的函数
        assert(result.warnings.isNotEmpty()) { "Expected warning for empty bytecode" }
        
        println("✓ Empty bytecode test passed")
    }



    /**
     * 测试10: 指令映射验证
     */
    fun testInstructionMapping() {
        println("\n" + "=".repeat(60))
        println("Test 10: Instruction Mapping Verification")
        println("=".repeat(60))
        
        // 测试标准指令映射
        val add2Mapping = InstructionMapping.mapStandardOpcode(PandaAsmOpcodes.StandardOpcode.ADD2)
        println("ADD2 -> $add2Mapping")
        assert(add2Mapping == Opcode.ADD) { "ADD2 should map to ADD" }
        
        val sub2Mapping = InstructionMapping.mapStandardOpcode(PandaAsmOpcodes.StandardOpcode.SUB2)
        println("SUB2 -> $sub2Mapping")
        assert(sub2Mapping == Opcode.SUB) { "SUB2 should map to SUB" }
        
        val returnMapping = InstructionMapping.mapStandardOpcode(PandaAsmOpcodes.StandardOpcode.RETURN)
        println("RETURN -> $returnMapping")
        assert(returnMapping == Opcode.RET) { "RETURN should map to RET" }
        
        // 测试指令类别
        val category = InstructionMapping.getInstructionCategory(Opcode.ADD)
        println("ADD category: $category")
        assert(category == InstructionCategory.ARITHMETIC) { 
            "ADD should be ARITHMETIC" 
        }
        
        println("✓ Instruction mapping test passed")
    }

    /**
     * 测试11: 多指令序列转换
     */
    fun testMultipleInstructions() {
        println("\n" + "=".repeat(60))
        println("Test 11: Multiple Instructions")
        println("=".repeat(60))
        
        // 创建一个包含多种指令的字节码序列
        val bytecode = byteArrayOf(
            // 加载各种常量
            0x04.toByte(),        // createemptyobject
            0x05.toByte(), 0x0A.toByte(),  // createemptyarray (capacity=10)
            0x02.toByte(),        // ldtrue
            0x1F.toByte(), 0x00.toByte(),  // neg
            0x20.toByte(), 0x00.toByte(),  // not
            0x64.toByte()         // return
        )
        
        val module = Module("test")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("multiple", bytecode, paramCount = 0)
        
        if (!result.isSuccess) {
            println("Errors: ${result.errors}")
            throw AssertionError("Conversion failed")
        }
        
        println("Generated IR:")
        println(result.function)
        
        assert(result.function.verify()) { "Function verification failed" }
        
        println("✓ Multiple instructions test passed")
    }

    /**
     * 测试12: 打印完整模块
     */
    fun testPrintModule() {
        println("\n" + "=".repeat(60))
        println("Test 12: Full Module Output")
        println("=".repeat(60))
        
        // 创建一个简单的字节码
        val bytecode = byteArrayOf(
            0x02.toByte(),        // ldtrue
            0x03.toByte(),        // ldfalse
            0x0A.toByte(), 0x00.toByte(), 0x00.toByte(),  // add2 (简化测试)
            0x64.toByte()         // return
        )
        
        val module = Module("test_module")
        val converter = BytecodeToIRConverter(module)
        val result = converter.convert("add", bytecode, paramCount = 2)
        
        println("Complete Module Output:")
        println(module.printDetailed())
        
        println("✓ Module output test passed")
    }

    /**
     * 运行所有测试
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println("\n" + "=".repeat(70))
        println(" PandaASM to SSA IR Conversion Tests ")
        println("=".repeat(70))
        
        var passed = 0
        var failed = 0
        
        val tests = listOf(
            ::testParseSimpleBytecode,
            ::testConvertReturnTrue,
            ::testConvertConstants,
            ::testConvertObjectAndArray,
            ::testUnaryOperations,
            ::testParseFromHex,
            ::testComplexInstructionSequence,
            ::testEmptyBytecode,
            ::testInstructionMapping,
            ::testMultipleInstructions,
            ::testPrintModule
        )
        
        for (test in tests) {
            try {
                test()
                passed++
            } catch (e: Exception) {
                println("\n✗ Test failed: ${e.message}")
                e.printStackTrace()
                failed++
            }
        }
        
        println("\n" + "=".repeat(70))
        println(" Test Results: $passed passed, $failed failed ")
        println("=".repeat(70))
        
        if (failed > 0) {
            System.exit(1)
        }
    }
}
