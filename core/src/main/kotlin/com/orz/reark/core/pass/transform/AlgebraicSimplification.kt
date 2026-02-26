package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

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
                is EqInst -> {
                    // x == x = true
                    if (inst.left == inst.right) {
                        inst.replaceAllUsesWith(ConstantInt.bool(true))
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
                is NeInst -> {
                    // x != x = false
                    if (inst.left == inst.right) {
                        inst.replaceAllUsesWith(ConstantInt.bool(false))
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
                is LtInst -> {
                    // x < x = false
                    if (inst.left == inst.right) {
                        inst.replaceAllUsesWith(ConstantInt.bool(false))
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
                is LeInst -> {
                    // x <= x = true
                    if (inst.left == inst.right) {
                        inst.replaceAllUsesWith(ConstantInt.bool(true))
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
                is GtInst -> {
                    // x > x = false
                    if (inst.left == inst.right) {
                        inst.replaceAllUsesWith(ConstantInt.bool(false))
                        inst.eraseFromBlock()
                        modified = true
                    }
                }
                is GeInst -> {
                    // x >= x = true
                    if (inst.left == inst.right) {
                        inst.replaceAllUsesWith(ConstantInt.bool(true))
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