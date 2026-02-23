package com.orz.reark.core.backend

import com.orz.reark.core.backend.converter.ControlFlowAnalyzer
import com.orz.reark.core.backend.converter.PrefixInstructionConverters
import com.orz.reark.core.backend.converter.StandardInstructionConverter
import com.orz.reark.core.ir.*
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * PandaASM 字节码到 SSA IR 的转换器
 * 
 * 主转换器，协调控制流分析、指令转换和 SSA 构造
 */
class BytecodeToIRConverter(private val module: Module) {
    
    /**
     * 转换结果
     */
    data class ConversionResult(
        val function: SSAFunction,
        val warnings: List<String>,
        val errors: List<String>
    ) {
        val isSuccess: Boolean get() = errors.isEmpty()
    }
    
    /**
     * 将字节码转换为 IR 函数
     */
    fun convert(
        functionName: String,
        bytecode: ByteArray,
        paramCount: Int = 0
    ): ConversionResult {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        // 创建函数
        val function = module.createFunction(functionName, anyType)
        
        // 添加参数
        for (i in 0 until paramCount) {
            function.addArgument(anyType, "arg$i")
        }
        
        // 解析字节码
        val parser = PandaAsmParser(bytecode)
        val instructions = try {
            parser.parseAll()
        } catch (e: Exception) {
            errors.add("Failed to parse bytecode: ${e.message}")
            return ConversionResult(function, warnings, errors)
        }
        
        if (instructions.isEmpty()) {
            warnings.add("Empty bytecode")
            val entryBlock = function.createBlock("entry")
            val builder = IRBuilder(entryBlock)
            builder.createRetVoid()
            return ConversionResult(function, warnings, errors)
        }
        
        // 执行转换
        performConversion(function, instructions, paramCount, warnings, errors)
        
        // 验证函数
        if (!function.verify()) {
            warnings.add("Function verification failed")
        }
        
        return ConversionResult(function, warnings, errors)
    }
    
    /**
     * 执行实际的转换工作
     */
    private fun performConversion(
        function: SSAFunction,
        instructions: List<PandaAsmParser.ParsedInstruction>,
        paramCount: Int,
        warnings: MutableList<String>,
        errors: MutableList<String>
    ) {
        // 分析控制流
        val blockBoundaries = ControlFlowAnalyzer.analyzeBlockBoundaries(instructions)
        
        // 创建基本块
        val blockMap = mutableMapOf<Int, BasicBlock>()
        for (offset in blockBoundaries.sorted()) {
            blockMap[offset] = function.createBlock("bb_$offset")
        }
        
        // 设置入口块和构建器
        val entryBlock = blockMap[instructions[0].offset] ?: function.createBlock("entry")
        val builder = IRBuilder(entryBlock)
        
        // 创建 SSA 构造上下文
        val context = SSAConstructionContext(builder, module)
        
        // 设置参数寄存器映射
        if (paramCount > 0) {
            context.registerMapper.setupArgumentRegisters(function.arguments())
        }
        
        // 转换指令
        var currentBlock: BasicBlock = entryBlock
        for (inst in instructions) {
            // 检查是否需要切换基本块
            val targetBlock = blockMap[inst.offset]
            if (targetBlock != null && targetBlock != currentBlock) {
                currentBlock = targetBlock
                context.setCurrentBlock(currentBlock)
                
                if (inst.offset in blockBoundaries) {
                    context.sealBlock(currentBlock)
                }
            }
            
            // 转换单条指令
            try {
                convertInstruction(inst, context)
            } catch (e: Exception) {
                errors.add("Failed to convert instruction at offset 0x${inst.offset.toString(16)}: ${e.message}\n")
            }
        }
        
        // 密封所有基本块
        for (block in function.blocks()) {
            context.registerMapper.sealBlock(block)
        }
    }
    
    /**
     * 转换单条指令
     */
    private fun convertInstruction(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext
    ) {
        when (inst.prefix) {
            PandaAsmParser.PrefixType.NONE -> 
                StandardInstructionConverter.convert(inst, context, module)
            PandaAsmParser.PrefixType.WIDE -> 
                PrefixInstructionConverters.convertWide(inst, context)
            PandaAsmParser.PrefixType.DEPRECATED -> 
                PrefixInstructionConverters.convertDeprecated(inst, context)
            PandaAsmParser.PrefixType.THROW -> 
                PrefixInstructionConverters.convertThrow(inst, context)
            PandaAsmParser.PrefixType.CALLRUNTIME -> 
                PrefixInstructionConverters.convertCallRuntime(inst, context)
        }
    }
}
