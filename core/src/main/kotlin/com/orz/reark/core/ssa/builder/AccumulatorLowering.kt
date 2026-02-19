package com.orz.reark.core.ssa.builder

import com.orz.reark.core.ssa.ir.*

/**
 * 累加器降低 (Accumulator Lowering)
 * 
 * pandaASM使用累加器作为隐式操作数，在转换为SSA时需要显式化。
 * 此类提供工具将累加器模型转换为纯SSA形式。
 */
class AccumulatorLowering(
    private val builder: IRBuilder
) {
    /**
     * 当前累加器值（在SSA中为显式值）
     */
    private var accumulator: Value? = null
    
    /**
     * 累加器类型跟踪
     */
    private var accumulatorType: Type = anyType
    
    /**
     * 获取当前累加器值
     */
    fun getAccumulator(): Value {
        return accumulator ?: throw IllegalStateException("Accumulator not set")
    }
    
    /**
     * 设置累加器值
     */
    fun setAccumulator(value: Value, type: Type = value.type) {
        accumulator = value
        accumulatorType = type
    }
    
    /**
     * 检查累加器是否已设置
     */
    fun hasAccumulator(): Boolean = accumulator != null
    
    /**
     * 清除累加器
     */
    fun clearAccumulator() {
        accumulator = null
    }
    
    /**
     * 创建加载到累加器的指令
     * 对应 pandaASM的 lda 指令
     */
    fun createLda(value: Value, type: Type = value.type) {
        setAccumulator(value, type)
    }
    
    /**
     * 创建从累加器存储的指令
     * 对应 pandaASM的 sta 指令
     */
    fun createSta(): Value {
        return getAccumulator()
    }
    
    /**
     * 创建二元运算（使用累加器作为左操作数）
     * 对应 pandaASM的 add2, sub2, 等
     */
    fun createBinaryOpWithAcc(op: BinaryOp, right: Value, name: String = ""): Value {
        val left = getAccumulator()
        val result = when (op) {
            BinaryOp.ADD -> builder.createAdd(left, right, name)
            BinaryOp.SUB -> builder.createSub(left, right, name)
            BinaryOp.MUL -> builder.createMul(left, right, name)
            BinaryOp.DIV -> builder.createDiv(left, right, name)
            BinaryOp.MOD -> builder.createMod(left, right, name)
            BinaryOp.SHL -> builder.createShl(left, right, name)
            BinaryOp.SHR -> builder.createShr(left, right, name)
            BinaryOp.ASHR -> builder.createAShr(left, right, name)
            BinaryOp.AND -> builder.createAnd(left, right, name)
            BinaryOp.OR -> builder.createOr(left, right, name)
            BinaryOp.XOR -> builder.createXor(left, right, name)
            BinaryOp.EXP -> builder.createExp(left, right, name)
        }
        setAccumulator(result)
        return result
    }
    
    /**
     * 创建比较运算（使用累加器作为左操作数）
     */
    fun createCompareOpWithAcc(op: CompareOp, right: Value, name: String = ""): Value {
        val left = getAccumulator()
        val result = when (op) {
            CompareOp.EQ -> builder.createICmpEQ(left, right, name)
            CompareOp.NE -> builder.createICmpNE(left, right, name)
            CompareOp.LT -> builder.createICmpSLT(left, right, name)
            CompareOp.LE -> builder.createICmpSLE(left, right, name)
            CompareOp.GT -> builder.createICmpSGT(left, right, name)
            CompareOp.GE -> builder.createICmpSGE(left, right, name)
            CompareOp.STRICT_EQ -> builder.createStrictEq(left, right, name)
            CompareOp.STRICT_NE -> builder.createStrictNe(left, right, name)
            CompareOp.ISIN -> builder.createIsIn(left, right, name)
            CompareOp.INSTANCEOF -> builder.createInstanceOf(left, right, name)
        }
        setAccumulator(result, boolType)
        return result
    }
    
    /**
     * 创建一元运算（使用累加器作为操作数）
     */
    fun createUnaryOpWithAcc(op: UnaryOp, name: String = ""): Value {
        val operand = getAccumulator()
        val result = when (op) {
            UnaryOp.NEG -> builder.createNeg(operand, name)
            UnaryOp.NOT -> builder.createNot(operand, name)
            UnaryOp.BIT_NOT -> builder.createBitNot(operand, name)
            UnaryOp.INC -> builder.createInc(operand, name)
            UnaryOp.DEC -> builder.createDec(operand, name)
            UnaryOp.TYPEOF -> builder.createTypeOf(operand, name)
            UnaryOp.TO_NUMBER -> builder.createToNumber(operand, name)
            UnaryOp.TO_NUMERIC -> builder.createToNumeric(operand, name)
            UnaryOp.IS_TRUE -> builder.createIsTrue(operand, name)
            UnaryOp.IS_FALSE -> builder.createIsFalse(operand, name)
        }
        setAccumulator(result)
        return result
    }
    
    /**
     * 创建调用（使用累加器作为被调用者）
     */
    fun createCallWithAcc(args: List<Value>, name: String = ""): Value {
        val callee = getAccumulator()
        val result = builder.createCall(callee, args, name)
        setAccumulator(result)
        return result
    }
    
    /**
     * 创建带this的调用（使用累加器作为被调用者）
     */
    fun createCallThisWithAcc(thisValue: Value, args: List<Value>, name: String = ""): Value {
        val callee = getAccumulator()
        val result = builder.createCallThis(callee, thisValue, args, name)
        setAccumulator(result)
        return result
    }
    
    /**
     * 创建属性访问（使用累加器作为对象）
     */
    fun createGetPropertyWithAcc(key: Value, name: String = ""): Value {
        val obj = getAccumulator()
        val result = builder.createGetProperty(obj, key, name)
        setAccumulator(result)
        return result
    }
    
    /**
     * 创建元素访问（使用累加器作为数组）
     */
    fun createGetElementWithAcc(index: Value, name: String = ""): Value {
        val array = getAccumulator()
        val result = builder.createGetElement(array, index, name)
        setAccumulator(result)
        return result
    }
    
    /**
     * 创建返回累加器
     */
    fun createReturnAcc(): Instruction {
        return builder.createRet(getAccumulator())
    }
}

/**
 * 二元操作类型
 */
enum class BinaryOp {
    ADD, SUB, MUL, DIV, MOD,
    SHL, SHR, ASHR,
    AND, OR, XOR, EXP
}

/**
 * 比较操作类型
 */
enum class CompareOp {
    EQ, NE, LT, LE, GT, GE,
    STRICT_EQ, STRICT_NE,
    ISIN, INSTANCEOF
}

/**
 * 一元操作类型
 */
enum class UnaryOp {
    NEG, NOT, BIT_NOT, INC, DEC,
    TYPEOF, TO_NUMBER, TO_NUMERIC,
    IS_TRUE, IS_FALSE
}

/**
 * 寄存器映射器
 * 
 * 将pandaASM的虚拟寄存器映射到SSA值
 */
class RegisterMapper {
    private val registerMap = mutableMapOf<Int, Value>()
    private val registerTypeMap = mutableMapOf<Int, Type>()
    
    /**
     * 设置寄存器值
     */
    fun setRegister(regNum: Int, value: Value, type: Type = value.type) {
        registerMap[regNum] = value
        registerTypeMap[regNum] = type
    }
    
    /**
     * 获取寄存器值
     */
    fun getRegister(regNum: Int): Value? = registerMap[regNum]
    
    /**
     * 获取寄存器类型
     */
    fun getRegisterType(regNum: Int): Type? = registerTypeMap[regNum]
    
    /**
     * 检查寄存器是否已设置
     */
    fun hasRegister(regNum: Int): Boolean = regNum in registerMap
    
    /**
     * 清除寄存器
     */
    fun clearRegister(regNum: Int) {
        registerMap.remove(regNum)
        registerTypeMap.remove(regNum)
    }
    
    /**
     * 清除所有寄存器
     */
    fun clearAll() {
        registerMap.clear()
        registerTypeMap.clear()
    }
    
    /**
     * 获取所有已使用的寄存器号
     */
    fun getUsedRegisters(): Set<Int> = registerMap.keys
}

/**
 * PandaASM到SSA的转换上下文
 */
class PandaToSSAContext(
    val builder: IRBuilder,
    val module: Module
) {
    val accumulatorLowering = AccumulatorLowering(builder)
    val registerMapper = RegisterMapper()
    
    /**
     * 基本块映射（用于跳转目标解析）
     */
    private val blockMap = mutableMapOf<Int, BasicBlock>()
    
    /**
     * 寄存器一个基本块
     */
    fun registerBlock(bytecodeOffset: Int, block: BasicBlock) {
        blockMap[bytecodeOffset] = block
    }
    
    /**
     * 获取或创建基本块
     */
    fun getOrCreateBlock(bytecodeOffset: Int): BasicBlock {
        return blockMap.getOrPut(bytecodeOffset) {
            builder.createBlock("bb_$bytecodeOffset")
        }
    }
    
    /**
     * 获取已注册的基本块
     */
    fun getBlock(bytecodeOffset: Int): BasicBlock? = blockMap[bytecodeOffset]
    
    /**
     * 值栈（用于表达式求值等）
     */
    private val valueStack = ArrayDeque<Value>()
    
    fun pushValue(value: Value) {
        valueStack.addLast(value)
    }
    
    fun popValue(): Value = valueStack.removeLast()
    
    fun peekValue(): Value = valueStack.last()
    
    fun isStackEmpty(): Boolean = valueStack.isEmpty()
    
    fun clearStack() {
        valueStack.clear()
    }
}
