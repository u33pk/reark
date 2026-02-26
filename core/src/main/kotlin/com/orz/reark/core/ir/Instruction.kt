package com.orz.reark.core.ir

/**
 * 指令操作码枚举 - 对应 pandaASM 到 SSA IR 的映射
 */
enum class Opcode {
    // ==================== 终止指令 ====================
    RET,              // 返回值
    RET_VOID,         // 无返回值
    BR,               // 无条件跳转
    BR_COND,          // 条件跳转（通用）
    BR_LT,            // 小于则跳转
    BR_LE,            // 小于等于则跳转
    BR_GT,            // 大于则跳转
    BR_GE,            // 大于等于则跳转
    BR_EQ,            // 等于则跳转
    BR_NE,            // 不等于则跳转
    SWITCH,           // 多路分支
    UNREACHABLE,      // 不可达

    // ==================== 二元运算 ====================
    ADD,              // 加法
    SUB,              // 减法
    MUL,              // 乘法
    DIV,              // 除法
    MOD,              // 取模
    SHL,              // 左移
    SHR,              // 逻辑右移
    ASHR,             // 算术右移
    AND,              // 按位与
    OR,               // 按位或
    XOR,              // 按位异或
    EXP,              // 指数运算

    // ==================== 复合赋值运算 ====================
    ADD_ASSIGN,       // i += 2
    SUB_ASSIGN,       // i -= 1
    MUL_ASSIGN,       // i *= 2
    DIV_ASSIGN,       // i /= 2
    MOD_ASSIGN,       // i %= 2
    INC,              // i++
    DEC,              // i--

    // ==================== 比较运算 ====================
    EQ,               // 相等
    NE,               // 不相等
    LT,               // 小于
    LE,               // 小于等于
    GT,               // 大于
    GE,               // 大于等于
    STRICT_EQ,        // 严格相等 (===)
    STRICT_NE,        // 严格不相等 (!==)
    ISIN,             // in 操作符
    INSTANCEOF,       // instanceof 操作符

    // ==================== 一元运算 ====================
    NEG,              // 取负
    NOT,              // 逻辑非
    BIT_NOT,          // 按位非
    TYPEOF,           // typeof 操作符
    TO_NUMBER,        // 转数字
    TO_NUMERIC,       // 转数值
    IS_TRUE,          // 是否为真
    IS_FALSE,         // 是否为假

    // ==================== 内存操作 ====================
    ALLOCA,           // 栈上分配
    LOAD,             // 加载
    STORE,            // 存储
    GET_ELEMENT_PTR,  // 获取元素指针

    // ==================== 对象操作 ====================
    CREATE_OBJECT,           // 创建空对象
    CREATE_ARRAY,            // 创建空数组
    CREATE_ARRAY_WITH_BUF,   // 从缓冲区创建数组
    CREATE_OBJECT_WITH_BUF,  // 从缓冲区创建对象
    CREATE_REGEXP,           // 创建正则表达式
    GET_PROPERTY,            // 获取属性
    SET_PROPERTY,            // 设置属性
    GET_PROPERTY_BY_NAME,    // 按名获取属性
    SET_PROPERTY_BY_NAME,    // 按名设置属性
    DELETE_PROPERTY,         // 删除属性
    SET_PROTO,               // 设置原型
    DEFINE_PROPERTY,         // 定义属性

    // ==================== 数组操作 ====================
    GET_ELEMENT,       // 获取数组元素
    SET_ELEMENT,       // 设置数组元素
    ARRAY_LENGTH,      // 数组长度
    COPY_REST_ARGS,    // 复制剩余参数

    // ==================== 调用相关 ====================
    CALL,              // 直接调用
    CALL_INDIRECT,     // 间接调用
    CALL_VIRT,         // 虚调用
    CALL_RUNTIME,      // 调用运行时函数
    CALL_THIS,         // 带 this 的调用
    APPLY,             // apply 调用
    NEW,               // new 操作符
    NEW_RANGE,         // new 操作符（范围参数）

    // ==================== 函数相关 ====================
    DEFINE_FUNC,       // 定义函数
    DEFINE_METHOD,     // 定义方法
    DEFINE_CLASS,      // 定义类

    // ==================== 环境/词法作用域 ====================
    NEW_LEX_ENV,       // 创建词法环境
    LOAD_LEX_VAR,      // 加载词法变量
    STORE_LEX_VAR,     // 存储词法变量
    POP_LEX_ENV,       // 弹出词法环境

    // ==================== 模块相关 ====================
    LOAD_MODULE_VAR,   // 加载模块变量
    STORE_MODULE_VAR,  // 存储模块变量
    GET_MODULE_NS,     // 获取模块命名空间
    DYNAMIC_IMPORT,    // 动态导入

    // ==================== 全局变量 ====================
    LOAD_GLOBAL,       // 加载全局变量
    STORE_GLOBAL,      // 存储全局变量
    TRY_LOAD_GLOBAL,   // 尝试加载全局变量
    TRY_STORE_GLOBAL,  // 尝试存储全局变量

    // ==================== this 相关 ====================
    LOAD_THIS,         // 加载 this
    GET_THIS_PROP,     // 获取 this 属性
    SET_THIS_PROP,     // 设置 this 属性

    // ==================== super 相关 ====================
    LOAD_SUPER,        // 加载 super
    CALL_SUPER,        // 调用 super
    GET_SUPER_PROP,    // 获取 super 属性
    SET_SUPER_PROP,    // 设置 super 属性

    // ==================== 生成器/异步 ====================
    CREATE_GENERATOR,  // 创建生成器对象
    RESUME_GENERATOR,  // 恢复生成器
    SUSPEND_GENERATOR, // 挂起生成器
    ASYNC_FUNC_ENTER,  // 异步函数进入
    ASYNC_FUNC_AWAIT,  // 异步等待
    ASYNC_FUNC_RESOLVE,// 异步解析
    ASYNC_FUNC_REJECT, // 异步拒绝
    GET_RESUME_MODE,   // 获取恢复模式

    // ==================== 异常处理 ====================
    THROW,             // 抛出异常
    LANDING_PAD,       // 着陆垫（异常捕获点）
    RESUME,            // 从异常恢复

    // ==================== 类型转换 ====================
    TRUNC,             // 截断
    ZEXT,              // 零扩展
    SEXT,              // 符号扩展
    FPTOI,             // 浮点转整数
    UITOF,             // 无符号整数转浮点
    SITOF,             // 有符号整数转浮点
    BITCAST,           // 位转换

    // ==================== 其他 ====================
    PHI,               // PHI 节点（SSA 关键）
    SELECT,            // 选择（条件值）
    EXTRACT_VALUE,     // 提取结构体成员
    INSERT_VALUE,      // 插入结构体成员
    DEBUG,             // 调试指令
    NOP,               // 空操作

    // ==================== pandaASM 特殊 ====================
    MOV_ACC,           // 移动到累加器（用于转换阶段）
    LDA,               // 加载到累加器
    STA,               // 存储累加器

    // ==================== SSA 构造辅助 ====================
    COPY,              // 复制值（用于虚拟寄存器到 SSA 的映射）
}

/**
 * 指令基类 - 所有 SSA 指令的基类
 */
abstract class Instruction(
    val opcode: Opcode,
    type: Type,
    name: String = ""
) : Value(type, name) {

    /**
     * 所属基本块
     */
    var parent: BasicBlock? = null
        internal set

    /**
     * 前一条指令
     */
    var prev: Instruction? = null
        internal set

    /**
     * 后一条指令
     */
    var next: Instruction? = null
        internal set

    /**
     * 操作数列表（内部存储）
     */
    protected val operandList = mutableListOf<Use>()

    /**
     * 获取所有操作数
     */
    fun getOperands(): List<Value> = operandList.map { it.usedValue }

    /**
     * 获取指定位置的操作数
     */
    fun getOperand(index: Int): Value = operandList[index].usedValue

    /**
     * 设置指定位置的操作数
     */
    fun setOperand(index: Int, value: Value) {
        operandList[index].set(value)
    }

    /**
     * 操作数数量
     */
    fun operandCount(): Int = operandList.size

    /**
     * 添加操作数
     */
    protected fun addOperand(value: Value) {
        operandList.add(Use(this, value))
        value.addUse(operandList.last())
    }

    /**
     * 是否是指令序列中的第一条
     */
    fun isFirstInBlock(): Boolean = prev == null

    /**
     * 是否是指令序列中的最后一条
     */
    fun isLastInBlock(): Boolean = next == null

    /**
     * 是否已插入到基本块中
     */
    fun isInserted(): Boolean = parent != null

    /**
     * 是否为终止指令（控制流结束）
     */
    open fun isTerminator(): Boolean = false

    /**
     * 是否为副作用指令
     */
    open fun mayHaveSideEffects(): Boolean = false

    /**
     * 是否可能抛出异常
     */
    open fun mayThrow(): Boolean = false

    /**
     * 是否纯指令（无副作用、无异常、结果只依赖操作数）
     */
    open fun isPure(): Boolean = !mayHaveSideEffects() && !mayThrow()

    /**
     * 从当前基本块中移除
     */
    open fun eraseFromBlock() {
        parent?.remove(this)
    }

    /**
     * 在移除前清理
     */
    open fun dropOperands() {
        operandList.forEach { it.usedValue.removeUse(it) }
        operandList.clear()
    }

    /**
     * 标记是否为复合赋值模式（如 i += 2）
     */
    var isCompoundAssign: Boolean = false

    /**
     * 获取指令的 LLVM 风格文本表示
     */
    abstract fun getOpcodeString(): String

    override fun toString(): String {
        // 复合赋值模式：i += 2 或 i++
        if (isCompoundAssign) {
            val ops = getOperands()
            return when (this) {
                is BinaryInstruction -> {
                    // i += value (第一个操作数是目标变量，第二个是值)
                    "${ops[0].getNameOrTemporary()} += ${ops[1].getNameOrTemporary()}"
                }
                is UnaryInstruction -> {
                    // i++
                    "${getNameOrTemporary()}++"
                }
                else -> {
                    val result = if (type.isVoid()) "" else "${getNameOrTemporary()} = "
                    val opStr = ops.joinToString(", ") { it.getNameOrTemporary() }
                    "$result${getOpcodeString()}$opStr"
                }
            }
        }

        // 普通模式：i = ADD 2, i
        val result = if (type.isVoid()) "" else "${getNameOrTemporary()} = "
        val ops = getOperands().joinToString(", ") { it.getNameOrTemporary() }
        return "$result${getOpcodeString()}$ops"
    }
}

/**
 * 一元指令
 */
abstract class UnaryInstruction(
    opcode: Opcode,
    type: Type,
    operand: Value
) : Instruction(opcode, type) {
    init {
        addOperand(operand)
    }

    val operand: Value get() = getOperand(0)

    override fun getOpcodeString(): String = "$opcode "
}

/**
 * 二元指令
 */
abstract class BinaryInstruction(
    opcode: Opcode,
    type: Type,
    left: Value,
    right: Value
) : Instruction(opcode, type) {
    init {
        addOperand(left)
        addOperand(right)
    }

    val left: Value get() = getOperand(0)
    val right: Value get() = getOperand(1)

    override fun getOpcodeString(): String = "$opcode "
}

/**
 * 比较指令
 */
abstract class CmpInstruction(
    opcode: Opcode,
    left: Value,
    right: Value
) : Instruction(opcode, Type.BoolType) {
    init {
        addOperand(left)
        addOperand(right)
    }

    val left: Value get() = getOperand(0)
    val right: Value get() = getOperand(1)

    override fun getOpcodeString(): String = "$opcode "
}

/**
 * 调用指令基类
 */
abstract class CallInstructionBase(
    opcode: Opcode,
    type: Type,
    callee: Value,
    name: String = ""
) : Instruction(opcode, type, name) {
    init {
        addOperand(callee)
    }

    val callee: Value get() = getOperand(0)

    /**
     * 获取参数列表（不包括 callee）
     */
    open fun getArgs(): List<Value> = (1 until operandCount()).map { getOperand(it) }

    override fun mayHaveSideEffects(): Boolean = true
    override fun mayThrow(): Boolean = true
}
