package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 常量传播 (Constant Propagation)
 *
 * 将常量值传播到使用点，替代使用常量的变量
 *
 * 主要功能：
 * 1. 识别 CopyInst 传输的常量值
 * 2. 传播常量到所有使用点
 * 3. 处理 Phi 节点的常量输入
 * 4. 支持链式常量传播（a = copy b, b = copy const => a = const）
 */
class ConstantPropagation : FunctionPass {

    override val name: String = "constprop"
    override val description: String = "Constant Propagation"

    override fun run(function: SSAFunction): PassResult {
        var modified = false

        // 收集所有可以传播的常量定义
        // 使用迭代方式处理链式传播
        // constantMap 记录 Value（包括指令产生的值）到常量的映射
        val constantMap = mutableMapOf<Value, Constant>()

        // 多次迭代以处理链式传播
        var mapChanged = true
        while (mapChanged) {
            mapChanged = false
            function.instructions().toList().forEach { inst ->
                if (inst is CopyInst && !constantMap.containsKey(inst)) {
                    val source = inst.source
                    val constant = when {
                        // 源直接是常量
                        source is Constant -> source
                        // 源是已知的常量映射
                        source is Value && constantMap.containsKey(source) -> constantMap[source]!!
                        else -> null
                    }
                    if (constant != null) {
                        // 记录 CopyInst 指令本身到常量的映射
                        constantMap[inst] = constant
                        mapChanged = true
                    }
                }
            }
        }

        // 传播常量到使用点（只运行一次）
        function.instructions().toList().forEach { inst ->
            if (inst !is CopyInst && !inst.isTerminator()) {
                // 替换所有操作数中的常量
                for (i in 0 until inst.operandCount()) {
                    val operand = inst.getOperand(i)
                    val constant = getConstantValue(operand, constantMap)
                    if (constant != null && constant != operand) {
                        inst.setOperand(i, constant)
                        modified = true
                    }
                }
            }
        }

        // 处理 Phi 节点：如果 Phi 的所有输入现在都是常量，折叠 Phi
        // 需要迭代处理，因为折叠一个 Phi 可能使另一个 Phi 的所有输入变成常量
        var phiChanged = true
        while (phiChanged) {
            phiChanged = false
            function.instructions().toList().forEach { inst ->
                if (inst is PhiInst) {
                    val folded = foldPhiToConstant(inst, constantMap)
                    if (folded != null) {
                        inst.replaceAllUsesWith(folded)
                        inst.eraseFromBlock()
                        modified = true
                        phiChanged = true
                        // 将折叠后的常量加入映射，以便其他 Phi 可以使用
                        constantMap[inst] = folded
                    }
                }
            }
        }

        return PassResult.Success(modified)
    }

    /**
     * 递归获取值的常量（如果存在）
     * 支持链式查找：如果值是 CopyInst，继续查找其源
     */
    private fun getConstantValue(value: Value, constantMap: Map<Value, Constant>): Constant? {
        // 避免无限递归，使用 visited 集合跟踪
        return getConstantValueRecursive(value, constantMap, mutableSetOf())
    }
    
    private fun getConstantValueRecursive(
        value: Value, 
        constantMap: Map<Value, Constant>,
        visited: MutableSet<Value>
    ): Constant? {
        // 避免循环引用
        if (!visited.add(value)) {
            return null
        }
        
        // 首先检查是否在常量映射中（包括 CopyInst 产生的值）
        // 使用 identity 比较，因为 Value 的 equals 可能不是基于引用的
        val mappedConstant = constantMap.entries.find { it.key === value }?.value
        if (mappedConstant != null) {
            return mappedConstant
        }
        
        return when {
            // 直接是常量
            value is Constant -> value
            // 是 CopyInst，递归查找源
            value is CopyInst -> getConstantValueRecursive(value.source, constantMap, visited)
            else -> null
        }
    }

    /**
     * 尝试将 Phi 节点折叠为常量
     * 如果 Phi 的所有输入都可以解析为同一个常量，则返回该常量
     */
    private fun foldPhiToConstant(phi: PhiInst, constantMap: Map<Value, Constant>): Constant? {
        if (phi.incomingCount() == 0) {
            return null
        }

        var firstConstant: Constant? = null

        for (i in 0 until phi.incomingCount()) {
            val operand = phi.getOperand(i)
            val constant = getConstantValue(operand, constantMap)

            if (constant == null) {
                return null
            }

            if (firstConstant == null) {
                firstConstant = constant
            } else if (constant != firstConstant) {
                return null
            }
        }

        return firstConstant
    }
}
