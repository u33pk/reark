package com.orz.reark.core.ssa.ir

/**
 * IR类型系统 - 用于表示SSA值的数据类型
 */
sealed class Type {
    
    /**
     * 获取类型的大小（位），对于引用类型返回指针大小
     */
    abstract val bitWidth: Int
    
    /**
     * 是否为整数类型
     */
    open fun isInteger(): Boolean = false
    
    /**
     * 是否为浮点类型
     */
    open fun isFloatingPoint(): Boolean = false
    
    /**
     * 是否为引用/对象类型
     */
    open fun isReference(): Boolean = false
    
    /**
     * 是否为void类型
     */
    open fun isVoid(): Boolean = false
    
    /**
     * 是否为指针类型
     */
    open fun isPointer(): Boolean = false
    
    /**
     * 是否为数组类型
     */
    open fun isArray(): Boolean = false
    
    /**
     * 是否为函数类型
     */
    open fun isFunction(): Boolean = false
    
    companion object {
        const val POINTER_SIZE = 64  // 假设64位平台
    }
    
    // ==================== 基本类型 ====================
    
    /**
     * 空类型 - 用于无返回值的指令
     */
    object VoidType : Type() {
        override val bitWidth: Int = 0
        override fun isVoid(): Boolean = true
        override fun toString(): String = "void"
    }
    
    /**
     * 32位整数类型
     */
    object I32Type : Type() {
        override val bitWidth: Int = 32
        override fun isInteger(): Boolean = true
        override fun toString(): String = "i32"
    }
    
    /**
     * 64位整数类型
     */
    object I64Type : Type() {
        override val bitWidth: Int = 64
        override fun isInteger(): Boolean = true
        override fun toString(): String = "i64"
    }
    
    /**
     * 32位浮点类型
     */
    object F32Type : Type() {
        override val bitWidth: Int = 32
        override fun isFloatingPoint(): Boolean = true
        override fun toString(): String = "f32"
    }
    
    /**
     * 64位浮点类型
     */
    object F64Type : Type() {
        override val bitWidth: Int = 64
        override fun isFloatingPoint(): Boolean = true
        override fun toString(): String = "f64"
    }
    
    /**
     * 布尔类型
     */
    object BoolType : Type() {
        override val bitWidth: Int = 1
        override fun isInteger(): Boolean = true
        override fun toString(): String = "bool"
    }
    
    // ==================== 引用类型 ====================
    
    /**
     * JavaScript任意类型 - 对应pandaASM的'any'类型
     */
    object AnyType : Type() {
        override val bitWidth: Int = POINTER_SIZE
        override fun isReference(): Boolean = true
        override fun toString(): String = "any"
    }
    
    /**
     * 对象引用类型
     */
    object ObjectType : Type() {
        override val bitWidth: Int = POINTER_SIZE
        override fun isReference(): Boolean = true
        override fun toString(): String = "object"
    }
    
    /**
     * 字符串类型
     */
    object StringType : Type() {
        override val bitWidth: Int = POINTER_SIZE
        override fun isReference(): Boolean = true
        override fun toString(): String = "string"
    }
    
    /**
     * 数组类型
     */
    data class ArrayType(val elementType: Type) : Type() {
        override val bitWidth: Int = POINTER_SIZE
        override fun isReference(): Boolean = true
        override fun isArray(): Boolean = true
        override fun toString(): String = "array<$elementType>"
    }
    
    /**
     * 函数类型
     */
    data class FunctionType(
        val returnType: Type,
        val paramTypes: List<Type>
    ) : Type() {
        override val bitWidth: Int = POINTER_SIZE
        override fun isReference(): Boolean = true
        override fun isFunction(): Boolean = true
        override fun toString(): String = 
            "(${paramTypes.joinToString(", ") { it.toString() }}) -> $returnType"
    }
    
    /**
     * 指针类型
     */
    data class PointerType(val pointeeType: Type) : Type() {
        override val bitWidth: Int = POINTER_SIZE
        override fun isPointer(): Boolean = true
        override fun toString(): String = "$pointeeType*"
    }
    
    /**
     * 结构体类型
     */
    data class StructType(
        val name: String,
        val fieldTypes: List<Type>
    ) : Type() {
        override val bitWidth: Int = POINTER_SIZE
        override fun isReference(): Boolean = true
        override fun toString(): String = "struct $name"
    }
    
    /**
     * 标签/基本块类型 - 用于跳转指令
     */
    object LabelType : Type() {
        override val bitWidth: Int = 0
        override fun toString(): String = "label"
    }
}

// 便捷访问
val voidType = Type.VoidType
val i32Type = Type.I32Type
val i64Type = Type.I64Type
val f32Type = Type.F32Type
val f64Type = Type.F64Type
val boolType = Type.BoolType
val anyType = Type.AnyType
val objectType = Type.ObjectType
val stringType = Type.StringType
val labelType = Type.LabelType
