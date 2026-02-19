package com.orz.reark.core.ssa.transform

import com.orz.reark.core.ssa.ir.*
import com.orz.reark.core.ssa.pass.*
import com.orz.reark.core.ssa.ir.Function as SSAFunction

/**
 * 常量折叠 (Constant Folding)
 * 
 * 在编译时计算常量表达式
 */
class ConstantFolding : FunctionPass {
    
    override val name: String = "constfold"
    override val description: String = "Constant Folding"
    
    override fun run(function: SSAFunction): PassResult {
        var modified = false
        
        function.instructions().toList().forEach { inst ->
            val result = foldConstant(inst)
            if (result != null) {
                inst.replaceAllUsesWith(result)
                inst.eraseFromBlock()
                modified = true
            }
        }
        
        return PassResult.Success(modified)
    }
    
    /**
     * 尝试折叠指令的常量
     */
    private fun foldConstant(inst: Instruction): Constant? {
        return when (inst) {
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

/**
 * 常量传播 (Constant Propagation)
 * 
 * 将常量值传播到使用点
 */
class ConstantPropagation : FunctionPass {
    
    override val name: String = "constprop"
    override val description: String = "Constant Propagation"
    
    override fun run(function: SSAFunction): PassResult {
        var modified = false
        
        // 简化的常量传播：遍历所有store和直接赋值
        // 实际实现需要数据流分析
        
        function.instructions().toList().forEach { inst ->
            if (inst is StoreInst) {
                // 如果存储的是常量，可以记录这个常量值
                // 简化版：直接检查是否可以简化
            }
        }
        
        return PassResult.Success(modified)
    }
}

/**
 * 代数简化 (Algebraic Simplification)
 * 
 * 应用代数恒等式简化表达式
 */
class AlgebraicSimplification : FunctionPass {
    
    override val name: String = "simplify"
    override val description: String = "Algebraic Simplification"
    
    override fun run(function: SSAFunction): PassResult {
        var modified = false
        
        function.instructions().toList().forEach { inst ->
            when (inst) {
                is AddInst -> {
                    // x + 0 = x
                    val right = inst.right
                    val left = inst.left
                    if (right is ConstantInt && right.isZero()) {
                        inst.replaceAllUsesWith(left)
                        inst.eraseFromBlock()
                        modified = true
                    } else if (left is ConstantInt && left.isZero()) {
                        inst.replaceAllUsesWith(right)
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
                is MulInst -> {
                    // x * 0 = 0
                    val left = inst.left
                    val right = inst.right
                    if ((left is ConstantInt && left.isZero()) ||
                        (right is ConstantInt && right.isZero())) {
                        inst.replaceAllUsesWith(ConstantInt.i64(0))
                        inst.eraseFromBlock()
                        modified = true
                    }
                    // x * 1 = x
                    else if (right is ConstantInt && right.isOne()) {
                        inst.replaceAllUsesWith(left)
                        inst.eraseFromBlock()
                        modified = true
                    } else if (left is ConstantInt && left.isOne()) {
                        inst.replaceAllUsesWith(right)
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
                is SubInst -> {
                    // x - 0 = x
                    val right = inst.right
                    val left = inst.left
                    if (right is ConstantInt && right.isZero()) {
                        inst.replaceAllUsesWith(left)
                        inst.eraseFromBlock()
                        modified = true
                    }
                    // x - x = 0
                    if (left == right) {
                        inst.replaceAllUsesWith(ConstantInt.i64(0))
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
                is DivInst -> {
                    // x / 1 = x
                    val right = inst.right
                    val left = inst.left
                    if (right is ConstantInt && right.isOne()) {
                        inst.replaceAllUsesWith(left)
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
                is AndInst -> {
                    // x & 0 = 0
                    val left = inst.left
                    val right = inst.right
                    if ((left is ConstantInt && left.isZero()) ||
                        (right is ConstantInt && right.isZero())) {
                        inst.replaceAllUsesWith(ConstantInt.i64(0))
                        inst.eraseFromBlock()
                        modified = true
                    }
                    // x & -1 = x
                    if (right is ConstantInt && right.value == -1L) {
                        inst.replaceAllUsesWith(left)
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
                is OrInst -> {
                    // x | 0 = x
                    val left = inst.left
                    val right = inst.right
                    if (right is ConstantInt && right.isZero()) {
                        inst.replaceAllUsesWith(left)
                        inst.eraseFromBlock()
                        modified = true
                    }
                    // x | -1 = -1
                    if (right is ConstantInt && right.value == -1L) {
                        inst.replaceAllUsesWith(ConstantInt.i64(-1))
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
            }
        }
        
        return PassResult.Success(modified)
    }
}
