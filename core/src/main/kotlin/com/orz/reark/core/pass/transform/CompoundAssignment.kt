package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 复合赋值优化 (Compound Assignment)
 *
 * 将原子算术指令转换为符合 JS 习惯的复合赋值形式
 *
 * 优化场景：
 * 1. `i = ADD 2, i` => `i += 2`
 * 2. `i = INC i` => `i++`
 *
 * 示例：
 * ```
 * // 优化前
 * bb_35:
 *   i = ADD 2, i
 *
 * // 优化后
 * bb_35:
 *   i += 2
 * ```
 */
class CompoundAssignment : FunctionPass {

    override val name: String = "compoundassign"
    override val description: String = "Compound Assignment"

    override fun run(function: SSAFunction): PassResult {
        var modified = false

        function.blocks().forEach { block ->
            block.instructions().toList().forEach { inst ->
                when (inst) {
                    // 检测 i = ADD const, i 模式 => i += const
                    is AddInst -> {
                        if (isCompoundAddAssign(inst)) {
                            inst.isCompoundAssign = true
                            modified = true
                        }
                    }

                    // 检测 i = INC i 模式 => i++
                    is IncInst -> {
                        if (isSelfIncrement(inst)) {
                            inst.isCompoundAssign = true
                            modified = true
                        }
                    }
                }
            }
        }

        return PassResult.Success(modified)
    }

    /**
     * 检查是否是复合加法赋值模式：i = ADD const, i
     */
    private fun isCompoundAddAssign(inst: AddInst): Boolean {
        val left = inst.left
        val right = inst.right
        val resultName = inst.name

        // 检查是否是 result = ADD const, var 且 result 和 var 同名
        val hasConst = left is ConstantInt || right is ConstantInt
        val hasVar = left is Argument || right is Argument || left is PhiInst || right is PhiInst

        if (hasConst && hasVar) {
            // 获取变量操作数的名字
            val varName = when {
                left is Argument || left is PhiInst -> left.name
                right is Argument || right is PhiInst -> right.name
                else -> null
            }
            // 检查结果名是否与变量名相同
            if (varName != null && resultName == varName) {
                // 确保操作数顺序正确：第一个是变量，第二个是常量
                // 如果当前顺序不对，交换操作数
                if (left is ConstantInt && (right is Argument || right is PhiInst)) {
                    // 需要交换：将变量放到第一个位置
                    inst.setOperand(0, right)
                    inst.setOperand(1, left)
                }
                return true
            }
        }
        return false
    }

    /**
     * 检查是否是自增模式：i = INC i
     */
    private fun isSelfIncrement(inst: IncInst): Boolean {
        val operandName = inst.operand.name
        val resultName = inst.name
        return operandName == resultName
    }
}
