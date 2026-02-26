package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.BasicBlock
import com.orz.reark.core.ir.BranchInst
import com.orz.reark.core.ir.CondBranchInst
import com.orz.reark.core.ir.ConstantInt
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 控制流图简化 (Simplify CFG)
 *
 * 合并基本块、移除不可达代码等
 */
class SimplifyCFG : FunctionPass {

    override val name: String = "simplifycfg"
    override val description: String = "Simplify Control Flow Graph"

    override fun run(function: SSAFunction): PassResult {
        var modified = false
        var changed: Boolean

        do {
            changed = false

            // 注意：不可达块移除由 AggressiveDeadCodeElimination 处理
            // 这里不再重复处理，避免错误地移除可达块

            // 1. 合并块（只合并无条件分支的块）
            if (mergeBlocks(function)) {
                changed = true
                modified = true
            }

            // 2. 简化分支
            if (simplifyBranches(function)) {
                changed = true
                modified = true
            }

            // 3. 消除空块（只有无条件跳转的块，且前驱没有条件分支）
            if (eliminateEmptyBlocks(function)) {
                changed = true
                modified = true
            }

        } while (changed)

        return PassResult.Success(modified)
    }

    /**
     * 移除不可达基本块
     */
    private fun removeUnreachableBlocks(function: SSAFunction): Boolean {
        val reachable = mutableSetOf<BasicBlock>()
        val worklist = ArrayDeque<BasicBlock>()

        worklist.add(function.entryBlock)
        while (worklist.isNotEmpty()) {
            val block = worklist.removeFirst()
            if (reachable.add(block)) {
                block.successors().forEach { worklist.add(it) }
            }
        }

        val unreachable = function.blocks().filter { it !in reachable }
        unreachable.forEach { function.removeBlock(it) }

        return unreachable.isNotEmpty()
    }

    /**
     * 合并基本块
     *
     * 只合并单前驱的情况，且只有当前块有无条件分支时才合并。
     * 如果当前块有条件分支，合并会丢失分支逻辑。
     */
    private fun mergeBlocks(function: SSAFunction): Boolean {
        var modified = false

        for (block in function.blocks().toList()) {
            // 检查块是否只有一个后继
            val succs = block.successors()
            if (succs.size != 1) continue

            val succ = succs[0]

            // 检查后继是否是入口块
            if (succ == function.entryBlock) continue

            // 额外检查：确保 succ 仍然属于函数
            if (succ.parent != function) continue

            // 只处理单前驱情况
            if (succ.predecessorCount() != 1) continue

            // 关键修复：只有当当前块的终止指令是无条件分支时才能合并
            // 如果当前块有条件分支，合并会丢失分支逻辑
            val terminator = block.terminator()
            if (terminator !is BranchInst) continue

            // 可以合并
            // 1. 移除当前块的终止指令（br 等）
            block.remove(terminator)
            terminator.dropOperands()

            // 2. 移动后继的所有指令到当前块
            succ.instructions().toList().forEach { inst ->
                succ.remove(inst)
                block.append(inst)
            }

            // 3. 转移后继的后继到当前块
            for (next in succ.successors().toList()) {
                // 先移除 succ 到 next 的边
                succ.removeSuccessor(next)
                // 然后添加 block 到 next 的边
                block.addSuccessor(next)
            }

            // 4. 移除后继
            function.removeBlock(succ)

            modified = true
            break // 重新开始（迭代器可能已失效）
        }

        return modified
    }

    /**
     * 简化分支
     */
    private fun simplifyBranches(function: SSAFunction): Boolean {
        var modified = false

        for (block in function.blocks()) {
            when (val terminator = block.terminator()) {
                is CondBranchInst -> {
                    val cond = terminator.condition

                    // 1. 条件为常量的分支
                    if (cond is ConstantInt) {
                        val target = if (cond.value != 0L) {
                            terminator.trueTarget
                        } else {
                            terminator.falseTarget
                        }

                        // 替换为无条件分支
                        block.remove(terminator)
                        block.addSuccessor(target)
                        block.append(BranchInst(target))

                        // 清理旧的后继关系
                        val otherTarget = if (cond.value != 0L) {
                            terminator.falseTarget
                        } else {
                            terminator.trueTarget
                        }
                        block.removeSuccessor(otherTarget)

                        modified = true
                    }

                    // 2. 两个目标相同
                    else if (terminator.trueTarget == terminator.falseTarget) {
                        val target = terminator.trueTarget

                        block.remove(terminator)
                        block.addSuccessor(target)
                        block.append(BranchInst(target))

                        modified = true
                    }
                }
                else -> {}
            }
        }

        return modified
    }

    /**
     * 消除空块（只有无条件跳转的块）
     *
     * 关键修复：如果块被条件分支引用，不能消除该块，
     * 否则会丢失控制流路径。
     */
    private fun eliminateEmptyBlocks(function: SSAFunction): Boolean {
        var modified = false

        for (block in function.blocks().toList()) {
            // 跳过入口块和包含 PHI 的块
            if (block == function.entryBlock || block.phis().isNotEmpty()) continue

            // 检查是否只有一条无条件分支
            val instructions = block.instructions().toList()
            if (instructions.size != 1) continue

            val branch = instructions[0] as? BranchInst ?: continue
            val target = branch.target

            // 避免无限循环
            if (target == block) continue

            // 关键修复：检查是否有前驱使用条件分支指向此块
            // 如果有，不能消除，因为需要保留条件分支的两条路径
            val hasCondBranchPred = block.predecessors().any { pred ->
                pred.terminator() is CondBranchInst
            }
            if (hasCondBranchPred) continue

            // 更新所有前驱
            val preds = block.predecessors().toList()
            for (pred in preds) {
                // 更新 PHI 节点
                for (phi in target.phis()) {
                    val value = phi.getValueForBlock(block)
                    if (value != null) {
                        phi.addIncoming(value, pred)
                    }
                }

                // 更新跳转目标
                pred.replaceSuccessor(block, target)

                // 更新终止指令
                when (val term = pred.terminator()) {
                    is BranchInst -> {
                        pred.remove(term)
                        pred.append(BranchInst(target))
                    }
                    is CondBranchInst -> {
                        // 创建新的条件分支指令，替换目标块
                        val newTrueTarget = if (term.trueTarget == block) target else term.trueTarget
                        val newFalseTarget = if (term.falseTarget == block) target else term.falseTarget
                        pred.remove(term)
                        pred.append(CondBranchInst(term.condition, newTrueTarget, newFalseTarget))
                    }
                }
            }

            function.removeBlock(block)
            modified = true
            break // 重新开始
        }

        return modified
    }
}