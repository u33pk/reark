package com.orz.reark.core.ssa.ir

/**
 * 模块 - 包含多个函数的编译单元
 */
class Module(
    val name: String = "module"
) {
    /**
     * 模块中的函数列表
     */
    private val functions = mutableListOf<Function>()
    
    /**
     * 全局变量列表
     */
    private val globalVariables = mutableMapOf<String, GlobalValue>()
    
    /**
     * 字符串常量池
     */
    private val stringConstants = mutableMapOf<String, ConstantString>()
    
    /**
     * 命名类型定义
     */
    private val namedTypes = mutableMapOf<String, Type>()
    
    /**
     * 模块级别的元数据
     */
    private val metadata = mutableMapOf<String, Any>()
    
    // ==================== 函数管理 ====================
    
    /**
     * 创建函数
     */
    fun createFunction(
        name: String,
        returnType: Type = voidType,
        isExternal: Boolean = false
    ): Function {
        require(getFunction(name) == null) { "Function $name already exists" }
        
        val func = Function(name, returnType, isExternal)
        func.parent = this
        functions.add(func)
        return func
    }
    
    /**
     * 获取函数
     */
    fun getFunction(name: String): Function? = 
        functions.find { it.name == name }
    
    /**
     * 获取或创建外部函数声明
     */
    fun getOrDeclareFunction(
        name: String,
        returnType: Type = anyType,
        paramTypes: List<Type> = emptyList()
    ): Function {
        return getFunction(name) ?: createFunction(name, returnType, true).apply {
            paramTypes.forEach { addArgument(it) }
        }
    }
    
    /**
     * 移除函数
     */
    fun removeFunction(func: Function) {
        functions.remove(func)
        func.parent = null
    }
    
    /**
     * 获取所有函数
     */
    fun functions(): List<Function> = functions.toList()
    
    /**
     * 获取已定义的函数（非外部声明）
     */
    fun definedFunctions(): List<Function> = 
        functions.filter { !it.isDeclaration() }
    
    /**
     * 获取函数数量
     */
    fun functionCount(): Int = functions.size
    
    // ==================== 全局变量管理 ====================
    
    /**
     * 添加全局变量
     */
    fun addGlobal(name: String, type: Type, isExternal: Boolean = false): GlobalValue {
        require(name !in globalVariables) { "Global variable $name already exists" }
        
        val global = GlobalValue(type, name, isExternal)
        globalVariables[name] = global
        return global
    }
    
    /**
     * 获取全局变量
     */
    fun getGlobal(name: String): GlobalValue? = globalVariables[name]
    
    /**
     * 获取所有全局变量
     */
    fun globals(): Collection<GlobalValue> = globalVariables.values
    
    // ==================== 字符串常量管理 ====================
    
    /**
     * 获取或创建字符串常量
     */
    fun getOrCreateStringConstant(value: String): ConstantString {
        return stringConstants.getOrPut(value) {
            ConstantString(value)
        }
    }
    
    /**
     * 获取所有字符串常量
     */
    fun stringConstants(): Collection<ConstantString> = stringConstants.values
    
    // ==================== 类型管理 ====================
    
    /**
     * 注册命名类型
     */
    fun addNamedType(name: String, type: Type) {
        namedTypes[name] = type
    }
    
    /**
     * 获取命名类型
     */
    fun getNamedType(name: String): Type? = namedTypes[name]
    
    // ==================== 元数据管理 ====================
    
    /**
     * 设置元数据
     */
    fun setMetadata(key: String, value: Any) {
        metadata[key] = value
    }
    
    /**
     * 获取元数据
     */
    fun getMetadata(key: String): Any? = metadata[key]
    
    // ==================== 遍历与分析 ====================
    
    /**
     * 遍历模块中所有指令
     */
    fun allInstructions(): Sequence<Instruction> = 
        functions.asSequence().flatMap { it.instructions() }
    
    /**
     * 遍历所有基本块
     */
    fun allBlocks(): Sequence<BasicBlock> = 
        functions.asSequence().flatMap { it.blocks().asSequence() }
    
    /**
     * 验证整个模块
     */
    fun verify(): Boolean {
        return functions.all { it.verify() }
    }
    
    // ==================== 输出 ====================
    
    override fun toString(): String {
        val sb = StringBuilder()
        
        // 字符串常量
        stringConstants.forEach { (value, constant) ->
            sb.appendLine("@str.${constant.id} = private constant $constant")
        }
        
        // 全局变量
        globalVariables.forEach { (name, global) ->
            val linkage = if (global.isExternal) "external" else "global"
            sb.appendLine("@$name = $linkage ${global.type}")
        }
        
        sb.appendLine()
        
        // 函数
        functions.forEach { func ->
            sb.appendLine(func.toString())
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    /**
     * 打印模块（更详细的调试输出）
     */
    fun printDetailed(): String {
        val sb = StringBuilder()
        sb.appendLine("; Module: $name")
        sb.appendLine("; Functions: ${functions.size}")
        sb.appendLine("; Global Variables: ${globalVariables.size}")
        sb.appendLine("; String Constants: ${stringConstants.size}")
        sb.appendLine()
        sb.append(toString())
        return sb.toString()
    }
}
