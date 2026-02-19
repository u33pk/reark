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
            
            // 1. 移除不可达块
            if (removeUnreachableBlocks(function)) {
                changed = true
                modified = true
            }
            
            // 2. 合并块（如果一个块只有一个后继且该后继只有一个前驱）
            if (mergeBlocks(function)) {
                changed = true
                modified = true
            }
            
            // 3. 简化分支
            if (simplifyBranches(function)) {
                changed = true
                modified = true
            }
            
            // 4. 消除空块
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
     */
    private fun mergeBlocks(function: SSAFunction): Boolean {
        var modified = false
        
        for (block in function.blocks().toList()) {
            // 检查块是否只有一个后继
            val succs = block.successors()
            if (succs.size != 1) continue
            
            val succ = succs[0]
            // 检查后继是否只有一个前驱且不是入口块
            if (succ.predecessorCount() != 1 || succ == function.entryBlock) continue
            
            // 可以合并
            // 1. 移动后继的所有指令到当前块
            succ.instructions().toList().forEach { inst ->
                succ.remove(inst)
                block.append(inst)
            }
            
            // 2. 转移后继的后继到当前块
            succ.successors().toList().forEach { next ->
                succ.removeSuccessor(next)
                block.addSuccessor(next)
            }
            
            // 3. 移除后继
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
     * 消除空块（只有跳转的块）
     */
    private fun eliminateEmptyBlocks(function: SSAFunction): Boolean {
        var modified = false
        
        for (block in function.blocks().toList()) {
            // 跳过入口块和包含PHI的块
            if (block == function.entryBlock || block.phis().isNotEmpty()) continue
            
            // 检查是否只有一条无条件分支
            val instructions = block.instructions().toList()
            if (instructions.size != 1) continue
            
            val branch = instructions[0] as? BranchInst ?: continue
            val target = branch.target
            
            // 避免无限循环
            if (target == block) continue
            
            // 更新所有前驱
            val preds = block.predecessors().toList()
            for (pred in preds) {
                // 更新PHI节点
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
                        if (term.trueTarget == block) {
                            term.trueTarget.removePredecessor(pred)
                            target.addPredecessor(pred)
                            // 简化处理：实际应该替换指令
                        }
                        if (term.falseTarget == block) {
                            term.falseTarget.removePredecessor(pred)
                            target.addPredecessor(pred)
                        }
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

/**
 * 扁平化控制流 (Flatten CFG)
 * 
 * 将结构化控制流转换为更简单的形式
 */
class FlattenCFG : FunctionPass {
    
    override val name: String = "flatten"
    override val description: String = "Flatten Control Flow Graph"
    
    override fun run(function: SSAFunction): PassResult {
        // 简化的实现
        return PassResult.Success(false)
    }
}
