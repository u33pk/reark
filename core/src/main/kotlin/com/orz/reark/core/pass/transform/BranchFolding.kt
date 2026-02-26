package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 分支折叠优化 (Branch Folding / Canonicalization)
 *
 * 将比较指令和条件分支合并为单一的条件分支指令
 *
 * 优化模式：
 * 1. LT + EQ zero + br => br_ge (如果小于为 false，则大于等于)
 * 2. LT + br => br_lt
 * 3. GT + EQ zero + br => br_le
 * 4. 等等...
 *
 * 示例：
 * ```
 * // 优化前
 * %18 = LT reg_7_phi, v2
 * zero = copy 0
 * %21 = EQ %18, zero
 * br %21, bb_68, bb_28
 *
 * // 优化后
 * br_ge reg_7_phi, v2, bb_68, bb_28
 * ```
 */
class BranchFolding : FunctionPass {

    override val name: String = "branchfold"
    override val description: String = "Branch Folding"

    override fun run(function: SSAFunction): PassResult {
        var modified = false

        // 遍历所有基本块
        function.blocks().forEach { block ->
            val terminator = block.terminator()
            if (terminator is CondBranchInst) {
                val result = foldBranch(terminator, block)
                if (result != null) {
                    // 替换条件分支
                    val newBranch = result.first
                    val deadInsts = result.second

                    // 在 terminator 之前插入新的分支指令
                    terminator.insertBefore(newBranch)

                    // 删除原来的条件分支
                    terminator.eraseFromBlock()

                    // 删除死亡的指令
                    deadInsts.forEach { it.eraseFromBlock() }

                    modified = true
                }
            }
        }

        return PassResult.Success(modified)
    }

    /**
     * 尝试折叠条件分支
     * @return Pair(新的分支指令，需要删除的指令列表) 或 null
     */
    private fun foldBranch(branch: CondBranchInst, block: BasicBlock): Pair<Instruction, List<Instruction>>? {
        val condition = branch.condition
        val trueTarget = branch.trueTarget
        val falseTarget = branch.falseTarget

        // 模式 1: EQ (CMP zero) + br => br_xx
        // EQ cmpResult, zero 表示 !(cmpResult)，即取反
        if (condition is CmpInstruction && condition.opcode == Opcode.EQ) {
            val eqLeft = condition.left
            val eqRight = condition.right

            // 检查是否是 EQ cmpResult, zero 模式
            val isEqZero = isZero(eqRight) || isZero(eqLeft)
            val cmpResult = if (isEqZero) {
                if (isZero(eqRight)) eqLeft else eqRight
            } else {
                null
            }

            if (cmpResult != null) {
                // 查找 cmpResult 的定义
                val cmpInst = cmpResult as? CmpInstruction
                if (cmpInst != null) {
                    // isEqZero = true 表示 EQ x, 0，即 !(x != 0)，需要取反
                    // 所以 isPositive = !isEqZero
                    val result = foldCmpBranch(cmpInst, trueTarget, falseTarget, isPositive = !isEqZero)
                    if (result != null) {
                        // 添加 EQ 指令到删除列表
                        val newBranch = result.first
                        val deadInsts = result.second.toMutableList()
                        deadInsts.add(condition)  // 添加 EQ 指令
                        return newBranch to deadInsts.distinct()
                    }
                }
            }
        }

        // 模式 2: 直接使用比较结果 + br => br_xx
        if (condition is CmpInstruction) {
            return foldCmpBranch(condition, trueTarget, falseTarget, isPositive = true)
        }

        return null
    }

    /**
     * 根据比较指令折叠分支
     * @param cmpInst 比较指令
     * @param trueTarget 条件为真时的目标块
     * @param falseTarget 条件为假时的目标块
     * @param isPositive 是否是正向条件（EQ 为 true，EQ zero 为 false）
     */
    private fun foldCmpBranch(
        cmpInst: CmpInstruction,
        trueTarget: BasicBlock,
        falseTarget: BasicBlock,
        isPositive: Boolean
    ): Pair<Instruction, List<Instruction>>? {
        val left = cmpInst.left
        val right = cmpInst.right
        val cmpOpcode = cmpInst.opcode

        // 确定要生成的分支指令
        val newBranch = when {
            // LT 比较
            cmpOpcode == Opcode.LT -> {
                if (isPositive) {
                    BranchLtInst(left, right, trueTarget, falseTarget)
                } else {
                    // !LT => GE
                    BranchGeInst(left, right, trueTarget, falseTarget)
                }
            }
            // LE 比较
            cmpOpcode == Opcode.LE -> {
                if (isPositive) {
                    BranchLeInst(left, right, trueTarget, falseTarget)
                } else {
                    // !LE => GT
                    BranchGtInst(left, right, trueTarget, falseTarget)
                }
            }
            // GT 比较
            cmpOpcode == Opcode.GT -> {
                if (isPositive) {
                    BranchGtInst(left, right, trueTarget, falseTarget)
                } else {
                    // !GT => LE
                    BranchLeInst(left, right, trueTarget, falseTarget)
                }
            }
            // GE 比较
            cmpOpcode == Opcode.GE -> {
                if (isPositive) {
                    BranchGeInst(left, right, trueTarget, falseTarget)
                } else {
                    // !GE => LT
                    BranchLtInst(left, right, trueTarget, falseTarget)
                }
            }
            // EQ 比较
            cmpOpcode == Opcode.EQ -> {
                if (isPositive) {
                    BranchEqInst(left, right, trueTarget, falseTarget)
                } else {
                    // !EQ => NE
                    BranchNeInst(left, right, trueTarget, falseTarget)
                }
            }
            // NE 比较
            cmpOpcode == Opcode.NE -> {
                if (isPositive) {
                    BranchNeInst(left, right, trueTarget, falseTarget)
                } else {
                    // !NE => EQ
                    BranchEqInst(left, right, trueTarget, falseTarget)
                }
            }
            else -> return null
        }

        // 收集需要删除的指令（比较指令和可能的 zero copy）
        val deadInsts = mutableListOf<Instruction>()
        deadInsts.add(cmpInst)

        // 查找并添加 zero copy 指令（如果存在）
        cmpInst.users().forEach { user ->
            if (user is CmpInstruction && user.opcode == Opcode.EQ) {
                val otherOperand = if (user.left == cmpInst) user.right else user.left
                if (isZero(otherOperand)) {
                    // 查找 zero 的定义
                    if (otherOperand is CopyInst) {
                        val src = otherOperand.source
                        if (src is ConstantInt && src.value == 0L) {
                            deadInsts.add(otherOperand)
                        }
                    }
                }
            }
        }

        return newBranch to deadInsts.distinct()
    }

    /**
     * 检查值是否是零
     */
    private fun isZero(value: Value): Boolean {
        return when (value) {
            is ConstantInt -> value.value == 0L
            is CopyInst -> {
                val source = value.source
                source is ConstantInt && source.value == 0L
            }
            else -> false
        }
    }
}

/**
 * 在指令之前插入新指令
 */
private fun Instruction.insertBefore(newInst: Instruction) {
    val block = this.parent ?: return
    block.insertBefore(newInst, this)
}
