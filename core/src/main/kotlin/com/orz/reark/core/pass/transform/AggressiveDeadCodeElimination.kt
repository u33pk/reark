package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.BasicBlock
import com.orz.reark.core.ir.Instruction
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * Aggressive Dead Code Elimination - 更激进的版本
 * 
 * 结合控制流分析，移除不可达块中的代码
 */
class AggressiveDeadCodeElimination : FunctionPass {
    
    override val name: String = "adce"
    override val description: String = "Aggressive Dead Code Elimination"
    
    override fun run(function: SSAFunction): PassResult {
        var modified = false
        
        // 第一步：移除不可达块
        modified = removeUnreachableBlocks(function)
        
        // 第二步：运行普通 DCE（已包含多轮迭代）
        val dce = DeadCodeElimination()
        val dceResult = dce.run(function)
        
        return PassResult.Success(
            modified || (dceResult as? PassResult.Success)?.modified ?: false,
            "Aggressive DCE completed"
        )
    }
    
    private fun removeUnreachableBlocks(function: SSAFunction): Boolean {
        var modified = false
        val liveBlocks = mutableSetOf<BasicBlock>()
        val worklist = ArrayDeque<BasicBlock>()
        
        // 从入口块开始标记可达块
        // 检查函数是否有基本块（入口块是第一个基本块）
        if (function.blocks().isEmpty()) {
            return false
        }
        worklist.add(function.entryBlock)
        while (worklist.isNotEmpty()) {
            val block = worklist.removeFirst()
            if (liveBlocks.add(block)) {
                block.successors().forEach { worklist.add(it) }
            }
        }
        
        // 移除不可达块
        val unreachableBlocks = function.blocks().filter { it !in liveBlocks }
        for (block in unreachableBlocks) {
            // 先清除块中的所有指令
            block.instructions().toList().forEach { inst ->
                inst.dropOperands()
                block.erase(inst)
            }
            // 然后从函数中移除块
            function.removeBlock(block)
            modified = true
        }
        
        return modified
    }
}