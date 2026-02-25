package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.BasicBlock
import com.orz.reark.core.ir.BranchInst
import com.orz.reark.core.ir.ReturnInst
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 冗余返回消除 (Redundant Return Elimination)
 *
 * 识别并消除指向统一出口块的冗余返回指令。
 *
 * 优化场景：
 * 1. 块以 `ret void` 结尾
 * 2. 存在一个统一的函数出口块（也是 `ret void`）
 * 3. 将 `ret void` 替换为 `br` 到出口块
 *
 * 这样后续的 SimplifyCFG 可以进一步合并块。
 *
 * 注意：此 pass 不会消除"有意义的"early return，只消除那些
 * 可以统一到一个出口块的冗余返回。
 */
class RedundantReturnElimination : FunctionPass {

    override val name: String = "redundantret"
    override val description: String = "Eliminate redundant return instructions"

    override fun run(function: SSAFunction): PassResult {
        var modified = false
        var changed: Boolean

        do {
            changed = false

            // 1. 识别统一的出口块
            val exitBlocks = findExitBlocks(function)
            if (exitBlocks.isEmpty()) {
                break
            }

            // 2. 选择主出口块（被最多前驱引用的）
            val mainExitBlock = exitBlocks.maxByOrNull { it.predecessorCount() } ?: exitBlocks.first()

            // 3. 遍历所有块，消除冗余返回
            for (block in function.blocks().toList()) {
                // 跳过出口块本身
                if (block == mainExitBlock) continue

                val terminator = block.terminator()
                if (terminator is ReturnInst && terminator.returnValue == null) {
                    // 这是一个 `ret void`
                    // 替换为 br mainExitBlock
                    block.remove(terminator)
                    terminator.dropOperands()

                    // 添加跳转
                    block.addSuccessor(mainExitBlock)
                    block.append(BranchInst(mainExitBlock))

                    modified = true
                    changed = true
                    break // 重新开始迭代
                }
            }

        } while (changed)

        return PassResult.Success(modified)
    }

    /**
     * 识别函数的出口块
     *
     * 出口块定义：
     * - 以 `ret void` 结尾
     * - 没有后继块
     */
    private fun findExitBlocks(function: SSAFunction): List<BasicBlock> {
        val exitBlocks = mutableListOf<BasicBlock>()

        for (block in function.blocks()) {
            if (isExitBlock(block)) {
                exitBlocks.add(block)
            }
        }

        return exitBlocks
    }

    /**
     * 判断一个块是否为出口块
     */
    private fun isExitBlock(block: BasicBlock): Boolean {
        // 必须有终止指令
        val terminator = block.terminator() ?: return false

        // 必须是 ret void
        if (terminator !is ReturnInst) return false
        if (terminator.returnValue != null) return false

        // 不能有后继块
        if (block.successors().isNotEmpty()) return false

        return true
    }
}
