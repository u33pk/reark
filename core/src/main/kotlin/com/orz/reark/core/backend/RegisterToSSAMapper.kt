package com.orz.reark.core.backend

import com.orz.reark.core.ir.*
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 寄存器到 SSA 值的映射管理器（Pruned SSA 版本）
 *
 * 处理 PandaASM 虚拟寄存器到 SSA 值的映射，包括：
 * 1. 寄存器值的版本控制（SSA 形式）
 * 2. PHI 节点的生成（仅当变量在不同路径上有不同值时）
 * 3. 基本块之间的寄存器状态传递
 *
 * Pruned SSA 优化：
 * - 只在变量被"杀死"（重新定义）时才创建 PHI 节点
 * - 如果所有前驱的值相同，不创建 PHI 节点
 * - 消除自引用的 PHI 节点
 */
class RegisterToSSAMapper {

    /**
     * 寄存器状态 - 记录单个寄存器的 SSA 值
     */
    data class RegisterState(
        val registerNum: Int,
        var currentValue: Value,
        var currentType: Type = currentValue.type
    )

    /**
     * 参数寄存器映射 - 记录哪些寄存器是参数寄存器，以及它们对应的参数
     */
    private val argumentRegisters = mutableMapOf<Int, Argument>()

    /**
     * 基本块状态 - 记录进入基本块时的寄存器状态
     */
    data class BlockRegisterState(
        val block: BasicBlock,
        val registerStates: MutableMap<Int, Value> = mutableMapOf(),
        val isSealed: Boolean = false,  // 是否已封闭（所有前驱已知）
        val killedRegisters: MutableSet<Int> = mutableSetOf()  // 在该块中被重新定义的寄存器
    )

    /**
     * 跟踪每个块中寄存器的最后写入值
     * 用于 PHI 节点正确获取前驱块的值
     */
    private val blockFinalValues = mutableMapOf<BasicBlock, MutableMap<Int, Value>>()

    // 当前寄存器状态
    private val registerStates = mutableMapOf<Int, RegisterState>()

    // 基本块寄存器状态映射
    private val blockStates = mutableMapOf<BasicBlock, BlockRegisterState>()

    // 未封闭的基本块（等待 PHI 节点生成）
    private val incompletePhis = mutableMapOf<BasicBlock, MutableMap<Int, PhiInst>>()

    // 当前基本块
    private var currentBlock: BasicBlock? = null

    // 用于检测循环的线程局部变量
    private val visitingBlocks = ThreadLocal.withInitial { mutableSetOf<BasicBlock>() }

    /**
     * 设置当前基本块
     */
    fun setCurrentBlock(block: BasicBlock) {
        currentBlock = block
    }

    /**
     * 获取当前基本块
     */
    fun getCurrentBlock(): BasicBlock? = currentBlock

    /**
     * 写入寄存器值
     *
     * @param registerNum 寄存器编号
     * @param value SSA 值
     * @param type 值类型（可选，默认使用 value.type）
     */
    fun writeRegister(registerNum: Int, value: Value, type: Type = value.type) {
        // 更新全局寄存器状态
        val state = RegisterState(registerNum, value, type)
        registerStates[registerNum] = state

        // 更新当前基本块的状态
        currentBlock?.let { block ->
            val blockState = getOrCreateBlockState(block)
            blockState.registerStates[registerNum] = value
            // 标记该寄存器在该块中被定义（杀死）
            blockState.killedRegisters.add(registerNum)
            // 同时更新块中寄存器的最后写入值
            blockFinalValues.getOrPut(block) { mutableMapOf() }[registerNum] = value
        }
    }

    /**
     * 读取寄存器值
     *
     * @param registerNum 寄存器编号
     * @return SSA 值，如果未定义则返回 null
     */
    fun readRegister(registerNum: Int): Value? {
        // 首先检查当前块的状态（块内定义）
        currentBlock?.let { block ->
            val blockState = blockStates[block]
            if (blockState != null && blockState.registerStates.containsKey(registerNum)) {
                return blockState.registerStates[registerNum]
            }
        }

        // 对于多前驱块，尝试从块的状态系统中获取（可能涉及 PHI 节点）
        currentBlock?.let { block ->
            val predecessors = block.predecessors()
            if (predecessors.size > 1) {
                // 多前驱块，需要 PHI 节点
                val result = readRegisterInBlock(registerNum, block)
                if (result != null) {
                    return result
                }
            } else if (predecessors.size == 1) {
                // 单前驱块，递归从前驱获取值
                val predBlock = predecessors.firstOrNull()
                if (predBlock != null) {
                    val predValue = readRegisterInBlock(registerNum, predBlock)
                    if (predValue != null) {
                        getOrCreateBlockState(block).registerStates[registerNum] = predValue
                        return predValue
                    }
                }
            }
        }

        // 检查全局寄存器状态
        registerStates[registerNum]?.let {
            return it.currentValue
        }

        return null
    }

    /**
     * 在指定基本块中读取寄存器值（Pruned SSA 版本）
     *
     * 关键优化：
     * 1. 只在变量被杀死时才需要 PHI 节点
     * 2. 如果所有前驱的值相同，直接使用该值
     * 3. 消除自引用的 PHI 节点
     */
    fun readRegisterInBlock(registerNum: Int, block: BasicBlock): Value? {
        // 1. 检查块内是否已有该寄存器的定义
        val blockState = blockStates[block]
        if (blockState != null && blockState.registerStates.containsKey(registerNum)) {
            return blockState.registerStates[registerNum]
        }

        // 检测循环依赖，避免无限递归
        val visiting = visitingBlocks.get()
        if (block in visiting) {
            val existingPhi = incompletePhis[block]?.get(registerNum)
            if (existingPhi != null) {
                return existingPhi
            }
            return registerStates[registerNum]?.currentValue
        }

        visiting.add(block)

        try {
            val predecessors = block.predecessors()

            return when {
                // 没有前驱（入口块）
                predecessors.isEmpty() -> {
                    registerStates[registerNum]?.currentValue
                }

                // 只有一个前驱，直接递归获取
                predecessors.size == 1 -> {
                    val predBlock = predecessors[0]
                    val predValue = readRegisterInBlock(registerNum, predBlock)
                    if (predValue != null) {
                        getOrCreateBlockState(block).registerStates[registerNum] = predValue
                        return predValue
                    }
                    val globalValue = registerStates[registerNum]?.currentValue
                    if (globalValue != null) {
                        getOrCreateBlockState(block).registerStates[registerNum] = globalValue
                        return globalValue
                    }
                    return null
                }

                // 多个前驱，需要 PHI 节点（Pruned SSA）
                else -> {
                    val isSealed = blockStates[block]?.isSealed == true

                    if (isSealed) {
                        // 收集所有前驱的值
                        val predValues = mutableListOf<Pair<BasicBlock, Value?>>()
                        for (pred in predecessors) {
                            val predValue = readRegisterInBlock(registerNum, pred)
                            predValues.add(pred to predValue)
                        }

                        // 检查是否有前驱有值
                        val hasValue = predValues.any { it.second != null }

                        if (!hasValue) {
                            return registerStates[registerNum]?.currentValue
                        }

                        // 获取所有不同的非空值
                        val distinctValues = predValues.mapNotNull { it.second }.distinct()

                        // Pruned SSA 关键优化：
                        // 如果所有前驱都有相同的值，不需要 PHI 节点
                        if (distinctValues.size == 1 && predValues.all { it.second != null }) {
                            val value = distinctValues.first()
                            getOrCreateBlockState(block).registerStates[registerNum] = value
                            return value
                        }

                        // 检查该寄存器是否在任何前驱块中被杀死
                        val isKilledInAnyPred = predecessors.any { pred ->
                            blockStates[pred]?.killedRegisters?.contains(registerNum) == true
                        }

                        // Pruned SSA：如果寄存器在所有前驱中都没有被杀死，
                        // 说明它是循环不变量，不需要 PHI 节点
                        if (!isKilledInAnyPred) {
                            // 使用第一个有值的前驱的值
                            val value = predValues.firstNotNullOfOrNull { it.second }
                                ?: registerStates[registerNum]?.currentValue
                            if (value != null) {
                                getOrCreateBlockState(block).registerStates[registerNum] = value
                                return value
                            }
                        }
                    }

                    // 需要创建 PHI 节点
                    val phi = incompletePhis[block]?.get(registerNum)
                        ?: createPhiForRegister(registerNum, block)

                    phi
                }
            }
        } finally {
            visiting.remove(block)
        }
    }

    /**
     * 为寄存器创建 PHI 节点
     */
    private fun createPhiForRegister(registerNum: Int, block: BasicBlock): PhiInst {
        val phiType = registerStates[registerNum]?.currentType
            ?: block.predecessors().firstOrNull()?.let { pred ->
                val predState = blockStates[pred]
                if (predState != null && predState.registerStates.containsKey(registerNum)) {
                    predState.registerStates[registerNum]?.type
                } else {
                    registerStates[registerNum]?.currentType
                }
            }
            ?: anyType

        val phi = block.createPhi(phiType, "reg_${registerNum}_phi")
        incompletePhis.getOrPut(block) { mutableMapOf() }[registerNum] = phi
        getOrCreateBlockState(block).registerStates[registerNum] = phi

        return phi
    }

    /**
     * 填充 PHI 节点的 incoming 值（带自循环消除）
     *
     * 关键修复：PHI 节点必须为所有前驱块提供输入值，即使某些前驱没有修改该变量。
     * 这确保 SSA 形式的完整性。
     */
    private fun fillPhiIncoming(phi: PhiInst, registerNum: Int, block: BasicBlock) {
        if (phi.incomingCount() > 0) {
            return
        }

        val predecessors = block.predecessors()

        // 首先收集每个前驱的值
        val predValueMap = mutableMapOf<BasicBlock, Value>()

        for (pred in predecessors) {
            var predValue = getRegisterValueInBlock(registerNum, pred, mutableSetOf())

            // 如果仍然没有值，使用全局寄存器状态
            if (predValue == null) {
                predValue = registerStates[registerNum]?.currentValue
            }

            // 如果仍然没有值，使用 undef
            if (predValue == null) {
                predValue = UndefValue(anyType)
            }

            // 自循环处理：如果值是该 PHI 节点本身，说明在循环中没有被重新定义
            // 应该使用入口块的值或参数
            if (predValue == phi) {
                // 尝试从入口块获取值
                val entryBlock = block.parent?.entryBlock
                if (entryBlock != null && entryBlock != block) {
                    predValue = getRegisterValueInBlock(registerNum, entryBlock, mutableSetOf())
                }
                // 如果入口块也没有值，尝试参数
                if (predValue == null || predValue == phi) {
                    predValue = getArgumentForRegister(registerNum)
                }
                // 最后使用 undef
                if (predValue == null) {
                    predValue = UndefValue(anyType)
                }
            }

            predValueMap[pred] = predValue
        }

        // 检查所有值是否相同
        val distinctValues = predValueMap.values.distinct()

        if (distinctValues.size == 1) {
            // 所有前驱的值相同，不需要 PHI 节点
            val singleValue = distinctValues.first()
            phi.replaceAllUsesWith(singleValue)
            phi.eraseFromBlock()
        } else {
            // 需要 PHI 节点，为所有前驱添加输入值
            for (pred in predecessors) {
                val value = predValueMap[pred] ?: UndefValue(anyType)
                phi.addIncoming(value, pred)
            }
        }
    }

    /**
     * 获取寄存器在指定块中的值
     * 递归查找直到找到值或到达入口块
     * 使用 visited 集合避免循环依赖
     */
    private fun getRegisterValueInBlock(
        registerNum: Int,
        block: BasicBlock,
        visited: MutableSet<BasicBlock>
    ): Value? {
        // 避免循环依赖
        if (!visited.add(block)) {
            // 遇到循环，返回全局寄存器状态
            return registerStates[registerNum]?.currentValue
        }

        // 1. 首先尝试从 blockFinalValues 获取（块中最后写入的值）
        var predValue = blockFinalValues[block]?.get(registerNum)

        // 2. 如果 blockFinalValues 中没有，尝试从 blockStates 获取
        if (predValue == null) {
            predValue = blockStates[block]?.registerStates?.get(registerNum)
        }

        // 3. 如果是入口块且没有值，尝试获取参数
        if (predValue == null && block.isEntryBlock()) {
            predValue = getArgumentForRegister(registerNum)
        }

        // 4. 如果仍然没有值，递归从前驱获取
        if (predValue == null) {
            val predecessors = block.predecessors()
            if (predecessors.size == 1) {
                // 单前驱，直接递归
                predValue = getRegisterValueInBlock(registerNum, predecessors.first(), visited)
            } else if (predecessors.size > 1) {
                // 多前驱，需要检查是否所有前驱有相同的值
                val values = predecessors.mapNotNull { pred ->
                    getRegisterValueInBlock(registerNum, pred, visited.toMutableSet())
                }
                if (values.isNotEmpty() && values.distinct().size == 1) {
                    predValue = values.first()
                }
            }
        }

        return predValue
    }

    /**
     * 封闭基本块
     */
    fun sealBlock(block: BasicBlock) {
        val state = getOrCreateBlockState(block)
        if (state.isSealed) return
        blockStates[block] = state.copy(isSealed = true)
    }

    /**
     * 获取或创建基本块状态
     */
    private fun getOrCreateBlockState(block: BasicBlock): BlockRegisterState {
        return blockStates.getOrPut(block) {
            BlockRegisterState(block)
        }
    }

    /**
     * 获取基本块状态
     */
    fun getBlockState(block: BasicBlock): BlockRegisterState? = blockStates[block]

    /**
     * 清除所有寄存器状态
     */
    fun clear() {
        registerStates.clear()
        blockStates.clear()
        blockFinalValues.clear()
        incompletePhis.clear()
        currentBlock = null
    }

    /**
     * 完成所有 PHI 节点的填充和修剪
     */
    fun finalizePhiNodes() {
        for ((block, phis) in incompletePhis) {
            for ((registerNum, phi) in phis) {
                fillPhiIncoming(phi, registerNum, block)
            }
        }
        incompletePhis.clear()
    }

    /**
     * 检查寄存器是否有定义
     */
    fun hasRegister(registerNum: Int): Boolean = registerStates.containsKey(registerNum)

    /**
     * 获取寄存器类型
     */
    fun getRegisterType(registerNum: Int): Type? = registerStates[registerNum]?.currentType

    /**
     * 获取所有已使用的寄存器
     */
    fun getUsedRegisters(): Set<Int> = registerStates.keys

    /**
     * 复制当前寄存器状态
     */
    fun snapshot(): Map<Int, Value> = registerStates.mapValues { it.value.currentValue }

    /**
     * 恢复寄存器状态
     */
    fun restore(snapshot: Map<Int, Value>) {
        for ((regNum, value) in snapshot) {
            writeRegister(regNum, value)
        }
    }

    /**
     * 将当前寄存器状态应用到基本块
     */
    fun applyToBlock(block: BasicBlock) {
        val blockState = getOrCreateBlockState(block)
        for ((regNum, state) in registerStates) {
            blockState.registerStates[regNum] = state.currentValue
        }
    }

    /**
     * 创建参数寄存器映射
     */
    fun setupArgumentRegisters(arguments: List<Argument>, firstReg: Int = 0) {
        for ((index, arg) in arguments.withIndex()) {
            val regNum = firstReg + index
            argumentRegisters[regNum] = arg
            writeRegister(regNum, arg, arg.type)
        }
    }

    /**
     * 检查寄存器是否是参数寄存器
     */
    fun isArgumentRegister(regNum: Int): Boolean = argumentRegisters.containsKey(regNum)

    /**
     * 获取参数寄存器对应的参数
     */
    fun getArgumentForRegister(regNum: Int): Argument? = argumentRegisters[regNum]

    /**
     * 调试输出当前寄存器状态
     */
    fun debugPrint(): String {
        val sb = StringBuilder()
        sb.appendLine("Register States:")
        for ((regNum, state) in registerStates.toSortedMap()) {
            sb.appendLine("  v$regNum = ${state.currentValue.getNameOrTemporary()} : ${state.currentType}")
        }
        return sb.toString()
    }
}

/**
 * SSA 构造上下文
 */
class SSAConstructionContext(
    val builder: IRBuilder,
    val module: Module,
    val blockMap: Map<Int, BasicBlock> = emptyMap()
) {
    val registerMapper = RegisterToSSAMapper()

    private var accumulatorValue: Value? = null
    private var accumulatorType: Type = anyType

    fun getAccumulator(): Value {
        return accumulatorValue ?: throw IllegalStateException("Accumulator not initialized")
    }

    fun tryGetAccumulator(): Value? = accumulatorValue

    fun setAccumulator(value: Value, type: Type = value.type) {
        accumulatorValue = value
        accumulatorType = type
    }

    fun hasAccumulator(): Boolean = accumulatorValue != null

    fun clearAccumulator() {
        accumulatorValue = null
    }

    fun getAccumulatorType(): Type = accumulatorType

    fun loadAccumulatorFromRegister(registerNum: Int) {
        val value = registerMapper.readRegister(registerNum)
        if (value == null) {
            throw IllegalStateException("Register v$registerNum not defined. Available registers: ${registerMapper.getUsedRegisters()}")
        }
        setAccumulator(value)
    }

    fun storeAccumulatorToRegister(registerNum: Int) {
        val acc = getAccumulator()
        registerMapper.writeRegister(registerNum, acc, accumulatorType)
    }

    fun setCurrentBlock(block: BasicBlock) {
        builder.setInsertPoint(block)
        registerMapper.setCurrentBlock(block)
    }

    fun createBlock(name: String = ""): BasicBlock {
        val block = builder.createBlock(name)
        setCurrentBlock(block)
        return block
    }

    fun sealBlock(block: BasicBlock) {
        registerMapper.sealBlock(block)
    }

    fun getCurrentBlock(): BasicBlock? = builder.currentBlock

    fun getCurrentFunction(): com.orz.reark.core.ir.Function? = builder.currentFunction

    fun getBlockByOffset(offset: Int): BasicBlock? = blockMap[offset]
}

/**
 * 基本块之间的寄存器状态传递
 */
class RegisterStateTransfer(private val mapper: RegisterToSSAMapper) {

    fun captureBranchState(): Map<Int, Value> = mapper.snapshot()

    fun mergeBranchStates(
        block: BasicBlock,
        branchStates: List<Pair<BasicBlock, Map<Int, Value>>>
    ) {
        if (branchStates.size < 2) return

        val allRegisters = branchStates.flatMap { it.second.keys }.toSortedSet()

        for (regNum in allRegisters) {
            val hasInAllBranches = branchStates.all { it.second.containsKey(regNum) }

            if (hasInAllBranches) {
                val values = branchStates.map { it.second[regNum]!! }
                val blocks = branchStates.map { it.first }

                if (values.distinct().size == 1) {
                    mapper.writeRegister(regNum, values[0])
                } else {
                    val phiType = values.first().type
                    val phi = block.createPhi(phiType, "reg_${regNum}_phi")
                    for ((predBlock, value) in blocks.zip(values)) {
                        phi.addIncoming(value, predBlock)
                    }
                    mapper.writeRegister(regNum, phi)
                }
            }
        }
    }

    fun mergeIfElseState(
        mergeBlock: BasicBlock,
        thenBlock: BasicBlock,
        thenState: Map<Int, Value>,
        elseBlock: BasicBlock? = null,
        elseState: Map<Int, Value>? = null
    ) {
        val branches = mutableListOf<Pair<BasicBlock, Map<Int, Value>>>()
        branches.add(thenBlock to thenState)
        if (elseBlock != null && elseState != null) {
            branches.add(elseBlock to elseState)
        }
        mergeBranchStates(mergeBlock, branches)
    }
}