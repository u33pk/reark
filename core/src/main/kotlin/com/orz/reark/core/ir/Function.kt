package com.orz.reark.core.ir

/**
 * 函数 - SSA IR中的函数表示
 */
class Function(
    val name: String,
    val returnType: Type,
    val isExternal: Boolean = false
) {
    /**
     * 函数唯一ID
     */
    val id: Int = nextId++
    
    /**
     * 所属模块
     */
    var parent: Module? = null
        internal set
    
    /**
     * 参数列表
     */
    private val arguments = mutableListOf<Argument>()
    
    /**
     * 基本块列表
     */
    private val basicBlocks = mutableListOf<BasicBlock>()
    
    /**
     * 入口块
     */
    lateinit var entryBlock: BasicBlock
        private set
    
    /**
     * 局部变量分配（栈分配）
     */
    private val allocas = mutableListOf<AllocaInst>()
    
    // ==================== 参数管理 ====================
    
    /**
     * 添加参数
     */
    fun addArgument(type: Type, name: String = ""): Argument {
        val arg = Argument(type, arguments.size, name)
        arguments.add(arg)
        return arg
    }
    
    /**
     * 获取所有参数
     */
    fun arguments(): List<Argument> = arguments.toList()
    
    /**
     * 获取指定位置的参数
     */
    fun getArgument(index: Int): Argument = arguments[index]
    
    /**
     * 参数数量
     */
    fun argumentCount(): Int = arguments.size
    
    // ==================== 基本块管理 ====================
    
    /**
     * 创建新的基本块
     */
    fun createBlock(name: String = ""): BasicBlock {
        val block = BasicBlock(name, this)
        basicBlocks.add(block)
        
        // 如果是第一个块，设为入口块
        if (!::entryBlock.isInitialized) {
            entryBlock = block
        }
        
        return block
    }
    
    /**
     * 在指定块后添加新块
     */
    fun addBlockAfter(block: BasicBlock, after: BasicBlock) {
        require(block.parent == null) { "Block already has parent" }
        require(after.parent == this) { "Reference block not in this function" }
        
        block.parent = this
        val index = basicBlocks.indexOf(after)
        basicBlocks.add(index + 1, block)
    }
    
    /**
     * 移除基本块
     */
    fun removeBlock(block: BasicBlock) {
        require(block.parent == this) { "Block not in this function" }
        
        basicBlocks.remove(block)
        block.parent = null
        
        // 清理前驱后继关系
        block.successors().forEach { it.removePredecessor(block) }
        block.predecessors().forEach { it.removeSuccessor(block) }
    }
    
    /**
     * 获取所有基本块
     */
    fun blocks(): List<BasicBlock> = basicBlocks.toList()
    
    /**
     * 基本块数量
     */
    fun blockCount(): Int = basicBlocks.size
    
    /**
     * 遍历所有指令
     */
    fun instructions(): Sequence<Instruction> = 
        basicBlocks.asSequence().flatMap { it.instructions() }
    
    /**
     * 获取所有局部变量分配
     */
    fun allocas(): List<AllocaInst> = allocas.toList()
    
    /**
     * 添加局部变量分配
     */
    internal fun addAlloca(alloca: AllocaInst) {
        allocas.add(alloca)
    }
    
    // ==================== 函数属性 ====================
    
    /**
     * 检查函数是否已定义（有基本块）
     */
    fun isDefined(): Boolean = basicBlocks.isNotEmpty()
    
    /**
     * 检查函数是否已声明但外部定义
     */
    fun isDeclaration(): Boolean = isExternal || !isDefined()
    
    /**
     * 获取函数类型
     */
    fun getFunctionType(): Type.FunctionType {
        return Type.FunctionType(
            returnType,
            arguments.map { it.type }
        )
    }
    
    // ==================== 分析辅助 ====================
    
    /**
     * 深度优先遍历基本块
     */
    fun dfsBlocks(): Sequence<BasicBlock> = sequence {
        val visited = mutableSetOf<BasicBlock>()
        val stack = ArrayDeque<BasicBlock>()
        
        if (::entryBlock.isInitialized) {
            stack.add(entryBlock)
        }
        
        while (stack.isNotEmpty()) {
            val block = stack.removeLast()
            if (block in visited) continue
            
            visited.add(block)
            yield(block)
            
            // 添加后继（逆序以保持某种一致性）
            block.successors().reversed().forEach { stack.add(it) }
        }
    }
    
    /**
     * 逆后序遍历基本块（RPO）- 常用于数据流分析
     */
    fun rpoBlocks(): Sequence<BasicBlock> = 
        dfsBlocks().toList().reversed().asSequence()
    
    /**
     * 获取支配树信息（简化版，实际实现需要单独Pass）
     */
    fun getDominatorInfo(): DominatorInfo {
        return DominatorInfo(this)
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 验证函数结构
     */
    fun verify(): Boolean {
        // 检查是否有入口块
        if (!::entryBlock.isInitialized) {
            return false
        }
        
        // 检查所有基本块是否都有终止指令
        for (block in basicBlocks) {
            if (!block.isTerminated()) {
                return false
            }
        }
        
        // 检查PHI节点的incoming blocks数量与前驱一致
        for (block in basicBlocks) {
            for (phi in block.phis()) {
                if (phi.incomingCount() != block.predecessorCount()) {
                    return false
                }
            }
        }
        
        return true
    }
    
    override fun toString(): String {
        val params = arguments.joinToString(", ") { "${it.type} ${it.name}" }
        val header = "define $returnType @$name($params)"
        
        if (isDeclaration()) {
            return "$header;"
        }
        
        val body = basicBlocks.joinToString("\n") { it.toString() }
        return "$header {\n$body\n}"
    }
    
    companion object {
        private var nextId = 0
        
        fun resetIdCounter() {
            nextId = 0
        }
    }
}

/**
 * 支配树信息
 */
class DominatorInfo(val function: Function) {
    private val dominators = mutableMapOf<BasicBlock, MutableSet<BasicBlock>>()
    private val immediateDominator = mutableMapOf<BasicBlock, BasicBlock?>()
    private val dominanceFrontier = mutableMapOf<BasicBlock, MutableSet<BasicBlock>>()
    
    init {
        computeDominators()
        computeImmediateDominators()
        computeDominanceFrontier()
    }
    
    /**
     * 计算支配集
     */
    private fun computeDominators() {
        val blocks = function.blocks()
        val entry = function.entryBlock
        
        // 初始化
        blocks.forEach { block ->
            dominators[block] = if (block == entry) {
                mutableSetOf(entry)
            } else {
                blocks.toMutableSet()
            }
        }
        
        // 迭代计算
        var changed = true
        while (changed) {
            changed = false
            for (block in blocks) {
                if (block == entry) continue
                
                val preds = block.predecessors()
                if (preds.isEmpty()) continue
                
                val newDom = preds.map { dominators[it]!! }.reduce { acc, set ->
                    acc.intersect(set).toMutableSet()
                }
                newDom.add(block)
                
                if (newDom != dominators[block]) {
                    dominators[block] = newDom
                    changed = true
                }
            }
        }
    }
    
    /**
     * 计算直接支配者
     */
    private fun computeImmediateDominators() {
        val entry = function.entryBlock
        immediateDominator[entry] = null
        
        for (block in function.blocks()) {
            if (block == entry) continue
            
            val doms = dominators[block]!! - block
            immediateDominator[block] = doms.find { dom ->
                doms.all { other -> other == dom || dom !in dominators[other]!! }
            }
        }
    }
    
    /**
     * 计算支配边界
     */
    private fun computeDominanceFrontier() {
        function.blocks().forEach { 
            dominanceFrontier[it] = mutableSetOf() 
        }
        
        for (block in function.blocks()) {
            val preds = block.predecessors()
            if (preds.size >= 2) {
                for (pred in preds) {
                    var runner = pred
                    val idom = immediateDominator[block]
                    while (runner != idom && runner != null) {
                        dominanceFrontier[runner]!!.add(block)
                        runner = immediateDominator[runner] ?: break
                    }
                }
            }
        }
    }
    
    /**
     * 获取块的支配集
     */
    fun getDominators(block: BasicBlock): Set<BasicBlock> = 
        dominators[block] ?: emptySet()
    
    /**
     * 获取直接支配者
     */
    fun getImmediateDominator(block: BasicBlock): BasicBlock? = 
        immediateDominator[block]
    
    /**
     * 获取支配边界
     */
    fun getDominanceFrontier(block: BasicBlock): Set<BasicBlock> = 
        dominanceFrontier[block] ?: emptySet()
    
    /**
     * 检查block1是否支配block2
     */
    fun dominates(block1: BasicBlock, block2: BasicBlock): Boolean =
        block1 in getDominators(block2)
    
    /**
     * 检查block1是否严格支配block2
     */
    fun strictlyDominates(block1: BasicBlock, block2: BasicBlock): Boolean =
        block1 != block2 && dominates(block1, block2)
}
