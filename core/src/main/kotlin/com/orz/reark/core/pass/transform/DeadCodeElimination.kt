package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.BasicBlock
import com.orz.reark.core.ir.Instruction
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 死代码消除 (Dead Code Elimination)
 * 
 * 移除没有副作用且结果未被使用的指令
 */
class DeadCodeElimination : FunctionPass {
    
    override val name: String = "dce"
    override val description: String = "Dead Code Elimination"
    
    override fun run(function: SSAFunction): PassResult {
        var modified = false
        val worklist = mutableListOf<Instruction>()
        
        // 收集所有可能有副作用的指令到工作列表
        function.instructions().forEach { inst ->
            // 如果指令有副作用、是终止指令、或者被使用，则视为活代码
            if (!inst.isPure() || inst.isTerminator() || inst.hasUsers()) {
                markLive(inst, worklist)
            }
        }
        
        // 迭代标记活代码
        while (worklist.isNotEmpty()) {
            val inst = worklist.removeAt(worklist.size - 1)
            
            // 标记操作数为活代码
            inst.getOperands().forEach { operand ->
                if (operand is Instruction) {
                    markLive(operand, worklist)
                }
            }
        }
        
        // 收集并移除死代码
        val deadInstructions = mutableListOf<Instruction>()
        function.instructions().forEach { inst ->
            if (canRemove(inst)) {
                deadInstructions.add(inst)
            }
        }
        
        // 移除死代码
        deadInstructions.forEach { inst ->
            if (canRemove(inst)) {
                inst.dropOperands()
                inst.eraseFromBlock()
                modified = true
            }
        }
        
        // 清理空块
        function.blocks().toList().forEach { block ->
            if (block.isEmpty() && block.predecessorCount() == 0 && block != function.entryBlock) {
                function.removeBlock(block)
                modified = true
            }
        }
        
        return PassResult.Success(modified, "Removed ${deadInstructions.size} dead instructions")
    }
    
    private val liveInstructions = mutableSetOf<Instruction>()
    
    private fun markLive(inst: Instruction, worklist: MutableList<Instruction>) {
        if (liveInstructions.add(inst) && inst !in worklist) {
            worklist.add(inst)
        }
    }
    
    private fun canRemove(inst: Instruction): Boolean {
        // 不能移除终止指令
        if (inst.isTerminator()) return false
        
        // 不能移除有副作用的指令
        if (!inst.isPure()) return false
        
        // 不能移除被使用的指令
        if (inst.hasUsers()) return false
        
        // 不能移除已标记为活代码的指令
        if (inst in liveInstructions) return false
        
        return true
    }
}

/**
 * Aggressive Dead Code Elimination - 更激进的版本
 */
class AggressiveDeadCodeElimination : FunctionPass {
    
    override val name: String = "adce"
    override val description: String = "Aggressive Dead Code Elimination"
    
    override fun run(function: SSAFunction): PassResult {
        var modified = false
        
        // 标记所有控制依赖为活代码
        val liveBlocks = mutableSetOf<BasicBlock>()
        val worklist = ArrayDeque<BasicBlock>()
        
        // 从出口块开始（反向传播活代码）
        // 简化版：标记所有可达块
        worklist.add(function.entryBlock)
        while (worklist.isNotEmpty()) {
            val block = worklist.removeFirst()
            if (liveBlocks.add(block)) {
                block.successors().forEach { worklist.add(it) }
            }
        }
        
        // 移除不可达块中的所有指令
        function.blocks().forEach { block ->
            if (block !in liveBlocks) {
                // 不可达块，可以移除所有非终止指令
                block.instructions().toList().forEach { inst ->
                    if (!inst.isTerminator()) {
                        inst.dropOperands()
                        block.erase(inst)
                        modified = true
                    }
                }
            }
        }
        
        // 运行普通DCE
        val dce = DeadCodeElimination()
        val dceResult = dce.run(function)
        
        return PassResult.Success(
            modified || (dceResult as? PassResult.Success)?.modified ?: false,
            "Aggressive DCE completed"
        )
    }
}
