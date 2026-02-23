package com.orz.reark.core.backend.converter

import com.orz.reark.core.backend.InstructionMapping
import com.orz.reark.core.backend.PandaAsmOpcodes
import com.orz.reark.core.backend.PandaAsmParser

/**
 * 控制流分析器
 * 
 * 分析 PandaASM 字节码的控制流，识别基本块边界
 */
object ControlFlowAnalyzer {
    
    /**
     * 分析基本块边界
     */
    fun analyzeBlockBoundaries(instructions: List<PandaAsmParser.ParsedInstruction>): Set<Int> {
        val boundaries = mutableSetOf<Int>()
        
        // 第一条指令是边界
        if (instructions.isNotEmpty()) {
            boundaries.add(instructions[0].offset)
        }
        
        for ((index, inst) in instructions.withIndex()) {
            // 跳转指令的目标是边界
            if (isJumpInstruction(inst)) {
                val targetOffset = calculateJumpTarget(inst)
                if (targetOffset != null) {
                    boundaries.add(targetOffset)
                }
                // 跳转指令之后也是边界（如果存在）
                if (index + 1 < instructions.size) {
                    boundaries.add(instructions[index + 1].offset)
                }
            }
            
            // 终止指令之后是边界
            if (isTerminatorInstruction(inst)) {
                if (index + 1 < instructions.size) {
                    boundaries.add(instructions[index + 1].offset)
                }
            }
        }
        
        return boundaries
    }
    
    /**
     * 判断是否为跳转指令
     */
    fun isJumpInstruction(inst: PandaAsmParser.ParsedInstruction): Boolean {
        return when (inst.prefix) {
            PandaAsmParser.PrefixType.NONE -> {
                val opcode = PandaAsmOpcodes.StandardOpcode.fromByte(inst.opcode)
                opcode?.let {
                    InstructionMapping.isConditionalBranch(it) ||
                    it in listOf(
                        PandaAsmOpcodes.StandardOpcode.JMP,
                        PandaAsmOpcodes.StandardOpcode.JMP_16,
                        PandaAsmOpcodes.StandardOpcode.JMP_32
                    )
                } ?: false
            }
            else -> false
        }
    }
    
    /**
     * 判断是否为终止指令
     */
    fun isTerminatorInstruction(inst: PandaAsmParser.ParsedInstruction): Boolean {
        return when (inst.prefix) {
            PandaAsmParser.PrefixType.NONE -> {
                val opcode = PandaAsmOpcodes.StandardOpcode.fromByte(inst.opcode)
                opcode in listOf(
                    PandaAsmOpcodes.StandardOpcode.RETURN,
                    PandaAsmOpcodes.StandardOpcode.RETURNUNDEFINED
                )
            }
            PandaAsmParser.PrefixType.THROW -> true
            else -> false
        }
    }
    
    /**
     * 计算跳转目标
     */
    fun calculateJumpTarget(inst: PandaAsmParser.ParsedInstruction): Int? {
        val offset = when (val operand = inst.operands.firstOrNull()) {
            is PandaAsmParser.Operand.JumpOffset -> operand.offset
            else -> return null
        }
        return inst.offset + offset
    }
}
