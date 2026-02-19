package com.orz.reark.core.ssa.ir

/**
 * 基本块 (Basic Block) - SSA控制流图中的节点
 * 
 * 基本块是单入口单出口的指令序列
 */
class BasicBlock(
    val name: String = "",
    var parent: Function? = null
) {
    /**
     * 块的唯一ID
     */
    val id: Int = nextId++
    
    /**
     * 第一条指令
     */
    private var head: Instruction? = null
    
    /**
     * 最后一条指令
     */
    private var tail: Instruction? = null
    
    /**
     * 前驱基本块列表
     */
    private val predecessors = mutableListOf<BasicBlock>()
    
    /**
     * 后继基本块列表
     */
    private val successors = mutableListOf<BasicBlock>()
    
    // ==================== 迭代器 ====================
    
    /**
     * 指令迭代器
     */
    fun instructions(): Sequence<Instruction> = sequence {
        var current = head
        while (current != null) {
            yield(current)
            current = current.next
        }
    }
    
    /**
     * 是否为空（无指令）
     */
    fun isEmpty(): Boolean = head == null
    
    /**
     * 指令数量
     */
    fun size(): Int = instructions().count()
    
    // ==================== 访问器 ====================
    
    /**
     * 获取第一条指令
     */
    fun first(): Instruction? = head
    
    /**
     * 获取第一条非PHI指令
     */
    fun firstNonPhi(): Instruction? {
        var current = head
        while (current is PhiInst) {
            current = current.next
        }
        return current
    }
    
    /**
     * 获取最后一条指令
     */
    fun last(): Instruction? = tail
    
    /**
     * 获取终止指令（如果存在）
     */
    fun terminator(): Instruction? = tail?.takeIf { it.isTerminator() }
    
    /**
     * 获取PHI节点列表
     */
    fun phis(): List<PhiInst> = 
        instructions().takeWhile { it is PhiInst }.map { it as PhiInst }.toList()
    
    // ==================== 前驱后继管理 ====================
    
    /**
     * 获取所有前驱
     */
    fun predecessors(): List<BasicBlock> = predecessors.toList()
    
    /**
     * 获取所有后继
     */
    fun successors(): List<BasicBlock> = successors.toList()
    
    /**
     * 前驱数量
     */
    fun predecessorCount(): Int = predecessors.size
    
    /**
     * 后继数量
     */
    fun successorCount(): Int = successors.size
    
    /**
     * 是否有前驱
     */
    fun hasPredecessors(): Boolean = predecessors.isNotEmpty()
    
    /**
     * 是否有后继
     */
    fun hasSuccessors(): Boolean = successors.isNotEmpty()
    
    /**
     * 是否为入口块
     */
    fun isEntryBlock(): Boolean = parent?.entryBlock == this
    
    /**
     * 是否为终止块（有终止指令）
     */
    fun isTerminated(): Boolean = terminator() != null
    
    /**
     * 添加前驱
     */
    internal fun addPredecessor(pred: BasicBlock) {
        if (pred !in predecessors) {
            predecessors.add(pred)
        }
    }
    
    /**
     * 移除前驱
     */
    internal fun removePredecessor(pred: BasicBlock) {
        predecessors.remove(pred)
        // 更新PHI节点
        phis().forEach { phi ->
            phi.getIncomingBlocks().indexOf(pred).takeIf { it >= 0 }?.let { index ->
                // 注意：这里简化处理，实际可能需要更复杂的PHI更新逻辑
            }
        }
    }
    
    /**
     * 添加后继
     */
    internal fun addSuccessor(succ: BasicBlock) {
        if (succ !in successors) {
            successors.add(succ)
            succ.addPredecessor(this)
        }
    }
    
    /**
     * 移除后继
     */
    internal fun removeSuccessor(succ: BasicBlock) {
        successors.remove(succ)
        succ.removePredecessor(this)
    }
    
    /**
     * 替换后继
     */
    fun replaceSuccessor(oldSucc: BasicBlock, newSucc: BasicBlock) {
        val index = successors.indexOf(oldSucc)
        if (index >= 0) {
            successors[index] = newSucc
            oldSucc.removePredecessor(this)
            newSucc.addPredecessor(this)
        }
    }
    
    /**
     * 替换前驱
     */
    fun replacePredecessor(oldPred: BasicBlock, newPred: BasicBlock) {
        val index = predecessors.indexOf(oldPred)
        if (index >= 0) {
            predecessors[index] = newPred
            // 更新PHI节点
            phis().forEach { phi ->
                phi.getIncomingBlocks().indexOf(oldPred).takeIf { it >= 0 }?.let { idx ->
                    // 简化处理
                }
            }
        }
    }
    
    // ==================== 指令插入 ====================
    
    /**
     * 在末尾插入指令
     */
    fun append(inst: Instruction) {
        require(inst.parent == null) { "Instruction already in a block" }
        
        inst.parent = this
        
        if (tail == null) {
            head = inst
            tail = inst
            inst.prev = null
            inst.next = null
        } else {
            inst.prev = tail
            inst.next = null
            tail!!.next = inst
            tail = inst
        }
    }
    
    /**
     * 在开头插入指令（PHI节点之前）
     */
    fun prepend(inst: Instruction) {
        require(inst.parent == null) { "Instruction already in a block" }
        
        inst.parent = this
        
        if (head == null) {
            head = inst
            tail = inst
            inst.prev = null
            inst.next = null
        } else {
            inst.prev = null
            inst.next = head
            head!!.prev = inst
            head = inst
        }
    }
    
    /**
     * 在指定指令之前插入
     */
    fun insertBefore(inst: Instruction, before: Instruction) {
        require(inst.parent == null) { "Instruction already in a block" }
        require(before.parent == this) { "Reference instruction not in this block" }
        
        inst.parent = this
        inst.next = before
        inst.prev = before.prev
        
        before.prev?.next = inst
        before.prev = inst
        
        if (before == head) {
            head = inst
        }
    }
    
    /**
     * 在指定指令之后插入
     */
    fun insertAfter(inst: Instruction, after: Instruction) {
        require(inst.parent == null) { "Instruction already in a block" }
        require(after.parent == this) { "Reference instruction not in this block" }
        
        inst.parent = this
        inst.prev = after
        inst.next = after.next
        
        after.next?.prev = inst
        after.next = inst
        
        if (after == tail) {
            tail = inst
        }
    }
    
    /**
     * 移除指令
     */
    fun remove(inst: Instruction) {
        require(inst.parent == this) { "Instruction not in this block" }
        
        if (inst.prev != null) {
            inst.prev!!.next = inst.next
        } else {
            head = inst.next
        }
        
        if (inst.next != null) {
            inst.next!!.prev = inst.prev
        } else {
            tail = inst.prev
        }
        
        inst.parent = null
        inst.prev = null
        inst.next = null
    }
    
    /**
     * 移除并删除指令
     */
    fun erase(inst: Instruction) {
        remove(inst)
        inst.dropOperands()
    }
    
    /**
     * 合并另一个块的所有指令到当前块末尾
     */
    fun merge(other: BasicBlock) {
        other.instructions().toList().forEach { inst ->
            other.remove(inst)
            append(inst)
        }
        
        // 更新前驱后继关系
        other.successors().forEach { removeSuccessor(it) }
        other.predecessors().forEach { it.removeSuccessor(other) }
    }
    
    /**
     * 分离此块（从函数中移除）
     */
    fun detachFromParent() {
        parent?.removeBlock(this)
    }
    
    /**
     * 分割块（在指定指令处分割）
     */
    fun split(at: Instruction, name: String = ""): BasicBlock {
        require(at.parent == this) { "Instruction not in this block" }
        
        val newBlock = BasicBlock(name, parent)
        
        // 移动at及其后的所有指令到新块
        var current: Instruction? = at
        while (current != null) {
            val next = current.next
            remove(current)
            newBlock.append(current)
            current = next
        }
        
        // 转移后继关系
        val succsCopy = successors.toList()
        successors.clear()
        succsCopy.forEach { succ ->
            removeSuccessor(succ)
            newBlock.addSuccessor(succ)
        }
        
        // 添加当前块到新块的边
        addSuccessor(newBlock)
        
        // 添加到父函数
        parent?.addBlockAfter(newBlock, this)
        
        return newBlock
    }
    
    // ==================== PHI节点管理 ====================
    
    /**
     * 创建PHI节点
     */
    fun createPhi(type: Type, name: String = ""): PhiInst {
        val phi = PhiInst(type, name)
        // PHI节点必须放在最前面
        val firstNonPhi = firstNonPhi()
        if (firstNonPhi != null) {
            insertBefore(phi, firstNonPhi)
        } else {
            append(phi)
        }
        return phi
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取名称（如果未指定使用临时名）
     */
    fun getNameOrTemporary(): String = name.ifEmpty { "bb$id" }
    
    override fun toString(): String = "${getNameOrTemporary()}:\n" +
        instructions().joinToString("\n") { "  $it" }
    
    companion object {
        private var nextId = 0
        
        fun resetIdCounter() {
            nextId = 0
        }
    }
}
