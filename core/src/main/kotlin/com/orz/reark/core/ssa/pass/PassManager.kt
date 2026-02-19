package com.orz.reark.core.ssa.pass

import com.orz.reark.core.ssa.ir.*
import com.orz.reark.core.ssa.ir.Function as SSAFunction
import com.orz.reark.core.ssa.transform.DeadCodeElimination
import com.orz.reark.core.ssa.transform.ConstantFolding
import com.orz.reark.core.ssa.transform.SimplifyCFG

/**
 * Pass管理器 - 管理和执行Pass的基础设施
 * 
 * 借鉴LLVM的PassManager设计，支持：
 * - Pass注册和调度
 * - 分析结果缓存和无效化
 * - Pass依赖管理
 * - 统计信息收集
 */
class PassManager {
    /**
     * 模块级别Pass管道
     */
    private val modulePasses = mutableListOf<ModulePass>()
    
    /**
     * 函数级别Pass管道
     */
    private val functionPasses = mutableListOf<FunctionPass>()
    
    /**
     * 分析结果缓存
     */
    private val analysisCache = mutableMapOf<String, Any>()
    
    /**
     * Pass统计信息
     */
    private val statistics = mutableMapOf<String, PassStatistics>()
    
    /**
     * 是否启用调试输出
     */
    var debug = false
    
    /**
     * 是否收集统计信息
     */
    var collectStatistics = false
    
    // ==================== Pass注册 ====================
    
    /**
     * 添加模块Pass
     */
    fun addPass(pass: ModulePass): PassManager {
        modulePasses.add(pass)
        return this
    }
    
    /**
     * 添加函数Pass（会被应用到所有函数）
     */
    fun addPass(pass: FunctionPass): PassManager {
        functionPasses.add(pass)
        return this
    }
    
    /**
     * 添加多个Pass
     */
    fun addPasses(vararg passes: Pass): PassManager {
        passes.forEach {
            when (it) {
                is ModulePass -> modulePasses.add(it)
                is FunctionPass -> functionPasses.add(it)
            }
        }
        return this
    }
    
    /**
     * 清空所有Pass
     */
    fun clear() {
        modulePasses.clear()
        functionPasses.clear()
    }
    
    // ==================== Pass执行 ====================
    
    /**
     * 在模块上运行所有Pass
     */
    fun run(module: Module): Boolean {
        if (debug) {
            println("; Running PassManager on module: ${module.name}")
        }
        
        // 首先运行模块级别Pass
        for (pass in modulePasses) {
            if (!runModulePass(pass, module)) {
                return false
            }
        }
        
        // 然后对每个函数运行函数级别Pass
        if (functionPasses.isNotEmpty()) {
            // 收集所有已定义的函数
            val functions = module.definedFunctions().toList()
            
            for (pass in functionPasses) {
                for (function in functions) {
                    if (!runFunctionPass(pass, function)) {
                        return false
                    }
                }
            }
        }
        
        if (debug) {
            println("; PassManager finished")
            printStatistics()
        }
        
        return true
    }
    
    /**
     * 运行单个模块Pass
     */
    private fun runModulePass(pass: ModulePass, module: Module): Boolean {
        val startTime = System.currentTimeMillis()
        
        if (debug) {
            println("; Running module pass: ${pass.name}")
        }
        
        val result = try {
            pass.run(module)
        } catch (e: Exception) {
            System.err.println("Pass ${pass.name} failed with exception: ${e.message}")
            e.printStackTrace()
            PassResult.Failure("Exception: ${e.message}", e)
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        updateStatistics(pass.name, elapsed, result)
        
        return when (result) {
            is PassResult.Success -> {
                if (debug && result.modified) {
                    println(";  -> Modified module")
                }
                true
            }
            is PassResult.Failure -> {
                System.err.println("Pass ${pass.name} failed: ${result.reason}")
                false
            }
        }
    }
    
    /**
     * 运行单个函数Pass
     */
    private fun runFunctionPass(pass: FunctionPass, function: SSAFunction): Boolean {
        val startTime = System.currentTimeMillis()
        
        if (debug) {
            println("; Running function pass: ${pass.name} on ${function.name}")
        }
        
        val result = try {
            pass.run(function)
        } catch (e: Exception) {
            System.err.println("Pass ${pass.name} failed on ${function.name}: ${e.message}")
            e.printStackTrace()
            PassResult.Failure("Exception: ${e.message}", e)
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        updateStatistics(pass.name, elapsed, result)
        
        return when (result) {
            is PassResult.Success -> true
            is PassResult.Failure -> {
                System.err.println("Pass ${pass.name} failed on ${function.name}: ${result.reason}")
                false
            }
        }
    }
    
    // ==================== 分析结果管理 ====================
    
    /**
     * 获取分析结果（如果已缓存则返回缓存，否则重新计算）
     */
    @Suppress("UNCHECKED_CAST")
    fun <Result> getAnalysis(
        module: Module,
        analysisClass: Class<out AnalysisPass<Result>>
    ): Result {
        val key = analysisClass.name
        
        @Suppress("UNCHECKED_CAST")
        val cached = analysisCache[key] as? Result
        if (cached != null) {
            return cached
        }
        
        // 创建并运行分析
        val analysis = analysisClass.getDeclaredConstructor().newInstance()
        val result = analysis.analyze(module)
        analysisCache[key] = result as Any
        
        return result
    }
    
    /**
     * 获取分析结果（带类型推断）
     */
    inline fun <reified Result> getAnalysis(module: Module): Result {
        @Suppress("UNCHECKED_CAST")
        return getAnalysis(module, Result::class.java as Class<AnalysisPass<Result>>)
    }
    
    /**
     * 使分析结果无效
     */
    fun invalidateAnalysis(analysisClass: Class<out AnalysisPass<*>>) {
        analysisCache.remove(analysisClass.name)
    }
    
    /**
     * 清空所有分析缓存
     */
    fun invalidateAllAnalyses() {
        analysisCache.clear()
    }
    
    // ==================== 统计信息 ====================
    
    private fun updateStatistics(passName: String, timeMs: Long, result: PassResult) {
        if (!collectStatistics) return
        
        val stats = statistics.getOrPut(passName) { 
            PassStatistics(passName) 
        }
        
        stats.runCount++
        stats.totalTimeMs += timeMs
        if (result is PassResult.Success && result.modified) {
            stats.modifiedCount++
        }
    }
    
    /**
     * 打印统计信息
     */
    fun printStatistics() {
        if (statistics.isEmpty()) return
        
        println("; Pass Statistics:")
        statistics.values.sortedBy { it.passName }.forEach {
            println(";   $it")
        }
    }
    
    /**
     * 获取所有统计信息
     */
    fun getStatistics(): Collection<PassStatistics> = statistics.values
}

/**
 * FunctionPassManager - 专门为单个函数运行的Pass管理器
 */
class FunctionPassManager {
    private val passes = mutableListOf<FunctionPass>()
    var debug = false
    
    /**
     * 添加Pass
     */
    fun addPass(pass: FunctionPass): FunctionPassManager {
        passes.add(pass)
        return this
    }
    
    /**
     * 在函数上运行所有Pass
     */
    fun run(function: SSAFunction): Boolean {
        if (debug) {
            println("; Running FunctionPassManager on ${function.name}")
        }
        
        for (pass in passes) {
            if (debug) {
                println(";  -> Running: ${pass.name}")
            }
            
            val result = try {
                pass.run(function)
            } catch (e: Exception) {
                System.err.println("Pass ${pass.name} failed: ${e.message}")
                e.printStackTrace()
                PassResult.Failure("Exception: ${e.message}", e)
            }
            
            when (result) {
                is PassResult.Success -> {
                    if (debug && result.modified) {
                        println(";     -> Modified")
                    }
                }
                is PassResult.Failure -> {
                    System.err.println("Pass ${pass.name} failed: ${result.reason}")
                    return false
                }
            }
        }
        
        return true
    }
}

/**
 * Pass管道构建器 - 便捷构建Pass管道
 */
class PassPipelineBuilder {
    private val manager = PassManager()
    
    fun addPass(pass: Pass): PassPipelineBuilder {
        when (pass) {
            is ModulePass -> manager.addPass(pass)
            is FunctionPass -> manager.addPass(pass)
        }
        return this
    }
    
    fun enableDebug(): PassPipelineBuilder {
        manager.debug = true
        return this
    }
    
    fun enableStatistics(): PassPipelineBuilder {
        manager.collectStatistics = true
        return this
    }
    
    fun build(): PassManager = manager
}

/**
 * 标准优化管道
 */
object StandardPasses {
    
    /**
     * 创建标准优化管道
     */
    fun createOptimizationPipeline(): PassManager {
        return PassPipelineBuilder()
            .addPass(DeadCodeElimination())
            .addPass(ConstantFolding())
            .addPass(SimplifyCFG())
            .enableStatistics()
            .build()
    }
    
    /**
     * 创建仅分析的管道
     */
    fun createAnalysisPipeline(): PassManager {
        return PassManager()
    }
}
