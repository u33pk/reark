package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 常量折叠 (Constant Folding)
 * 
 * 在编译时计算常量表达式
 * 
 * 增强功能：
 * 1. 处理 PHI 节点 - 如果 PHI 的所有输入都是相同常量，则替换为该常量
 * 2. 迭代折叠 - 重复运行直到没有更多常量可以折叠
 */
class ConstantFolding : FunctionPass {
    
    override val name: String = "constfold"
    override val description: String = "Constant Folding"
    
    override fun run(function: SSAFunction): PassResult {
        var modified = false
        var changed = true

        // 迭代运行，直到没有更多变化
        // 这样可以处理链式常量传播
        while (changed) {
            changed = false

            // 处理所有指令的常量折叠
            function.instructions().toList().forEach { inst ->
                val result = foldConstant(inst)
                if (result != null) {
                    inst.replaceAllUsesWith(result)
                    inst.eraseFromBlock()
                    modified = true
                    changed = true
                }
            }
        }

        return PassResult.Success(modified)
    }

    /**
     * 折叠 PHI 节点
     * 如果 PHI 的所有输入都是相同的常量，则返回该常量
     * 注意：不处理变量输入，这是常量传播的工作
     */
    private fun foldPhi(phi: PhiInst): Constant? {
        if (phi.incomingCount() == 0) {
            return null
        }

        // 收集所有输入值
        val values = mutableListOf<Value>()
        for (i in 0 until phi.incomingCount()) {
            values.add(phi.getOperand(i))
        }

        // 检查所有值是否都是相同的常量
        val firstValue = values.first()
        if (firstValue !is Constant) {
            return null
        }

        if (values.all { it == firstValue }) {
            return firstValue
        }

        return null
    }
    
    /**
     * 尝试折叠指令的常量
     */
    private fun foldConstant(inst: Instruction): Constant? {
        return when (inst) {
            is PhiInst -> foldPhi(inst)
            is AddInst -> foldBinary(inst.left, inst.right) { a, b -> a + b }
            is SubInst -> foldBinary(inst.left, inst.right) { a, b -> a - b }
            is MulInst -> foldBinary(inst.left, inst.right) { a, b -> a * b }
            is DivInst -> foldBinary(inst.left, inst.right) { a, b ->
                if (b == 0.0) null else a / b
            }
            is ModInst -> foldBinary(inst.left, inst.right) { a, b ->
                if (b == 0.0) null else a % b
            }
            is EqInst -> foldComparison(inst.left, inst.right) { a, b -> a == b }
            is NeInst -> foldComparison(inst.left, inst.right) { a, b -> a != b }
            is LtInst -> foldComparison(inst.left, inst.right) { a, b -> a < b }
            is LeInst -> foldComparison(inst.left, inst.right) { a, b -> a <= b }
            is GtInst -> foldComparison(inst.left, inst.right) { a, b -> a > b }
            is GeInst -> foldComparison(inst.left, inst.right) { a, b -> a >= b }
            is AndInst -> foldBinaryInt(inst.left, inst.right) { a, b -> a and b }
            is OrInst -> foldBinaryInt(inst.left, inst.right) { a, b -> a or b }
            is XorInst -> foldBinaryInt(inst.left, inst.right) { a, b -> a xor b }
            is ShlInst -> foldBinaryInt(inst.left, inst.right) { a, b -> a shl b.toInt() }
            is ShrInst -> foldBinaryInt(inst.left, inst.right) { a, b -> a ushr b.toInt() }
            is AShrInst -> foldBinaryInt(inst.left, inst.right) { a, b -> a shr b.toInt() }
            is NegInst -> foldUnary(inst.operand) { -it }
            is NotInst -> foldUnaryBool(inst.operand) { !it }
            is BitNotInst -> foldUnaryInt(inst.operand) { it.inv() }
            is SelectInst -> foldSelect(inst.condition, inst.trueValue, inst.falseValue)
            else -> null
        }
    }
    
    /**
     * 二元操作（浮点）
     */
    private fun foldBinary(
        left: Value,
        right: Value,
        op: (Double, Double) -> Double?
    ): Constant? {
        val a = getConstantValue(left) ?: return null
        val b = getConstantValue(right) ?: return null
        val result = op(a, b) ?: return null
        return ConstantFP.f64(result)
    }
    
    /**
     * 二元操作（整数）
     */
    private fun foldBinaryInt(
        left: Value,
        right: Value,
        op: (Long, Long) -> Long
    ): ConstantInt? {
        val a = getConstantIntValue(left) ?: return null
        val b = getConstantIntValue(right) ?: return null
        return ConstantInt.i64(op(a, b))
    }
    
    /**
     * 比较操作
     */
    private fun foldComparison(
        left: Value,
        right: Value,
        op: (Double, Double) -> Boolean
    ): ConstantInt? {
        val a = getConstantValue(left) ?: return null
        val b = getConstantValue(right) ?: return null
        return ConstantInt.bool(op(a, b))
    }
    
    /**
     * 一元操作
     */
    private fun foldUnary(
        operand: Value,
        op: (Double) -> Double
    ): Constant? {
        val a = getConstantValue(operand) ?: return null
        return ConstantFP.f64(op(a))
    }
    
    /**
     * 一元布尔操作
     */
    private fun foldUnaryBool(
        operand: Value,
        op: (Boolean) -> Boolean
    ): ConstantInt? {
        val a = getConstantBoolValue(operand) ?: return null
        return ConstantInt.bool(op(a))
    }
    
    /**
     * 一元整数操作
     */
    private fun foldUnaryInt(
        operand: Value,
        op: (Long) -> Long
    ): ConstantInt? {
        val a = getConstantIntValue(operand) ?: return null
        return ConstantInt.i64(op(a))
    }
    
    /**
     * 选择操作
     */
    private fun foldSelect(
        condition: Value,
        trueValue: Value,
        falseValue: Value
    ): Constant? {
        val cond = getConstantBoolValue(condition) ?: return null
        return if (cond) {
            when (val v = trueValue) {
                is Constant -> v
                else -> null
            }
        } else {
            when (val v = falseValue) {
                is Constant -> v
                else -> null
            }
        }
    }
    
    /**
     * 获取常量数值
     */
    private fun getConstantValue(value: Value): Double? {
        return when (value) {
            is ConstantFP -> value.value
            is ConstantInt -> value.value.toDouble()
            else -> null
        }
    }
    
    /**
     * 获取常量整数值
     */
    private fun getConstantIntValue(value: Value): Long? {
        return when (value) {
            is ConstantInt -> value.value
            else -> null
        }
    }
    
    /**
     * 获取常量布尔值
     */
    private fun getConstantBoolValue(value: Value): Boolean? {
        return when (value) {
            is ConstantInt -> value.value != 0L
            else -> null
        }
    }
}