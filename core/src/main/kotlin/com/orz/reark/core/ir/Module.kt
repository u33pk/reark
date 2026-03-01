package com.orz.reark.core.ir

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
     * 字符串池（用于反编译时恢复原始字符串）
     * key: 字符串 ID (Int), value: 原始字符串值
     */
    private val stringPool = mutableMapOf<Int, String>()

    /**
     * 字符串 ID 到字符串值的映射（用于反编译时恢复原始字符串）
     * key: 字符串 ID (如 "str_0", "str_1" 等)
     * value: 原始字符串值
     */
    private val stringIdMap = mutableMapOf<String, String>()
    
    /**
     * 全局变量符号映射（用于反编译时恢复原始名称）
     * key: 全局变量 ID (如 "global_0", "global_1" 等)
     * value: 原始名称 (如 "console", "Math" 等)
     */
    private val globalSymbolMap = mutableMapOf<String, String>()
    
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
     * 设置字符串池
     */
    fun setStringPool(pool: Map<Int, String>) {
        stringPool.putAll(pool)
    }

    /**
     * 获取字符串池
     */
    fun getStringPool(): Map<Int, String> = stringPool.toMap()

    /**
     * 注册字符串 ID 映射
     * @param strId 字符串 ID (如 "str_0")
     * @param value 原始字符串值 (如 "console")
     */
    fun registerStringMapping(strId: String, value: String) {
        stringIdMap[strId] = value
    }

    /**
     * 获取字符串 ID 对应的原始值
     */
    fun getStringById(strId: String): String? = stringIdMap[strId]
    
    /**
     * 注册全局变量符号映射
     * @param globalId 全局变量 ID (如 "global_0")
     * @param symbolName 原始符号名称 (如 "console")
     */
    fun registerGlobalSymbol(globalId: String, symbolName: String) {
        globalSymbolMap[globalId] = symbolName
    }
    
    /**
     * 获取全局变量 ID 对应的原始符号名称
     */
    fun getGlobalSymbolById(globalId: String): String? = globalSymbolMap[globalId]
    
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
