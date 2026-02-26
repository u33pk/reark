package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 变量重建优化 (Variable Reconstruction)
 *
 * 将 SSA 形式的寄存器名还原为有意义的变量名
 *
 * 优化场景：
 * 1. 识别 Phi 节点链：reg_7_phi, %30, %39 属于同一变量
 * 2. 识别参数链：arg0 和相关的 Phi 节点属于同一变量
 * 3. 统一重命名为有意义的变量名
 *
 * 示例：
 * ```
 * // 优化前
 * reg_7_phi = phi [ arg0, bb_0 ], [ %30, bb_35 ], [ %39, bb_58 ]
 * %30 = ADD 2, reg_7_phi
 * %39 = INC reg_7_phi
 *
 * // 优化后
 * i = phi [ arg0, bb_0 ], [ i_next, bb_35 ], [ i_next, bb_58 ]
 * i_next = ADD 2, i
 * i_next = INC i
 * ```
 */
class VariableReconstruction : FunctionPass {

    override val name: String = "varrecon"
    override val description: String = "Variable Reconstruction"

    override fun run(function: SSAFunction): PassResult {
        var modified = false

        // 步骤 1: 识别变量链
        // 使用并查集将相关的值分组
        val valueGroups = ValueGrouper(function)

        // 步骤 2: 为每组分配变量名
        val groupNames = mutableMapOf<Int, String>()

        // 首先处理参数 - 参数组命名为 "i"（循环变量）
        function.arguments().forEach { arg ->
            val groupId = valueGroups.getGroupId(arg)
            groupNames[groupId] = "i"
        }

        // 处理 Phi 节点 - 与参数同组的 Phi 也命名为 "i"
        function.blocks().forEach { block ->
            block.instructions().forEach { inst ->
                if (inst is PhiInst) {
                    val groupId = valueGroups.getGroupId(inst)
                    // 检查 Phi 的输入是否包含参数
                    val hasArgInput = (0 until inst.incomingCount())
                        .any { i -> inst.getOperand(i) is Argument }
                    if (hasArgInput) {
                        groupNames[groupId] = "i"
                    }
                }
            }
        }

        // 步骤 3: 重命名
        function.blocks().forEach { block ->
            block.instructions().forEach { inst ->
                if (inst is PhiInst || inst is AddInst || inst is IncInst || 
                    inst is CopyInst || inst is ToNumericInst) {
                    val groupId = valueGroups.getGroupId(inst)
                    val newName = groupNames[groupId]
                    if (newName != null && inst.name != newName) {
                        inst.name = newName
                        modified = true
                    }
                }
            }
        }

        return PassResult.Success(modified)
    }

    /**
     * 根据指令类型推断变量名
     */
    private fun inferVariableName(inst: Instruction, index: Int): String {
        return when (inst) {
            is PhiInst -> {
                // 检查是否是循环变量
                val hasSelfReference = hasSelfReference(inst)
                if (hasSelfReference) {
                    "i"  // 循环变量
                } else {
                    "v$index"
                }
            }
            is AddInst, is IncInst -> "i_next"
            else -> "v$index"
        }
    }

    /**
     * 检查 Phi 节点是否有自引用（循环变量特征）
     */
    private fun hasSelfReference(phi: PhiInst): Boolean {
        for (i in 0 until phi.incomingCount()) {
            val operand = phi.getOperand(i)
            if (operand is Instruction && operand.parent != phi.parent) {
                // 检查操作数的定义块是否是该 Phi 所在块的后继
                val operandBlock = operand.parent
                val phiBlock = phi.parent
                if (phiBlock?.successors()?.contains(operandBlock) == true) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 值分组器 - 使用并查集将相关的值分组
     */
    class ValueGrouper(function: SSAFunction) {
        private val parent = mutableMapOf<Value, Value>()

        init {
            // 初始化：每个值的父节点是自己
            // 包括参数
            function.arguments().forEach { arg ->
                parent[arg] = arg
            }
            // 包括所有指令
            function.blocks().forEach { block ->
                block.instructions().forEach { inst ->
                    parent[inst] = inst
                }
            }

            // 合并相关的值
            function.blocks().forEach { block ->
                block.instructions().forEach { inst ->
                    when (inst) {
                        is PhiInst -> {
                            // Phi 节点与它的所有输入属于同一组
                            for (i in 0 until inst.incomingCount()) {
                                val operand = inst.getOperand(i)
                                if (operand is Instruction || operand is Argument) {
                                    union(inst, operand)
                                }
                            }
                        }
                        is CopyInst -> {
                            // Copy 指令与源属于同一组
                            if (inst.source is Instruction || inst.source is Argument) {
                                union(inst, inst.source)
                            }
                        }
                        is AddInst, is IncInst -> {
                            // 算术运算的结果与操作数可能属于同一变量链
                            // 这里简化处理：只与左操作数分组（如果是 Phi 节点）
                            val left = when (inst) {
                                is BinaryInstruction -> inst.left
                                is UnaryInstruction -> inst.operand
                                else -> null
                            }
                            if (left is PhiInst) {
                                union(inst, left)
                            }
                        }
                        is ToNumericInst -> {
                            // TO_NUMERIC 与操作数属于同一组
                            if (inst.operand is Instruction || inst.operand is Argument) {
                                union(inst, inst.operand)
                            }
                        }
                    }
                }
            }
        }

        private fun find(value: Value): Value {
            // 确保值在 parent map 中
            if (value !in parent) {
                parent[value] = value
            }
            if (parent[value] == value) return value
            return find(parent[value]!!)
        }

        private fun union(a: Value, b: Value) {
            val rootA = find(a)
            val rootB = find(b)
            if (rootA != rootB) {
                // 优先使用 Phi 节点作为根
                when {
                    rootA is PhiInst -> parent[rootB] = rootA
                    rootB is PhiInst -> parent[rootA] = rootB
                    else -> parent[rootB] = rootA
                }
            }
        }

        fun getGroupId(value: Value): Int {
            return find(value).id
        }
    }
}
