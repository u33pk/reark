package com.orz.reark.core.backend

import com.orz.reark.core.ir.*

/**
 * PandaASM 指令到 IR Opcode 的映射表
 * 
 * 提供 PandaASM 指令与 SSA IR 指令之间的双向映射关系
 */
object InstructionMapping {
    
    /**
     * PandaASM 标准指令到 IR Opcode 的映射
     */
    fun mapStandardOpcode(pandaOpcode: PandaAsmOpcodes.StandardOpcode): Opcode? {
        return when (pandaOpcode) {
            // ==================== 终止指令 ====================
            PandaAsmOpcodes.StandardOpcode.RETURN -> Opcode.RET
            PandaAsmOpcodes.StandardOpcode.RETURNUNDEFINED -> Opcode.RET_VOID
            
            // ==================== 二元运算 ====================
            PandaAsmOpcodes.StandardOpcode.ADD2 -> Opcode.ADD
            PandaAsmOpcodes.StandardOpcode.SUB2 -> Opcode.SUB
            PandaAsmOpcodes.StandardOpcode.MUL2 -> Opcode.MUL
            PandaAsmOpcodes.StandardOpcode.DIV2 -> Opcode.DIV
            PandaAsmOpcodes.StandardOpcode.MOD2 -> Opcode.MOD
            PandaAsmOpcodes.StandardOpcode.SHL2 -> Opcode.SHL
            PandaAsmOpcodes.StandardOpcode.SHR2 -> Opcode.SHR
            PandaAsmOpcodes.StandardOpcode.ASHR2 -> Opcode.ASHR
            PandaAsmOpcodes.StandardOpcode.AND2 -> Opcode.AND
            PandaAsmOpcodes.StandardOpcode.OR2 -> Opcode.OR
            PandaAsmOpcodes.StandardOpcode.XOR2 -> Opcode.XOR
            PandaAsmOpcodes.StandardOpcode.EXP -> Opcode.EXP
            
            // ==================== 比较运算 ====================
            PandaAsmOpcodes.StandardOpcode.EQ -> Opcode.EQ
            PandaAsmOpcodes.StandardOpcode.NOTEQ -> Opcode.NE
            PandaAsmOpcodes.StandardOpcode.LESS -> Opcode.LT
            PandaAsmOpcodes.StandardOpcode.LESSEQ -> Opcode.LE
            PandaAsmOpcodes.StandardOpcode.GREATER -> Opcode.GT
            PandaAsmOpcodes.StandardOpcode.GREATEREQ -> Opcode.GE
            PandaAsmOpcodes.StandardOpcode.STRICTEQ -> Opcode.STRICT_EQ
            PandaAsmOpcodes.StandardOpcode.STRICTNOTEQ -> Opcode.STRICT_NE
            PandaAsmOpcodes.StandardOpcode.ISIN -> Opcode.ISIN
            PandaAsmOpcodes.StandardOpcode.INSTANCEOF -> Opcode.INSTANCEOF
            
            // ==================== 一元运算 ====================
            PandaAsmOpcodes.StandardOpcode.NEG -> Opcode.NEG
            PandaAsmOpcodes.StandardOpcode.NOT -> Opcode.NOT
            PandaAsmOpcodes.StandardOpcode.INC -> Opcode.INC
            PandaAsmOpcodes.StandardOpcode.DEC -> Opcode.DEC
            PandaAsmOpcodes.StandardOpcode.TYPEOF,
            PandaAsmOpcodes.StandardOpcode.TYPEOF_16 -> Opcode.TYPEOF
            PandaAsmOpcodes.StandardOpcode.TONUMBER -> Opcode.TO_NUMBER
            PandaAsmOpcodes.StandardOpcode.TONUMERIC -> Opcode.TO_NUMERIC
            PandaAsmOpcodes.StandardOpcode.ISTRUE -> Opcode.IS_TRUE
            PandaAsmOpcodes.StandardOpcode.ISFALSE -> Opcode.IS_FALSE
            
            // ==================== 对象创建 ====================
            PandaAsmOpcodes.StandardOpcode.CREATEEMPTYOBJECT -> Opcode.CREATE_OBJECT
            PandaAsmOpcodes.StandardOpcode.CREATEEMPTYARRAY,
            PandaAsmOpcodes.StandardOpcode.CREATEEMPTYARRAY -> Opcode.CREATE_ARRAY
            PandaAsmOpcodes.StandardOpcode.CREATEARRAYWITHBUFFER,
            PandaAsmOpcodes.StandardOpcode.CREATEARRAYWITHBUFFER_16 -> Opcode.CREATE_ARRAY_WITH_BUF
            PandaAsmOpcodes.StandardOpcode.CREATEOBJECTWITHBUFFER,
            PandaAsmOpcodes.StandardOpcode.CREATEOBJECTWITHBUFFER_16 -> Opcode.CREATE_OBJECT_WITH_BUF
            PandaAsmOpcodes.StandardOpcode.CREATEREGEXPWITHLITERAL_8,
            PandaAsmOpcodes.StandardOpcode.CREATEREGEXPWITHLITERAL_16 -> Opcode.CREATE_REGEXP
            
            // ==================== 属性访问 ====================
            PandaAsmOpcodes.StandardOpcode.LDOBJBYVALUE,
            PandaAsmOpcodes.StandardOpcode.LDOBJBYVALUE_16 -> Opcode.GET_PROPERTY
            PandaAsmOpcodes.StandardOpcode.STOBJBYVALUE,
            PandaAsmOpcodes.StandardOpcode.STOBJBYVALUE_16 -> Opcode.SET_PROPERTY
            PandaAsmOpcodes.StandardOpcode.LDOBJBYNAME,
            PandaAsmOpcodes.StandardOpcode.LDOBJBYNAME_16 -> Opcode.GET_PROPERTY_BY_NAME
            PandaAsmOpcodes.StandardOpcode.STOBJBYNAME,
            PandaAsmOpcodes.StandardOpcode.STOBJBYNAME_16 -> Opcode.SET_PROPERTY_BY_NAME
            PandaAsmOpcodes.StandardOpcode.LDOBJBYINDEX,
            PandaAsmOpcodes.StandardOpcode.LDOBJBYINDEX_16 -> Opcode.GET_ELEMENT
            PandaAsmOpcodes.StandardOpcode.STOBJBYINDEX,
            PandaAsmOpcodes.StandardOpcode.STOBJBYINDEX_16 -> Opcode.SET_ELEMENT
            PandaAsmOpcodes.StandardOpcode.DELOBJPROP -> Opcode.DELETE_PROPERTY
            
            // ==================== 调用相关 ====================
            PandaAsmOpcodes.StandardOpcode.CALLARG0,
            PandaAsmOpcodes.StandardOpcode.CALLARG1,
            PandaAsmOpcodes.StandardOpcode.CALLARGS2,
            PandaAsmOpcodes.StandardOpcode.CALLARGS3,
            PandaAsmOpcodes.StandardOpcode.CALLRANGE,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS0,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS1,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS2,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS3,
            PandaAsmOpcodes.StandardOpcode.CALLTHISRANGE -> Opcode.CALL
            
            PandaAsmOpcodes.StandardOpcode.NEWOBJRANGE,
            PandaAsmOpcodes.StandardOpcode.NEWOBJRANGE_16 -> Opcode.NEW
            PandaAsmOpcodes.StandardOpcode.NEWOBJAPPLY,
            PandaAsmOpcodes.StandardOpcode.NEWOBJAPPLY_16 -> Opcode.APPLY
            
            // ==================== 函数/类定义 ====================
            PandaAsmOpcodes.StandardOpcode.DEFINEFUNC,
            PandaAsmOpcodes.StandardOpcode.DEFINEFUNC_16 -> Opcode.DEFINE_FUNC
            PandaAsmOpcodes.StandardOpcode.DEFINEMETHOD,
            PandaAsmOpcodes.StandardOpcode.DEFINEMETHOD_16 -> Opcode.DEFINE_METHOD
            PandaAsmOpcodes.StandardOpcode.DEFINECLASSWITHBUFFER,
            PandaAsmOpcodes.StandardOpcode.DEFINECLASSWITHBUFFER_16 -> Opcode.DEFINE_CLASS
            
            // ==================== 词法环境 ====================
            PandaAsmOpcodes.StandardOpcode.NEWLEXENV,
            PandaAsmOpcodes.StandardOpcode.NEWLEXENVWITHNAME -> Opcode.NEW_LEX_ENV
            PandaAsmOpcodes.StandardOpcode.POPLEXENV -> Opcode.POP_LEX_ENV
            PandaAsmOpcodes.StandardOpcode.LDLEXVAR,
            PandaAsmOpcodes.StandardOpcode.LDLEXVAR_8 -> Opcode.LOAD_LEX_VAR
            PandaAsmOpcodes.StandardOpcode.STLEXVAR,
            PandaAsmOpcodes.StandardOpcode.STLEXVAR_8 -> Opcode.STORE_LEX_VAR
            
            // ==================== 模块相关 ====================
            PandaAsmOpcodes.StandardOpcode.DYNAMICIMPORT -> Opcode.DYNAMIC_IMPORT
            
            // ==================== 全局变量 ====================
            PandaAsmOpcodes.StandardOpcode.TRYLDGLOBALBYNAME,
            PandaAsmOpcodes.StandardOpcode.TRYLDGLOBALBYNAME_16 -> Opcode.TRY_LOAD_GLOBAL
            PandaAsmOpcodes.StandardOpcode.TRYSTGLOBALBYNAME,
            PandaAsmOpcodes.StandardOpcode.TRYSTGLOBALBYNAME_16 -> Opcode.TRY_STORE_GLOBAL
            PandaAsmOpcodes.StandardOpcode.LDGLOBALVAR -> Opcode.LOAD_GLOBAL
            PandaAsmOpcodes.StandardOpcode.LDGLOBAL -> Opcode.LOAD_GLOBAL
            
            // ==================== this相关 ====================
            PandaAsmOpcodes.StandardOpcode.LDTHIS -> Opcode.LOAD_THIS
            PandaAsmOpcodes.StandardOpcode.LDTHISBYNAME,
            PandaAsmOpcodes.StandardOpcode.LDTHISBYNAME_16 -> Opcode.GET_THIS_PROP
            PandaAsmOpcodes.StandardOpcode.STTHISBYNAME,
            PandaAsmOpcodes.StandardOpcode.STTHISBYNAME_16 -> Opcode.SET_THIS_PROP
            PandaAsmOpcodes.StandardOpcode.LDTHISBYVALUE,
            PandaAsmOpcodes.StandardOpcode.LDTHISBYVALUE_16 -> Opcode.GET_THIS_PROP
            PandaAsmOpcodes.StandardOpcode.STTHISBYVALUE,
            PandaAsmOpcodes.StandardOpcode.STTHISBYVALUE_16 -> Opcode.SET_THIS_PROP
            
            // ==================== super相关 ====================
            PandaAsmOpcodes.StandardOpcode.LDSUPERBYVALUE,
            PandaAsmOpcodes.StandardOpcode.LDSUPERBYVALUE_16 -> Opcode.GET_SUPER_PROP
            PandaAsmOpcodes.StandardOpcode.STSUPERBYVALUE_8,
            PandaAsmOpcodes.StandardOpcode.STSUPERBYVALUE_16 -> Opcode.SET_SUPER_PROP
            PandaAsmOpcodes.StandardOpcode.STSUPERBYNAME_8,
            PandaAsmOpcodes.StandardOpcode.STSUPERBYNAME_16 -> Opcode.SET_SUPER_PROP
            PandaAsmOpcodes.StandardOpcode.SUPERCALLTHISRANGE -> Opcode.CALL_SUPER
            PandaAsmOpcodes.StandardOpcode.SUPERCALLARROWRANGE -> Opcode.CALL_SUPER
            PandaAsmOpcodes.StandardOpcode.SUPERCALLSPREAD -> Opcode.CALL_SUPER
            
            // ==================== 生成器/异步 ====================
            PandaAsmOpcodes.StandardOpcode.CREATEGENERATOROBJ -> Opcode.CREATE_GENERATOR
            PandaAsmOpcodes.StandardOpcode.CREATEASYNCGENERATOROBJ -> Opcode.CREATE_GENERATOR
            PandaAsmOpcodes.StandardOpcode.RESUMEGENERATOR -> Opcode.RESUME_GENERATOR
            PandaAsmOpcodes.StandardOpcode.GETRESUMEMODE -> Opcode.GET_RESUME_MODE
            PandaAsmOpcodes.StandardOpcode.SUSPENDGENERATOR -> Opcode.SUSPEND_GENERATOR
            PandaAsmOpcodes.StandardOpcode.ASYNCFUNCTIONENTER -> Opcode.ASYNC_FUNC_ENTER
            PandaAsmOpcodes.StandardOpcode.ASYNCFUNCTIONAWAITUNCAUGHT -> Opcode.ASYNC_FUNC_AWAIT
            PandaAsmOpcodes.StandardOpcode.ASYNCFUNCTIONRESOLVE -> Opcode.ASYNC_FUNC_RESOLVE
            PandaAsmOpcodes.StandardOpcode.ASYNCFUNCTIONREJECT -> Opcode.ASYNC_FUNC_REJECT
            
            // ==================== 跳转 (映射为条件/无条件分支) ====================
            PandaAsmOpcodes.StandardOpcode.JMP,
            PandaAsmOpcodes.StandardOpcode.JMP_16,
            PandaAsmOpcodes.StandardOpcode.JMP_32 -> Opcode.BR
            PandaAsmOpcodes.StandardOpcode.JEQZ,
            PandaAsmOpcodes.StandardOpcode.JEQZ_16,
            PandaAsmOpcodes.StandardOpcode.JEQZ_32 -> Opcode.BR_COND
            PandaAsmOpcodes.StandardOpcode.JNEZ,
            PandaAsmOpcodes.StandardOpcode.JNEZ_16,
            PandaAsmOpcodes.StandardOpcode.JNEZ_32 -> Opcode.BR_COND
            
            // ==================== 其他 ====================
            PandaAsmOpcodes.StandardOpcode.LDA -> Opcode.LDA
            PandaAsmOpcodes.StandardOpcode.STA -> Opcode.STA
            PandaAsmOpcodes.StandardOpcode.MOV_4,
            PandaAsmOpcodes.StandardOpcode.MOV_8,
            PandaAsmOpcodes.StandardOpcode.MOV_16 -> Opcode.MOV_ACC
            PandaAsmOpcodes.StandardOpcode.NOP -> Opcode.NOP
            PandaAsmOpcodes.StandardOpcode.DEBUGGER -> Opcode.DEBUG
            
            // 默认情况返回 null（需要特殊处理）
            else -> null
        }
    }
    
    /**
     * Wide 前缀指令到 IR Opcode 的映射
     */
    fun mapWideOpcode(wideOpcode: PandaAsmOpcodes.WideOpcode): Opcode? {
        return when (wideOpcode) {
            PandaAsmOpcodes.WideOpcode.NEWOBJRANGE -> Opcode.NEW
            PandaAsmOpcodes.WideOpcode.CALLRANGE -> Opcode.CALL
            PandaAsmOpcodes.WideOpcode.CALLTHISRANGE -> Opcode.CALL_THIS
            PandaAsmOpcodes.WideOpcode.SUPERCALLTHISRANGE,
            PandaAsmOpcodes.WideOpcode.SUPERCALLARROWRANGE -> Opcode.CALL_SUPER
            PandaAsmOpcodes.WideOpcode.LDLEXVAR -> Opcode.LOAD_LEX_VAR
            PandaAsmOpcodes.WideOpcode.STLEXVAR -> Opcode.STORE_LEX_VAR
            PandaAsmOpcodes.WideOpcode.GETMODULENAMESPACE -> Opcode.GET_MODULE_NS
            PandaAsmOpcodes.WideOpcode.STMODULEVAR -> Opcode.STORE_MODULE_VAR
            PandaAsmOpcodes.WideOpcode.LDLOCALMODULEVAR,
            PandaAsmOpcodes.WideOpcode.LDEXTERNALMODULEVAR -> Opcode.LOAD_MODULE_VAR
            PandaAsmOpcodes.WideOpcode.COPYRESTARGS -> Opcode.COPY_REST_ARGS
            PandaAsmOpcodes.WideOpcode.CREATEOBJECTWITHEXCLUDEDKEYS -> Opcode.CREATE_OBJECT
            PandaAsmOpcodes.WideOpcode.NEWLEXENV,
            PandaAsmOpcodes.WideOpcode.NEWLEXENVWITHNAME -> Opcode.NEW_LEX_ENV
            else -> null
        }
    }
    
    /**
     * Throw 前缀指令到 IR Opcode 的映射
     */
    fun mapThrowOpcode(throwOpcode: PandaAsmOpcodes.ThrowOpcode): Opcode? {
        return when (throwOpcode) {
            PandaAsmOpcodes.ThrowOpcode.THROW -> Opcode.THROW
            else -> Opcode.THROW  // 其他 throw 变体也映射为 THROW
        }
    }
    
    /**
     * CallRuntime 前缀指令到 IR Opcode 的映射
     */
    fun mapCallRuntimeOpcode(runtimeOpcode: PandaAsmOpcodes.CallRuntimeOpcode): Opcode? {
        return when (runtimeOpcode) {
            PandaAsmOpcodes.CallRuntimeOpcode.TOPROPERTYKEY -> Opcode.CALL_RUNTIME
            PandaAsmOpcodes.CallRuntimeOpcode.CREATEPRIVATEPROPERTY -> Opcode.CALL_RUNTIME
            PandaAsmOpcodes.CallRuntimeOpcode.DEFINEPRIVATEPROPERTY -> Opcode.CALL_RUNTIME
            PandaAsmOpcodes.CallRuntimeOpcode.DEFINEFIELDBYVALUE -> Opcode.CALL_RUNTIME
            PandaAsmOpcodes.CallRuntimeOpcode.DEFINEFIELDBYINDEX -> Opcode.CALL_RUNTIME
            PandaAsmOpcodes.CallRuntimeOpcode.NOTIFYCONCURRENTRESULT -> Opcode.CALL_RUNTIME
            PandaAsmOpcodes.CallRuntimeOpcode.CALLINIT -> Opcode.CALL_RUNTIME
            PandaAsmOpcodes.CallRuntimeOpcode.ISTRUE -> Opcode.IS_TRUE
            PandaAsmOpcodes.CallRuntimeOpcode.ISFALSE -> Opcode.IS_FALSE
            PandaAsmOpcodes.CallRuntimeOpcode.LDLAZYMODULEVAR,
            PandaAsmOpcodes.CallRuntimeOpcode.WIDELDLAZYMODULEVAR -> Opcode.LOAD_MODULE_VAR
            PandaAsmOpcodes.CallRuntimeOpcode.LDLAZYSENDABLEMODULEVAR,
            PandaAsmOpcodes.CallRuntimeOpcode.WIDELDLAZYSENDABLEMODULEVAR -> Opcode.LOAD_MODULE_VAR
            PandaAsmOpcodes.CallRuntimeOpcode.LDSSENDABLEVAR_4,
            PandaAsmOpcodes.CallRuntimeOpcode.LDSSENDABLEVAR_8,
            PandaAsmOpcodes.CallRuntimeOpcode.WIDELDSSENDABLEVAR -> Opcode.LOAD_LEX_VAR
            PandaAsmOpcodes.CallRuntimeOpcode.STSSENDABLEVAR_4,
            PandaAsmOpcodes.CallRuntimeOpcode.STSSENDABLEVAR_8,
            PandaAsmOpcodes.CallRuntimeOpcode.WIDESTSSENDABLEVAR -> Opcode.STORE_LEX_VAR
            PandaAsmOpcodes.CallRuntimeOpcode.NEWSENDABLEENV,
            PandaAsmOpcodes.CallRuntimeOpcode.WIDENEWSENDABLEENV -> Opcode.NEW_LEX_ENV
            PandaAsmOpcodes.CallRuntimeOpcode.SUPERCALLFORWARDALLARGS -> Opcode.CALL_SUPER
            else -> Opcode.CALL_RUNTIME
        }
    }
    
    /**
     * 获取指令的 IR 指令类别
     */
    fun getInstructionCategory(opcode: Opcode): InstructionCategory {
        return when (opcode) {
            Opcode.RET, Opcode.RET_VOID -> InstructionCategory.TERMINATOR
            Opcode.BR, Opcode.BR_COND, Opcode.SWITCH, Opcode.UNREACHABLE -> InstructionCategory.TERMINATOR
            Opcode.ADD, Opcode.SUB, Opcode.MUL, Opcode.DIV, Opcode.MOD -> InstructionCategory.ARITHMETIC
            Opcode.SHL, Opcode.SHR, Opcode.ASHR, Opcode.AND, Opcode.OR, Opcode.XOR -> InstructionCategory.BITWISE
            Opcode.EXP -> InstructionCategory.ARITHMETIC
            Opcode.EQ, Opcode.NE, Opcode.LT, Opcode.LE, Opcode.GT, Opcode.GE,
            Opcode.STRICT_EQ, Opcode.STRICT_NE, Opcode.ISIN, Opcode.INSTANCEOF -> InstructionCategory.COMPARISON
            Opcode.NEG, Opcode.NOT, Opcode.BIT_NOT, Opcode.INC, Opcode.DEC,
            Opcode.TYPEOF, Opcode.TO_NUMBER, Opcode.TO_NUMERIC,
            Opcode.IS_TRUE, Opcode.IS_FALSE -> InstructionCategory.UNARY
            Opcode.CALL, Opcode.CALL_INDIRECT, Opcode.CALL_VIRT, 
            Opcode.CALL_RUNTIME, Opcode.CALL_THIS, Opcode.NEW -> InstructionCategory.CALL
            Opcode.CREATE_OBJECT, Opcode.CREATE_ARRAY, Opcode.CREATE_ARRAY_WITH_BUF,
            Opcode.CREATE_OBJECT_WITH_BUF, Opcode.CREATE_REGEXP -> InstructionCategory.OBJECT_CREATION
            Opcode.GET_PROPERTY, Opcode.SET_PROPERTY, Opcode.GET_PROPERTY_BY_NAME,
            Opcode.SET_PROPERTY_BY_NAME, Opcode.GET_ELEMENT, Opcode.SET_ELEMENT,
            Opcode.DELETE_PROPERTY -> InstructionCategory.PROPERTY_ACCESS
            Opcode.LOAD, Opcode.STORE, Opcode.ALLOCA, Opcode.GET_ELEMENT_PTR -> InstructionCategory.MEMORY
            Opcode.BR, Opcode.BR_COND -> InstructionCategory.CONTROL_FLOW
            Opcode.PHI -> InstructionCategory.PHI
            Opcode.LDA, Opcode.STA, Opcode.MOV_ACC -> InstructionCategory.REGISTER_MOVE
            Opcode.NOP -> InstructionCategory.OTHER
            else -> InstructionCategory.OTHER
        }
    }
    
    /**
     * 检查指令是否可能抛出异常
     */
    fun mayThrow(pandaOpcode: PandaAsmOpcodes.StandardOpcode): Boolean {
        return when (pandaOpcode) {
            // 除法/取模可能除以零
            PandaAsmOpcodes.StandardOpcode.DIV2,
            PandaAsmOpcodes.StandardOpcode.MOD2 -> true
            // 属性访问可能访问 null/undefined
            PandaAsmOpcodes.StandardOpcode.LDOBJBYVALUE,
            PandaAsmOpcodes.StandardOpcode.LDOBJBYVALUE_16,
            PandaAsmOpcodes.StandardOpcode.LDOBJBYNAME,
            PandaAsmOpcodes.StandardOpcode.LDOBJBYNAME_16,
            PandaAsmOpcodes.StandardOpcode.LDOBJBYINDEX,
            PandaAsmOpcodes.StandardOpcode.LDOBJBYINDEX_16 -> true
            // 调用可能抛出
            PandaAsmOpcodes.StandardOpcode.CALLARG0,
            PandaAsmOpcodes.StandardOpcode.CALLARG1,
            PandaAsmOpcodes.StandardOpcode.CALLARGS2,
            PandaAsmOpcodes.StandardOpcode.CALLARGS3,
            PandaAsmOpcodes.StandardOpcode.CALLRANGE,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS0,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS1,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS2,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS3,
            PandaAsmOpcodes.StandardOpcode.CALLTHISRANGE -> true
            // new 操作可能抛出
            PandaAsmOpcodes.StandardOpcode.NEWOBJRANGE,
            PandaAsmOpcodes.StandardOpcode.NEWOBJRANGE_16 -> true
            // 其他可能抛出的操作
            PandaAsmOpcodes.StandardOpcode.INSTANCEOF,
            PandaAsmOpcodes.StandardOpcode.ISIN -> true
            else -> false
        }
    }
    
    /**
     * 检查指令是否有副作用
     */
    fun hasSideEffects(pandaOpcode: PandaAsmOpcodes.StandardOpcode): Boolean {
        return when (pandaOpcode) {
            // 存储操作
            PandaAsmOpcodes.StandardOpcode.STA,
            PandaAsmOpcodes.StandardOpcode.STOBJBYVALUE,
            PandaAsmOpcodes.StandardOpcode.STOBJBYVALUE_16,
            PandaAsmOpcodes.StandardOpcode.STOBJBYNAME,
            PandaAsmOpcodes.StandardOpcode.STOBJBYNAME_16,
            PandaAsmOpcodes.StandardOpcode.STOBJBYINDEX,
            PandaAsmOpcodes.StandardOpcode.STOBJBYINDEX_16,
            PandaAsmOpcodes.StandardOpcode.STOWNBYVALUE,
            PandaAsmOpcodes.StandardOpcode.STOWNBYVALUE_16,
            PandaAsmOpcodes.StandardOpcode.STOWNBYNAME,
            PandaAsmOpcodes.StandardOpcode.STTHISBYNAME,
            PandaAsmOpcodes.StandardOpcode.STTHISBYNAME_16,
            PandaAsmOpcodes.StandardOpcode.STTHISBYVALUE,
            PandaAsmOpcodes.StandardOpcode.STTHISBYVALUE_16 -> true
            // 调用操作
            PandaAsmOpcodes.StandardOpcode.CALLARG0,
            PandaAsmOpcodes.StandardOpcode.CALLARG1,
            PandaAsmOpcodes.StandardOpcode.CALLARGS2,
            PandaAsmOpcodes.StandardOpcode.CALLARGS3,
            PandaAsmOpcodes.StandardOpcode.CALLRANGE,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS0,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS1,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS2,
            PandaAsmOpcodes.StandardOpcode.CALLTHIS3,
            PandaAsmOpcodes.StandardOpcode.CALLTHISRANGE -> true
            // 定义操作
            PandaAsmOpcodes.StandardOpcode.DEFINEFUNC,
            PandaAsmOpcodes.StandardOpcode.DEFINEFUNC_16,
            PandaAsmOpcodes.StandardOpcode.DEFINEMETHOD,
            PandaAsmOpcodes.StandardOpcode.DEFINEMETHOD_16,
            PandaAsmOpcodes.StandardOpcode.DEFINECLASSWITHBUFFER,
            PandaAsmOpcodes.StandardOpcode.DEFINECLASSWITHBUFFER_16 -> true
            // 其他副作用操作
            PandaAsmOpcodes.StandardOpcode.DELOBJPROP,
            PandaAsmOpcodes.StandardOpcode.SETGENERATORSTATE,
            PandaAsmOpcodes.StandardOpcode.DYNAMICIMPORT -> true
            else -> false
        }
    }
    
    /**
     * 判断是否为条件跳转指令
     */
    fun isConditionalBranch(pandaOpcode: PandaAsmOpcodes.StandardOpcode): Boolean {
        return when (pandaOpcode) {
            PandaAsmOpcodes.StandardOpcode.JEQZ,
            PandaAsmOpcodes.StandardOpcode.JEQZ_16,
            PandaAsmOpcodes.StandardOpcode.JEQZ_32,
            PandaAsmOpcodes.StandardOpcode.JNEZ,
            PandaAsmOpcodes.StandardOpcode.JNEZ_16,
            PandaAsmOpcodes.StandardOpcode.JNEZ_32,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQZ,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQZ_16,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQZ,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQZ_16,
            PandaAsmOpcodes.StandardOpcode.JEQNULL,
            PandaAsmOpcodes.StandardOpcode.JEQNULL_16,
            PandaAsmOpcodes.StandardOpcode.JNENULL,
            PandaAsmOpcodes.StandardOpcode.JNENULL_16,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQNULL,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQNULL_16,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQNULL,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQNULL_16,
            PandaAsmOpcodes.StandardOpcode.JEQUNDEFINED,
            PandaAsmOpcodes.StandardOpcode.JEQUndefined_16,
            PandaAsmOpcodes.StandardOpcode.JNEUNDEFINED,
            PandaAsmOpcodes.StandardOpcode.JNEUndefined_16,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQUndefined,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQUndefined_16,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQUndefined,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQUndefined_16,
            PandaAsmOpcodes.StandardOpcode.JEQ,
            PandaAsmOpcodes.StandardOpcode.JEQ_16,
            PandaAsmOpcodes.StandardOpcode.JNE,
            PandaAsmOpcodes.StandardOpcode.JNE_16,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQ,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQ_16,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQ,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQ_16 -> true
            else -> false
        }
    }
    
    /**
     * 获取条件跳转的比较类型
     */
    fun getBranchConditionType(pandaOpcode: PandaAsmOpcodes.StandardOpcode): BranchConditionType {
        return when (pandaOpcode) {
            PandaAsmOpcodes.StandardOpcode.JEQZ,
            PandaAsmOpcodes.StandardOpcode.JEQZ_16,
            PandaAsmOpcodes.StandardOpcode.JEQZ_32 -> BranchConditionType.EQZ
            PandaAsmOpcodes.StandardOpcode.JNEZ,
            PandaAsmOpcodes.StandardOpcode.JNEZ_16,
            PandaAsmOpcodes.StandardOpcode.JNEZ_32 -> BranchConditionType.NEZ
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQZ,
            PandaAsmOpcodes.StandardOpcode.JSTRICTEQZ_16 -> BranchConditionType.STRICT_EQZ
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQZ,
            PandaAsmOpcodes.StandardOpcode.JNSTRICTEQZ_16 -> BranchConditionType.STRICT_NEZ
            PandaAsmOpcodes.StandardOpcode.JEQNULL,
            PandaAsmOpcodes.StandardOpcode.JEQNULL_16 -> BranchConditionType.EQ_NULL
            PandaAsmOpcodes.StandardOpcode.JNENULL,
            PandaAsmOpcodes.StandardOpcode.JNENULL_16 -> BranchConditionType.NE_NULL
            PandaAsmOpcodes.StandardOpcode.JEQUNDEFINED,
            PandaAsmOpcodes.StandardOpcode.JEQUndefined_16 -> BranchConditionType.EQ_UNDEFINED
            PandaAsmOpcodes.StandardOpcode.JNEUNDEFINED,
            PandaAsmOpcodes.StandardOpcode.JNEUndefined_16 -> BranchConditionType.NE_UNDEFINED
            else -> BranchConditionType.UNKNOWN
        }
    }
}

/**
 * 指令类别枚举
 */
enum class InstructionCategory {
    TERMINATOR,         // 终止指令 (ret, br, etc.)
    ARITHMETIC,         // 算术运算
    BITWISE,            // 位运算
    COMPARISON,         // 比较运算
    UNARY,              // 一元运算
    CALL,               // 调用
    OBJECT_CREATION,    // 对象创建
    PROPERTY_ACCESS,    // 属性访问
    MEMORY,             // 内存操作
    CONTROL_FLOW,       // 控制流
    PHI,                // PHI节点
    REGISTER_MOVE,      // 寄存器移动
    OTHER               // 其他
}

/**
 * 分支条件类型
 */
enum class BranchConditionType {
    EQZ,                // 等于零
    NEZ,                // 不等于零
    STRICT_EQZ,         // 严格等于零
    STRICT_NEZ,         // 严格不等于零
    EQ_NULL,            // 等于 null
    NE_NULL,            // 不等于 null
    STRICT_EQ_NULL,     // 严格等于 null
    STRICT_NE_NULL,     // 严格不等于 null
    EQ_UNDEFINED,       // 等于 undefined
    NE_UNDEFINED,       // 不等于 undefined
    STRICT_EQ_UNDEFINED,// 严格等于 undefined
    STRICT_NE_UNDEFINED,// 严格不等于 undefined
    EQ,                 // 等于
    NE,                 // 不等于
    STRICT_EQ,          // 严格等于
    STRICT_NE,          // 严格不等于
    UNKNOWN             // 未知
}
