package com.orz.reark.core.ir

// ==================== 终止指令 ====================

/**
 * 返回指令
 */
class ReturnInst(
    returnValue: Value? = null
) : Instruction(
    if (returnValue != null) Opcode.RET else Opcode.RET_VOID,
    voidType
) {
    init {
        returnValue?.let { addOperand(it) }
    }
    
    val returnValue: Value? get() = if (operandCount() > 0) getOperand(0) else null
    
    override fun isTerminator(): Boolean = true
    override fun getOpcodeString(): String = "ret "
    
    override fun toString(): String {
        return if (returnValue != null) "ret ${returnValue!!.getNameOrTemporary()}" else "ret void"
    }
}

/**
 * 无条件分支
 */
class BranchInst(
    val target: BasicBlock
) : Instruction(Opcode.BR, voidType) {
    
    override fun isTerminator(): Boolean = true
    override fun getOpcodeString(): String = "br "
    
    override fun toString(): String = "br ${target.getNameOrTemporary()}"
}

/**
 * 条件分支（通用）
 */
class CondBranchInst(
    condition: Value,
    val trueTarget: BasicBlock,
    val falseTarget: BasicBlock
) : Instruction(Opcode.BR_COND, voidType) {

    init {
        addOperand(condition)
    }

    val condition: Value get() = getOperand(0)

    override fun isTerminator(): Boolean = true
    override fun getOpcodeString(): String = "br "

    override fun toString(): String =
        "br ${condition.getNameOrTemporary()}, ${trueTarget.getNameOrTemporary()}, ${falseTarget.getNameOrTemporary()}"
}

// ==================== 条件分支指令（带比较操作） ====================

/**
 * 条件分支基类 - 带比较操作的条件分支
 */
abstract class CondBranchCmpInst(
    opcode: Opcode,
    left: Value,
    right: Value,
    val trueTarget: BasicBlock,
    val falseTarget: BasicBlock
) : Instruction(opcode, voidType) {

    init {
        addOperand(left)
        addOperand(right)
    }

    val left: Value get() = getOperand(0)
    val right: Value get() = getOperand(1)

    override fun isTerminator(): Boolean = true
    override fun getOpcodeString(): String = opcode.name.lowercase().replace("br_", "br_")
}

/**
 * 小于则跳转 (Branch if Less Than)
 */
class BranchLtInst(
    left: Value,
    right: Value,
    trueTarget: BasicBlock,
    falseTarget: BasicBlock
) : CondBranchCmpInst(Opcode.BR_LT, left, right, trueTarget, falseTarget) {

    override fun toString(): String =
        "br_lt ${left.getNameOrTemporary()}, ${right.getNameOrTemporary()}, ${trueTarget.getNameOrTemporary()}, ${falseTarget.getNameOrTemporary()}"
}

/**
 * 小于等于则跳转 (Branch if Less or Equal)
 */
class BranchLeInst(
    left: Value,
    right: Value,
    trueTarget: BasicBlock,
    falseTarget: BasicBlock
) : CondBranchCmpInst(Opcode.BR_LE, left, right, trueTarget, falseTarget) {

    override fun toString(): String =
        "br_le ${left.getNameOrTemporary()}, ${right.getNameOrTemporary()}, ${trueTarget.getNameOrTemporary()}, ${falseTarget.getNameOrTemporary()}"
}

/**
 * 大于则跳转 (Branch if Greater Than)
 */
class BranchGtInst(
    left: Value,
    right: Value,
    trueTarget: BasicBlock,
    falseTarget: BasicBlock
) : CondBranchCmpInst(Opcode.BR_GT, left, right, trueTarget, falseTarget) {

    override fun toString(): String =
        "br_gt ${left.getNameOrTemporary()}, ${right.getNameOrTemporary()}, ${trueTarget.getNameOrTemporary()}, ${falseTarget.getNameOrTemporary()}"
}

/**
 * 大于等于则跳转 (Branch if Greater or Equal)
 */
class BranchGeInst(
    left: Value,
    right: Value,
    trueTarget: BasicBlock,
    falseTarget: BasicBlock
) : CondBranchCmpInst(Opcode.BR_GE, left, right, trueTarget, falseTarget) {

    override fun toString(): String =
        "br_ge ${left.getNameOrTemporary()}, ${right.getNameOrTemporary()}, ${trueTarget.getNameOrTemporary()}, ${falseTarget.getNameOrTemporary()}"
}

/**
 * 等于则跳转 (Branch if Equal)
 */
class BranchEqInst(
    left: Value,
    right: Value,
    trueTarget: BasicBlock,
    falseTarget: BasicBlock
) : CondBranchCmpInst(Opcode.BR_EQ, left, right, trueTarget, falseTarget) {

    override fun toString(): String =
        "br_eq ${left.getNameOrTemporary()}, ${right.getNameOrTemporary()}, ${trueTarget.getNameOrTemporary()}, ${falseTarget.getNameOrTemporary()}"
}

/**
 * 不等于则跳转 (Branch if Not Equal)
 */
class BranchNeInst(
    left: Value,
    right: Value,
    trueTarget: BasicBlock,
    falseTarget: BasicBlock
) : CondBranchCmpInst(Opcode.BR_NE, left, right, trueTarget, falseTarget) {

    override fun toString(): String =
        "br_ne ${left.getNameOrTemporary()}, ${right.getNameOrTemporary()}, ${trueTarget.getNameOrTemporary()}, ${falseTarget.getNameOrTemporary()}"
}

/**
 * 不可达指令
 */
class UnreachableInst : Instruction(Opcode.UNREACHABLE, voidType) {
    
    override fun isTerminator(): Boolean = true
    override fun getOpcodeString(): String = "unreachable"
    
    override fun toString(): String = "unreachable"
}

// ==================== 二元运算指令 ====================

/**
 * 加法指令
 */
class AddInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.ADD, type, left, right)

/**
 * 减法指令
 */
class SubInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.SUB, type, left, right)

/**
 * 乘法指令
 */
class MulInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.MUL, type, left, right)

/**
 * 除法指令
 */
class DivInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.DIV, type, left, right) {
    override fun mayThrow(): Boolean = true  // 除以零
}

/**
 * 取模指令
 */
class ModInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.MOD, type, left, right) {
    override fun mayThrow(): Boolean = true  // 除以零
}

/**
 * 左移指令
 */
class ShlInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.SHL, type, left, right)

/**
 * 逻辑右移指令
 */
class ShrInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.SHR, type, left, right)

/**
 * 算术右移指令
 */
class AShrInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.ASHR, type, left, right)

/**
 * 按位与指令
 */
class AndInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.AND, type, left, right)

/**
 * 按位或指令
 */
class OrInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.OR, type, left, right)

/**
 * 按位异或指令
 */
class XorInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.XOR, type, left, right)

/**
 * 指数指令
 */
class ExpInst(
    left: Value,
    right: Value,
    type: Type = left.type
) : BinaryInstruction(Opcode.EXP, type, left, right)

// ==================== 比较指令 ====================

class EqInst(left: Value, right: Value) : CmpInstruction(Opcode.EQ, left, right)
class NeInst(left: Value, right: Value) : CmpInstruction(Opcode.NE, left, right)
class LtInst(left: Value, right: Value) : CmpInstruction(Opcode.LT, left, right)
class LeInst(left: Value, right: Value) : CmpInstruction(Opcode.LE, left, right)
class GtInst(left: Value, right: Value) : CmpInstruction(Opcode.GT, left, right)
class GeInst(left: Value, right: Value) : CmpInstruction(Opcode.GE, left, right)
class StrictEqInst(left: Value, right: Value) : CmpInstruction(Opcode.STRICT_EQ, left, right)
class StrictNeInst(left: Value, right: Value) : CmpInstruction(Opcode.STRICT_NE, left, right)

/**
 * in 操作符
 */
class IsInInst(
    property: Value,
    obj: Value
) : CmpInstruction(Opcode.ISIN, property, obj) {
    override fun mayThrow(): Boolean = true
}

/**
 * instanceof 操作符
 */
class InstanceOfInst(
    obj: Value,
    constructor: Value
) : CmpInstruction(Opcode.INSTANCEOF, obj, constructor) {
    override fun mayThrow(): Boolean = true
}

// ==================== 一元运算指令 ====================

/**
 * 取负
 */
class NegInst(
    operand: Value,
    type: Type = operand.type
) : UnaryInstruction(Opcode.NEG, type, operand)

/**
 * 逻辑非
 */
class NotInst(
    operand: Value
) : UnaryInstruction(Opcode.NOT, boolType, operand)

/**
 * 按位非
 */
class BitNotInst(
    operand: Value,
    type: Type = operand.type
) : UnaryInstruction(Opcode.BIT_NOT, type, operand)

/**
 * 自增
 */
class IncInst(
    operand: Value,
    type: Type = operand.type
) : UnaryInstruction(Opcode.INC, type, operand)

/**
 * 自减
 */
class DecInst(
    operand: Value,
    type: Type = operand.type
) : UnaryInstruction(Opcode.DEC, type, operand)

/**
 * typeof 操作符
 */
class TypeOfInst(
    operand: Value
) : UnaryInstruction(Opcode.TYPEOF, stringType, operand)

/**
 * 转数字
 */
class ToNumberInst(
    operand: Value
) : UnaryInstruction(Opcode.TO_NUMBER, f64Type, operand)

/**
 * 转数值
 */
class ToNumericInst(
    operand: Value
) : UnaryInstruction(Opcode.TO_NUMERIC, f64Type, operand)

/**
 * 判断是否为真
 */
class IsTrueInst(
    operand: Value
) : UnaryInstruction(Opcode.IS_TRUE, boolType, operand)

/**
 * 判断是否为假
 */
class IsFalseInst(
    operand: Value
) : UnaryInstruction(Opcode.IS_FALSE, boolType, operand)

// ==================== 内存操作指令 ====================

/**
 * 栈上分配
 */
class AllocaInst(
    val allocatedType: Type,
    name: String = ""
) : Instruction(Opcode.ALLOCA, Type.PointerType(allocatedType), name) {
    override fun getOpcodeString(): String = "alloca $allocatedType"
}

/**
 * 加载指令
 */
class LoadInst(
    pointer: Value,
    name: String = ""
) : Instruction(
    Opcode.LOAD,
    (pointer.type as? Type.PointerType)?.pointeeType ?: anyType,
    name
) {
    init {
        addOperand(pointer)
    }
    
    val pointer: Value get() = getOperand(0)
    
    override fun mayThrow(): Boolean = true  // 可能访问无效内存
    override fun getOpcodeString(): String = "load "
    
    override fun toString(): String = "${getNameOrTemporary()} = load ${pointer.getNameOrTemporary()}"
}

/**
 * 存储指令
 */
class StoreInst(
    value: Value,
    pointer: Value
) : Instruction(Opcode.STORE, voidType) {
    init {
        addOperand(value)
        addOperand(pointer)
    }
    
    val value: Value get() = getOperand(0)
    val pointer: Value get() = getOperand(1)
    
    override fun mayHaveSideEffects(): Boolean = true
    override fun mayThrow(): Boolean = true
    override fun getOpcodeString(): String = "store "
    
    override fun toString(): String = 
        "store ${value.getNameOrTemporary()}, ${pointer.getNameOrTemporary()}"
}

// ==================== PHI 节点 ====================

/**
 * PHI节点 - SSA关键指令，用于合并来自不同前驱的值
 */
class PhiInst(
    type: Type,
    name: String = ""
) : Instruction(Opcode.PHI, type, name) {
    
    /**
     * 添加一个incoming value
     */
    fun addIncoming(value: Value, block: BasicBlock) {
        addOperand(value)
        incomingBlocks.add(block)
    }
    
    private val incomingBlocks = mutableListOf<BasicBlock>()
    
    /**
     * 获取所有incoming块
     */
    fun getIncomingBlocks(): List<BasicBlock> = incomingBlocks.toList()
    
    /**
     * 获取指定位置的incoming block
     */
    fun getIncomingBlock(index: Int): BasicBlock = incomingBlocks[index]
    
    /**
     * 获取指定incoming块的值
     */
    fun getValueForBlock(block: BasicBlock): Value? {
        val index = incomingBlocks.indexOf(block)
        return if (index >= 0) getOperand(index) else null
    }
    
    /**
     * 设置指定incoming块的值
     */
    fun setValueForBlock(block: BasicBlock, value: Value) {
        val index = incomingBlocks.indexOf(block)
        if (index >= 0) {
            setOperand(index, value)
        }
    }
    
    /**
     * incoming value数量
     */
    fun incomingCount(): Int = incomingBlocks.size
    
    override fun isPure(): Boolean = true  // PHI节点是纯的
    override fun getOpcodeString(): String = "phi "
    
    override fun toString(): String {
        val pairs = (0 until incomingCount()).joinToString(", ") { i ->
            "[ ${getOperand(i).getNameOrTemporary()}, ${incomingBlocks[i].getNameOrTemporary()} ]"
        }
        return "${getNameOrTemporary()} = phi $type $pairs"
    }
}

// ==================== 选择指令 ====================

/**
 * 选择指令（三元运算符）
 */
class SelectInst(
    condition: Value,
    trueValue: Value,
    falseValue: Value,
    name: String = ""
) : Instruction(Opcode.SELECT, trueValue.type, name) {
    init {
        addOperand(condition)
        addOperand(trueValue)
        addOperand(falseValue)
    }
    
    val condition: Value get() = getOperand(0)
    val trueValue: Value get() = getOperand(1)
    val falseValue: Value get() = getOperand(2)
    
    override fun isPure(): Boolean = true
    override fun getOpcodeString(): String = "select "
    
    override fun toString(): String =
        "${getNameOrTemporary()} = select ${condition.getNameOrTemporary()}, " +
        "${trueValue.getNameOrTemporary()}, ${falseValue.getNameOrTemporary()}"
}

// ==================== 调用指令 ====================

/**
 * 直接调用指令
 */
class CallInst(
    callee: Value,
    args: List<Value>,
    name: String = ""
) : CallInstructionBase(Opcode.CALL, anyType, callee, name) {
    init {
        args.forEach { addOperand(it) }
    }
    
    override fun getOpcodeString(): String = "call "
    
    override fun toString(): String {
        val argsStr = getArgs().joinToString(", ") { it.getNameOrTemporary() }
        return "${getNameOrTemporary()} = call ${callee.getNameOrTemporary()}($argsStr)"
    }
}

/**
 * 带this的调用
 */
class CallThisInst(
    callee: Value,
    thisValue: Value,
    args: List<Value>,
    name: String = ""
) : CallInstructionBase(Opcode.CALL_THIS, anyType, callee, name) {
    init {
        addOperand(thisValue)
        args.forEach { addOperand(it) }
    }
    
    val thisValue: Value get() = getOperand(1)
    
    override fun getArgs(): List<Value> = (2 until operandCount()).map { getOperand(it) }
    
    override fun getOpcodeString(): String = "call_this "
    
    override fun toString(): String {
        val argsStr = getArgs().joinToString(", ") { it.getNameOrTemporary() }
        return "${getNameOrTemporary()} = call_this ${callee.getNameOrTemporary()}, " +
               "this=${thisValue.getNameOrTemporary()}($argsStr)"
    }
}

/**
 * new 操作符
 */
class NewInst(
    constructor: Value,
    args: List<Value>,
    name: String = ""
) : CallInstructionBase(Opcode.NEW, objectType, constructor, name) {
    init {
        args.forEach { addOperand(it) }
    }
    
    override fun getOpcodeString(): String = "new "
    
    override fun toString(): String {
        val argsStr = getArgs().joinToString(", ") { it.getNameOrTemporary() }
        return "${getNameOrTemporary()} = new ${callee.getNameOrTemporary()}($argsStr)"
    }
}

/**
 * 运行时调用
 */
class CallRuntimeInst(
    val runtimeFunc: String,
    args: List<Value>,
    name: String = ""
) : Instruction(Opcode.CALL_RUNTIME, anyType, name) {
    init {
        args.forEach { addOperand(it) }
    }
    
    fun getArgs(): List<Value> = getOperands()
    
    override fun mayHaveSideEffects(): Boolean = true
    override fun mayThrow(): Boolean = true
    override fun getOpcodeString(): String = "call_runtime $runtimeFunc"
    
    override fun toString(): String {
        val argsStr = getArgs().joinToString(", ") { it.getNameOrTemporary() }
        return "${getNameOrTemporary()} = call_runtime $runtimeFunc($argsStr)"
    }
}

// ==================== 对象操作指令 ====================

/**
 * 创建空对象
 */
class CreateEmptyObjectInst(name: String = "") : 
    Instruction(Opcode.CREATE_OBJECT, objectType, name) {
    override fun getOpcodeString(): String = "create_empty_object"
    override fun toString(): String = "${getNameOrTemporary()} = create_empty_object"
}

/**
 * 创建空数组
 */
class CreateEmptyArrayInst(
    val initialCapacity: Int = 0,
    name: String = ""
) : Instruction(Opcode.CREATE_ARRAY, Type.ArrayType(anyType), name) {
    override fun getOpcodeString(): String = "create_empty_array"
    override fun toString(): String = 
        "${getNameOrTemporary()} = create_empty_array(cap=$initialCapacity)"
}

/**
 * 获取属性
 */
class GetPropertyInst(
    obj: Value,
    key: Value,
    name: String = ""
) : Instruction(Opcode.GET_PROPERTY, anyType, name) {
    init {
        addOperand(obj)
        addOperand(key)
    }
    
    val obj: Value get() = getOperand(0)
    val key: Value get() = getOperand(1)
    
    override fun mayThrow(): Boolean = true
    override fun getOpcodeString(): String = "get_property "
    
    override fun toString(): String = 
        "${getNameOrTemporary()} = get_property ${obj.getNameOrTemporary()}[${key.getNameOrTemporary()}]"
}

/**
 * 设置属性
 */
class SetPropertyInst(
    obj: Value,
    key: Value,
    value: Value
) : Instruction(Opcode.SET_PROPERTY, voidType) {
    init {
        addOperand(obj)
        addOperand(key)
        addOperand(value)
    }
    
    val obj: Value get() = getOperand(0)
    val key: Value get() = getOperand(1)
    val value: Value get() = getOperand(2)
    
    override fun mayHaveSideEffects(): Boolean = true
    override fun mayThrow(): Boolean = true
    override fun getOpcodeString(): String = "set_property "
    
    override fun toString(): String = 
        "set_property ${obj.getNameOrTemporary()}[${key.getNameOrTemporary()}] = ${value.getNameOrTemporary()}"
}

/**
 * 获取元素（数组索引访问）
 */
class GetElementInst(
    array: Value,
    index: Value,
    name: String = ""
) : Instruction(Opcode.GET_ELEMENT, anyType, name) {
    init {
        addOperand(array)
        addOperand(index)
    }
    
    val array: Value get() = getOperand(0)
    val index: Value get() = getOperand(1)
    
    override fun mayThrow(): Boolean = true
    override fun getOpcodeString(): String = "get_element "
    
    override fun toString(): String = 
        "${getNameOrTemporary()} = get_element ${array.getNameOrTemporary()}[${index.getNameOrTemporary()}]"
}

/**
 * 设置元素
 */
class SetElementInst(
    array: Value,
    index: Value,
    value: Value
) : Instruction(Opcode.SET_ELEMENT, voidType) {
    init {
        addOperand(array)
        addOperand(index)
        addOperand(value)
    }
    
    val array: Value get() = getOperand(0)
    val index: Value get() = getOperand(1)
    val value: Value get() = getOperand(2)
    
    override fun mayHaveSideEffects(): Boolean = true
    override fun mayThrow(): Boolean = true
    override fun getOpcodeString(): String = "set_element "
    
    override fun toString(): String = 
        "set_element ${array.getNameOrTemporary()}[${index.getNameOrTemporary()}] = ${value.getNameOrTemporary()}"
}

// ==================== 异常处理 ====================

/**
 * 抛出异常
 */
class ThrowInst(
    exception: Value
) : Instruction(Opcode.THROW, voidType) {
    init {
        addOperand(exception)
    }
    
    val exception: Value get() = getOperand(0)
    
    override fun isTerminator(): Boolean = true
    override fun mayThrow(): Boolean = true
    override fun getOpcodeString(): String = "throw "
    
    override fun toString(): String = "throw ${exception.getNameOrTemporary()}"
}

// ==================== 类型转换 ====================

class TruncInst(operand: Value, type: Type, name: String = "") : 
    UnaryInstruction(Opcode.TRUNC, type, operand) {
    override fun getOpcodeString(): String = "trunc to $type "
}

class ZExtInst(operand: Value, type: Type, name: String = "") : 
    UnaryInstruction(Opcode.ZEXT, type, operand) {
    override fun getOpcodeString(): String = "zext to $type "
}

class SExtInst(operand: Value, type: Type, name: String = "") : 
    UnaryInstruction(Opcode.SEXT, type, operand) {
    override fun getOpcodeString(): String = "sext to $type "
}

class FPToIInst(operand: Value, type: Type, name: String = "") : 
    UnaryInstruction(Opcode.FPTOI, type, operand) {
    override fun getOpcodeString(): String = "fptoi to $type "
}

class UIToFPInst(operand: Value, type: Type, name: String = "") : 
    UnaryInstruction(Opcode.UITOF, type, operand) {
    override fun getOpcodeString(): String = "uitofp to $type "
}

class SIToFPInst(operand: Value, type: Type, name: String = "") : 
    UnaryInstruction(Opcode.SITOF, type, operand) {
    override fun getOpcodeString(): String = "sitofp to $type "
}

class BitCastInst(operand: Value, type: Type, name: String = "") : 
    UnaryInstruction(Opcode.BITCAST, type, operand) {
    override fun getOpcodeString(): String = "bitcast to $type "
}

// ==================== 空操作 ====================

class NopInst : Instruction(Opcode.NOP, voidType) {
    override fun getOpcodeString(): String = "nop"
    override fun toString(): String = "nop"
}

// ==================== SSA构造辅助指令 ====================

/**
 * 复制指令 - 用于显式表示虚拟寄存器的SSA赋值
 * 
 * 在PandaVM到SSA IR的转换中，虚拟寄存器的每次存储（STA）
 * 对应一个COPY指令，使得寄存器值在IR中有明确定义。
 */
class CopyInst(
    operand: Value,
    type: Type = operand.type,
    name: String = ""
) : Instruction(Opcode.COPY, type, name) {
    
    init {
        addOperand(operand)
    }
    
    val source: Value get() = getOperand(0)
    
    override fun isPure(): Boolean = true
    override fun getOpcodeString(): String = "copy "
    
    override fun toString(): String = 
        "${getNameOrTemporary()} = copy ${source.getNameOrTemporary()}"
}
