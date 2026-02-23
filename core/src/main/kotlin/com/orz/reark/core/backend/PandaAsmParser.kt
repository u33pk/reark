package com.orz.reark.core.backend

/**
 * PandaASM 字节码解析器
 * 
 * 将 PandaVM 字节码解析为可处理的指令序列
 */
class PandaAsmParser(private val bytecode: ByteArray) {
    
    private var position = 0
    
    /**
     * 解析后的指令
     */
    data class ParsedInstruction(
        val offset: Int,                    // 指令在字节码中的偏移
        val opcode: Int,                    // 操作码字节
        val prefix: PrefixType,             // 前缀类型
        val operands: List<Operand>,        // 操作数列表
        val rawBytes: ByteArray             // 原始字节（用于调试）
    ) {
        override fun toString(): String {
            val prefixStr = when (prefix) {
                PrefixType.NONE -> ""
                PrefixType.WIDE -> "wide."
                PrefixType.THROW -> "throw."
                PrefixType.DEPRECATED -> "deprecated."
                PrefixType.CALLRUNTIME -> "callruntime."
            }
            val opcodeName = try {
                PandaAsmOpcodes.StandardOpcode.entries.find { it.opcode == opcode }?.name 
                    ?: "0x${opcode.toString(16)}"
            } catch (e: Exception) {
                "0x${opcode.toString(16)}"
            }
            val operandsStr = operands.joinToString(", ") { it.toString() }
            return String.format("0x%04X: %s%s %s", offset, prefixStr, opcodeName, operandsStr)
        }
    }
    
    /**
     * 操作数类型
     */
    sealed class Operand {
        data class Immediate8(val value: Int) : Operand() {
            override fun toString(): String = "#$value"
        }
        data class Immediate16(val value: Int) : Operand() {
            override fun toString(): String = "#$value"
        }
        data class Immediate32(val value: Int) : Operand() {
            override fun toString(): String = "#$value"
        }
        data class Immediate64(val value: Long) : Operand() {
            override fun toString(): String = "#$value"
        }
        data class Register8(val regNum: Int) : Operand() {
            override fun toString(): String = "v$regNum"
        }
        data class Register16(val regNum: Int) : Operand() {
            override fun toString(): String = "v$regNum"
        }
        data class StringId(val id: Int) : Operand() {
            override fun toString(): String = "str#$id"
        }
        data class MethodId(val id: Int) : Operand() {
            override fun toString(): String = "method#$id"
        }
        data class TypeId(val id: Int) : Operand() {
            override fun toString(): String = "type#$id"
        }
        data class FieldId(val id: Int) : Operand() {
            override fun toString(): String = "field#$id"
        }
        data class LiteralArrayId(val id: Int) : Operand() {
            override fun toString(): String = "literal#$id"
        }
        data class JumpOffset(val offset: Int) : Operand() {
            override fun toString(): String = "->${if (offset >= 0) "+" else ""}$offset"
        }
    }
    
    /**
     * 前缀类型
     */
    enum class PrefixType {
        NONE,
        WIDE,       // 0xFD
        DEPRECATED, // 0xFC
        THROW,      // 0xFE
        CALLRUNTIME // 0xFB
    }
    
    /**
     * 解析所有指令
     */
    fun parseAll(): List<ParsedInstruction> {
        val instructions = mutableListOf<ParsedInstruction>()
        
        while (position < bytecode.size) {
            val instruction = parseNextInstruction()
            if (instruction != null) {
                instructions.add(instruction)
            }
        }
        
        return instructions
    }
    
    /**
     * 解析下一条指令
     */
    fun parseNextInstruction(): ParsedInstruction? {
        if (position >= bytecode.size) return null
        
        val startOffset = position
        val startPos = position
        
        // 读取操作码或前缀
        var opcode = readUnsignedByte()
        var prefix = PrefixType.NONE
        
        // 检查前缀
        when (opcode) {
            PandaAsmOpcodes.PREFIX_WIDE -> {
                prefix = PrefixType.WIDE
                opcode = readUnsignedByte()
            }
            PandaAsmOpcodes.PREFIX_DEPRECATED -> {
                prefix = PrefixType.DEPRECATED
                opcode = readUnsignedByte()
            }
            PandaAsmOpcodes.PREFIX_THROW -> {
                prefix = PrefixType.THROW
                opcode = readUnsignedByte()
            }
            PandaAsmOpcodes.PREFIX_CALLRUNTIME -> {
                prefix = PrefixType.CALLRUNTIME
                opcode = readUnsignedByte()
            }
        }
        
        // 解析操作数
        val operands = parseOperands(opcode, prefix)
        
        // 提取原始字节
        val rawBytes = bytecode.copyOfRange(startPos, position)
        
        return ParsedInstruction(startOffset, opcode, prefix, operands, rawBytes)
    }
    
    /**
     * 根据指令格式解析操作数
     */
    private fun parseOperands(opcode: Int, prefix: PrefixType): List<Operand> {
        return when (prefix) {
            PrefixType.NONE -> parseStandardOperands(opcode)
            PrefixType.WIDE -> parseWideOperands(opcode)
            PrefixType.DEPRECATED -> parseDeprecatedOperands(opcode)
            PrefixType.THROW -> parseThrowOperands(opcode)
            PrefixType.CALLRUNTIME -> parseCallRuntimeOperands(opcode)
        }
    }
    
    /**
     * 解析标准指令的操作数
     */
    private fun parseStandardOperands(opcode: Int): List<Operand> {
        return when (opcode) {
            // 无操作数
            0x00, 0x01, 0x02, 0x03, 0x04, 0x23, 0x24, 0x64, 0x65, 0x66, 0x69, 0x6A, 0x6B,
            0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0xBF, 0xC0, 0xD5 -> emptyList()
            
            // 8位立即数 (单操作数)
            0x05, 0x09, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33,
            0x34, 0x35, 0x36, 0x3C, 0x3D,
            0x4F, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5B,
            0x67, 0x68, 0x73, 0x76, 0x77, 0x7B, 0x7C, 0x7D, 0x7E, 0xCF, 0xD6, 0xD7 -> 
                listOf(Operand.Immediate8(readUnsignedByte()))
            
            // 8位寄存器 (lda=0x60, sta=0x61)
            0x60, 0x61 -> listOf(Operand.Register8(readUnsignedByte()))
            
            // 16位立即数
            0x41, 0x4E, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F, 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5,
            0xA6, 0xA7, 0xA8, 0xA9, 0xAA, 0xAB, 0xAC -> listOf(Operand.Immediate16(readUnsignedShort()))
            
            // 32位立即数（跳转偏移）
            0x4D, 0x98, 0x9A -> listOf(Operand.JumpOffset(readInt()))
            
            // 16位字符串ID
            0x3E -> listOf(Operand.StringId(readUnsignedShort()))
            
            // 8位跳转偏移（条件跳转）
            in 0x4F..0x5F -> listOf(Operand.JumpOffset(readSignedByte()))
            
            // 16位跳转偏移（条件跳转）
            0x9B, 0x9C, 0x9D, 0x9E, 0x9F, 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7,
            0xA8, 0xA9, 0xAA -> listOf(Operand.JumpOffset(readSignedShort()))
            
            // 8位寄存器
            0x08, 0x36, 0x5C, 0x5D, 0x5E, 0x5F -> listOf(Operand.Register8(readUnsignedByte()))
            
            // 8位立即数 + 8位寄存器 (二元运算和比较指令)
            // add2(0x0A), sub2(0x0B), mul2(0x0C), div2(0x0D), mod2(0x0E)
            // eq(0x0F), noteq(0x10), less(0x11), lesseq(0x12), greater(0x13), greatereq(0x14)
            // shl2(0x15), shr2(0x16), ashr2(0x17), and2(0x18), or2(0x19), xor2(0x1A), exp(0x1B)
            // isin(0x25), instanceof(0x26), strictnoteq(0x27), stricteq(0x28)
            0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
            0x17, 0x18, 0x19, 0x1A, 0x1B, 
            0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28 -> {
                val imm = readUnsignedByte()
                val reg = readUnsignedByte()
                listOf(Operand.Immediate8(imm), Operand.Register8(reg))
            }
            
            // 32位立即数 (ldai)
            0x62 -> listOf(Operand.Immediate32(readInt()))
            
            // 64位浮点立即数 (fldai) 
            0x63 -> listOf(Operand.Immediate64(readLong()))
            
            // 16位立即数 + 16位ID (ldglobalvar, stglobalvar, etc.)
            0x41, 0x47, 0x48 -> {
                val imm = readUnsignedShort()
                val id = readUnsignedShort()
                listOf(Operand.Immediate16(imm), Operand.StringId(id))
            }
            
            // 8位立即数 + 16位ID (tryldglobalbyname, trystglobalbyname, etc.)
            0x3F, 0x40, 0x42, 0x43, 0x49, 0x4A -> {
                val imm = readUnsignedByte()
                val id = readUnsignedShort()
                listOf(Operand.Immediate8(imm), Operand.StringId(id))
            }
            
            // callthis 系列: imm8 + this + args...
            // callthis0(0x2D): imm8 + v(this)
            0x2D -> {
                val imm = readUnsignedByte()
                val thisReg = readUnsignedByte()
                listOf(Operand.Immediate8(imm), Operand.Register8(thisReg))
            }
            // callthis1(0x2E): imm8 + v(this) + v(arg1)
            0x2E -> {
                val imm = readUnsignedByte()
                val thisReg = readUnsignedByte()
                val arg1 = readUnsignedByte()
                listOf(Operand.Immediate8(imm), Operand.Register8(thisReg), Operand.Register8(arg1))
            }
            // callthis2(0x2F): imm8 + v(this) + v(arg1) + v(arg2)
            0x2F -> {
                val imm = readUnsignedByte()
                val thisReg = readUnsignedByte()
                val arg1 = readUnsignedByte()
                val arg2 = readUnsignedByte()
                listOf(Operand.Immediate8(imm), Operand.Register8(thisReg), 
                       Operand.Register8(arg1), Operand.Register8(arg2))
            }
            // callthis3(0x30): imm8 + v(this) + v(arg1) + v(arg2) + v(arg3)
            0x30 -> {
                val imm = readUnsignedByte()
                val thisReg = readUnsignedByte()
                val arg1 = readUnsignedByte()
                val arg2 = readUnsignedByte()
                val arg3 = readUnsignedByte()
                listOf(Operand.Immediate8(imm), Operand.Register8(thisReg), 
                       Operand.Register8(arg1), Operand.Register8(arg2), Operand.Register8(arg3))
            }
            
            // 两个8位寄存器
            0xB1, 0xB2, 0xB7 -> {
                val reg1 = readUnsignedByte()
                val reg2 = readUnsignedByte()
                listOf(Operand.Register8(reg1), Operand.Register8(reg2))
            }
            
            // 三个8位寄存器
            0xB8, 0xC5, 0xC6 -> {
                val reg1 = readUnsignedByte()
                val reg2 = readUnsignedByte()
                val reg3 = readUnsignedByte()
                listOf(Operand.Register8(reg1), Operand.Register8(reg2), Operand.Register8(reg3))
            }
            
            // 四个8位寄存器
            0xBC -> {
                val reg1 = readUnsignedByte()
                val reg2 = readUnsignedByte()
                val reg3 = readUnsignedByte()
                val reg4 = readUnsignedByte()
                listOf(
                    Operand.Register8(reg1), Operand.Register8(reg2),
                    Operand.Register8(reg3), Operand.Register8(reg4)
                )
            }
            
            // 复杂格式 - 默认读取剩余字节
            else -> {
                // 尝试推断格式
                val remaining = bytecode.size - position
                when {
                    remaining >= 8 -> listOf(
                        Operand.Immediate8(readUnsignedByte()),
                        Operand.Immediate16(readUnsignedShort()),
                        Operand.Immediate16(readUnsignedShort()),
                        Operand.Immediate16(readUnsignedShort())
                    )
                    remaining >= 4 -> listOf(
                        Operand.Immediate8(readUnsignedByte()),
                        Operand.Immediate16(readUnsignedShort())
                    )
                    remaining >= 2 -> listOf(Operand.Immediate16(readUnsignedShort()))
                    remaining >= 1 -> listOf(Operand.Immediate8(readUnsignedByte()))
                    else -> emptyList()
                }
            }
        }
    }
    
    /**
     * 解析 Wide 前缀指令的操作数
     */
    private fun parseWideOperands(opcode: Int): List<Operand> {
        return when (opcode) {
            // 16位立即数
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x12, 0x13 -> listOf(Operand.Immediate16(readUnsignedShort()))
            
            // 16位立即数 + 8位寄存器
            0x04, 0x05, 0x06, 0x07 -> {
                val imm = readUnsignedShort()
                val reg = readUnsignedByte()
                listOf(Operand.Immediate16(imm), Operand.Register8(reg))
            }
            
            // 32位立即数
            0x08, 0x09, 0x0A -> {
                val imm32 = readInt()
                listOf(Operand.Immediate32(imm32))
            }
            
            // 两个16位立即数
            0x0C, 0x0D -> {
                val imm1 = readUnsignedShort()
                val imm2 = readUnsignedShort()
                listOf(Operand.Immediate16(imm1), Operand.Immediate16(imm2))
            }
            
            else -> listOf(Operand.Immediate16(readUnsignedShort()))
        }
    }
    
    /**
     * 解析 Deprecated 前缀指令的操作数
     */
    private fun parseDeprecatedOperands(opcode: Int): List<Operand> {
        return when (opcode) {
            // 无操作数
            0x00, 0x01, 0x2B -> emptyList()
            
            // 8位立即数
            0x2C -> listOf(Operand.Immediate8(readUnsignedByte()))
            
            // 16位立即数
            0x03, 0x04, 0x0F, 0x12, 0x2C -> listOf(Operand.Immediate16(readUnsignedShort()))
            
            // 8位寄存器
            0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x13, 0x14, 0x15, 0x17, 0x18, 0x19,
            0x2D, 0x2E -> listOf(Operand.Register8(readUnsignedByte()))
            
            // 两个8位寄存器
            0x02, 0x0C, 0x10, 0x16, 0x1A, 0x1B, 0x1C, 0x1E, 0x1F -> {
                val reg1 = readUnsignedByte()
                val reg2 = readUnsignedByte()
                listOf(Operand.Register8(reg1), Operand.Register8(reg2))
            }
            
            // 三个8位寄存器
            0x0D, 0x1E, 0x1F -> {
                val reg1 = readUnsignedByte()
                val reg2 = readUnsignedByte()
                val reg3 = readUnsignedByte()
                listOf(Operand.Register8(reg1), Operand.Register8(reg2), Operand.Register8(reg3))
            }
            
            // 四个8位寄存器
            0x0E -> {
                val reg1 = readUnsignedByte()
                val reg2 = readUnsignedByte()
                val reg3 = readUnsignedByte()
                val reg4 = readUnsignedByte()
                listOf(
                    Operand.Register8(reg1), Operand.Register8(reg2),
                    Operand.Register8(reg3), Operand.Register8(reg4)
                )
            }
            
            // 32位ID
            0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A -> {
                val id = readInt()
                listOf(Operand.StringId(id))
            }
            
            // 16位ID + 两个8位寄存器
            0x12 -> {
                val id = readUnsignedShort()
                val reg1 = readUnsignedByte()
                val reg2 = readUnsignedByte()
                listOf(Operand.MethodId(id), Operand.Register8(reg1), Operand.Register8(reg2))
            }
            
            else -> emptyList()
        }
    }
    
    /**
     * 解析 Throw 前缀指令的操作数
     */
    private fun parseThrowOperands(opcode: Int): List<Operand> {
        return when (opcode) {
            // 无操作数
            0x00, 0x01, 0x02, 0x03 -> emptyList()
            
            // 8位寄存器
            0x04, 0x05, 0x06 -> listOf(Operand.Register8(readUnsignedByte()))
            
            // 8/16位立即数
            0x07 -> listOf(Operand.Immediate8(readUnsignedByte()))
            0x08 -> listOf(Operand.Immediate16(readUnsignedShort()))
            
            // 16位字符串ID
            0x09 -> listOf(Operand.StringId(readUnsignedShort()))
            
            // 两个8位寄存器
            0x06 -> {
                val reg1 = readUnsignedByte()
                val reg2 = readUnsignedByte()
                listOf(Operand.Register8(reg1), Operand.Register8(reg2))
            }
            
            else -> emptyList()
        }
    }
    
    /**
     * 解析 CallRuntime 前缀指令的操作数
     */
    private fun parseCallRuntimeOperands(opcode: Int): List<Operand> {
        return when (opcode) {
            // 无操作数
            0x00, 0x03 -> emptyList()
            
            // 8位立即数
            0x09, 0x0B, 0x15, 0x17 -> listOf(Operand.Immediate8(readUnsignedByte()))
            
            // 16位立即数
            0x0A, 0x0C, 0x0F, 0x10, 0x11, 0x12, 0x16, 0x18 -> listOf(Operand.Immediate16(readUnsignedShort()))
            
            // 8位立即数 + 两个8位寄存器
            0x01, 0x06 -> {
                val imm = readUnsignedByte()
                val reg1 = readUnsignedByte()
                val reg2 = readUnsignedByte()
                listOf(Operand.Immediate8(imm), Operand.Register8(reg1), Operand.Register8(reg2))
            }
            
            // 8位 + 32位立即数 + 8位寄存器
            0x02 -> {
                val imm1 = readUnsignedByte()
                val imm2 = readInt()
                val reg = readUnsignedByte()
                listOf(Operand.Immediate8(imm1), Operand.Immediate32(imm2), Operand.Register8(reg))
            }
            
            // 16位立即数 + 16位ID
            0x04 -> {
                val imm = readUnsignedShort()
                val id = readUnsignedShort()
                listOf(Operand.Immediate16(imm), Operand.LiteralArrayId(id))
            }
            
            // 8位 + 两个16位立即数 + 8位寄存器
            0x05 -> {
                val imm1 = readUnsignedByte()
                val imm2 = readUnsignedShort()
                val imm3 = readUnsignedShort()
                val reg = readUnsignedByte()
                listOf(
                    Operand.Immediate8(imm1), Operand.Immediate16(imm2),
                    Operand.Immediate16(imm3), Operand.Register8(reg)
                )
            }
            
            // 复杂格式
            0x07 -> {
                val imm1 = readUnsignedShort()
                val methodId = readUnsignedShort()
                val literalId = readUnsignedShort()
                val imm2 = readUnsignedShort()
                val reg = readUnsignedByte()
                listOf(
                    Operand.Immediate16(imm1), Operand.MethodId(methodId),
                    Operand.LiteralArrayId(literalId), Operand.Immediate16(imm2),
                    Operand.Register8(reg)
                )
            }
            
            // 两个4位立即数
            0x0D, 0x0E -> {
                val byte = readUnsignedByte()
                val imm1 = byte shr 4
                val imm2 = byte and 0x0F
                listOf(Operand.Immediate8(imm1), Operand.Immediate8(imm2))
            }
            
            // 8位寄存器
            0x19 -> listOf(Operand.Register8(readUnsignedByte()))
            
            // 两个8位立即数
            0x13, 0x14 -> {
                val imm1 = readUnsignedByte()
                val imm2 = readUnsignedByte()
                listOf(Operand.Immediate8(imm1), Operand.Immediate8(imm2))
            }
            
            else -> emptyList()
        }
    }
    
    // ==================== 读取辅助方法 ====================
    
    private fun readUnsignedByte(): Int {
        if (position >= bytecode.size) return 0
        return bytecode[position++].toInt() and 0xFF
    }
    
    private fun readSignedByte(): Int {
        if (position >= bytecode.size) return 0
        return bytecode[position++].toInt()
    }
    
    private fun readUnsignedShort(): Int {
        if (position + 1 >= bytecode.size) {
            position = bytecode.size
            return 0
        }
        val b1 = bytecode[position++].toInt() and 0xFF
        val b2 = bytecode[position++].toInt() and 0xFF
        return (b2 shl 8) or b1
    }
    
    private fun readSignedShort(): Int {
        val value = readUnsignedShort()
        return if (value >= 0x8000) value - 0x10000 else value
    }
    
    private fun readInt(): Int {
        if (position + 3 >= bytecode.size) {
            position = bytecode.size
            return 0
        }
        val b1 = bytecode[position++].toInt() and 0xFF
        val b2 = bytecode[position++].toInt() and 0xFF
        val b3 = bytecode[position++].toInt() and 0xFF
        val b4 = bytecode[position++].toInt() and 0xFF
        return (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1
    }
    
    private fun readLong(): Long {
        if (position + 7 >= bytecode.size) {
            position = bytecode.size
            return 0
        }
        val b1 = bytecode[position++].toLong() and 0xFF
        val b2 = bytecode[position++].toLong() and 0xFF
        val b3 = bytecode[position++].toLong() and 0xFF
        val b4 = bytecode[position++].toLong() and 0xFF
        val b5 = bytecode[position++].toLong() and 0xFF
        val b6 = bytecode[position++].toLong() and 0xFF
        val b7 = bytecode[position++].toLong() and 0xFF
        val b8 = bytecode[position++].toLong() and 0xFF
        return (b8 shl 56) or (b7 shl 48) or (b6 shl 40) or (b5 shl 32) or
               (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1
    }
    
    companion object {
        /**
         * 从字节数组创建解析器
         */
        fun fromBytes(bytecode: ByteArray): PandaAsmParser {
            return PandaAsmParser(bytecode)
        }
        
        /**
         * 从十六进制字符串创建解析器（用于测试）
         */
        fun fromHex(hexString: String): PandaAsmParser {
            val cleanHex = hexString.replace(Regex("\\s+"), "")
            val bytes = cleanHex.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            return PandaAsmParser(bytes)
        }
    }
}
