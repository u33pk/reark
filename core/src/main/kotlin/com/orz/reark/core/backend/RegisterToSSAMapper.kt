package com.orz.reark.core.backend

import com.orz.reark.core.ir.*
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 寄存器到 SSA 值的映射管理器
 * 
 * 处理 PandaASM 虚拟寄存器到 SSA 值的映射，包括：
 * 1. 寄存器值的版本控制（SSA 形式）
 * 2. PHI 节点的生成（用于合并来自不同路径的寄存器值）
 * 3. 基本块之间的寄存器状态传递
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
     * 基本块状态 - 记录进入基本块时的寄存器状态
     */
    data class BlockRegisterState(
        val block: BasicBlock,
        val registerStates: MutableMap<Int, Value> = mutableMapOf(),
        val isSealed: Boolean = false  // 是否已封闭（所有前驱已知）
    )
    
    // 当前寄存器状态
    private val registerStates = mutableMapOf<Int, RegisterState>()
    
    // 基本块寄存器状态映射
    private val blockStates = mutableMapOf<BasicBlock, BlockRegisterState>()
    
    // 未封闭的基本块（等待 PHI 节点生成）
    private val incompletePhis = mutableMapOf<BasicBlock, MutableMap<Int, PhiInst>>()
    
    // 当前基本块
    private var currentBlock: BasicBlock? = null
    
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
        val state = RegisterState(registerNum, value, type)
        registerStates[registerNum] = state

        // 更新当前基本块的状态
        currentBlock?.let { block ->
            getOrCreateBlockState(block).registerStates[registerNum] = value
        }
    }

    /**
     * 读取寄存器值
     *
     * @param registerNum 寄存器编号
     * @return SSA 值，如果未定义则返回 null
     */
    fun readRegister(registerNum: Int): Value? {
        // 首先检查当前寄存器状态
        registerStates[registerNum]?.let {
            return it.currentValue
        }

        // 如果当前块有状态，从块状态读取
        currentBlock?.let { block ->
            return readRegisterInBlock(registerNum, block)
        }

        // 如果寄存器完全未定义，返回 null
        return null
    }
    
    // 用于检测循环的线程局部变量
    private val visitingBlocks = ThreadLocal.withInitial { mutableSetOf<BasicBlock>() }

    /**
     * 在指定基本块中读取寄存器值
     *
     * 这是 SSA 构造的核心算法，处理：
     * 1. 本地定义
     * 2. 前驱块的值传递
     * 3. PHI 节点的生成
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
            // 检测到循环，返回 null
            return null
        }

        // 标记为正在访问
        visiting.add(block)

        try {
            // 2. 如果块未封闭且不是入口块，可能需要生成 PHI
            val predecessors = block.predecessors()

            return when {
                // 没有前驱（入口块）
                predecessors.isEmpty() -> {
                    // 尝试从全局寄存器状态获取
                    registerStates[registerNum]?.currentValue
                }

                // 只有一个前驱，直接递归获取
                predecessors.size == 1 -> {
                    val predBlock = predecessors[0]
                    val predValue = readRegisterInBlock(registerNum, predBlock)
                    predValue?.let {
                        // 缓存到当前块
                        getOrCreateBlockState(block).registerStates[registerNum] = it
                    }
                    predValue
                }

                // 多个前驱，需要 PHI 节点
                else -> {
                    // 检查是否所有前驱都能提供该寄存器的值
                    val allPredsHaveValue = predecessors.all { pred ->
                        readRegisterInBlock(registerNum, pred) != null
                    }

                    if (allPredsHaveValue) {
                        // 创建 PHI 节点（如果尚未创建）
                        val phi = incompletePhis[block]?.get(registerNum)
                            ?: createPhiForRegister(registerNum, block)

                        // 如果块已封闭，填充 PHI 的 incoming
                        if (blockState?.isSealed == true) {
                            fillPhiIncoming(phi, registerNum, block)
                        }

                        phi
                    } else {
                        // 如果不是所有前驱都有该寄存器的定义，可能是因为某些分支没有执行到
                        // 尝试从已经定义的地方获取值，或返回 null
                        for (pred in predecessors) {
                            val predValue = readRegisterInBlock(registerNum, pred)
                            if (predValue != null) {
                                return predValue
                            }
                        }
                        // 如果都没有找到，返回 null
                        null
                    }
                }
            }
        } finally {
            // 完成访问，移除标记
            visiting.remove(block)
        }
    }
    
    /**
     * 为寄存器创建 PHI 节点
     */
    private fun createPhiForRegister(registerNum: Int, block: BasicBlock): PhiInst {
        // 确定 PHI 类型
        val phiType = registerStates[registerNum]?.currentType
            ?: block.predecessors().firstOrNull()?.let { pred ->
                // 使用非递归方式获取类型，避免循环依赖
                val predState = blockStates[pred]
                if (predState != null && predState.registerStates.containsKey(registerNum)) {
                    predState.registerStates[registerNum]?.type
                } else {
                    registerStates[registerNum]?.currentType
                }
            }
            ?: anyType

        // 创建 PHI 节点
        val phi = block.createPhi(phiType, "reg_${registerNum}_phi")

        // 缓存 PHI 节点
        incompletePhis.getOrPut(block) { mutableMapOf() }[registerNum] = phi

        // 更新块状态
        getOrCreateBlockState(block).registerStates[registerNum] = phi

        return phi
    }
    
    /**
     * 填充 PHI 节点的 incoming 值
     */
    private fun fillPhiIncoming(phi: PhiInst, registerNum: Int, block: BasicBlock) {
        // 清除现有的 incoming（如果有）
        // 注意：这里的实现取决于PhiInst的具体API，如果无法清空，则直接添加新的
        while (phi.incomingCount() > 0) {
            // PHI 节点不支持移除单个 incoming，需要重新创建
            // 这里简化处理，假设 PHI 是新的
            break
        }

        // 从所有前驱获取值，并且只添加那些有值的incoming
        for (pred in block.predecessors()) {
            val predValue = readRegisterInBlock(registerNum, pred)
            if (predValue != null) {
                phi.addIncoming(predValue, pred)
            }
        }
    }
    
    /**
     * 封闭基本块
     * 
     * 当所有前驱都已知时调用，完成 PHI 节点的生成
     */
    fun sealBlock(block: BasicBlock) {
        val state = getOrCreateBlockState(block)
        if (state.isSealed) return
        
        // 标记为已封闭
        blockStates[block] = state.copy(isSealed = true)
        
        // 完成该块的未决 PHI 节点
        incompletePhis[block]?.let { phis ->
            for ((registerNum, phi) in phis) {
                fillPhiIncoming(phi, registerNum, block)
            }
            incompletePhis.remove(block)
        }
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
        incompletePhis.clear()
        currentBlock = null
    }
    
    /**
     * 检查寄存器是否有定义
     */
    fun hasRegister(registerNum: Int): Boolean {
        return registerStates.containsKey(registerNum)
    }
    
    /**
     * 获取寄存器类型
     */
    fun getRegisterType(registerNum: Int): Type? {
        return registerStates[registerNum]?.currentType
    }
    
    /**
     * 获取所有已使用的寄存器
     */
    fun getUsedRegisters(): Set<Int> = registerStates.keys
    
    /**
     * 复制当前寄存器状态
     * 
     * 用于分支前的状态保存
     */
    fun snapshot(): Map<Int, Value> {
        return registerStates.mapValues { it.value.currentValue }
    }
    
    /**
     * 恢复寄存器状态
     * 
     * 用于合并分支后的状态恢复
     */
    fun restore(snapshot: Map<Int, Value>) {
        for ((regNum, value) in snapshot) {
            writeRegister(regNum, value)
        }
    }
    
    /**
     * 将当前寄存器状态应用到基本块
     * 
     * 用于在基本块开头设置寄存器的初始值
     */
    fun applyToBlock(block: BasicBlock) {
        val blockState = getOrCreateBlockState(block)
        for ((regNum, state) in registerStates) {
            blockState.registerStates[regNum] = state.currentValue
        }
    }
    
    /**
     * 创建参数寄存器映射
     * 
     * 将函数参数映射到寄存器编号
     * PandaASM 约定：参数从寄存器 0 开始依次存放
     * 
     * @param arguments 函数参数列表
     * @param firstReg 第一个参数的起始寄存器编号（默认为0）
     */
    fun setupArgumentRegisters(arguments: List<Argument>, firstReg: Int = 0) {
        for ((index, arg) in arguments.withIndex()) {
            writeRegister(firstReg + index, arg, arg.type)
        }
    }
    
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
 * 
 * 整合寄存器映射、累加器管理和 IR 构建
 */
class SSAConstructionContext(
    val builder: IRBuilder,
    val module: Module,
    val blockMap: Map<Int, BasicBlock> = emptyMap()
) {
    // 寄存器到 SSA 的映射
    val registerMapper = RegisterToSSAMapper()
    
    // 累加器状态
    private var accumulatorValue: Value? = null
    private var accumulatorType: Type = anyType
    
    /**
     * 获取累加器值
     */
    fun getAccumulator(): Value {
        return accumulatorValue ?: throw IllegalStateException("Accumulator not initialized")
    }

    /**
     * 尝试获取累加器值，如果未初始化则返回null
     */
    fun tryGetAccumulator(): Value? {
        return accumulatorValue
    }
    
    /**
     * 设置累加器值
     */
    fun setAccumulator(value: Value, type: Type = value.type) {
        accumulatorValue = value
        accumulatorType = type
    }
    
    /**
     * 检查累加器是否已设置
     */
    fun hasAccumulator(): Boolean = accumulatorValue != null
    
    /**
     * 清除累加器
     */
    fun clearAccumulator() {
        accumulatorValue = null
    }
    
    /**
     * 获取累加器类型
     */
    fun getAccumulatorType(): Type = accumulatorType
    
    /**
     * 从寄存器加载到累加器
     */
    fun loadAccumulatorFromRegister(registerNum: Int) {
        val value = registerMapper.readRegister(registerNum)
        if (value == null) {
            // 如果寄存器未定义，这可能是一个错误或需要特殊的处理逻辑
            // 我们可以创建一个特殊的未定义值或抛出异常
            // 这里我们先抛出异常，因为通常情况下寄存器应该已经被定义了
            throw IllegalStateException("Register v$registerNum not defined. Available registers: ${registerMapper.getUsedRegisters()}")
        }
        setAccumulator(value)
    }
    
    /**
     * 从累加器存储到寄存器
     */
    fun storeAccumulatorToRegister(registerNum: Int) {
        val acc = getAccumulator()
        registerMapper.writeRegister(registerNum, acc, accumulatorType)
    }
    
    /**
     * 设置当前基本块
     */
    fun setCurrentBlock(block: BasicBlock) {
        builder.setInsertPoint(block)
        registerMapper.setCurrentBlock(block)
    }
    
    /**
     * 创建新的基本块并设为当前块
     */
    fun createBlock(name: String = ""): BasicBlock {
        val block = builder.createBlock(name)
        setCurrentBlock(block)
        return block
    }
    
    /**
     * 密封基本块
     */
    fun sealBlock(block: BasicBlock) {
        registerMapper.sealBlock(block)
    }
    
    /**
     * 获取当前基本块
     */
    fun getCurrentBlock(): BasicBlock? = builder.currentBlock

    /**
     * 获取当前函数
     */
    fun getCurrentFunction(): com.orz.reark.core.ir.Function? = builder.currentFunction

    /**
     * 根据字节码偏移量获取预创建的基本块
     */
    fun getBlockByOffset(offset: Int): BasicBlock? = blockMap[offset]
}

/**
 * 基本块之间的寄存器状态传递
 * 
 * 用于处理控制流合并时的寄存器值传递
 */
class RegisterStateTransfer(
    private val mapper: RegisterToSSAMapper
) {
    
    /**
     * 记录分支前的寄存器状态
     */
    fun captureBranchState(): Map<Int, Value> {
        return mapper.snapshot()
    }
    
    /**
     * 在控制流合并处创建 PHI 节点
     * 
     * @param block 合并目标块
     * @param branchStates 各个分支的寄存器状态
     */
    fun mergeBranchStates(
        block: BasicBlock,
        branchStates: List<Pair<BasicBlock, Map<Int, Value>>>
    ) {
        if (branchStates.size < 2) return
        
        // 收集所有分支中定义的所有寄存器
        val allRegisters = branchStates.flatMap { it.second.keys }.toSortedSet()
        
        for (regNum in allRegisters) {
            // 检查所有分支是否都有该寄存器的定义
            val hasInAllBranches = branchStates.all { it.second.containsKey(regNum) }
            
            if (hasInAllBranches) {
                // 所有分支都有定义，创建 PHI 节点
                val values = branchStates.map { it.second[regNum]!! }
                val blocks = branchStates.map { it.first }
                
                // 检查值是否都相同
                if (values.distinct().size == 1) {
                    // 所有值相同，不需要 PHI
                    mapper.writeRegister(regNum, values[0])
                } else {
                    // 创建 PHI 节点
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
    
    /**
     * 简化版本的控制流合并
     * 
     * 用于 if-else 结构
     */
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
