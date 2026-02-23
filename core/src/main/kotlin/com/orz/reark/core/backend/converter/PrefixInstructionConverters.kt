package com.orz.reark.core.backend.converter

import com.orz.reark.core.backend.PandaAsmOpcodes
import com.orz.reark.core.backend.PandaAsmParser
import com.orz.reark.core.backend.SSAConstructionContext
import com.orz.reark.core.ir.*

/**
 * 前缀指令转换器
 * 
 * 处理 Wide、Deprecated、Throw、CallRuntime 前缀指令
 */
object PrefixInstructionConverters {
    
    /**
     * 转换 Wide 前缀指令
     */
    fun convertWide(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext
    ) {
        val builder = context.builder
        val opcode = PandaAsmOpcodes.WideOpcode.fromByte(inst.opcode)
            ?: throw IllegalArgumentException("Unknown wide opcode: 0x${inst.opcode.toString(16)}")
        
        when (opcode) {
            PandaAsmOpcodes.WideOpcode.CALLRANGE -> {
                val callee = context.getAccumulator()
                context.setAccumulator(builder.createCall(callee, emptyList()))
            }
            PandaAsmOpcodes.WideOpcode.CALLTHISRANGE -> {
                val callee = context.getAccumulator()
                context.setAccumulator(builder.createCallThis(callee, ConstantSpecial.UNDEFINED, emptyList()))
            }
            PandaAsmOpcodes.WideOpcode.NEWOBJRANGE -> {
                val callee = context.getAccumulator()
                context.setAccumulator(builder.createNew(callee, emptyList()))
            }
            PandaAsmOpcodes.WideOpcode.COPYRESTARGS -> {
                context.setAccumulator(builder.createEmptyArray())
            }
            else -> {
                // 未实现的 wide 指令
            }
        }
    }
    
    /**
     * 转换 Deprecated 前缀指令
     */
    fun convertDeprecated(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext
    ) {
        // 废弃指令通常映射到新的等价指令或生成警告
        // 简化处理：映射为 nop 或尝试找到等价的非废弃指令
    }
    
    /**
     * 转换 Throw 前缀指令
     */
    fun convertThrow(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext
    ) {
        val builder = context.builder
        val opcode = PandaAsmOpcodes.ThrowOpcode.fromByte(inst.opcode)
            ?: throw IllegalArgumentException("Unknown throw opcode: 0x${inst.opcode.toString(16)}")
        
        when (opcode) {
            PandaAsmOpcodes.ThrowOpcode.THROW -> {
                val exception = context.getAccumulator()
                builder.createThrow(exception)
            }
            else -> {
                // 其他 throw 变体也生成 throw
                val exception = context.getAccumulator()
                builder.createThrow(exception)
            }
        }
    }
    
    /**
     * 转换 CallRuntime 前缀指令
     */
    fun convertCallRuntime(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext
    ) {
        val builder = context.builder
        val opcode = PandaAsmOpcodes.CallRuntimeOpcode.fromByte(inst.opcode)
            ?: throw IllegalArgumentException("Unknown callruntime opcode: 0x${inst.opcode.toString(16)}")
        
        when (opcode) {
            PandaAsmOpcodes.CallRuntimeOpcode.ISTRUE -> {
                val operand = context.getAccumulator()
                context.setAccumulator(builder.createIsTrue(operand))
            }
            PandaAsmOpcodes.CallRuntimeOpcode.ISFALSE -> {
                val operand = context.getAccumulator()
                context.setAccumulator(builder.createIsFalse(operand))
            }
            PandaAsmOpcodes.CallRuntimeOpcode.TOPROPERTYKEY -> {
                val operand = context.getAccumulator()
                context.setAccumulator(builder.createCallRuntime("toPropertyKey", listOf(operand)))
            }
            else -> {
                // 其他运行时调用
                val operand = context.getAccumulator()
                context.setAccumulator(builder.createCallRuntime(opcode.name.lowercase(), listOf(operand)))
            }
        }
    }
}
