package com.orz.reark.core.backend.converter

import com.orz.reark.core.backend.PandaAsmOpcodes
import com.orz.reark.core.backend.PandaAsmParser
import com.orz.reark.core.backend.SSAConstructionContext
import com.orz.reark.core.ir.*

/**
 * 标准指令转换器
 * 
 * 处理 PandaASM 标准指令（无前缀）到 SSA IR 的转换
 */
object StandardInstructionConverter {
    
    /**
     * 转换标准指令
     */
    fun convert(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext,
        module: Module
    ) {
        val builder = context.builder
        val opcode = PandaAsmOpcodes.StandardOpcode.fromByte(inst.opcode)
            ?: throw IllegalArgumentException("Unknown standard opcode: 0x${inst.opcode.toString(16)}")
        
        when (opcode) {
            // ==================== 常量加载 ====================
            PandaAsmOpcodes.StandardOpcode.LDUNDEFINED -> {
                val copyInst = builder.createCopy(ConstantSpecial.UNDEFINED, "acc_undefined")
                context.setAccumulator(copyInst)
            }
            PandaAsmOpcodes.StandardOpcode.LDNULL -> {
                val copyInst = builder.createCopy(ConstantSpecial.NULL, "acc_null")
                context.setAccumulator(copyInst)
            }
            PandaAsmOpcodes.StandardOpcode.LDTRUE -> {
                val copyInst = builder.createCopy(ConstantInt.TRUE, "acc_true")
                context.setAccumulator(copyInst)
            }
            PandaAsmOpcodes.StandardOpcode.LDFALSE -> {
                val copyInst = builder.createCopy(ConstantInt.FALSE, "acc_false")
                context.setAccumulator(copyInst)
            }
            PandaAsmOpcodes.StandardOpcode.LDNAN -> {
                val copyInst = builder.createCopy(ConstantSpecial.NAN, "acc_nan")
                context.setAccumulator(copyInst)
            }
            PandaAsmOpcodes.StandardOpcode.LDINFINITY -> {
                val copyInst = builder.createCopy(ConstantSpecial.POS_INFINITY, "acc_infinity")
                context.setAccumulator(copyInst)
            }
            PandaAsmOpcodes.StandardOpcode.LDHOLE -> {
                val copyInst = builder.createCopy(ConstantSpecial.HOLE, "acc_hole")
                context.setAccumulator(copyInst)
            }
            PandaAsmOpcodes.StandardOpcode.LDAI -> {
                val value = when (val op = inst.operands.getOrNull(0)) {
                    is PandaAsmParser.Operand.Immediate8 -> op.value
                    is PandaAsmParser.Operand.Immediate32 -> op.value
                    else -> throw IllegalArgumentException("Expected immediate operand for ldai")
                }
                // 创建COPY指令包装常量，使得常量在IR中有明确定义
                val constValue = ConstantInt.i32(value)
                val copyInst = builder.createCopy(constValue, "acc_const_${value}")
                context.setAccumulator(copyInst)
            }
            PandaAsmOpcodes.StandardOpcode.FLDAI -> {
                context.setAccumulator(ConstantFP.ZERO_F64)
            }
            PandaAsmOpcodes.StandardOpcode.LDA_STR -> {
                val strId = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.StringId
                    ?: throw IllegalArgumentException("Expected string ID operand")
                val strValue = module.getOrCreateStringConstant("str_${strId.id}")
                val copyInst = builder.createCopy(strValue, "acc_str_${strId.id}")
                context.setAccumulator(copyInst)
            }
            
            // ==================== 寄存器操作 ====================
            PandaAsmOpcodes.StandardOpcode.LDA -> {
                val reg = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected register operand")
                // 从寄存器读取值，如果是常量则包装成COPY指令
                val regValue = context.registerMapper.readRegister(reg.regNum)
                if (regValue != null) {
                    // 如果寄存器值不是指令（比如是直接从ConstantInt创建的），创建COPY
                    val accValue = if (regValue is Instruction) {
                        regValue
                    } else {
                        builder.createCopy(regValue, "v${reg.regNum}_to_acc")
                    }
                    context.setAccumulator(accValue)
                } else {
                    // 寄存器未定义，创建未定义值并包装
                    val undefValue = UndefValue(anyType)
                    val copyInst = builder.createCopy(undefValue, "v${reg.regNum}_undef")
                    context.setAccumulator(copyInst)
                }
            }
            PandaAsmOpcodes.StandardOpcode.STA -> {
                val reg = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected register operand")
                // 创建COPY指令表示寄存器存储，使得寄存器值在IR中有明确定义
                val accValue = context.getAccumulator()
                val copyInst = builder.createCopy(accValue, "v${reg.regNum}")
                context.registerMapper.writeRegister(reg.regNum, copyInst, accValue.type)
            }
            PandaAsmOpcodes.StandardOpcode.MOV_4,
            PandaAsmOpcodes.StandardOpcode.MOV_8,
            PandaAsmOpcodes.StandardOpcode.MOV_16 -> {
                val srcReg = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected source register")
                val dstReg = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected destination register")
                val value = context.registerMapper.readRegister(srcReg.regNum)
                    ?: throw IllegalStateException("Source register v${srcReg.regNum} not defined")
                context.registerMapper.writeRegister(dstReg.regNum, value)
            }
            
            // ==================== 二元运算 ====================
            PandaAsmOpcodes.StandardOpcode.ADD2,
            PandaAsmOpcodes.StandardOpcode.SUB2,
            PandaAsmOpcodes.StandardOpcode.MUL2,
            PandaAsmOpcodes.StandardOpcode.DIV2,
            PandaAsmOpcodes.StandardOpcode.MOD2,
            PandaAsmOpcodes.StandardOpcode.SHL2,
            PandaAsmOpcodes.StandardOpcode.SHR2,
            PandaAsmOpcodes.StandardOpcode.ASHR2,
            PandaAsmOpcodes.StandardOpcode.AND2,
            PandaAsmOpcodes.StandardOpcode.OR2,
            PandaAsmOpcodes.StandardOpcode.XOR2,
            PandaAsmOpcodes.StandardOpcode.EXP -> {
                convertBinaryOperation(inst, context, opcode)
            }
            
            // ==================== 比较运算 ====================
            PandaAsmOpcodes.StandardOpcode.EQ,
            PandaAsmOpcodes.StandardOpcode.NOTEQ,
            PandaAsmOpcodes.StandardOpcode.LESS,
            PandaAsmOpcodes.StandardOpcode.LESSEQ,
            PandaAsmOpcodes.StandardOpcode.GREATER,
            PandaAsmOpcodes.StandardOpcode.GREATEREQ,
            PandaAsmOpcodes.StandardOpcode.STRICTEQ,
            PandaAsmOpcodes.StandardOpcode.STRICTNOTEQ,
            PandaAsmOpcodes.StandardOpcode.ISIN,
            PandaAsmOpcodes.StandardOpcode.INSTANCEOF -> {
                convertComparison(inst, context, opcode)
            }
            
            // ==================== 一元运算 ====================
            PandaAsmOpcodes.StandardOpcode.NEG -> {
                // neg imm:u8 - 操作数是 IC 槽索引，从累加器读取输入
                val imm = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                    ?: throw IllegalArgumentException("Expected immediate operand for neg")
                val value = context.getAccumulator()
                context.setAccumulator(builder.createNeg(value))
            }
            PandaAsmOpcodes.StandardOpcode.NOT -> {
                // not imm:u8 - 操作数是 IC 槽索引，从累加器读取输入
                val imm = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                    ?: throw IllegalArgumentException("Expected immediate operand for not")
                val value = context.getAccumulator()
                context.setAccumulator(builder.createNot(value))
            }
            PandaAsmOpcodes.StandardOpcode.INC -> {
                // inc imm:u8 - 操作数是 IC 槽索引，从累加器读取输入
                val imm = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                    ?: throw IllegalArgumentException("Expected immediate operand for inc")
                val value = context.getAccumulator()
                context.setAccumulator(builder.createInc(value))
            }
            PandaAsmOpcodes.StandardOpcode.DEC -> {
                // dec imm:u8 - 操作数是 IC 槽索引，从累加器读取输入
                val imm = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                    ?: throw IllegalArgumentException("Expected immediate operand for dec")
                val value = context.getAccumulator()
                context.setAccumulator(builder.createDec(value))
            }
            PandaAsmOpcodes.StandardOpcode.TYPEOF,
            PandaAsmOpcodes.StandardOpcode.TYPEOF_16 -> {
                // typeof imm:u8 - 操作数是 IC 槽索引，从累加器读取输入
                val imm = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                    ?: throw IllegalArgumentException("Expected immediate operand for typeof")
                val value = context.getAccumulator()
                context.setAccumulator(builder.createTypeOf(value))
            }
            PandaAsmOpcodes.StandardOpcode.TONUMBER -> {
                // tonumber imm:u8 - 操作数是 IC 槽索引，从累加器读取输入
                val imm = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                    ?: throw IllegalArgumentException("Expected immediate operand for tonumber")
                val value = context.getAccumulator()
                context.setAccumulator(builder.createToNumber(value))
            }
            PandaAsmOpcodes.StandardOpcode.TONUMERIC -> {
                // tonumeric imm:u8 - 操作数是 IC 槽索引，从累加器读取输入
                val imm = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                    ?: throw IllegalArgumentException("Expected immediate operand for tonumeric")
                val value = context.getAccumulator()
                context.setAccumulator(builder.createToNumeric(value))
            }
            PandaAsmOpcodes.StandardOpcode.ISTRUE -> {
                // istrue imm:u8 - 操作数是 IC 槽索引，从累加器读取输入
                val imm = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                    ?: throw IllegalArgumentException("Expected immediate operand for istrue")
                val value = context.getAccumulator()
                context.setAccumulator(builder.createIsTrue(value))
            }
            PandaAsmOpcodes.StandardOpcode.ISFALSE -> {
                // isfalse imm:u8 - 操作数是 IC 槽索引，从累加器读取输入
                val imm = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                    ?: throw IllegalArgumentException("Expected immediate operand for isfalse")
                val value = context.getAccumulator()
                context.setAccumulator(builder.createIsFalse(value))
            }
            
            // ==================== 对象创建 ====================
            PandaAsmOpcodes.StandardOpcode.CREATEEMPTYOBJECT -> {
                context.setAccumulator(builder.createEmptyObject())
            }
            PandaAsmOpcodes.StandardOpcode.CREATEEMPTYARRAY -> {
                val imm = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                val capacity = imm?.value ?: 0
                context.setAccumulator(builder.createEmptyArray(capacity))
            }
            
            // ==================== 属性访问 ====================
            PandaAsmOpcodes.StandardOpcode.LDOBJBYVALUE,
            PandaAsmOpcodes.StandardOpcode.LDOBJBYVALUE_16 -> {
                convertLoadObjByValue(inst, context)
            }
            PandaAsmOpcodes.StandardOpcode.STOBJBYVALUE,
            PandaAsmOpcodes.StandardOpcode.STOBJBYVALUE_16 -> {
                convertStoreObjByValue(inst, context)
            }
            PandaAsmOpcodes.StandardOpcode.LDOBJBYNAME,
            PandaAsmOpcodes.StandardOpcode.LDOBJBYNAME_16 -> {
                convertLoadObjByName(inst, context, module)
            }
            
            // ==================== 全局变量 ====================
            PandaAsmOpcodes.StandardOpcode.TRYLDGLOBALBYNAME,
            PandaAsmOpcodes.StandardOpcode.TRYLDGLOBALBYNAME_16 -> {
                convertTryLoadGlobalByName(inst, context, module)
            }
            PandaAsmOpcodes.StandardOpcode.LDGLOBALVAR -> {
                val globalRef = GlobalValue(anyType, "global_var", true)
                context.setAccumulator(globalRef)
            }
            
            // ==================== 调用 ====================
            PandaAsmOpcodes.StandardOpcode.CALLARG0 -> {
                val callee = context.getAccumulator()
                context.setAccumulator(builder.createCall(callee, emptyList()))
            }
            PandaAsmOpcodes.StandardOpcode.CALLARG1 -> {
                convertCallArg1(inst, context)
            }
            PandaAsmOpcodes.StandardOpcode.CALLARGS2 -> {
                convertCallArgs2(inst, context)
            }
            PandaAsmOpcodes.StandardOpcode.CALLARGS3 -> {
                convertCallArgs3(inst, context)
            }
            PandaAsmOpcodes.StandardOpcode.CALLTHIS0 -> {
                convertCallThis0(inst, context)
            }
            PandaAsmOpcodes.StandardOpcode.CALLTHIS1 -> {
                convertCallThis1(inst, context)
            }
            PandaAsmOpcodes.StandardOpcode.CALLTHIS2 -> {
                convertCallThis2(inst, context)
            }
            PandaAsmOpcodes.StandardOpcode.CALLTHIS3 -> {
                convertCallThis3(inst, context)
            }
            
            // ==================== 返回 ====================
            PandaAsmOpcodes.StandardOpcode.RETURN -> {
                builder.createRet(context.getAccumulator())
            }
            PandaAsmOpcodes.StandardOpcode.RETURNUNDEFINED -> {
                builder.createRetVoid()
            }
            
            // ==================== 跳转 ====================
            PandaAsmOpcodes.StandardOpcode.JMP,
            PandaAsmOpcodes.StandardOpcode.JMP_16,
            PandaAsmOpcodes.StandardOpcode.JMP_32 -> {
                // 无条件跳转
                val jumpOffset = (inst.operands.firstOrNull() as? PandaAsmParser.Operand.JumpOffset)?.offset ?: 0
                // PandaASM 的跳转偏移是相对于指令起始位置的
                val jumpTarget = inst.offset + jumpOffset
                val targetBlock = context.getBlockByOffset(jumpTarget)
                    ?: context.getCurrentFunction()?.createBlock("jmp_$jumpTarget")
                if (targetBlock != null) {
                    context.builder.createBr(targetBlock)
                }
            }
            PandaAsmOpcodes.StandardOpcode.JEQZ,
            PandaAsmOpcodes.StandardOpcode.JEQZ_16,
            PandaAsmOpcodes.StandardOpcode.JEQZ_32 -> {
            convertConditionalBranch(inst, context)
            }
            
            // 带寄存器的条件跳转
            PandaAsmOpcodes.StandardOpcode.JEQ,
            PandaAsmOpcodes.StandardOpcode.JEQ_16,
            PandaAsmOpcodes.StandardOpcode.JNE,
            PandaAsmOpcodes.StandardOpcode.JNE_16,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQ,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQ_16,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQ,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQ_16 -> {
                convertRegisterConditionalBranch(inst, context, opcode)
            }
            
            // ==================== 其他 ====================
            PandaAsmOpcodes.StandardOpcode.NOP -> {
                // 不做任何操作
            }
            PandaAsmOpcodes.StandardOpcode.LDTHIS -> {
                val thisArg = context.getCurrentFunction()?.arguments()?.firstOrNull()
                context.setAccumulator(thisArg ?: ConstantSpecial.UNDEFINED)
            }
            PandaAsmOpcodes.StandardOpcode.DEBUGGER -> {
                // 调试器指令映射为 nop
            }
            
            else -> {
                // 未实现的指令处理
            }
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private fun convertBinaryOperation(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext,
        opcode: PandaAsmOpcodes.StandardOpcode
    ) {
        val builder = context.builder
        // 二元运算指令格式: op_imm_8_v_8
        // 第一个操作数是立即数(IC槽索引)，第二个操作数是寄存器
        if (inst.operands.size < 2) {
            throw IllegalArgumentException("Binary operation requires 2 operands, got ${inst.operands.size}")
        }

        val imm = inst.operands[0]
        val reg = inst.operands[1] as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand at index 1, got ${inst.operands[1]}")

        val rightValue = context.registerMapper.readRegister(reg.regNum)
            ?: throw IllegalStateException("Register v${reg.regNum} not defined. Available registers: ${context.registerMapper.getUsedRegisters()}")

        val leftValue = context.getAccumulator()
        val result = when (opcode) {
            PandaAsmOpcodes.StandardOpcode.ADD2 -> builder.createAdd(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.SUB2 -> builder.createSub(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.MUL2 -> builder.createMul(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.DIV2 -> builder.createDiv(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.MOD2 -> builder.createMod(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.SHL2 -> builder.createShl(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.SHR2 -> builder.createShr(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.ASHR2 -> builder.createAShr(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.AND2 -> builder.createAnd(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.OR2 -> builder.createOr(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.XOR2 -> builder.createXor(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.EXP -> builder.createExp(leftValue, rightValue)
            else -> throw IllegalStateException("Unexpected opcode: $opcode")
        }
        context.setAccumulator(result)
    }
    
    private fun convertComparison(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext,
        opcode: PandaAsmOpcodes.StandardOpcode
    ) {
        val builder = context.builder
        // 二元运算和比较指令格式: op_imm_8_v_8
        // 第一个操作数是立即数(IC槽索引)，第二个操作数是寄存器
        if (inst.operands.size < 2) {
            throw IllegalArgumentException("Comparison instruction requires 2 operands, got ${inst.operands.size}")
        }

        val imm = inst.operands[0]
        val reg = inst.operands[1] as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand at index 1, got ${inst.operands[1]}")

        val rightValue = context.registerMapper.readRegister(reg.regNum)
            ?: throw IllegalStateException("Register v${reg.regNum} not defined. Available registers: ${context.registerMapper.getUsedRegisters()}")

        val leftValue = context.getAccumulator()

        val result = when (opcode) {
            PandaAsmOpcodes.StandardOpcode.EQ -> builder.createICmpEQ(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.NOTEQ -> builder.createICmpNE(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.LESS -> builder.createICmpSLT(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.LESSEQ -> builder.createICmpSLE(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.GREATER -> builder.createICmpSGT(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.GREATEREQ -> builder.createICmpSGE(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.STRICTEQ -> builder.createStrictEq(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.STRICTNOTEQ -> builder.createStrictNe(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.ISIN -> builder.createIsIn(rightValue, leftValue)
            PandaAsmOpcodes.StandardOpcode.INSTANCEOF -> builder.createInstanceOf(leftValue, rightValue)
            else -> throw IllegalStateException("Unexpected opcode: $opcode")
        }
        context.setAccumulator(result)
    }
    
    private fun convertLoadObjByValue(inst: PandaAsmParser.ParsedInstruction, context: SSAConstructionContext) {
        val reg = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val key = context.registerMapper.readRegister(reg.regNum)
            ?: throw IllegalStateException("Register v${reg.regNum} not defined")
        val obj = context.getAccumulator()
        context.setAccumulator(context.builder.createGetProperty(obj, key))
    }
    
    private fun convertStoreObjByValue(inst: PandaAsmParser.ParsedInstruction, context: SSAConstructionContext) {
        val builder = context.builder
        val reg1 = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val reg2 = inst.operands.getOrNull(2) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected value register")
        val obj = context.registerMapper.readRegister(reg1.regNum)
            ?: throw IllegalStateException("Register v${reg1.regNum} not defined")
        val key = context.getAccumulator()
        val value = context.registerMapper.readRegister(reg2.regNum)
            ?: throw IllegalStateException("Register v${reg2.regNum} not defined")
        builder.createSetProperty(obj, key, value)
    }
    
    private fun convertLoadObjByName(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext,
        module: Module
    ) {
        val strId = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.StringId
            ?: throw IllegalArgumentException("Expected string ID")
        val obj = context.getAccumulator()
        val key = module.getOrCreateStringConstant("prop_${strId.id}")
        context.setAccumulator(context.builder.createGetProperty(obj, key))
    }
    
    private fun convertTryLoadGlobalByName(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext,
        module: Module
    ) {
        val strId = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.StringId
            ?: throw IllegalArgumentException("Expected string ID")
        val name = "global_${strId.id}"
        val globalRef = GlobalValue(anyType, name, true)
        context.setAccumulator(globalRef)
    }
    
    private fun convertCallArg1(inst: PandaAsmParser.ParsedInstruction, context: SSAConstructionContext) {
        val reg = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val arg = context.registerMapper.readRegister(reg.regNum)
            ?: throw IllegalStateException("Register v${reg.regNum} not defined")
        val callee = context.getAccumulator()
        context.setAccumulator(context.builder.createCall(callee, listOf(arg)))
    }
    
    private fun convertCallArgs2(inst: PandaAsmParser.ParsedInstruction, context: SSAConstructionContext) {
        val reg1 = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val reg2 = inst.operands.getOrNull(2) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val arg1 = context.registerMapper.readRegister(reg1.regNum)
            ?: throw IllegalStateException("Register v${reg1.regNum} not defined")
        val arg2 = context.registerMapper.readRegister(reg2.regNum)
            ?: throw IllegalStateException("Register v${reg2.regNum} not defined")
        val callee = context.getAccumulator()
        context.setAccumulator(context.builder.createCall(callee, listOf(arg1, arg2)))
    }
    
    private fun convertCallArgs3(inst: PandaAsmParser.ParsedInstruction, context: SSAConstructionContext) {
        val reg1 = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val reg2 = inst.operands.getOrNull(2) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val reg3 = inst.operands.getOrNull(3) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val arg1 = context.registerMapper.readRegister(reg1.regNum)
            ?: throw IllegalStateException("Register v${reg1.regNum} not defined")
        val arg2 = context.registerMapper.readRegister(reg2.regNum)
            ?: throw IllegalStateException("Register v${reg2.regNum} not defined")
        val arg3 = context.registerMapper.readRegister(reg3.regNum)
            ?: throw IllegalStateException("Register v${reg3.regNum} not defined")
        val callee = context.getAccumulator()
        context.setAccumulator(context.builder.createCall(callee, listOf(arg1, arg2, arg3)))
    }
    
    private fun convertCallThis0(inst: PandaAsmParser.ParsedInstruction, context: SSAConstructionContext) {
        val reg = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val thisValue = context.registerMapper.readRegister(reg.regNum)
            ?: throw IllegalStateException("Register v${reg.regNum} not defined")
        val callee = context.getAccumulator()
        context.setAccumulator(context.builder.createCallThis(callee, thisValue, emptyList()))
    }
    
    private fun convertCallThis1(inst: PandaAsmParser.ParsedInstruction, context: SSAConstructionContext) {
        // callthis1格式: imm8 + v(this) + v(arg1)
        if (inst.operands.size < 3) {
            throw IllegalArgumentException("callthis1 requires 3 operands, got ${inst.operands.size}")
        }

        val imm = inst.operands[0]
        val reg1 = inst.operands[1] as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand at index 1 for 'this', got ${inst.operands[1]}")
        val reg2 = inst.operands[2] as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand at index 2 for arg1, got ${inst.operands[2]}")

        val thisValue = context.registerMapper.readRegister(reg1.regNum)
            ?: throw IllegalStateException("Register v${reg1.regNum} not defined")
        val arg = context.registerMapper.readRegister(reg2.regNum)
            ?: throw IllegalStateException("Register v${reg2.regNum} not defined")
        val callee = context.getAccumulator()
        context.setAccumulator(context.builder.createCallThis(callee, thisValue, listOf(arg)))
    }
    
    private fun convertCallThis2(inst: PandaAsmParser.ParsedInstruction, context: SSAConstructionContext) {
        val reg1 = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val reg2 = inst.operands.getOrNull(2) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val reg3 = inst.operands.getOrNull(3) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val thisValue = context.registerMapper.readRegister(reg1.regNum)
            ?: throw IllegalStateException("Register v${reg1.regNum} not defined")
        val arg1 = context.registerMapper.readRegister(reg2.regNum)
            ?: throw IllegalStateException("Register v${reg2.regNum} not defined")
        val arg2 = context.registerMapper.readRegister(reg3.regNum)
            ?: throw IllegalStateException("Register v${reg3.regNum} not defined")
        val callee = context.getAccumulator()
        context.setAccumulator(context.builder.createCallThis(callee, thisValue, listOf(arg1, arg2)))
    }
    
    private fun convertCallThis3(inst: PandaAsmParser.ParsedInstruction, context: SSAConstructionContext) {
        val reg1 = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val reg2 = inst.operands.getOrNull(2) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val reg3 = inst.operands.getOrNull(3) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val reg4 = inst.operands.getOrNull(4) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val thisValue = context.registerMapper.readRegister(reg1.regNum)
            ?: throw IllegalStateException("Register v${reg1.regNum} not defined")
        val arg1 = context.registerMapper.readRegister(reg2.regNum)
            ?: throw IllegalStateException("Register v${reg2.regNum} not defined")
        val arg2 = context.registerMapper.readRegister(reg3.regNum)
            ?: throw IllegalStateException("Register v${reg3.regNum} not defined")
        val arg3 = context.registerMapper.readRegister(reg4.regNum)
            ?: throw IllegalStateException("Register v${reg4.regNum} not defined")
        val callee = context.getAccumulator()
        context.setAccumulator(context.builder.createCallThis(callee, thisValue, listOf(arg1, arg2, arg3)))
    }
    
    private fun convertConditionalBranch(inst: PandaAsmParser.ParsedInstruction, context: SSAConstructionContext) {
        val builder = context.builder
        val acc = context.getAccumulator()

        // 计算跳转目标（PandaASM 的偏移量是相对于指令起始位置的）
        val jumpOffset = (inst.operands.firstOrNull() as? PandaAsmParser.Operand.JumpOffset)?.offset ?: 0
        val jumpTarget = inst.offset + jumpOffset

        // 获取预创建的目标块和 fall-through 块
        val targetBlock = context.getBlockByOffset(jumpTarget)
            ?: context.getCurrentFunction()?.createBlock("jmp_target_$jumpTarget")
        // fall-through 块是下一条指令的块
        val instLength = ControlFlowAnalyzer.calculateInstructionLength(inst)
        val fallThroughOffset = inst.offset + instLength
        val fallThroughBlock = context.getBlockByOffset(fallThroughOffset)
            ?: context.getCurrentFunction()?.createBlock("jmp_fallthrough_${inst.offset}")

        // 根据 JEQZ 的语义创建条件分支
        // JEQZ: Jump if Equal to Zero - 如果累加器等于 0 则跳转
        // 需要创建条件：acc == 0
        val cond = when (val opcode = PandaAsmOpcodes.StandardOpcode.fromByte(inst.opcode)) {
            PandaAsmOpcodes.StandardOpcode.JEQZ,
            PandaAsmOpcodes.StandardOpcode.JEQZ_16,
            PandaAsmOpcodes.StandardOpcode.JEQZ_32 -> {
                // acc == 0 时跳转
                val zero = when (acc.type) {
                    i32Type -> builder.getConstantI32(0)
                    i64Type -> builder.getConstantI64(0)
                    else -> builder.getConstantI32(0)
                }
                // 包装零值为COPY指令，确保在IR中有明确定义
                val zeroCopy = builder.createCopy(zero, "zero")
                builder.createICmpEQ(acc, zeroCopy)
            }
            PandaAsmOpcodes.StandardOpcode.JNEZ,
            PandaAsmOpcodes.StandardOpcode.JNEZ_16,
            PandaAsmOpcodes.StandardOpcode.JNEZ_32 -> {
                // acc != 0 时跳转
                val zero = when (acc.type) {
                    i32Type -> builder.getConstantI32(0)
                    i64Type -> builder.getConstantI64(0)
                    else -> builder.getConstantI32(0)
                }
                // 包装零值为COPY指令，确保在IR中有明确定义
                val zeroCopy = builder.createCopy(zero, "zero")
                builder.createICmpNE(acc, zeroCopy)
            }
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQZ,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQZ_16 -> {
                // acc === 0 时跳转
                val zero = when (acc.type) {
                    i32Type -> builder.getConstantI32(0)
                    i64Type -> builder.getConstantI64(0)
                    else -> builder.getConstantI32(0)
                }
                // 包装零值为COPY指令，确保在IR中有明确定义
                val zeroCopy = builder.createCopy(zero, "zero")
                builder.createStrictEq(acc, zeroCopy)
            }
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQZ,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQZ_16 -> {
                // acc !== 0 时跳转
                val zero = when (acc.type) {
                    i32Type -> builder.getConstantI32(0)
                    i64Type -> builder.getConstantI64(0)
                    else -> builder.getConstantI32(0)
                }
                // 包装零值为COPY指令，确保在IR中有明确定义
                val zeroCopy = builder.createCopy(zero, "zero")
                builder.createStrictNe(acc, zeroCopy)
            }
            else -> {
                // 其他条件跳转，直接使用累加器作为条件
                acc
            }
        }

        if (targetBlock != null && fallThroughBlock != null) {
            // createCondBr 的语义：条件为真跳转到第一个目标，否则跳转到第二个目标
            builder.createCondBr(cond, targetBlock, fallThroughBlock)
        }
    }
    
    /**
     * 转换带寄存器的条件跳转指令 (JEQ, JNE, JSTRICTEQ, JNSTRICTEQ 等)
     * 这些指令比较累加器和寄存器，然后根据结果跳转
     */
    private fun convertRegisterConditionalBranch(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext,
        opcode: PandaAsmOpcodes.StandardOpcode
    ) {
        val builder = context.builder
        
        // 获取寄存器操作数（第一个操作数是寄存器）
        val reg = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand for $opcode")
        
        // 获取跳转偏移（第二个操作数）
        val jumpOffset = (inst.operands.getOrNull(1) as? PandaAsmParser.Operand.JumpOffset)?.offset ?: 0
        
        // 读取寄存器值
        val regValue = context.registerMapper.readRegister(reg.regNum)
            ?: throw IllegalStateException("Register v${reg.regNum} not defined")
        
        // 获取累加器值
        val accValue = context.getAccumulator()
        
        // 执行比较
        val cond = when (opcode) {
            PandaAsmOpcodes.StandardOpcode.JEQ,
            PandaAsmOpcodes.StandardOpcode.JEQ_16 -> builder.createICmpEQ(accValue, regValue)
            PandaAsmOpcodes.StandardOpcode.JNE,
            PandaAsmOpcodes.StandardOpcode.JNE_16 -> builder.createICmpNE(accValue, regValue)
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQ,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQ_16 -> builder.createStrictEq(accValue, regValue)
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQ,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQ_16 -> builder.createStrictNe(accValue, regValue)
            else -> throw IllegalStateException("Unexpected opcode: $opcode")
        }
        
        // 计算跳转目标
        val jumpTarget = inst.offset + jumpOffset
        
        // 获取目标块和 fall-through 块
        // JEQ 等指令：如果比较相等（cond为真）则跳转
        val trueBlock = context.getBlockByOffset(jumpTarget)
            ?: context.getCurrentFunction()?.createBlock("jmp_true_$jumpTarget")
        
        val instLength = ControlFlowAnalyzer.calculateInstructionLength(inst)
        val fallThroughOffset = inst.offset + instLength
        val falseBlock = context.getBlockByOffset(fallThroughOffset)
            ?: context.getCurrentFunction()?.createBlock("jmp_false_${inst.offset}")
        
        if (trueBlock != null && falseBlock != null) {
            context.builder.createCondBr(cond, trueBlock, falseBlock)
        }
    }
}
