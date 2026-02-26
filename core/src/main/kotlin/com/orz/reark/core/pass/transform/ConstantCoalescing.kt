package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 常量合并优化 (Constant Coalescing)
 *
 * 合并相同的常量定义，消除冗余的常量 copy 链
 *
 * 优化场景：
 * 1. 消除中间 accumulator 常量：
 *    ```
 *    acc_const_2 = copy 2
 *    v0 = copy acc_const_2
 *    ```
 *    优化为：
 *    ```
 *    v0 = copy 2
 *    ```
 *
 * 2. 合并相同的常量：
 *    如果有多个相同的常量定义，只保留一个
 *
 * 3. 重命名常量变量为更简洁的形式：
 *    ```
 *    c1 = 2
 *    c2 = 5
 *    c3 = 10
 *    ```
 */
class ConstantCoalescing : FunctionPass {

    override val name: String = "constcoal"
    override val description: String = "Constant Coalescing"

    override fun run(function: SSAFunction): PassResult {
        var modified = false

        // 收集所有常量定义
        val constantDefs = mutableMapOf<Constant, CopyInst>()
        val toRemove = mutableListOf<CopyInst>()

        function.instructions().toList().forEach { inst ->
            if (inst is CopyInst && inst.source is Constant) {
                val constant = inst.source as Constant

                // 检查是否已经有相同的常量定义
                if (constantDefs.containsKey(constant)) {
                    // 已有相同常量，标记为删除，使用已有的定义
                    toRemove.add(inst)
                } else {
                    // 新的常量，记录
                    constantDefs[constant] = inst
                }
            }
        }

        // 删除冗余的常量 copy
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { inst ->
                // 找到对应的常量定义
                val existingDef = constantDefs[inst.source]
                if (existingDef != null) {
                    inst.replaceAllUsesWith(existingDef)
                    inst.eraseFromBlock()
                    modified = true
                }
            }
        }

        // 优化 accumulator 常量命名
        function.instructions().toList().forEach { inst ->
            if (inst is CopyInst && inst.source is Constant) {
                // 检查名称是否是 accumulator 风格
                if (inst.name.startsWith("acc_const_")) {
                    // 重命名为更简洁的形式
                    val constant = inst.source as Constant
                    val newName = "c_${constant.toString()}"
                    if (inst.name != newName) {
                        inst.name = newName
                        modified = true
                    }
                }
            }
        }

        return PassResult.Success(modified)
    }
}
