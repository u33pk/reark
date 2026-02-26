package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 全局值编号 (Global Value Numbering, GVN)
 *
 * 识别并复用相同的表达式计算
 *
 * 优化场景：
 * 1. 相同的全局属性查找：`get_property global_0["prop_1"]` 只计算一次
 * 2. 相同的纯函数调用
 * 3. 相同的常量表达式
 *
 * 示例：
 * ```
 * // 优化前
 * bb_0:
 *   %1 = get_property global_0["log"]
 * bb_exit:
 *   %2 = get_property global_0["log"]  // 重复计算
 *
 * // 优化后
 * bb_0:
 *   %1 = get_property global_0["log"]
 * bb_exit:
 *   // %2 被替换为 %1
 * ```
 */
class GlobalValueNumbering : FunctionPass {

    override val name: String = "gvn"
    override val description: String = "Global Value Numbering"

    override fun run(function: SSAFunction): PassResult {
        var modified = false

        // 构建值编号映射：表达式签名 -> 产生该值的指令
        val valueNumberMap = mutableMapOf<ExpressionKey, Value>()

        // 第一遍：收集所有表达式
        function.blocks().forEach { block ->
            block.instructions().toList().forEach { inst ->
                if (isPureExpression(inst)) {
                    val key = buildExpressionKey(inst)
                    if (key != null) {
                        if (valueNumberMap.containsKey(key)) {
                            // 已有相同表达式，替换
                            val existingValue = valueNumberMap[key]!!
                            inst.replaceAllUsesWith(existingValue)
                            inst.eraseFromBlock()
                            modified = true
                        } else {
                            // 记录新表达式
                            valueNumberMap[key] = inst
                        }
                    }
                }
            }
        }

        return PassResult.Success(modified)
    }

    /**
     * 判断指令是否是纯表达式（可以被复用）
     */
    private fun isPureExpression(inst: Instruction): Boolean {
        // 终止指令不是纯表达式
        if (inst.isTerminator()) return false

        // PHI 节点不是纯表达式
        if (inst is PhiInst) return false

        // 有副作用的指令不是纯表达式
        if (inst.mayHaveSideEffects()) return false

        // 全局属性查找是纯表达式
        if (inst is GetPropertyInst) {
            val obj = inst.obj
            if (obj is GlobalValue) return true
        }

        // Copy 指令如果是复制全局值，也是纯表达式
        if (inst is CopyInst && inst.source is GlobalValue) {
            return true
        }

        return false
    }

    /**
     * 构建表达式的唯一键
     */
    private fun buildExpressionKey(inst: Instruction): ExpressionKey? {
        return when (inst) {
            is GetPropertyInst -> {
                val obj = inst.obj
                val key = inst.key
                // 全局属性查找：GlobalValue + 属性名
                if (obj is GlobalValue) {
                    ExpressionKey("get_property", listOf(obj.name, key.toString()))
                } else {
                    null
                }
            }
            is CopyInst -> {
                // Copy 全局值
                if (inst.source is GlobalValue) {
                    ExpressionKey("copy_global", listOf(inst.source.name))
                } else {
                    null
                }
            }
            else -> null
        }
    }

    /**
     * 表达式键 - 用于识别相同的表达式
     */
    data class ExpressionKey(
        val opcode: String,
        val operands: List<String>
    ) {
        override fun toString(): String = "$opcode(${operands.joinToString(", ")})"
    }
}
