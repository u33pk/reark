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
                context.setAccumulator(ConstantSpecial.UNDEFINED)
            }
            PandaAsmOpcodes.StandardOpcode.LDNULL -> {
                context.setAccumulator(ConstantSpecial.NULL)
            }
            PandaAsmOpcodes.StandardOpcode.LDTRUE -> {
                context.setAccumulator(ConstantInt.TRUE)
            }
            PandaAsmOpcodes.StandardOpcode.LDFALSE -> {
                context.setAccumulator(ConstantInt.FALSE)
            }
            PandaAsmOpcodes.StandardOpcode.LDNAN -> {
                context.setAccumulator(ConstantSpecial.NAN)
            }
            PandaAsmOpcodes.StandardOpcode.LDINFINITY -> {
                context.setAccumulator(ConstantSpecial.POS_INFINITY)
            }
            PandaAsmOpcodes.StandardOpcode.LDHOLE -> {
                context.setAccumulator(ConstantSpecial.HOLE)
            }
            PandaAsmOpcodes.StandardOpcode.LDAI -> {
                val value = when (val op = inst.operands.getOrNull(0)) {
                    is PandaAsmParser.Operand.Immediate8 -> op.value
                    is PandaAsmParser.Operand.Immediate32 -> op.value
                    else -> throw IllegalArgumentException("Expected immediate operand for ldai")
                }
                context.setAccumulator(ConstantInt.i32(value))
            }
            PandaAsmOpcodes.StandardOpcode.FLDAI -> {
                context.setAccumulator(ConstantFP.ZERO_F64)
            }
            PandaAsmOpcodes.StandardOpcode.LDA_STR -> {
                val strId = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.StringId
                    ?: throw IllegalArgumentException("Expected string ID operand")
                val strValue = module.getOrCreateStringConstant("str_${strId.id}")
                context.setAccumulator(strValue)
            }
            
            // ==================== 寄存器操作 ====================
            PandaAsmOpcodes.StandardOpcode.LDA -> {
                val reg = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected register operand")
                context.loadAccumulatorFromRegister(reg.regNum)
            }
            PandaAsmOpcodes.StandardOpcode.STA -> {
                val reg = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected register operand")
                context.storeAccumulatorToRegister(reg.regNum)
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
            PandaAsmOpcodes.StandardOpcode.STRICTNOTEQ -> {
                convertComparison(inst, context, opcode)
            }
            
            // ==================== 一元运算 ====================
            PandaAsmOpcodes.StandardOpcode.NEG -> {
                context.setAccumulator(builder.createNeg(context.getAccumulator()))
            }
            PandaAsmOpcodes.StandardOpcode.NOT -> {
                context.setAccumulator(builder.createNot(context.getAccumulator()))
            }
            PandaAsmOpcodes.StandardOpcode.INC -> {
                context.setAccumulator(builder.createInc(context.getAccumulator()))
            }
            PandaAsmOpcodes.StandardOpcode.DEC -> {
                context.setAccumulator(builder.createDec(context.getAccumulator()))
            }
            PandaAsmOpcodes.StandardOpcode.TYPEOF,
            PandaAsmOpcodes.StandardOpcode.TYPEOF_16 -> {
                context.setAccumulator(builder.createTypeOf(context.getAccumulator()))
            }
            PandaAsmOpcodes.StandardOpcode.TONUMBER -> {
                context.setAccumulator(builder.createToNumber(context.getAccumulator()))
            }
            PandaAsmOpcodes.StandardOpcode.TONUMERIC -> {
                context.setAccumulator(builder.createToNumeric(context.getAccumulator()))
            }
            PandaAsmOpcodes.StandardOpcode.ISTRUE -> {
                context.setAccumulator(builder.createIsTrue(context.getAccumulator()))
            }
            PandaAsmOpcodes.StandardOpcode.ISFALSE -> {
                context.setAccumulator(builder.createIsFalse(context.getAccumulator()))
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
                builder.createUnreachable()
            }
            PandaAsmOpcodes.StandardOpcode.JEQZ,
            PandaAsmOpcodes.StandardOpcode.JEQZ_16,
            PandaAsmOpcodes.StandardOpcode.JEQZ_32 -> {
                convertConditionalBranch(inst, context)
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
        val reg = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val rightValue = context.registerMapper.readRegister(reg.regNum)
            ?: throw IllegalStateException("Register v${reg.regNum} not defined")
        
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
            else -> throw IllegalStateException("Unexpected opcode")
        }
        context.setAccumulator(result)
    }
    
    private fun convertComparison(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext,
        opcode: PandaAsmOpcodes.StandardOpcode
    ) {
        val builder = context.builder
        val reg = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val rightValue = context.registerMapper.readRegister(reg.regNum)
            ?: throw IllegalStateException("Register v${reg.regNum} not defined")
        
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
            else -> throw IllegalStateException("Unexpected opcode")
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
        val reg1 = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
        val reg2 = inst.operands.getOrNull(2) as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand")
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
        val cond = context.getAccumulator()
        val func = context.getCurrentFunction() ?: throw IllegalStateException("No current function")
        val trueBlock = func.createBlock("jmp_true_${inst.offset}")
        val falseBlock = func.createBlock("jmp_false_${inst.offset}")
        context.builder.createCondBr(cond, trueBlock, falseBlock)
    }
}
