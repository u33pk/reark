package com.orz.reark.core.graph

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.yricky.oh.abcd.isa.Asm.AsmItem
import me.yricky.oh.abcd.isa.util.ExternModuleParser
import me.yricky.oh.abcd.isa.util.V2AInstParser
import me.yricky.oh.abcd.isa.Asm
import me.yricky.oh.abcd.isa.asmArgs
import me.yricky.oh.abcd.isa.asmName


class BaseBlockControlFlow(
    val asms: List<AsmItem>
){
    companion object{
        val operandParser = listOf(V2AInstParser, ExternModuleParser)
//        val operandParser2 = listOf(V2AInstParser, ExternModuleParser)
    }

    val baseBlockCF : MutableValueGraph<BaseBlock, Boolean> = ValueGraphBuilder
        .directed()
        .allowsSelfLoops(true)
        .build()

    val baseBlockOffAsm by lazy {
        val off2Asm: HashMap<Number, AsmItem> = HashMap()
        asms.forEach {
            off2Asm.put(it.codeOffset, it)
        }
        off2Asm
    }
    val baseBlockPath: HashMap<Number, AsmItem> = HashMap()
    val baseBlocks by lazy {
        li
    }
    val revBaseblocks by lazy {
        var rev = HashMap<BaseBlock, Int>()
        baseBlocks.withIndex().forEach { (idx, bbk) ->
            rev.put(bbk, idx)
        }
        rev
    }

    fun findBBKbyAsm(asmItem: AsmItem): BaseBlock?{
        baseBlocks.forEach { bbk ->
            if(asmItem in bbk.li)
                return bbk
        }
        return null
    }
    //    val basicBlockMap = mutableMapOf<Int, BaseBlock>()
    val basicBlockMap by lazy {
        val bbkMap = mutableMapOf<Int, BaseBlock>()
        li.forEach { bbk ->
            bbkMap.put(bbk.offset,bbk)
        }
        bbkMap
    }
    val li by lazy { generaStep1() }


    // 获取正确的基本块列表 还没有连接信息
    fun generaStep1() : ArrayList<BaseBlock>{
        val start_flags = ArrayList<Number>()
        val end_flags = ArrayList<Number>()
        val bbkli = ArrayList<BaseBlock>()
        var next_end_flag = false
        start_flags.add(0)
        end_flags.add(asms.last().codeOffset)
        asms.forEach { _asm ->
            if(next_end_flag) {
                start_flags.add(_asm.codeOffset)
                next_end_flag = false
            }
            if(_asm.bbk_type == "terminal" || _asm.bbk_type == "jump") {
                next_end_flag = true
            }
            if(_asm.bbk_type == "jump"){
                _asm.asmArgs(operandParser).forEach { (index,argString) ->
                    if(argString != null) {
                        start_flags.add(_asm.codeOffset + argString.toInt())
                    }
                }
            }
        }
        next_end_flag = false
        var bbk = BaseBlock()
        var pOff = 0
        asms.forEach { _asm ->
            if(_asm.codeOffset in start_flags){
                if(pOff != 0)
                    end_flags.add(pOff)
            }
            pOff = _asm.codeOffset
        }
        asms.forEach { _asm ->
            if(_asm.codeOffset in start_flags){
                bbk = BaseBlock()
                if(pOff != 0)
                    end_flags.add(pOff)
                if (_asm.codeOffset == 0) bbk.isStart = true
            }
            bbk.addAsm(_asm)
            if(_asm.codeOffset in end_flags){
                bbkli.add(bbk)
//                bbkMap.put(bbk.offset,bbk)
            }
        }
        return bbkli
    }

    //生成 base block path
    fun generaStep2() {
        asms.forEach { _asm ->
            if(_asm.bbk_type != "jump"){
                baseBlockOffAsm.get(_asm.nextOffset)?.let { next_asm ->
                    baseBlockPath.put(_asm.codeOffset, next_asm)
                }
            }
            else {
                _asm.asmArgs(operandParser).forEach { (index, argString) ->
                    if (index > 0) {
                        argString?.toInt()?.let {
                            baseBlockOffAsm.get(_asm.codeOffset + it)?.let { next_asm ->
                                baseBlockPath.put(_asm.codeOffset, next_asm)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 生成有向图
     */
    val conditionJmp = arrayOf("jeqz", "jnez")
    val directJmp = arrayOf("jmp")
    val returnAsm = arrayOf("return", "returnundefined")

    fun generaStep3(){
        basicBlockMap.forEach{(_off, bbk) ->
            baseBlockCF.addNode(bbk)
            val terminator = bbk.getTermiinator()
            if(terminator.asmName in conditionJmp){//条件跳转
                val dest = terminator.codeOffset + terminator.opUnits[1].toInt()
                val fall = terminator.nextOffset
                val destNode = basicBlockMap.get(dest)
                val fallNode = basicBlockMap.get(fall)
                baseBlockCF.addNode(destNode)
                baseBlockCF.addNode(fallNode)
                baseBlockCF.putEdgeValue(bbk,destNode,false)
                baseBlockCF.putEdgeValue(bbk,fallNode,true)
            }else if(terminator.asmName in directJmp){//直接跳转
                val dest = terminator.codeOffset + terminator.opUnits[1].toInt()
                val destNode = basicBlockMap.get(dest)
                baseBlockCF.addNode(destNode)
                baseBlockCF.putEdgeValue(bbk,destNode,true)
            }else if(terminator.asmName in returnAsm){
                //返回语句，无fallNode，无须处理
            }else{//其他情况，顺序执行
                val fall = terminator.nextOffset
                //若当前块是最后一个指令，则不添加fallNode
                if(fall < basicBlockMap.entries.last().value.getTermiinator().codeOffset){
                    val fallNode = basicBlockMap.get(fall)
                    baseBlockCF.addNode(fallNode)
                    baseBlockCF.putEdgeValue(bbk,fallNode,true)
                }
            }
        }
    }

    data class StructType(
        var IF:Boolean = false,
        var ELSE:Boolean = false,
        var LOOP:Boolean = false,
        var LOOP_END:Boolean = false,
        var END:Int = 0
    )

    class BaseBlock {
        val li = ArrayList<AsmItem>()
        var name = "Normal"
        var entry = false
        var struct = StructType()
        var isStart = false
        val offset: Int
            get() {
                return if(li.size > 0)
                    li[0].codeOffset
                else -1
            }

        fun getTermiinator(): Asm.AsmItem{
            return li.last()
        }
        fun toJson(): JsonObject {
            val bbkJson = JsonObject()
            bbkJson.addProperty("offset", offset)
            val itemsJson = JsonArray()
            this.li.forEach {
                itemsJson.add(it.toJson())
            }
            bbkJson.add("asm", itemsJson)
            return bbkJson
        }
        val offStr: String get() = offset.toString()
        fun addAsm(asm: AsmItem): Boolean {
            if(asm.asmName.startsWith("return"))
                name = "Final"
            else if(asm.asmName == "jmp")
                name = "Jmp"
            li.add(asm)
            return asm.bbk_type == "terminal" || asm.bbk_type == "jump"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BaseBlock) return false
            return offset == other.offset
        }
        operator fun compareTo(other: BaseBlock): Int {
            if(this === other) return 0
            return offset - other.offset
        }

        override fun hashCode(): Int {
            return offset
        }

        override fun toString(): String {
            val bbksb = StringBuilder()
            this.li.forEach {
                bbksb.append("    ${it.codeOffset}  ${it}\n")
//                println(it.pseudoString)
            }
            return bbksb.toString()
        }
        fun toString(all: Boolean): String{
            if(all)
                return this.toString()
            else{
                val bbksb = StringBuilder()
                if(this.struct.ELSE)bbksb.append("} else {\n")
                this.li.forEach {
                    val _code = it.toString().split("# ")[1]
                    if(_code.indexOf("---") == -1){
                        bbksb.append("    ${_code}\n")
                    }
                }
                if(this.struct.LOOP){
                    this.struct.IF = false
                    val condition = li[li.size-2].toString().split("acc = ")[1]
                    bbksb.append("while(${condition}){\n")
                }
                if(this.struct.IF){
                    val condition = li[li.size-2].toString().split("acc = ")[1]
                    bbksb.append("if(${condition}){\n")
                }

                repeat(this.struct.END){
                    bbksb.append("}\n")
                }
                return bbksb.toString()
            }
        }
    }
}