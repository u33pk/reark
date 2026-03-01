package com.orz.reark.core.backend

/**
 * PandaASM 指令操作码枚举
 *
 * 基于 isa.yaml 定义的 PandaVM 字节码指令集
 * 包含完整的操作码定义和前缀处理
 */
object PandaAsmOpcodes {

    // 前缀字节码
    const val PREFIX_THROW: Int = 0xFE
    const val PREFIX_WIDE: Int = 0xFD
    const val PREFIX_DEPRECATED: Int = 0xFC
    const val PREFIX_CALLRUNTIME: Int = 0xFB

    /**
     * 普通指令操作码 (无前缀)
     */
    enum class StandardOpcode(val opcode: Int) {
        // ==================== 常量加载 ====================
        LDUNDEFINED(0x00),      // 加载 undefined
        LDNULL(0x01),           // 加载 null
        LDTRUE(0x02),           // 加载 true
        LDFALSE(0x03),          // 加载 false
        CREATEEMPTYOBJECT(0x04),// 创建空对象
        CREATEEMPTYARRAY(0x05), // 创建空数组 (8 位)
        CREATEARRAYWITHBUFFER(0x06), // 从缓冲区创建数组 (8 位)
        CREATEOBJECTWITHBUFFER(0x07), // 从缓冲区创建对象 (8 位)
        NEWOBJRANGE(0x08),      // new 操作符范围 (8 位)
        NEWLEXENV(0x09),        // 新词法环境 (8 位)

        // ==================== 二元运算 (使用累加器) ====================
        ADD2(0x0A),             // 加法
        SUB2(0x0B),             // 减法
        MUL2(0x0C),             // 乘法
        DIV2(0x0D),             // 除法
        MOD2(0x0E),             // 取模
        EQ(0x0F),               // 等于
        NOTEQ(0x10),            // 不等于
        LESS(0x11),             // 小于
        LESSEQ(0x12),           // 小于等于
        GREATER(0x13),          // 大于
        GREATEREQ(0x14),        // 大于等于
        SHL2(0x15),             // 左移
        SHR2(0x16),             // 逻辑右移
        ASHR2(0x17),            // 算术右移
        AND2(0x18),             // 按位与
        OR2(0x19),              // 按位或
        XOR2(0x1A),             // 按位异或
        EXP(0x1B),              // 指数

        // ==================== 一元运算 ====================
        TYPEOF(0x1C),           // typeof (8 位)
        TONUMBER(0x1D),         // 转数字
        TONUMERIC(0x1E),        // 转数值
        NEG(0x1F),              // 取负
        NOT(0x20),              // 逻辑非
        INC(0x21),              // 自增
        DEC(0x22),              // 自减
        ISTRUE(0x23),           // 是否为真
        ISFALSE(0x24),          // 是否为假

        // ==================== 比较运算 ====================
        ISIN(0x25),             // in 操作符
        INSTANCEOF(0x26),       // instanceof
        STRICTNOTEQ(0x27),      // 严格不等于
        STRICTEQ(0x28),         // 严格等于

        // ==================== 调用指令 ====================
        CALLARG0(0x29),         // 调用无参数
        CALLARG1(0x2A),         // 调用 1 个参数
        CALLARGS2(0x2B),        // 调用 2 个参数
        CALLARGS3(0x2C),        // 调用 3 个参数
        CALLTHIS0(0x2D),        // 带 this 调用 0 参数
        CALLTHIS1(0x2E),        // 带 this 调用 1 参数
        CALLTHIS2(0x2F),        // 带 this 调用 2 参数
        CALLTHIS3(0x30),        // 带 this 调用 3 参数
        CALLTHISRANGE(0x31),    // 带 this 调用范围
        SUPERCALLTHISRANGE(0x32), // super 调用范围
        DEFINEFUNC(0x33),       // 定义函数 (8 位)
        DEFINEMETHOD(0x34),     // 定义方法 (8 位)
        DEFINECLASSWITHBUFFER(0x35), // 定义类 (8 位)
        GETNEXTPROPNAME(0x36),  // 获取下一个属性名
        LDOBJBYVALUE(0x37),     // 按值加载对象 (8 位)
        STOBJBYVALUE(0x38),     // 按值存储对象 (8 位)
        LDSUPERBYVALUE(0x39),   // 按值加载 super (8 位)
        LDOBJBYINDEX(0x3A),     // 按索引加载对象 (8 位)
        STOBJBYINDEX(0x3B),     // 按索引存储对象 (8 位)
        LDLEXVAR(0x3C),         // 加载词法变量 (4/4 位)
        STLEXVAR(0x3D),         // 存储词法变量 (4/4 位)

        // ==================== 字符串和全局变量 ====================
        LDA_STR(0x3E),          // 加载字符串
        TRYLDGLOBALBYNAME(0x3F),// 尝试按名加载全局 (8 位)
        TRYSTGLOBALBYNAME(0x40),// 尝试按名存储全局 (8 位)
        LDGLOBALVAR(0x41),      // 加载全局变量
        LDOBJBYNAME(0x42),      // 按名加载对象 (8 位)
        STOBJBYNAME(0x43),      // 按名存储对象 (8 位)
        STTOGLOBALRECORD(0x48), // 存储到全局记录 (16 位)
        LDTHISBYNAME(0x49),     // 按名加载 this (8 位)
        STTHISBYNAME(0x4A),     // 按名存储 this (8 位)
        LDTHISBYVALUE(0x4B),    // 按值加载 this (8 位)
        STTHISBYVALUE(0x4C),    // 按值存储 this (8 位)

        // ==================== 跳转指令 ====================
        JMP(0x4D),              // 跳转 (8 位)
        JMP_16(0x4E),           // 跳转 (16 位)
        JEQZ(0x4F),             // 等于零跳转 (8 位)
        JEQZ_16(0x50),          // 等于零跳转 (16 位)
        JNEZ(0x51),             // 不等于零跳转 (8 位)
        JSTRICTEQZ(0x52),       // 严格等于零跳转 (8 位)
        JNSTRICTEQZ(0x53),      // 严格不等于零跳转 (8 位)
        JEQNULL(0x54),          // 等于 null 跳转 (8 位)
        JNENULL(0x55),          // 不等于 null 跳转 (8 位)
        JSTRICTEQNULL(0x56),    // 严格等于 null 跳转 (8 位)
        JNSTRICTEQNULL(0x57),   // 严格不等于 null 跳转 (8 位)
        JEQUNDEFINED(0x58),     // 等于 undefined 跳转 (8 位)
        JNEUNDEFINED(0x59),     // 不等于 undefined 跳转 (8 位)
        JSTRICTEQUndefined(0x5A), // 严格等于 undefined 跳转 (8 位)
        JNSTRICTEQUndefined(0x5B), // 严格不等于 undefined 跳转 (8 位)
        JEQ(0x5C),              // 等于跳转 (8 位)
        JNE(0x5D),              // 不等于跳转 (8 位)
        JSTRICTEQ(0x5E),        // 严格等于跳转 (8 位)
        JNSTRICTEQ(0x5F),       // 严格不等于跳转 (8 位)

        // ==================== 寄存器操作 ====================
        MOV_4(0x44),            // 移动寄存器 (4/4 位)
        MOV_8(0x45),            // 移动寄存器 (8/8 位)
        LDA(0x60),              // 加载到累加器
        STA(0x61),              // 存储累加器
        LDAI(0x62),             // 加载立即数到累加器
        FLDAI(0x63),            // 加载浮点立即数

        // ==================== 返回 ====================
        RETURN(0x64),           // 返回累加器
        RETURNUNDEFINED(0x65),  // 返回 undefined

        // ==================== 迭代器 ====================
        GETPROPITERATOR(0x66),  // 获取属性迭代器
        GETITERATOR_8(0x67),    // 获取迭代器 (8 位)
        CLOSEITERATOR_8(0x68),  // 关闭迭代器 (8 位)
        POPLEXENV(0x69),        // 弹出词法环境

        // ==================== 常量加载 ====================
        LDNAN(0x6A),            // 加载 NaN
        LDINFINITY(0x6B),       // 加载 Infinity
        GETUNMAPPEDARGS(0x6C),  // 获取未映射参数
        LDGLOBAL(0x6D),         // 加载全局
        LDNEWTARGET(0x6E),      // 加载 new.target
        LDTHIS(0x6F),           // 加载 this
        LDHOLE(0x70),           // 加载 hole
        CREATEREGEXPWITHLITERAL_8(0x71), // 创建正则 (8 位)
        CREATEREGEXPWITHLITERAL_16(0x72), // 创建正则 (16 位)
        CALLRANGE(0x73),        // 调用范围 (8 位)
        DEFINEFUNC_16(0x74),    // 定义函数 (16 位)
        DEFINECLASSWITHBUFFER_16(0x75), // 定义类 (16 位)
        GETTEMPLATEOBJECT_8(0x76), // 获取模板对象 (8 位)
        SETOBJECTWITHPROTO_8(0x77), // 设置对象原型 (8 位)
        CREATEARRAYWITHBUFFER_16(0x80), // 从缓冲区创建数组 (16 位)
        CREATEOBJECTWITHBUFFER_16(0x82), // 从缓冲区创建对象 (16 位)
        NEWOBJRANGE_16(0x83),   // new 范围 (16 位)
        TYPEOF_16(0x84),        // typeof (16 位)
        LDOBJBYVALUE_16(0x85),  // 按值加载对象 (16 位)
        STOBJBYVALUE_16(0x86),  // 按值存储对象 (16 位)
        LDSUPERBYVALUE_16(0x87), // 按值加载 super (16 位)
        LDOBJBYINDEX_16(0x88),  // 按索引加载对象 (16 位)
        STOBJBYINDEX_16(0x89),  // 按索引存储对象 (16 位)
        LDLEXVAR_8(0x8A),       // 加载词法变量 (8/8 位)
        STLEXVAR_8(0x8B),       // 存储词法变量 (8/8 位)
        TRYLDGLOBALBYNAME_16(0x8C), // 尝试按名加载全局 (16 位)
        TRYSTGLOBALBYNAME_16(0x8D), // 尝试按名存储全局 (16 位)
        MOV_16(0x8F),           // 移动寄存器 (16/16 位)

        // ==================== 更多指令 (0x90-0xD6) ====================
        LDOBJBYNAME_16(0x90),   // 按名加载对象 (16 位)
        STOBJBYNAME_16(0x91),   // 按名存储对象 (16 位)
        LDSUPERBYNAME_16(0x92), // 按名加载 super (16 位)
        LDTHISBYNAME_16(0x93),  // 按名加载 this (16 位)
        STTHISBYNAME_16(0x94),  // 按名存储 this (16 位)
        LDTHISBYVALUE_16(0x95), // 按值加载 this (16 位)
        STTHISBYVALUE_16(0x96), // 按值存储 this (16 位)
        ASYNCGENERATORREJECT(0x97), // 异步生成器拒绝
        JMP_32(0x98),           // 跳转 (32 位)
        JEQZ_32(0x9A),          // 等于零跳转 (32 位)
        JNEZ_16(0x9B),          // 不等于零跳转 (16 位)
        JNEZ_32(0x9C),          // 不等于零跳转 (32 位)
        JSTRICTEQZ_16(0x9D),    // 严格等于零跳转 (16 位)
        JNSTRICTEQZ_16(0x9E),   // 严格不等于零跳转 (16 位)
        JEQNULL_16(0x9F),       // 等于 null 跳转 (16 位)
        JNENULL_16(0xA0),       // 不等于 null 跳转 (16 位)
        JSTRICTEQNULL_16(0xA1), // 严格等于 null 跳转 (16 位)
        JNSTRICTEQNULL_16(0xA2), // 严格不等于 null 跳转 (16 位)
        JEQUndefined_16(0xA3),  // 等于 undefined 跳转 (16 位)
        JNEUndefined_16(0xA4),  // 不等于 undefined 跳转 (16 位)
        JSTRICTEQUndefined_16(0xA5), // 严格等于 undefined 跳转 (16 位)
        JNSTRICTEQUndefined_16(0xA6), // 严格不等于 undefined 跳转 (16 位)
        JEQ_16(0xA7),           // 等于跳转 (16 位)
        JNE_16(0xA8),           // 不等于跳转 (16 位)
        JSTRICTEQ_16(0xA9),     // 严格等于跳转 (16 位)
        JNSTRICTEQ_16(0xAA),    // 严格不等于跳转 (16 位)
        GETITERATOR_16(0xAB),   // 获取迭代器 (16 位)
        CLOSEITERATOR_16(0xAC), // 关闭迭代器 (16 位)
        LDSYMBOL(0xAD),         // 加载 Symbol
        ASYNCFUNCTIONENTER(0xAE), // 异步函数进入
        LDFUNCTION(0xAF),       // 加载函数
        DEBUGGER(0xB0),         // 调试器断点
        CREATEGENERATOROBJ(0xB1), // 创建生成器对象
        CREATEITERRESULTOBJ(0xB2), // 创建迭代结果对象
        CREATEOBJECTWITHEXCLUDEDKEYS(0xB3), // 创建排除键的对象
        NEWOBJAPPLY(0xB4),      // new apply (8 位)
        NEWOBJAPPLY_16(0xB5),   // new apply (16 位)
        NEWLEXENVWITHNAME(0xB6), // 新词法环境带名称
        CREATEASYNCGENERATOROBJ(0xB7), // 创建异步生成器对象
        ASYNCGENERATORRESOLVE(0xB8), // 异步生成器解析
        SUPERCALLSPREAD(0xB9),  // super 调用 spread
        APPLY(0xBA),            // apply
        SUPERCALLARROWRANGE(0xBB), // super 调用 arrow 范围
        DEFINEGETTERSETTERBYVALUE(0xBC), // 定义 getter/setter
        DYNAMICIMPORT(0xBD),    // 动态导入
        DEFINEMETHOD_16(0xBE),  // 定义方法 (16 位)
        RESUMEGENERATOR(0xBF),  // 恢复生成器
        GETRESUMEMODE(0xC0),    // 获取恢复模式
        GETTEMPLATEOBJECT_16(0xC1), // 获取模板对象 (16 位)
        DELOBJPROP(0xC2),       // 删除对象属性
        SUSPENDGENERATOR(0xC3), // 挂起生成器
        ASYNCFUNCTIONAWAITUNCAUGHT(0xC4), // 异步函数 await
        COPYDATAPROPERTIES(0xC5), // 复制数据属性
        STARRAYSPREAD(0xC6),    // 存储数组 spread
        SETOBJECTWITHPROTO_16(0xC7), // 设置对象原型 (16 位)
        STOWNBYVALUE(0xC8),     // 存储 own 属性 (8 位)
        STOWNBYVALUE_16(0xC9),  // 存储 own 属性 (16 位)
        STSUPERBYVALUE_8(0xCA), // 存储 super 属性 (8 位)
        STSUPERBYVALUE_16(0xCB), // 存储 super 属性 (16 位)
        STOWNBYNAME(0xCC),      // 按名存储 own 属性
        ASYNCFUNCTIONRESOLVE(0xCD), // 异步函数解析
        ASYNCFUNCTIONREJECT(0xCE), // 异步函数拒绝
        COPYRESTARGS(0xCF),     // 复制剩余参数
        STSUPERBYNAME_8(0xD0),  // 按名存储 super (8 位)
        STSUPERBYNAME_16(0xD1), // 按名存储 super (16 位)
        STOWNBYVALUEWITHNAMESET(0xD2), // 存储 own 属性带名称设置
        LDBIGINT(0xD3),         // 加载大整数
        STOWNBYNAMEWITHNAMESET(0xD4), // 按名存储 own 属性带名称设置
        NOP(0xD5),              // 空操作
        SETGENERATORSTATE(0xD6), // 设置生成器状态

        // ==================== 异步/迭代器 (0xD7-0xDC) ====================
        GETASYNCITERATOR(0xD7), // 获取异步迭代器
        LDPRIVATEPROPERTY(0xD8), // 加载私有属性
        STPRIVATEPROPERTY(0xD9), // 存储私有属性
        TESTIN(0xDA),           // 测试 in
        DEFINEFIELDBYNAME(0xDB), // 按名定义字段
        DEFINEPROPERTYBYNAME(0xDC); // 按名定义属性

        companion object {
            private val opcodeMap = entries.associateBy { it.opcode }

            fun fromByte(opcode: Int): StandardOpcode? = opcodeMap[opcode and 0xFF]
        }
    }

    /**
     * Wide 前缀指令 (0xFD)
     */
    enum class WideOpcode(val opcode: Int) {
        CREATEOBJECTWITHEXCLUDEDKEYS(0x00), // 创建排除键的对象
        NEWOBJRANGE(0x01),          // new 范围
        NEWLEXENV(0x02),            // 新词法环境
        NEWLEXENVWITHNAME(0x03),    // 新词法环境带名称
        CALLRANGE(0x04),            // 调用范围
        CALLTHISRANGE(0x05),        // 带 this 调用范围
        SUPERCALLTHISRANGE(0x06),   // super 调用范围
        SUPERCALLARROWRANGE(0x07),  // super 调用 arrow 范围
        LDOBJBYINDEX(0x08),         // 按索引加载对象
        STOBJBYINDEX(0x09),         // 按索引存储对象
        STOWNBYINDEX(0x0A),         // 存储 own 索引
        COPYRESTARGS(0x0B),         // 复制剩余参数
        LDLEXVAR(0x0C),             // 加载词法变量
        STLEXVAR(0x0D),             // 存储词法变量
        GETMODULENAMESPACE(0x0E),   // 获取模块命名空间
        STMODULEVAR(0x0F),          // 存储模块变量
        LDLOCALMODULEVAR(0x10),     // 加载本地模块变量
        LDEXTERNALMODULEVAR(0x11),  // 加载外部模块变量
        LDPATCHVAR(0x12),           // 加载 patch 变量
        STPATCHVAR(0x13);           // 存储 patch 变量

        companion object {
            private val opcodeMap = entries.associateBy { it.opcode }

            fun fromByte(opcode: Int): WideOpcode? = opcodeMap[opcode and 0xFF]
        }
    }

    /**
     * Deprecated 前缀指令 (0xFC)
     */
    enum class DeprecatedOpcode(val opcode: Int) {
        LDLEXENV(0x00),             // 加载词法环境
        POPLEXENV(0x01),            // 弹出词法环境
        GETITERATORNEXT(0x02),      // 获取迭代器下一个
        CREATEARRAYWITHBUFFER(0x03), // 从缓冲区创建数组
        CREATEOBJECTWITHBUFFER(0x04), // 从缓冲区创建对象
        TONUMBER(0x05),             // 转数字
        TONUMERIC(0x06),            // 转数值
        NEG(0x07),                  // 取负
        NOT(0x08),                  // 逻辑非
        INC(0x09),                  // 自增
        DEC(0x0A),                  // 自减
        CALLARG0(0x0B),             // 调用 0 参数
        CALLARG1(0x0C),             // 调用 1 个参数
        CALLARGS2(0x0D),            // 调用 2 个参数
        CALLARGS3(0x0E),            // 调用 3 个参数
        CALLRANGE(0x0F),            // 调用范围
        CALLSPREAD(0x10),           // 调用 spread
        CALLTHISRANGE(0x11),        // 带 this 调用范围
        DEFINECLASSWITHBUFFER(0x12), // 定义类
        RESUMEGENERATOR(0x13),      // 恢复生成器
        GETRESUMEMODE(0x14),        // 获取恢复模式
        GETTEMPLATEOBJECT(0x15),    // 获取模板对象
        DELOBJPROP(0x16),           // 删除对象属性
        SUSPENDGENERATOR(0x17),     // 挂起生成器
        ASYNCFUNCTIONAWAITUNCAUGHT(0x18), // 异步函数 await
        COPYDATAPROPERTIES(0x19),   // 复制数据属性
        SETOBJECTWITHPROTO(0x1A),   // 设置对象原型
        LDOBJBYVALUE(0x1B),         // 按值加载对象
        LDSUPERBYVALUE(0x1C),       // 按值加载 super
        LDOBJBYINDEX(0x1D),         // 按索引加载对象
        ASYNCFUNCTIONRESOLVE(0x1E), // 异步函数解析
        ASYNCFUNCTIONREJECT(0x1F),  // 异步函数拒绝
        STLEXVAR_4(0x20),           // 存储词法变量 (4/4 位)
        STLEXVAR_8(0x21),           // 存储词法变量 (8/8 位)
        STLEXVAR_16(0x22),          // 存储词法变量 (16/16 位)
        GETMODULENAMESPACE(0x23),   // 获取模块命名空间
        STMODULEVAR(0x24),          // 存储模块变量
        LDOBJBYNAME(0x25),          // 按名加载对象
        LDSUPERBYNAME(0x26),        // 按名加载 super
        LDMODEULEVAR(0x27),         // 加载模块变量
        STCONSTTOGLOBALRECORD(0x28), // 存储常量到全局记录
        STLETTTOGLOBALRECORD(0x29), // 存储 let 到全局记录
        STCLASSTOGLOBALRECORD(0x2A), // 存储类到全局记录
        LDHOMEOBJECT(0x2B),         // 加载 home 对象
        CREATEOBJECTHAVINGMETHOD(0x2C), // 创建有方法的对象
        DYNAMICIMPORT(0x2D),        // 动态导入
        ASYNCGENERATORREJECT(0x2E); // 异步生成器拒绝

        companion object {
            private val opcodeMap = entries.associateBy { it.opcode }

            fun fromByte(opcode: Int): DeprecatedOpcode? = opcodeMap[opcode and 0xFF]
        }
    }

    /**
     * Throw 前缀指令 (0xFE)
     */
    enum class ThrowOpcode(val opcode: Int) {
        THROW(0x00),                // 抛出异常
        THROW_NOTEXISTS(0x01),      // 抛出不存在
        THROW_PATTERNNONCOERCIBLE(0x02), // 抛出模式不可强制
        THROW_DELETESUPERPROPERTY(0x03), // 抛出删除 super 属性
        THROW_CONSTASSIGNMENT(0x04), // 抛出常量赋值
        THROW_IFNOTOBJECT(0x05),    // 如果不是对象则抛出
        THROW_UNDEFINEDIFHOLE(0x06), // 如果 hole 则抛出 undefined
        THROW_IFSUPERNOTCORRECTCALL_8(0x07), // 如果 super 调用不正确则抛出 (8 位)
        THROW_IFSUPERNOTCORRECTCALL_16(0x08), // 如果 super 调用不正确则抛出 (16 位)
        THROW_UNDEFINEDIFHOLEWITHNAME(0x09); // 如果 hole 则抛出带名称的 undefined

        companion object {
            private val opcodeMap = entries.associateBy { it.opcode }

            fun fromByte(opcode: Int): ThrowOpcode? = opcodeMap[opcode and 0xFF]
        }
    }

    /**
     * CallRuntime 前缀指令 (0xFB)
     */
    enum class CallRuntimeOpcode(val opcode: Int) {
        NOTIFYCONCURRENTRESULT(0x00),       // 通知并发结果
        DEFINEFIELDBYVALUE(0x01),           // 按值定义字段
        DEFINEFIELDBYINDEX(0x02),           // 按索引定义字段
        TOPROPERTYKEY(0x03),                // 转属性键
        CREATEPRIVATEPROPERTY(0x04),        // 创建私有属性
        DEFINEPRIVATEPROPERTY(0x05),        // 定义私有属性
        CALLINIT(0x06),                     // 调用初始化
        DEFINESENDABLECLASS(0x07),          // 定义可发送类
        LDSENDABLECLASS(0x08),              // 加载可发送类
        LDSENDABLEEXTERNALMODULEVAR(0x09),  // 加载可发送外部模块变量
        WIDELDSENDABLEEXTERNALMODULEVAR(0x0A), // 宽加载可发送外部模块变量
        NEWSENDABLEENV(0x0B),               // 新可发送环境
        WIDENEWSENDABLEENV(0x0C),           // 宽新可发送环境
        STSSENDABLEVAR_4(0x0D),             // 存储可发送变量 (4/4 位)
        STSSENDABLEVAR_8(0x0E),             // 存储可发送变量 (8/8 位)
        WIDESTSSENDABLEVAR(0x0F),           // 宽存储可发送变量
        LDSSENDABLEVAR_4(0x10),             // 加载可发送变量 (4/4 位)
        LDSSENDABLEVAR_8(0x11),             // 加载可发送变量 (8/8 位)
        WIDELDSSENDABLEVAR(0x12),           // 宽加载可发送变量
        ISTRUE(0x13),                       // 是否为真
        ISFALSE(0x14),                      // 是否为假
        LDLAZYMODULEVAR(0x15),              // 加载延迟模块变量
        WIDELDLAZYMODULEVAR(0x16),          // 宽加载延迟模块变量
        LDLAZYSENDABLEMODULEVAR(0x17),      // 加载延迟可发送模块变量
        WIDELDLAZYSENDABLEMODULEVAR(0x18),  // 宽加载延迟可发送模块变量
        SUPERCALLFORWARDALLARGS(0x19),      // super 调用转发所有参数
        LDSENDABLELOCALMODULEVAR(0x1A),     // 加载可发送本地模块变量
        WIDELDSENDABLELOCALMODULEVAR(0x1B); // 宽加载可发送本地模块变量

        companion object {
            private val opcodeMap = entries.associateBy { it.opcode }

            fun fromByte(opcode: Int): CallRuntimeOpcode? = opcodeMap[opcode and 0xFF]
        }
    }

    /**
     * 指令格式类型
     */
    enum class InstructionFormat {
        OP_NONE,                    // 无操作数
        OP_IMM_8,                   // 8 位立即数
        OP_IMM_16,                  // 16 位立即数
        OP_IMM_32,                  // 32 位立即数
        OP_IMM_64,                  // 64 位立即数
        OP_ID_16,                   // 16 位 ID
        OP_ID_32,                   // 32 位 ID
        OP_V_8,                     // 8 位寄存器
        OP_V_16,                    // 16 位寄存器
        OP_IMM_8_V_8,               // 8 位立即数 + 8 位寄存器
        OP_IMM_16_V_8,              // 16 位立即数 + 8 位寄存器
        OP_V_8_IMM_8,               // 8 位寄存器 + 8 位立即数
        OP_V_8_IMM_16,              // 8 位寄存器 + 16 位立即数
        OP_V1_8_V2_8,               // 两个 8 位寄存器
        OP_V1_4_V2_4,               // 两个 4 位寄存器
        OP_V1_16_V2_16,             // 两个 16 位寄存器
        OP_V1_8_V2_8_V3_8,          // 三个 8 位寄存器
        OP_V1_8_V2_8_V3_8_V4_8,     // 四个 8 位寄存器
        OP_IMM_8_ID_16,             // 8 位立即数 + 16 位 ID
        OP_IMM_16_ID_16,            // 16 位立即数 + 16 位 ID
        OP_ID_16_V_8,               // 16 位 ID + 8 位寄存器
        OP_ID_32_V_8,               // 32 位 ID + 8 位寄存器
        OP_IMM_8_V1_8_V2_8,         // 8 位立即数 + 两个 8 位寄存器
        OP_IMM_16_V1_8_V2_8,        // 16 位立即数 + 两个 8 位寄存器
        OP_IMM1_8_IMM2_8,           // 两个 8 位立即数
        OP_IMM1_8_IMM2_16,          // 8 位立即数 + 16 位立即数
        OP_IMM1_16_IMM2_16,         // 两个 16 位立即数
        OP_IMM1_4_IMM2_4,           // 两个 4 位立即数
        OP_IMM1_8_IMM2_16_V_8,      // 8 位 + 16 位立即数 + 寄存器
        OP_IMM_8_V_8_IMM_16,        // 8 位立即数 + 寄存器 + 16 位立即数
        OP_IMM_8_ID_16_V_8,         // 8 位立即数 + 16 位 ID + 寄存器
        OP_IMM_16_ID_16_V_8,        // 16 位立即数 + 16 位 ID + 寄存器
        OP_IMM1_8_ID1_16_ID2_16_IMM2_16, // 复杂格式
        OP_IMM1_8_ID1_16_ID2_16_IMM2_16_V_8, // 复杂格式带寄存器
        PREF_OP_NONE,               // 前缀 + 无操作数
        PREF_OP_IMM_8,              // 前缀 + 8 位立即数
        PREF_OP_IMM_16,             // 前缀 + 16 位立即数
        PREF_OP_IMM_32,             // 前缀 + 32 位立即数
        PREF_OP_V_8,                // 前缀 + 8 位寄存器
        PREF_OP_V_8_IMM_32,         // 前缀 + 8 位寄存器 + 32 位立即数
        PREF_OP_V1_8_V2_8,          // 前缀 + 两个 8 位寄存器
        PREF_OP_V1_8_V2_8_V3_8,     // 前缀 + 三个 8 位寄存器
        PREF_OP_V1_8_V2_8_V3_8_V4_8,// 前缀 + 四个 8 位寄存器
        PREF_OP_IMM_16_V_8,         // 前缀 + 16 位立即数 + 8 位寄存器
        PREF_OP_IMM_16_ID_16,       // 前缀 + 16 位立即数 + 16 位 ID
        PREF_OP_ID_16,              // 前缀 + 16 位 ID
        PREF_OP_ID_16_V_8,          // 前缀 + 16 位 ID + 8 位寄存器
        PREF_OP_ID_32,              // 前缀 + 32 位 ID
        PREF_OP_ID_32_V_8,          // 前缀 + 32 位 ID + 8 位寄存器
        PREF_OP_IMM_16_V1_8_V2_8,   // 前缀 + 16 位立即数 + 两个寄存器
        PREF_OP_IMM1_16_ID1_16_ID2_16_IMM2_16_V_8, // 复杂前缀格式
        PREF_OP_IMM_8_V1_8_V2_8,    // 前缀 + 8 位立即数 + 两个寄存器
        PREF_OP_IMM1_8_IMM2_16_IMM3_16, // 前缀 + 8 位 + 16 位 + 16 位立即数
        PREF_OP_IMM1_8_IMM2_16_IMM3_16_V_8, // 前缀 + 8 位 + 16 位 + 16 位立即数 + 寄存器
        PREF_OP_IMM1_8_IMM2_32_V_8, // 前缀 + 8 位 + 32 位立即数 + 寄存器
        PREF_OP_IMM1_16_IMM2_16,    // 前缀 + 两个 16 位立即数
        PREF_OP_IMM1_4_IMM2_4,      // 前缀 + 两个 4 位立即数
        PREF_OP_IMM1_4_IMM2_4_V_8,  // 前缀 + 两个 4 位立即数 + 寄存器
        PREF_OP_IMM1_8_IMM2_8,      // 前缀 + 两个 8 位立即数
        PREF_OP_IMM1_8_IMM2_8_V_8,  // 前缀 + 两个 8 位立即数 + 寄存器
        UNKNOWN                     // 未知格式
    }
}
