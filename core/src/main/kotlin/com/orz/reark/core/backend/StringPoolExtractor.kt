package com.orz.reark.core.backend

import me.yricky.oh.abcd.AbcBuf
import me.yricky.oh.abcd.code.Code
import me.yricky.oh.abcd.isa.literalArrays
import me.yricky.oh.abcd.literal.LiteralArray
import me.yricky.oh.common.value

/**
 * 字符串池提取器
 *
 * 从 ABC 文件的 Code 中提取字符串池，用于字节码反编译时恢复原始字符串
 */
object StringPoolExtractor {

    /**
     * 从 Code 中提取字符串池
     *
     * @param abc ABC 文件对象
     * @param code 方法代码对象
     * @return Map<stringId, stringValue> 字符串 ID 到字符串值的映射
     */
    fun extract(abc: AbcBuf, code: Code): Map<Int, String> {
        val stringPool = mutableMapOf<Int, String>()

        // 1. 从 literalArray 中提取字符串（用于 defineclasswithbuffer 等指令）
        code.asm.list.forEach { asmItem ->
            asmItem.literalArrays.forEach { literalArray ->
                literalArray.content.forEachIndexed { index, literal ->
                    if (literal is LiteralArray.Literal.Str) {
                        val strValue = literal.get(abc)
                        val strOffset = literal.offset
                        stringPool[index] = strValue
                    }
                }
            }
        }

        // 2. 从方法所在的 region 的 mslIndex 中获取字符串列表
        // mslIndex 包含 method/string/literal 的偏移量
        val method = code.method
        val region = method.region
        val mslIndex = region.mslIndex

        // 遍历 mslIndex，尝试获取所有字符串
        // 注意：mslIndex 中包含 method、string 和 literal 的偏移量，需要过滤出字符串
        mslIndex.forEachIndexed { index, strOffset ->
            try {
                val strData = abc.stringItem(strOffset)
                val strValue = strData.value
                stringPool[index] = strValue
            } catch (e: Exception) {
                // 不是有效的字符串偏移量，跳过
            }
        }

        return stringPool
    }

    /**
     * 从 Code 中提取字符串池（带调试信息）
     *
     * @param abc ABC 文件对象
     * @param code 方法代码对象
     * @param debug 是否输出调试信息
     * @return Map<stringId, stringValue> 字符串 ID 到字符串值的映射
     */
    fun extractWithDebug(abc: AbcBuf, code: Code, debug: Boolean = true): Map<Int, String> {
        val stringPool = mutableMapOf<Int, String>()

        // 1. 从 literalArray 中提取字符串
        code.asm.list.forEach { asmItem ->
            asmItem.literalArrays.forEach { literalArray ->
                literalArray.content.forEachIndexed { index, literal ->
                    if (literal is LiteralArray.Literal.Str) {
                        val strValue = literal.get(abc)
                        val strOffset = literal.offset
                        stringPool[index] = strValue
                        if (debug) {
                            println("DEBUG: literalArray str[$index] = \"$strValue\" (offset: 0x${strOffset.toString(16)})")
                        }
                    }
                }
            }
        }

        // 2. 从方法所在的 region 的 mslIndex 中获取字符串列表
        val method = code.method
        val region = method.region
        val mslIndex = region.mslIndex

        if (debug) {
            println("DEBUG: Region mslIndex size = ${mslIndex.size}")
        }

        // 遍历 mslIndex，尝试获取所有字符串
        mslIndex.forEachIndexed { index, strOffset ->
            try {
                val strData = abc.stringItem(strOffset)
                val strValue = strData.value
                stringPool[index] = strValue
                if (debug) {
                    println("DEBUG: mslIndex str[$index] = \"$strValue\" (offset: 0x${strOffset.toString(16)})")
                }
            } catch (e: Exception) {
                if (debug) {
                    println("DEBUG: mslIndex[$index] = 0x${strOffset.toString(16)} is not a valid string")
                }
            }
        }

        return stringPool
    }
}
