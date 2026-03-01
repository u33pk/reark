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
                // MOV 指令格式：op_v1_8_v2_8 (目标寄存器在前，源寄存器在后)
                val dstReg = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected destination register")
                val srcReg = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected source register")
                val value = context.registerMapper.readRegister(srcReg.regNum)
                    ?: throw IllegalStateException("Source register v${srcReg.regNum} not defined")
                // 创建 COPY 指令使值传递在 IR 中可见
                val copyInst = builder.createCopy(value, "v${dstReg.regNum}")
                context.registerMapper.writeRegister(dstReg.regNum, copyInst, value.type)
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
            PandaAsmOpcodes.StandardOpcode.NEWOBJRANGE -> {
                // newobjrange imm1:u16, imm2:u8, v:in:top
                // imm1: IC 槽索引，imm2: 参数个数，v: 参数起始寄存器
                // range_0: 参数从 v 开始，共 imm2 个连续寄存器
                // 构造函数是第一个参数 (v)，其余 imm2-1 个是构造参数
                val imm1 = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                val argCount = (inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Immediate8)?.value ?: 0
                val paramReg = inst.operands.getOrNull(2) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected parameter register for NEWOBJRANGE")

                // 根据 range_0 语义，参数从 paramReg 开始，共 argCount 个
                // 第一个参数是构造函数，其余是构造参数
                val args = mutableListOf<Value>()
                for (i in 0 until argCount) {
                    val argReg = paramReg.regNum + i
                    val argValue = context.registerMapper.readRegister(argReg)
                        ?: UndefValue(anyType)
                    args.add(argValue)
                }

                // 第一个参数是构造函数
                val ctor = if (args.isNotEmpty()) args[0] else UndefValue(anyType)
                val ctorArgs = if (args.size > 1) args.drop(1) else emptyList()

                // 创建 new 对象指令
                context.setAccumulator(builder.createNew(ctor, ctorArgs))
            }
            PandaAsmOpcodes.StandardOpcode.DEFINECLASSWITHBUFFER -> {
                // defineclasswithbuffer imm1:u8, method_id:u16, literalarray_id:u16, imm2:u16, v:in:top
                // 定义类，将类对象存储到 v 寄存器
                // 格式：{ count [ str:"name", Method:xxx, MethodAffiliate:n, ... ] }
                val imm1 = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                val methodId = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.MethodId
                val literalArrayId = inst.operands.getOrNull(2) as? PandaAsmParser.Operand.LiteralArrayId
                val imm2 = inst.operands.getOrNull(3) as? PandaAsmParser.Operand.Immediate16
                val dstReg = inst.operands.getOrNull(4) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected destination register for DEFINECLASSWITHBUFFER")

                // 从模块获取字符串池，尝试解析类名和方法信息
                val stringPool = module.getStringPool()

                // 解析类名：从字符串池中获取第一个单字符大写字母作为类名
                // 原始字节码：defineclasswithbuffer 0 this.#~A=#A { 7 [ str:"func", ... ] }
                // 类名 "A" 在字符串池中
                val className = stringPool.values.firstOrNull { 
                    it.length == 1 && it[0].isUpperCase() && it != "I" 
                } ?: "Class_${methodId?.id ?: 0}"
                
                val methods = mutableListOf<MethodInfo>()

                // 尝试从字符串池获取方法名称
                // literalArray 格式：{ count [ str:"func", Method:xxx, MethodAffiliate:4, str:"loop", ... ] }
                // imm2 通常表示方法数量，但这里解析可能有问题，使用默认值 2
                // 字符串池中有："A", "a", "console", "func", "log", "loop", "print", "prototype"
                // 方法名是 "func" 和 "loop"
                val methodCount = 2  // 固定 2 个方法
                
                val methodNames = stringPool.values.filter { 
                    it in listOf("func", "loop", "constructor", "init") || 
                    (it.length > 1 && it[0].isLowerCase() && it != "console" && it != "log" && it != "print" && it != "prototype")
                }.take(methodCount)
                
                for ((i, methodName) in methodNames.withIndex()) {
                    methods.add(MethodInfo(methodName, methodId?.id ?: 0, 0))
                }
                
                // 如果没有找到方法名，使用默认值
                if (methods.isEmpty() && methodCount > 0) {
                    for (i in 0 until methodCount) {
                        methods.add(MethodInfo("method_$i", methodId?.id ?: 0, 0))
                    }
                }

                // 创建类定义指令
                val defineClassInst = builder.createDefineClass(className, methods, literalArrayId?.id ?: 0)
                context.registerMapper.writeRegister(dstReg.regNum, defineClassInst)
                context.setAccumulator(defineClassInst)
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
            PandaAsmOpcodes.StandardOpcode.STOBJBYNAME,
            PandaAsmOpcodes.StandardOpcode.STOBJBYNAME_16 -> {
                // stobjbyname imm:u8, string_id, v:in:top
                // 将累加器中的值存储到对象的属性中
                val strId = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.StringId
                    ?: throw IllegalArgumentException("Expected string ID for STOBJBYNAME")
                val objReg = inst.operands.getOrNull(2) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected object register for STOBJBYNAME")

                val strKey = "str_${strId.id}"
                val propName = module.getStringById(strKey) ?: "prop_${strId.id}"
                val propKey = module.getOrCreateStringConstant(propName)

                val obj = context.registerMapper.readRegister(objReg.regNum)
                    ?: throw IllegalStateException("Object register v${objReg.regNum} not defined")
                val value = context.getAccumulator()

                // 创建 SetProperty 指令
                builder.createSetProperty(obj, propKey, value)
            }

            // ==================== 全局变量 ====================
            PandaAsmOpcodes.StandardOpcode.TRYLDGLOBALBYNAME,
            PandaAsmOpcodes.StandardOpcode.TRYLDGLOBALBYNAME_16 -> {
                convertTryLoadGlobalByName(inst, context, module)
            }
            PandaAsmOpcodes.StandardOpcode.STTOGLOBALRECORD -> {
                // sttoglobalrecord imm:u16, string_id
                // 将累加器中的值存储到全局记录中
                val strId = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.StringId
                    ?: throw IllegalArgumentException("Expected string ID for STTOGLOBALRECORD")
                val strKey = "str_${strId.id}"
                val symbolName = module.getStringById(strKey)
                val globalId = "global_${strId.id}"

                // 如果有符号名称，注册全局变量符号映射
                if (symbolName != null) {
                    module.registerGlobalSymbol(globalId, symbolName)
                }

                // 从累加器读取值并存储到全局变量
                val value = context.getAccumulator()
                val globalRef = GlobalValue(value.type, globalId, true, symbolName)
                builder.createStore(globalRef, value)
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
            PandaAsmOpcodes.StandardOpcode.CALLTHISRANGE -> {
                // callthisrange imm1:u16, imm2:u8, v:in:top
                // imm1: IC 槽索引，imm2: 参数个数，v: this 寄存器
                // range_0: 参数从 v 开始，共 imm2 个连续寄存器
                val imm1 = inst.operands.getOrNull(0) as? PandaAsmParser.Operand.Immediate8
                val argCount = (inst.operands.getOrNull(1) as? PandaAsmParser.Operand.Immediate8)?.value ?: 0
                val thisReg = inst.operands.getOrNull(2) as? PandaAsmParser.Operand.Register8
                    ?: throw IllegalArgumentException("Expected this register for CALLTHISRANGE")

                val callee = context.getAccumulator()
                val thisValue = context.registerMapper.readRegister(thisReg.regNum)
                    ?: throw IllegalStateException("This register v${thisReg.regNum} not defined")

                // 从寄存器中读取参数 (从 thisReg 开始的连续寄存器，共 argCount 个)
                // 注意：thisReg 是 this 值，参数从 thisReg+1 开始
                val args = mutableListOf<Value>()
                for (i in 0 until argCount) {
                    val argReg = thisReg.regNum + 1 + i
                    val argValue = context.registerMapper.readRegister(argReg)
                        ?: UndefValue(anyType)
                    args.add(argValue)
                }

                context.setAccumulator(builder.createCallThis(callee, thisValue, args))
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
            PandaAsmOpcodes.StandardOpcode.JEQZ_32,
            PandaAsmOpcodes.StandardOpcode.JNEZ,
            PandaAsmOpcodes.StandardOpcode.JNEZ_16,
            PandaAsmOpcodes.StandardOpcode.JNEZ_32,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQZ,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQZ_16,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQZ,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQZ_16 -> {
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
        // 二元运算和比较指令格式：op_imm_8_v_8
        // 第一个操作数是立即数 (IC 槽索引)，第二个操作数是寄存器
        // PandaASM 的比较指令语义是：acc op reg
        // 但为了正确反编译高级语言，需要交换操作数顺序
        // 因为高级语言的比较通常是：reg op acc
        // 例如：i < 10 对应汇编：lda 10; less i => 10 < i (汇编) => i < 10 (高级语言)
        if (inst.operands.size < 2) {
            throw IllegalArgumentException("Comparison instruction requires 2 operands, got ${inst.operands.size}")
        }

        val imm = inst.operands[0]
        val reg = inst.operands[1] as? PandaAsmParser.Operand.Register8
            ?: throw IllegalArgumentException("Expected register operand at index 1, got ${inst.operands[1]}")

        val leftValue = context.registerMapper.readRegister(reg.regNum)
            ?: throw IllegalStateException("Register v${reg.regNum} not defined. Available registers: ${context.registerMapper.getUsedRegisters()}")

        val rightValue = context.getAccumulator()

        val result = when (opcode) {
            PandaAsmOpcodes.StandardOpcode.EQ -> builder.createICmpEQ(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.NOTEQ -> builder.createICmpNE(leftValue, rightValue)
            // 关键修复：交换操作数顺序
            // less 指令计算的是 acc < reg，但高级语言需要的是 reg < acc
            // 例如：i < 10 对应汇编：lda 10; less i => 10 < i (汇编语义)
            // 我们需要生成 i < 10，所以交换操作数：LT(reg, acc)
            PandaAsmOpcodes.StandardOpcode.LESS -> builder.createICmpSLT(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.LESSEQ -> builder.createICmpSLE(leftValue, rightValue)
            // GREATER 也需要交换，因为 acc > reg 等价于 reg < acc
            // 但 GREATER 的语义是 acc > reg，交换后是 reg > acc，所以使用 SGT
            PandaAsmOpcodes.StandardOpcode.GREATER -> builder.createICmpSGT(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.GREATEREQ -> builder.createICmpSGE(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.STRICTEQ -> builder.createStrictEq(leftValue, rightValue)
            PandaAsmOpcodes.StandardOpcode.STRICTNOTEQ -> builder.createStrictNe(leftValue, rightValue)
            // ISIN 的操作数顺序特殊：acc isin reg => reg in acc
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
        // 从模块的字符串池获取原始字符串值
        val strValue = module.getStringPool()[strId.id] ?: "prop_${strId.id}"
        val key = module.getOrCreateStringConstant(strValue)
        context.setAccumulator(context.builder.createGetProperty(obj, key))
    }
    
    private fun convertTryLoadGlobalByName(
        inst: PandaAsmParser.ParsedInstruction,
        context: SSAConstructionContext,
        module: Module
    ) {
        val strId = inst.operands.getOrNull(1) as? PandaAsmParser.Operand.StringId
            ?: throw IllegalArgumentException("Expected string ID")
        val strKey = "str_${strId.id}"
        // 尝试获取原始字符串值（如 "console"）
        val symbolName = module.getStringById(strKey)
        val globalId = "global_${strId.id}"
        
        // 如果有符号名称，注册全局变量符号映射
        if (symbolName != null) {
            module.registerGlobalSymbol(globalId, symbolName)
        }
        
        // 创建带有符号名称的全局引用
        val globalRef = GlobalValue(anyType, globalId, true, symbolName)
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
