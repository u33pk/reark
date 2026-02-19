package com.orz.reark.core.pass

import com.orz.reark.core.ir.BasicBlock
import com.orz.reark.core.ir.Module
import kotlin.reflect.KClass
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * Pass基类 - LLVM风格的Pass模式
 * 
 * Pass是IR转换和分析的基本单元
 */
interface Pass {
    /**
     * Pass名称
     */
    val name: String
    
    /**
     * Pass描述
     */
    val description: String
        get() = ""
}

/**
 * Pass运行结果
 */
sealed class PassResult {
    /**
     * 成功，可能包含修改信息
     */
    data class Success(
        val modified: Boolean = false,
        val message: String = ""
    ) : PassResult()
    
    /**
     * 失败
     */
    data class Failure(
        val reason: String,
        val exception: Throwable? = null
    ) : PassResult()
}

/**
 * 模块级别的Pass - 遍历整个模块
 */
interface ModulePass : Pass {
    /**
     * 在模块上运行Pass
     */
    fun run(module: Module): PassResult
}

/**
 * 函数级别的Pass - 遍历单个函数
 */
interface FunctionPass : Pass {
    /**
     * 在函数上运行Pass
     */
    fun run(function: SSAFunction): PassResult
}

/**
 * 基本块级别的Pass - 遍历基本块
 */
interface BasicBlockPass : Pass {
    /**
     * 在基本块上运行Pass
     */
    fun run(block: BasicBlock): PassResult
}

/**
 * 循环Pass - 遍历循环结构
 */
interface LoopPass : Pass {
    /**
     * 在循环上运行Pass
     */
    fun run(loop: Loop): PassResult
}

/**
 * 分析Pass - 不修改IR，只进行分析
 */
interface AnalysisPass<Result> : Pass {
    /**
     * 运行分析
     */
    fun analyze(module: Module): Result
}

/**
 * 循环信息
 */
class Loop(
    val header: BasicBlock,
    val blocks: List<BasicBlock>,
    val parent: Loop? = null
) {
    private val subLoops = mutableListOf<Loop>()
    
    fun addSubLoop(loop: Loop) {
        subLoops.add(loop)
    }
    
    fun subLoops(): List<Loop> = subLoops.toList()
    
    /**
     * 检查块是否在循环中
     */
    fun contains(block: BasicBlock): Boolean = block in blocks
    
    /**
     * 获取循环深度
     */
    fun getDepth(): Int {
        var depth = 1
        var current = parent
        while (current != null) {
            depth++
            current = current.parent
        }
        return depth
    }
}

/**
 * Pass依赖关系
 */
annotation class RequiresPass(val passClass: KClass<out Pass>)

/**
 * Pass无效化标记
 * 
 * 标记此Pass会修改IR的哪些部分，用于管理分析结果缓存
 */
annotation class Invalidates(
    val analyses: Array<KClass<out AnalysisPass<*>>> = []
)

/**
 * Pass统计信息
 */
class PassStatistics(
    val passName: String,
    var runCount: Int = 0,
    var totalTimeMs: Long = 0,
    var modifiedCount: Int = 0
) {
    val averageTimeMs: Double
        get() = if (runCount > 0) totalTimeMs.toDouble() / runCount else 0.0
    
    override fun toString(): String = 
        "$passName: runs=$runCount, avg=${String.format("%.2f", averageTimeMs)}ms, modified=$modifiedCount"
}
