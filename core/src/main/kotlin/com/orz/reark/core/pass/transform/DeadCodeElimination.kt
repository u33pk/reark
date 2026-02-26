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
        var totalRemoved = 0
        
        // 多轮消除，直到没有可消除的指令
        while (true) {
            val liveInstructions = mutableSetOf<Instruction>()
            val worklist = mutableListOf<Instruction>()
            
            // 收集所有可能有副作用或是终止指令的指令到工作列表
            // 注意：这里不再使用 hasUsers() 作为初始条件！
            function.instructions().forEach { inst ->
                // 只有副作用或终止指令才是活的根
                if (!inst.isPure() || inst.isTerminator()) {
                    markLive(inst, liveInstructions, worklist)
                }
            }
            
            // 迭代标记活代码：从活的指令出发，标记它们依赖的操作数
            while (worklist.isNotEmpty()) {
                val inst = worklist.removeAt(worklist.size - 1)
                
                // 标记操作数为活代码
                inst.getOperands().forEach { operand ->
                    if (operand is Instruction) {
                        markLive(operand, liveInstructions, worklist)
                    }
                }
            }
            
            // 收集本轮的死代码
            val deadInstructions = mutableListOf<Instruction>()
            function.instructions().forEach { inst ->
                if (canRemove(inst, liveInstructions)) {
                    deadInstructions.add(inst)
                }
            }
            
            // 本轮没有可消除的指令，结束循环
            if (deadInstructions.isEmpty()) {
                break
            }
            
            // 移除死代码
            deadInstructions.forEach { inst ->
                inst.dropOperands()
                inst.eraseFromBlock()
                modified = true
            }
            
            totalRemoved += deadInstructions.size
        }
        
        // 清理空块
        function.blocks().toList().forEach { block ->
            if (block.isEmpty() && block.predecessorCount() == 0 && block != function.entryBlock) {
                function.removeBlock(block)
                modified = true
            }
        }
        
        return PassResult.Success(modified, "Removed $totalRemoved dead instructions")
    }
    
    private fun markLive(
        inst: Instruction, 
        liveInstructions: MutableSet<Instruction>,
        worklist: MutableList<Instruction>
    ) {
        if (liveInstructions.add(inst) && inst !in worklist) {
            worklist.add(inst)
        }
    }
    
    private fun canRemove(inst: Instruction, liveInstructions: Set<Instruction>): Boolean {
        // 不能移除终止指令
        if (inst.isTerminator()) return false
        
        // 不能移除有副作用的指令
        if (!inst.isPure()) return false
        
        // 不能移除已标记为活代码的指令
        if (inst in liveInstructions) return false
        
        return true
    }
}