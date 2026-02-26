package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 冗余 Copy 消除 (Redundant Copy Elimination)
 *
 * 消除冗余的 Copy 指令链
 *
 * 优化场景：
 * 1. 链式 Copy：`a = copy b, b = copy c` => `a = copy c`
 * 2. 自引用 Copy：`a = copy a` => 删除
 * 3. Copy 常量：`a = copy b, b = 2` => `a = 2`
 * 4. 无用 Copy：如果 copy 的源和目标是同一个值，删除
 *
 * 目标形态：
 * ```
 * // 优化前
 * acc_const_2 = copy 2
 * v0 = copy acc_const_2
 *
 * // 优化后
 * c1 = 2  // 直接使用常量
 * ```
 */
class RedundantCopyElimination : FunctionPass {

    override val name: String = "redundantcopy"
    override val description: String = "Redundant Copy Elimination"

    override fun run(function: SSAFunction): PassResult {
        var modified = false
        var changed = true

        // 迭代运行直到达到不动点
        while (changed) {
            changed = false

            // 构建 Copy 链映射：Value -> 最终的常量/值
            val copyChainMap = buildCopyChainMap(function)

            // 处理所有 Copy 指令
            function.instructions().toList().forEach { inst ->
                if (inst is CopyInst) {
                    // 情况 1：Copy 的源是常量，直接用常量替换所有使用
                    if (inst.source is Constant) {
                        inst.replaceAllUsesWith(inst.source)
                        inst.eraseFromBlock()
                        modified = true
                        changed = true
                    }
                    // 情况 2：Copy 的源可以通过 Copy 链解析为常量
                    else if (copyChainMap.containsKey(inst.source)) {
                        val finalValue = copyChainMap[inst.source]!!
                        if (finalValue is Constant) {
                            inst.replaceAllUsesWith(finalValue)
                            inst.eraseFromBlock()
                            modified = true
                            changed = true
                        }
                    }
                }
            }

            // 处理其他指令中的 Copy 源
            function.instructions().toList().forEach { inst ->
                if (inst !is CopyInst && !inst.isTerminator()) {
                    for (i in 0 until inst.operandCount()) {
                        val operand = inst.getOperand(i)
                        if (copyChainMap.containsKey(operand)) {
                            val finalValue = copyChainMap[operand]!!
                            if (finalValue != operand) {
                                inst.setOperand(i, finalValue)
                                modified = true
                                changed = true
                            }
                        }
                    }
                }
            }
        }

        return PassResult.Success(modified)
    }

    /**
     * 构建 Copy 链映射
     * 对于每个 CopyInst，找到它最终指向的常量或值
     */
    private fun buildCopyChainMap(function: SSAFunction): Map<Value, Value> {
        val copyMap = mutableMapOf<Value, Value>()

        // 第一遍：收集所有 CopyInst
        val copyInsts = mutableListOf<CopyInst>()
        function.instructions().forEach { inst ->
            if (inst is CopyInst) {
                copyInsts.add(inst)
                copyMap[inst] = inst.source
            }
        }

        // 第二遍：解析 Copy 链
        var changed = true
        while (changed) {
            changed = false
            copyInsts.forEach { copyInst ->
                val currentSource = copyMap[copyInst]!!
                if (currentSource is CopyInst && copyMap.containsKey(currentSource)) {
                    // 链式 Copy，继续追踪
                    val finalSource = copyMap[currentSource]!!
                    if (copyMap[copyInst] != finalSource) {
                        copyMap[copyInst] = finalSource
                        changed = true
                    }
                }
            }
        }

        // 第三遍：构建 Value 到最终值的映射
        val resultMap = mutableMapOf<Value, Value>()
        copyInsts.forEach { copyInst ->
            val finalValue = copyMap[copyInst]!!
            if (finalValue !is CopyInst) {
                resultMap[copyInst] = finalValue
            }
        }

        return resultMap
    }
}
