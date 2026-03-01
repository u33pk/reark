package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.Module
import com.orz.reark.core.pass.ModulePass
import com.orz.reark.core.pass.PassResult

/**
 * 字符串池提取 Pass
 *
 * 从 ABC 文件中提取字符串池并存储到 Module 中
 *
 * 使用方法：
 * ```kotlin
 * val passes = listOf(
 *     StringPoolExtractionPass(abc, code),  // 首先运行：提取字符串
 *     StringPoolRegistrationPass(),          // 转换前运行：注册字符串池
 *     ...
 * )
 * ```
 */
class StringPoolExtractionPass(
    private val abc: me.yricky.oh.abcd.AbcBuf,
    private val code: me.yricky.oh.abcd.code.Code
) : ModulePass {

    override val name: String = "stringpool_extract"
    override val description: String = "Extract string pool from ABC file"

    override fun run(module: Module): PassResult {
        val stringPool = com.orz.reark.core.backend.StringPoolExtractor.extract(abc, code)
        module.setStringPool(stringPool)
        return PassResult.Success(stringPool.isNotEmpty())
    }
}

/**
 * 字符串池注册 Pass
 *
 * 从 Module 中读取字符串池并注册到 stringIdMap 中，用于恢复反编译时的原始字符串
 *
 * 使用方法：
 * 1. 先运行 StringPoolExtractionPass 提取字符串到 Module
 * 2. 运行此 Pass 注册字符串池
 * 3. 运行 BytecodeToIRConverter 转换字节码
 *
 * 示例：
 * ```kotlin
 * val passes = listOf(
 *     StringPoolExtractionPass(abc, code),  // 提取字符串
 *     StringPoolRegistrationPass(),          // 注册字符串池
 *     // 然后转换字节码...
 * )
 * ```
 */
class StringPoolRegistrationPass : ModulePass {

    override val name: String = "stringpool"
    override val description: String = "Register string pool for bytecode decompilation"

    override fun run(module: Module): PassResult {
        // 从 Module 中读取字符串池并注册到 stringIdMap
        val stringPool = module.getStringPool()
        stringPool.forEach { (id, value) ->
            module.registerStringMapping("str_$id", value)
        }

        return PassResult.Success(stringPool.isNotEmpty())
    }
}
