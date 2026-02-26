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
     * 
     * @param functionName 函数名称
     * @param bytecode 字节码
     * @param paramCount 实际函数参数数量（不包括隐式参数 FunctionObject, NewTarget, this）
     * @param numVRegs 虚拟寄存器总数（用于计算参数寄存器位置）
     * @param numArgs 总参数数量（包括隐式参数，用于计算参数寄存器位置）
     * @param stringPool 可选的字符串池，用于恢复原始字符串
     */
    fun convert(
        functionName: String,
        bytecode: ByteArray,
        paramCount: Int = 0,
        numVRegs: Int = 0,
        numArgs: Int = 0,
        stringPool: Map<Int, String>? = null
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
        performConversion(function, instructions, paramCount, numVRegs, numArgs, warnings, errors, stringPool)
        
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
        numVRegs: Int,
        numArgs: Int,
        warnings: MutableList<String>,
        errors: MutableList<String>,
        stringPool: Map<Int, String>? = null
    ) {
        // 分析控制流
        val blockBoundaries = ControlFlowAnalyzer.analyzeBlockBoundaries(instructions)

        // 创建基本块
        val blockMap = mutableMapOf<Int, BasicBlock>()
        for (offset in blockBoundaries.sorted()) {
            blockMap[offset] = function.createBlock("bb_$offset")
        }

        // 首先建立 CFG 边 (前驱/后继关系)
        establishCFGEdges(instructions, blockMap)

        // 设置入口块和构建器
        val entryBlock = blockMap[instructions[0].offset] ?: function.createBlock("entry")
        val builder = IRBuilder(entryBlock)

        // 创建 SSA 构造上下文，传入 blockMap 以便指令转换器可以使用预创建的块
        val context = SSAConstructionContext(builder, module, blockMap)

        // 注册字符串映射（如果提供了字符串池）
        stringPool?.entries?.forEach { entry ->
            module.registerStringMapping("str_${entry.key}", entry.value)
        }

        // 设置参数寄存器映射
        // PandaVM 调用约定：
        // - numVRegs 是局部变量寄存器数量 (v0 到 v{numVRegs-1})
        // - 参数从 numVRegs + numArgs - paramCount 开始存放
        // - numArgs 包括 3 个隐式参数 (FunctionObject, NewTarget, this) + 实际参数
        if (paramCount > 0 && numVRegs > 0) {
            // 第一个实际参数的寄存器编号 = numVRegs + numArgs - paramCount
            // 例如：numVRegs=4, numArgs=5, paramCount=2
            // firstArgReg = 4 + 5 - 2 = 7 (即 v7, v8)
            val firstArgReg = numVRegs + numArgs - paramCount
            context.registerMapper.setupArgumentRegisters(function.arguments(), firstArgReg)
        } else if (paramCount > 0) {
            // 如果没有 numVRegs 信息，使用默认方式（从 0 开始）
            context.registerMapper.setupArgumentRegisters(function.arguments())
        }

        // 转换指令
        var currentBlock: BasicBlock = entryBlock
        // 首先设置初始块的上下文
        context.setCurrentBlock(currentBlock)

        for (inst in instructions) {
            // 检查是否需要切换基本块
            val targetBlock = blockMap[inst.offset]
            if (targetBlock != null && targetBlock != currentBlock) {
                currentBlock = targetBlock
                // 在切换块之前，确保 builder 的插入点正确设置
                context.builder.setInsertPoint(currentBlock)
                context.setCurrentBlock(currentBlock)
            }
            // 确保每次转换前插入点正确
            context.builder.setInsertPoint(currentBlock)

            // 转换单条指令
            try {
                convertInstruction(inst, context)
            } catch (e: Exception) {
                errors.add("Failed to convert instruction at offset 0x${inst.offset.toString(16)}: ${e.message}\n")
            }
        }

        // 后处理：为没有终止指令的块添加 fall-through 跳转
        // 如果一个块没有终止指令但有后继块，添加无条件跳转到第一个后继
        for (block in function.blocks().toList()) {
            if (!block.isTerminated() && block.successors().isNotEmpty()) {
                // 跳转到第一个后继块
                val targetBlock = block.successors()[0]
                context.builder.setInsertPoint(block)
                context.builder.createBr(targetBlock)
            } else if (!block.isTerminated()) {
                // 没有后继，添加 ret void
                context.builder.setInsertPoint(block)
                context.builder.createRetVoid()
            }
        }

        // 密封所有基本块 - 这是关键步骤，确保所有基本块都被正确密封
        // 按照支配顺序密封块（简单起见，这里按块 ID 顺序）
        val blocksInOrder = function.blocks().sortedBy { it.id }
        for (block in blocksInOrder) {
            context.registerMapper.sealBlock(block)
        }
        
        // 关键修复：在所有块都处理完毕后，完成 PHI 节点的填充
        // 这确保 PHI 节点正确获取前驱块中最后写入的值
        context.registerMapper.finalizePhiNodes()
    }

    /**
     * 建立基本块之间的控制流边
     */
    private fun establishCFGEdges(
        instructions: List<PandaAsmParser.ParsedInstruction>,
        blockMap: Map<Int, BasicBlock>
    ) {
        // 辅助函数：找到指令所属的块（块的起始 offset <= 指令 offset）
        fun findBlockForOffset(offset: Int): BasicBlock? {
            val blockOffset = blockMap.keys.filter { it <= offset }.maxOrNull()
            return blockOffset?.let { blockMap[it] }
        }

        // 辅助函数：获取指令所在块的结束偏移（下一条指令的起始偏移 - 1）
        fun getBlockEndOffset(instOffset: Int): Int {
            val blockOffset = blockMap.keys.filter { it <= instOffset }.maxOrNull()
                ?: return instOffset
            // 找到下一个块的起始偏移
            val nextBlockOffset = blockMap.keys.filter { it > blockOffset }.minOrNull()
            // 如果没有下一个块，返回当前指令偏移
            return nextBlockOffset?.let { it - 1 } ?: instOffset + 100
        }

        for ((index, inst) in instructions.withIndex()) {
            val currentBlock = findBlockForOffset(inst.offset)
            if (currentBlock == null) {
                continue
            }

            val isJump = ControlFlowAnalyzer.isJumpInstruction(inst)
            val isTerminator = ControlFlowAnalyzer.isTerminatorInstruction(inst)

            // 检查是否为跳转指令
            if (isJump) {
                val jumpTarget = ControlFlowAnalyzer.calculateJumpTarget(inst)
                if (jumpTarget != null) {
                    val targetBlock = blockMap[jumpTarget]
                    if (targetBlock != null) {
                        currentBlock.addSuccessor(targetBlock)
                    }
                }

                // 只有条件跳转才有 fall-through 块
                // 无条件跳转 (JMP, JMP_16, JMP_32) 没有 fall-through
                val isUnconditionalJump = PandaAsmOpcodes.StandardOpcode.fromByte(inst.opcode) in listOf(
                    PandaAsmOpcodes.StandardOpcode.JMP,
                    PandaAsmOpcodes.StandardOpcode.JMP_16,
                    PandaAsmOpcodes.StandardOpcode.JMP_32
                )

                if (!isUnconditionalJump) {
                    // fall-through 是下一条指令所属的块
                    if (index + 1 < instructions.size) {
                        val nextInstOffset = instructions[index + 1].offset
                        val fallThroughBlock = findBlockForOffset(nextInstOffset)
                        if (fallThroughBlock != null && fallThroughBlock != currentBlock) {
                            currentBlock.addSuccessor(fallThroughBlock)
                        }
                    }
                }
            } else if (isTerminator) {
                // 终止指令没有后继
                // 不做任何操作
            } else {
                // 普通指令：检查是否是块的最后一条指令
                // 如果是，需要添加 fall-through 边到下一条指令所属的块
                if (index + 1 < instructions.size) {
                    val nextInstOffset = instructions[index + 1].offset
                    val nextBlock = findBlockForOffset(nextInstOffset)
                    if (nextBlock != null && nextBlock != currentBlock) {
                        // 下一条指令属于不同的块，添加 fall-through 边
                        currentBlock.addSuccessor(nextBlock)
                    }
                }
            }
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
