package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 循环不变量提升 (Loop Invariant Code Motion)
 *
 * 将循环体内不随迭代变化的指令提升到循环外
 *
 * 优化场景：
 * 1. 全局属性查找：`get_property global_0["prop_1"]` 在循环中不变
 * 2. 常量表达式：操作数都是常量的表达式
 * 3. 循环外定义的操作：操作数都在循环外定义
 *
 * 示例：
 * ```
 * // 优化前
 * loop:
 *   %x = get_property global_0["log"]  // 不变
 *   call %x(...)
 *
 * // 优化后
 * %x = get_property global_0["log"]    // 提升到循环外
 * loop:
 *   call %x(...)
 * ```
 */
class LoopInvariantCodeMotion : FunctionPass {

    override val name: String = "licm"
    override val description: String = "Loop Invariant Code Motion"

    override fun run(function: SSAFunction): PassResult {
        var modified = false

        // 查找所有循环
        val loops = findLoops(function)

        for (loop in loops) {
            // 对每个循环，查找并提升不变量
            val lifted = hoistLoopInvariants(loop, function)
            if (lifted) {
                modified = true
            }
        }

        return PassResult.Success(modified)
    }

    /**
     * 查找函数中的所有自然循环
     */
    private fun findLoops(function: SSAFunction): List<Loop> {
        val loops = mutableListOf<Loop>()
        val visited = mutableSetOf<BasicBlock>()

        // 使用 DFS 查找回边
        fun dfs(block: BasicBlock, path: MutableSet<BasicBlock>) {
            if (block in path) return
            if (block in visited) return

            visited.add(block)
            path.add(block)

            for (succ in block.successors()) {
                if (succ in path) {
                    // 找到回边：block -> succ
                    // 识别循环头和循环体
                    val loopBody = findLoopBody(succ, block, mutableSetOf())
                    loops.add(Loop(succ, loopBody, block))
                } else {
                    dfs(succ, path)
                }
            }

            path.remove(block)
        }

        function.entryBlock?.let { dfs(it, mutableSetOf()) }
        return loops
    }

    /**
     * 查找循环体（所有能到达回边且经过循环头的块）
     */
    private fun findLoopBody(
        header: BasicBlock,
        backEdgeFrom: BasicBlock,
        visited: MutableSet<BasicBlock>
    ): Set<BasicBlock> {
        if (header in visited) return emptySet()

        val body = mutableSetOf<BasicBlock>()
        val worklist = ArrayDeque<BasicBlock>()
        worklist.add(backEdgeFrom)

        while (worklist.isNotEmpty()) {
            val block = worklist.removeLast()
            if (block in visited) continue
            if (block == header) continue

            visited.add(block)
            body.add(block)

            for (pred in block.predecessors()) {
                if (pred !in visited) {
                    worklist.add(pred)
                }
            }
        }

        return body
    }

    /**
     * 提升循环不变量
     */
    private fun hoistLoopInvariants(loop: Loop, function: SSAFunction): Boolean {
        var modified = false

        // 使用外部前驱块作为提升目标
        val preHeader = findOrCreatePreHeader(loop, function)

        // 收集循环体内所有块中的循环不变量
        val toHoist = mutableListOf<Instruction>()

        for (block in loop.body) {
            block.instructions().toList().forEach { inst ->
                if (isLoopInvariant(inst, loop)) {
                    toHoist.add(inst)
                }
            }
        }

        // 按依赖顺序排序：先提升没有依赖的，再提升有依赖的
        val hoisted = mutableSetOf<Instruction>()
        var changed = true
        while (changed) {
            changed = false
            for (inst in toHoist) {
                if (inst in hoisted) continue

                // 检查所有操作数是否已经提升或在循环外
                val canHoist = inst.operands().all { operand ->
                    if (operand is Instruction) {
                        // 操作数是指令：要么已经提升，要么在循环外
                        operand in hoisted || operand.parent !in loop.body
                    } else {
                        true  // 非常量操作数
                    }
                }

                if (canHoist) {
                    // 提升到 preHeader 的终止指令之前
                    val terminator = preHeader.terminator()
                    if (terminator != null) {
                        inst.eraseFromBlock()
                        preHeader.insertBefore(inst, terminator)
                        hoisted.add(inst)
                        modified = true
                        changed = true
                    }
                }
            }
        }

        return modified
    }

    /**
     * 查找或创建循环预头块
     * 预头块是循环头之前的一个块，专门用于存放循环不变量
     */
    private fun findOrCreatePreHeader(loop: Loop, function: SSAFunction): BasicBlock {
        // 查找循环头的前驱块（不在循环体内的）
        val externalPreds = loop.header.predecessors().filter { it !in loop.body }

        if (externalPreds.isEmpty()) {
            // 没有外部前驱，无法提升
            return loop.header
        }

        // 返回第一个外部前驱
        // 简化实现：不创建新的预头块，直接使用现有前驱
        return externalPreds.first()
    }

    /**
     * 判断指令是否是循环不变量
     */
    private fun isLoopInvariant(inst: Instruction, loop: Loop): Boolean {
        // 终止指令不能提升
        if (inst.isTerminator()) return false

        // PHI 节点不能提升
        if (inst is PhiInst) return false

        // 有副作用的指令不能提升
        if (inst.mayHaveSideEffects()) return false

        // 检查所有操作数是否在循环外定义
        for (operand in inst.operands()) {
            if (operand is Instruction) {
                // 如果操作数是指令，检查它是否在循环体内
                if (operand.parent in loop.body) {
                    return false
                }
                // 如果操作数是指令且在循环头，也不能提升
                if (operand.parent == loop.header) {
                    return false
                }
            }
        }

        // 特殊处理：全局属性查找
        if (inst is GetPropertyInst) {
            val obj = inst.obj
            if (obj is GlobalValue || (obj is CopyInst && obj.source is GlobalValue)) {
                return true
            }
        }

        return true
    }

    /**
     * 循环表示
     */
    data class Loop(
        val header: BasicBlock,      // 循环头
        val body: Set<BasicBlock>,   // 循环体
        val backEdgeFrom: BasicBlock // 回边起点
    )
}

/**
 * 获取指令的所有操作数
 */
private fun Instruction.operands(): List<Value> {
    return (0 until operandCount()).map { getOperand(it) }
}
