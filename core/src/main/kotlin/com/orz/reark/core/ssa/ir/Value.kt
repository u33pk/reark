package com.orz.reark.core.ssa.ir

/**
 * Use类 - 表示一个Value被另一个Value使用的关系
 * 用于SSA的def-use链维护
 */
class Use(
    var user: Value,
    var usedValue: Value
) {
    /**
     * 替换使用的值
     */
    fun set(newValue: Value) {
        if (usedValue !== newValue) {
            usedValue.removeUse(this)
            usedValue = newValue
            newValue.addUse(this)
        }
    }
    
    override fun toString(): String = "Use of ${usedValue.name} in ${user.name}"
}

/**
 * Value基类 - SSA形式中的所有值
 * 
 * 在LLVM风格的SSA IR中，Instruction是Value的子类，
 * 因为指令本身也产生值。
 */
abstract class Value(
    val type: Type,
    var name: String = ""
) {
    /**
     * 值的唯一ID（用于命名）
     */
    val id: Int = nextId++
    
    /**
     * 使用此值的所有Use关系
     */
    private val useList = mutableListOf<Use>()
    
    /**
     * 获取所有使用此值的Use关系
     */
    fun uses(): List<Use> = useList.toList()
    
    /**
     * 获取使用此值的所有User
     */
    fun users(): List<Value> = useList.map { it.user }
    
    /**
     * 获取使用数量
     */
    fun useCount(): Int = useList.size
    
    /**
     * 是否有任何使用
     */
    fun hasUsers(): Boolean = useList.isNotEmpty()
    
    /**
     * 是否只有一个使用
     */
    fun hasOneUse(): Boolean = useList.size == 1
    
    /**
     * 是否只有指定数量的使用
     */
    fun hasNUses(n: Int): Boolean = useList.size == n
    
    /**
     * 添加一个Use关系
     */
    internal fun addUse(use: Use) {
        useList.add(use)
    }
    
    /**
     * 移除一个Use关系
     */
    internal fun removeUse(use: Use) {
        useList.remove(use)
    }
    
    /**
     * 替换所有使用此值的Use为newValue
     */
    fun replaceAllUsesWith(newValue: Value) {
        // 复制列表防止并发修改问题
        val usesCopy = useList.toList()
        usesCopy.forEach { it.set(newValue) }
        useList.clear()
    }
    
    /**
     * 移除所有使用
     */
    fun dropAllUses() {
        useList.clear()
    }
    
    /**
     * 获取值的名称（如果未指定，使用临时名称）
     */
    fun getNameOrTemporary(): String {
        return name.ifEmpty { "%$id" }
    }
    
    /**
     * 值的文本表示
     */
    abstract override fun toString(): String
    
    companion object {
        private var nextId = 0
        
        /**
         * 重置ID计数器（主要用于测试）
         */
        fun resetIdCounter() {
            nextId = 0
        }
    }
}

/**
 * 常量值基类
 */
abstract class Constant(type: Type) : Value(type) {
    /**
     * 是否为null常量
     */
    open fun isNull(): Boolean = false
    
    /**
     * 是否为零值
     */
    open fun isZero(): Boolean = false
    
    /**
     * 是否为一值
     */
    open fun isOne(): Boolean = false
}

/**
 * 整数常量
 */
class ConstantInt(
    val value: Long,
    type: Type = Type.I64Type
) : Constant(type) {
    
    init {
        require(type.isInteger()) { "ConstantInt requires integer type" }
    }
    
    override fun isZero(): Boolean = value == 0L
    override fun isOne(): Boolean = value == 1L
    
    override fun toString(): String = 
        if (type == Type.I32Type) value.toInt().toString() else "${value}i64"
    
    companion object {
        fun i32(value: Int) = ConstantInt(value.toLong(), Type.I32Type)
        fun i64(value: Long) = ConstantInt(value, Type.I64Type)
        fun bool(value: Boolean) = ConstantInt(if (value) 1L else 0L, Type.BoolType)
        
        val TRUE = bool(true)
        val FALSE = bool(false)
        val ZERO_I32 = i32(0)
        val ZERO_I64 = i64(0L)
        val ONE_I32 = i32(1)
        val ONE_I64 = i64(1L)
    }
}

/**
 * 浮点常量
 */
class ConstantFP(
    val value: Double,
    type: Type = Type.F64Type
) : Constant(type) {
    
    init {
        require(type.isFloatingPoint()) { "ConstantFP requires floating point type" }
    }
    
    override fun isZero(): Boolean = value == 0.0
    override fun isOne(): Boolean = value == 1.0
    
    override fun toString(): String = 
        when {
            value.isNaN() -> "nan"
            value.isInfinite() -> if (value > 0) "+inf" else "-inf"
            else -> value.toString()
        }
    
    companion object {
        fun f32(value: Float) = ConstantFP(value.toDouble(), Type.F32Type)
        fun f64(value: Double) = ConstantFP(value, Type.F64Type)
        
        val ZERO_F32 = f32(0.0f)
        val ZERO_F64 = f64(0.0)
        val ONE_F32 = f32(1.0f)
        val ONE_F64 = f64(1.0)
    }
}

/**
 * 特殊数值常量（用于JavaScript的NaN和Infinity）
 */
class ConstantSpecial(
    val kind: SpecialValueKind,
    type: Type = Type.F64Type
) : Constant(type) {
    
    enum class SpecialValueKind {
        NAN,
        POS_INFINITY,
        NEG_INFINITY,
        UNDEFINED,
        NULL,
        HOLE  // JavaScript数组的hole值
    }
    
    override fun isNull(): Boolean = kind == SpecialValueKind.NULL
    
    override fun toString(): String = when (kind) {
        SpecialValueKind.NAN -> "NaN"
        SpecialValueKind.POS_INFINITY -> "+Infinity"
        SpecialValueKind.NEG_INFINITY -> "-Infinity"
        SpecialValueKind.UNDEFINED -> "undefined"
        SpecialValueKind.NULL -> "null"
        SpecialValueKind.HOLE -> "hole"
    }
    
    companion object {
        val NAN = ConstantSpecial(SpecialValueKind.NAN)
        val POS_INFINITY = ConstantSpecial(SpecialValueKind.POS_INFINITY)
        val NEG_INFINITY = ConstantSpecial(SpecialValueKind.NEG_INFINITY)
        val UNDEFINED = ConstantSpecial(SpecialValueKind.UNDEFINED, Type.AnyType)
        val NULL = ConstantSpecial(SpecialValueKind.NULL, Type.AnyType)
        val HOLE = ConstantSpecial(SpecialValueKind.HOLE, Type.AnyType)
    }
}

/**
 * 字符串常量
 */
class ConstantString(
    val value: String
) : Constant(Type.StringType) {
    
    override fun toString(): String = "\"${escapeString(value)}\""
    
    companion object {
        fun escapeString(s: String): String {
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
        }
    }
}

/**
 * 未定义/占位符值 - 用于尚未解析的引用
 */
class UndefValue(type: Type) : Value(type, "undef") {
    override fun toString(): String = "undef"
    
    companion object {
        fun get(type: Type) = UndefValue(type)
    }
}

/**
 * 参数值 - 函数参数
 */
class Argument(
    type: Type,
    val argIndex: Int,
    name: String = ""
) : Value(type, name.ifEmpty { "arg$argIndex" }) {
    
    override fun toString(): String = getNameOrTemporary()
}

/**
 * 全局值引用
 */
class GlobalValue(
    type: Type,
    name: String,
    val isExternal: Boolean = false
) : Value(type, name) {
    
    override fun toString(): String = "@$name"
}
