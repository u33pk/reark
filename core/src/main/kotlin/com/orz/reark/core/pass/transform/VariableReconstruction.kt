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
 * 4. 区分不同循环的迭代变量（i1, i2, i3...）
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

        // 步骤 2: 识别所有循环变量（有回边且有 INC/DEC 更新的 Phi 节点）
        val loopVariablePhis = identifyLoopVariablePhis(function)

        // 步骤 3: 为每个循环变量 Phi 分配唯一的名称 (i1, i2, i3...)
        val phiNames = mutableMapOf<PhiInst, String>()
        var loopVarCounter = 1
        loopVariablePhis.forEach { phi ->
            phiNames[phi] = "i$loopVarCounter"
            loopVarCounter++
        }

        // 步骤 4: 重命名
        // 对于循环变量 Phi 及其相关指令，使用分配的变量名
        // 对于其他指令，使用 ValueGrouper 分组来查找对应的循环变量名
        val groupToPhiName = mutableMapOf<Int, String>()
        loopVariablePhis.forEach { phi ->
            val groupId = valueGroups.getGroupId(phi)
            val name = phiNames[phi]
            if (name != null) {
                groupToPhiName[groupId] = name
            }
        }

        function.blocks().forEach { block ->
            block.instructions().forEach { inst ->
                if (inst is PhiInst) {
                    // Phi 节点直接使用 phiNames
                    val newName = phiNames[inst]
                    if (newName != null && inst.name != newName) {
                        inst.name = newName
                        modified = true
                    }
                } else if (inst is AddInst || inst is IncInst || inst is CopyInst ||
                    inst is ToNumericInst || inst is SubInst || inst is DecInst) {
                    // 其他指令通过 ValueGrouper 分组查找对应的变量名
                    val groupId = valueGroups.getGroupId(inst)
                    val newName = groupToPhiName[groupId]
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
     * 识别所有循环变量 Phi 节点
     * 循环变量的特征：
     * 1. Phi 节点有来自回边（back edge）的输入
     * 2. 该 Phi 节点的值在循环体中被 INC/DEC 指令更新（通过 copy 链回写）
     */
    private fun identifyLoopVariablePhis(
        function: SSAFunction
    ): List<PhiInst> {
        val loopVariablePhis = mutableSetOf<PhiInst>()

        // 第一步：建立从 Phi 节点到其更新指令（INC/DEC）的映射
        // 查找模式：Phi -> ... -> INC/DEC(Phi 的值) -> copy -> Phi
        val phiToUpdate = mutableMapOf<PhiInst, Instruction>()
        
        function.blocks().forEach { block ->
            block.instructions().forEach { inst ->
                if (inst is IncInst || inst is DecInst) {
                    // 查找 INC/DEC 的操作数来源
                    val operand = inst.operand
                    // 如果操作数是 Phi 节点或通过 copy 链追溯到 Phi
                    val sourcePhi = findSourcePhi(operand)
                    if (sourcePhi != null) {
                        phiToUpdate[sourcePhi] = inst
                    }
                }
            }
        }

        // 第二步：找到有回边输入且有对应更新指令的 Phi 节点
        function.blocks().forEach { block ->
            block.instructions().forEach { inst ->
                if (inst is PhiInst && hasBackEdgeInput(inst)) {
                    if (inst in phiToUpdate) {
                        loopVariablePhis.add(inst)
                    }
                }
            }
        }

        return loopVariablePhis.toList()
    }
    
    /**
     * 查找值的来源 Phi 节点（通过 copy 链追溯）
     */
    private fun findSourcePhi(value: Value, visited: MutableSet<Value> = mutableSetOf()): PhiInst? {
        if (value in visited) return null
        visited.add(value)
        
        return when (value) {
            is PhiInst -> value
            is CopyInst -> findSourcePhi(value.source, visited)
            is ToNumericInst -> findSourcePhi(value.operand, visited)
            else -> null
        }
    }

    /**
     * 检查 Phi 节点是否有回边输入（循环变量特征）
     * 回边：incoming block 是 phi 所在块的后继
     */
    private fun hasBackEdgeInput(phi: PhiInst): Boolean {
        val phiBlock = phi.parent ?: return false

        for (i in 0 until phi.incomingCount()) {
            val incomingBlock = phi.getIncomingBlock(i)
            // 检查 incoming block 是否是 phiBlock 的后继（形成回边）
            if (incomingBlock != phiBlock &&
                incomingBlock.successors().contains(phiBlock)) {
                return true
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
                        is AddInst, is IncInst, is SubInst, is DecInst -> {
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
