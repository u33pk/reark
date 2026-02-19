package com.orz.reark.core.ssa.builder

import com.orz.reark.core.ssa.ir.*
import com.orz.reark.core.ssa.ir.Function as SSAFunction

/**
 * IR构建器 - 便捷的IR构造工具
 * 
 * 提供类似LLVM IRBuilder的API，简化IR生成
 */
class IRBuilder(
    var currentBlock: BasicBlock? = null
) {
    /**
     * 当前函数
     */
    val currentFunction: SSAFunction?
        get() = currentBlock?.parent
    
    /**
     * 设置插入点
     */
    fun setInsertPoint(block: BasicBlock?) {
        currentBlock = block
    }
    
    /**
     * 在块末尾设置插入点
     */
    fun setInsertPoint(block: BasicBlock, beforeTerminator: Boolean) {
        currentBlock = block
        if (beforeTerminator && block.isTerminated()) {
            // 实际处理在创建指令时进行
        }
    }
    
    /**
     * 创建新的基本块并设为插入点
     */
    fun createBlock(name: String = ""): BasicBlock {
        val func = currentFunction ?: throw IllegalStateException("No current function")
        return func.createBlock(name).also { setInsertPoint(it) }
    }
    
    // ==================== 终止指令 ====================
    
    fun createRet(value: Value?): Instruction {
        val inst = ReturnInst(value)
        insert(inst)
        return inst
    }
    
    fun createRetVoid(): Instruction {
        val inst = ReturnInst(null)
        insert(inst)
        return inst
    }
    
    fun createBr(target: BasicBlock): Instruction {
        currentBlock?.addSuccessor(target)
        val inst = BranchInst(target)
        insert(inst)
        return inst
    }
    
    fun createCondBr(
        condition: Value,
        trueTarget: BasicBlock,
        falseTarget: BasicBlock
    ): Instruction {
        val inst = CondBranchInst(condition, trueTarget, falseTarget)
        insert(inst)
        return inst
    }
    
    fun createUnreachable(): Instruction {
        val inst = UnreachableInst()
        insert(inst)
        return inst
    }
    
    // ==================== 二元运算 ====================
    
    fun createAdd(left: Value, right: Value, name: String = ""): Value =
        insert(AddInst(left, right, left.type), name)
    
    fun createSub(left: Value, right: Value, name: String = ""): Value =
        insert(SubInst(left, right, left.type), name)
    
    fun createMul(left: Value, right: Value, name: String = ""): Value =
        insert(MulInst(left, right, left.type), name)
    
    fun createDiv(left: Value, right: Value, name: String = ""): Value =
        insert(DivInst(left, right, left.type), name)
    
    fun createMod(left: Value, right: Value, name: String = ""): Value =
        insert(ModInst(left, right, left.type), name)
    
    fun createShl(left: Value, right: Value, name: String = ""): Value =
        insert(ShlInst(left, right, left.type), name)
    
    fun createShr(left: Value, right: Value, name: String = ""): Value =
        insert(ShrInst(left, right, left.type), name)
    
    fun createAShr(left: Value, right: Value, name: String = ""): Value =
        insert(AShrInst(left, right, left.type), name)
    
    fun createAnd(left: Value, right: Value, name: String = ""): Value =
        insert(AndInst(left, right, left.type), name)
    
    fun createOr(left: Value, right: Value, name: String = ""): Value =
        insert(OrInst(left, right, left.type), name)
    
    fun createXor(left: Value, right: Value, name: String = ""): Value =
        insert(XorInst(left, right, left.type), name)
    
    fun createExp(left: Value, right: Value, name: String = ""): Value =
        insert(ExpInst(left, right, left.type), name)
    
    // ==================== 比较运算 ====================
    
    fun createICmpEQ(left: Value, right: Value, name: String = ""): Value =
        insert(EqInst(left, right), name)
    
    fun createICmpNE(left: Value, right: Value, name: String = ""): Value =
        insert(NeInst(left, right), name)
    
    fun createICmpSLT(left: Value, right: Value, name: String = ""): Value =
        insert(LtInst(left, right), name)
    
    fun createICmpSLE(left: Value, right: Value, name: String = ""): Value =
        insert(LeInst(left, right), name)
    
    fun createICmpSGT(left: Value, right: Value, name: String = ""): Value =
        insert(GtInst(left, right), name)
    
    fun createICmpSGE(left: Value, right: Value, name: String = ""): Value =
        insert(GeInst(left, right), name)
    
    fun createStrictEq(left: Value, right: Value, name: String = ""): Value =
        insert(StrictEqInst(left, right), name)
    
    fun createStrictNe(left: Value, right: Value, name: String = ""): Value =
        insert(StrictNeInst(left, right), name)
    
    fun createIsIn(property: Value, obj: Value, name: String = ""): Value =
        insert(IsInInst(property, obj), name)
    
    fun createInstanceOf(obj: Value, constructor: Value, name: String = ""): Value =
        insert(InstanceOfInst(obj, constructor), name)
    
    // ==================== 一元运算 ====================
    
    fun createNeg(operand: Value, name: String = ""): Value =
        insert(NegInst(operand, operand.type), name)
    
    fun createNot(operand: Value, name: String = ""): Value =
        insert(NotInst(operand), name)
    
    fun createBitNot(operand: Value, name: String = ""): Value =
        insert(BitNotInst(operand, operand.type), name)
    
    fun createInc(operand: Value, name: String = ""): Value =
        insert(IncInst(operand, operand.type), name)
    
    fun createDec(operand: Value, name: String = ""): Value =
        insert(DecInst(operand, operand.type), name)
    
    fun createTypeOf(operand: Value, name: String = ""): Value =
        insert(TypeOfInst(operand), name)
    
    fun createToNumber(operand: Value, name: String = ""): Value =
        insert(ToNumberInst(operand), name)
    
    fun createToNumeric(operand: Value, name: String = ""): Value =
        insert(ToNumericInst(operand), name)
    
    fun createIsTrue(operand: Value, name: String = ""): Value =
        insert(IsTrueInst(operand), name)
    
    fun createIsFalse(operand: Value, name: String = ""): Value =
        insert(IsFalseInst(operand), name)
    
    // ==================== 内存操作 ====================
    
    fun createAlloca(type: Type, name: String = ""): Value =
        insert(AllocaInst(type, name), name)
    
    fun createLoad(pointer: Value, name: String = ""): Value =
        insert(LoadInst(pointer, name), name)
    
    fun createStore(value: Value, pointer: Value): Instruction =
        insert(StoreInst(value, pointer))
    
    // ==================== PHI节点 ====================
    
    fun createPhi(type: Type, name: String = ""): PhiInst {
        val phi = PhiInst(type, name)
        insert(phi)
        return phi
    }
    
    fun createPhi(
        type: Type,
        incoming: List<Pair<Value, BasicBlock>>,
        name: String = ""
    ): PhiInst {
        val phi = PhiInst(type, name)
        incoming.forEach { (value, block) ->
            phi.addIncoming(value, block)
        }
        insert(phi)
        return phi
    }
    
    // ==================== 选择 ====================
    
    fun createSelect(
        condition: Value,
        trueValue: Value,
        falseValue: Value,
        name: String = ""
    ): Value =
        insert(SelectInst(condition, trueValue, falseValue, name), name)
    
    // ==================== 调用 ====================
    
    fun createCall(
        callee: Value,
        args: List<Value>,
        name: String = ""
    ): Value =
        insert(CallInst(callee, args, name), name)
    
    /**
     * 调用函数（直接传入 SSA Function）
     */
    fun createCall(
        func: SSAFunction,
        args: List<Value>,
        name: String = ""
    ): Value {
        // 创建全局值引用作为被调用者
        val calleeRef = GlobalValue(func.getFunctionType(), func.name, func.isExternal)
        return insert(CallInst(calleeRef, args, name), name)
    }
    
    fun createCallThis(
        callee: Value,
        thisValue: Value,
        args: List<Value>,
        name: String = ""
    ): Value =
        insert(CallThisInst(callee, thisValue, args, name), name)
    
    fun createNew(
        constructor: Value,
        args: List<Value>,
        name: String = ""
    ): Value =
        insert(NewInst(constructor, args, name), name)
    
    fun createCallRuntime(
        runtimeFunc: String,
        args: List<Value>,
        name: String = ""
    ): Value =
        insert(CallRuntimeInst(runtimeFunc, args, name), name)
    
    // ==================== 对象操作 ====================
    
    fun createEmptyObject(name: String = ""): Value =
        insert(CreateEmptyObjectInst(name), name)
    
    fun createEmptyArray(capacity: Int = 0, name: String = ""): Value =
        insert(CreateEmptyArrayInst(capacity, name), name)
    
    fun createGetProperty(obj: Value, key: Value, name: String = ""): Value =
        insert(GetPropertyInst(obj, key, name), name)
    
    fun createSetProperty(obj: Value, key: Value, value: Value): Instruction =
        insert(SetPropertyInst(obj, key, value))
    
    fun createGetElement(array: Value, index: Value, name: String = ""): Value =
        insert(GetElementInst(array, index, name), name)
    
    fun createSetElement(array: Value, index: Value, value: Value): Instruction =
        insert(SetElementInst(array, index, value))
    
    // ==================== 异常 ====================
    
    fun createThrow(exception: Value): Instruction =
        insert(ThrowInst(exception))
    
    // ==================== 类型转换 ====================
    
    fun createTrunc(value: Value, type: Type, name: String = ""): Value =
        insert(TruncInst(value, type, name), name)
    
    fun createZExt(value: Value, type: Type, name: String = ""): Value =
        insert(ZExtInst(value, type, name), name)
    
    fun createSExt(value: Value, type: Type, name: String = ""): Value =
        insert(SExtInst(value, type, name), name)
    
    fun createFPToI(value: Value, type: Type, name: String = ""): Value =
        insert(FPToIInst(value, type, name), name)
    
    fun createUIToFP(value: Value, type: Type, name: String = ""): Value =
        insert(UIToFPInst(value, type, name), name)
    
    fun createSIToFP(value: Value, type: Type, name: String = ""): Value =
        insert(SIToFPInst(value, type, name), name)
    
    fun createBitCast(value: Value, type: Type, name: String = ""): Value =
        insert(BitCastInst(value, type, name), name)
    
    // ==================== 常量 ====================
    
    fun getConstantInt(value: Long, type: Type = i64Type): ConstantInt =
        ConstantInt(value, type)
    
    fun getConstantI32(value: Int): ConstantInt =
        ConstantInt.i32(value)
    
    fun getConstantI64(value: Long): ConstantInt =
        ConstantInt.i64(value)
    
    fun getConstantBool(value: Boolean): ConstantInt =
        ConstantInt.bool(value)
    
    fun getConstantFP(value: Double, type: Type = f64Type): ConstantFP =
        ConstantFP(value, type)
    
    fun getConstantF32(value: Float): ConstantFP =
        ConstantFP.f32(value)
    
    fun getConstantF64(value: Double): ConstantFP =
        ConstantFP.f64(value)
    
    fun getConstantString(value: String): ConstantString =
        ConstantString(value)
    
    fun getNull(): ConstantSpecial = ConstantSpecial.NULL
    fun getUndefined(): ConstantSpecial = ConstantSpecial.UNDEFINED
    fun getNaN(): ConstantSpecial = ConstantSpecial.NAN
    
    // ==================== 内部方法 ====================
    
    private fun insert(inst: Instruction, name: String = ""): Instruction {
        val block = currentBlock ?: throw IllegalStateException("No insertion point")
        
        // 如果块已有终止指令且当前指令也是终止指令，报错
        if (inst.isTerminator() && block.isTerminated()) {
            throw IllegalStateException("Block already has terminator")
        }
        
        // 如果块已有终止指令，在终止指令前插入
        if (block.isTerminated() && !inst.isTerminator()) {
            val terminator = block.terminator()!!
            block.insertBefore(inst, terminator)
        } else {
            block.append(inst)
        }
        
        if (name.isNotEmpty()) {
            inst.name = name
        }
        
        return inst
    }
}

/**
 * IR构建器DSL
 * 
 * 提供更简洁的语法
 */
class IRBuilderDSL(private val builder: IRBuilder) {
    
    operator fun Value.plus(other: Value): Value = builder.createAdd(this, other)
    operator fun Value.minus(other: Value): Value = builder.createSub(this, other)
    operator fun Value.times(other: Value): Value = builder.createMul(this, other)
    operator fun Value.div(other: Value): Value = builder.createDiv(this, other)
    operator fun Value.rem(other: Value): Value = builder.createMod(this, other)
    operator fun Value.unaryMinus(): Value = builder.createNeg(this)
    operator fun Value.not(): Value = builder.createNot(this)
    
    infix fun Value.eq(other: Value): Value = builder.createICmpEQ(this, other)
    infix fun Value.ne(other: Value): Value = builder.createICmpNE(this, other)
    infix fun Value.lt(other: Value): Value = builder.createICmpSLT(this, other)
    infix fun Value.le(other: Value): Value = builder.createICmpSLE(this, other)
    infix fun Value.gt(other: Value): Value = builder.createICmpSGT(this, other)
    infix fun Value.ge(other: Value): Value = builder.createICmpSGE(this, other)
    
    fun Value.load(): Value = builder.createLoad(this)
    fun store(value: Value, ptr: Value) = builder.createStore(value, ptr)
    
    fun ret(value: Value? = null) = builder.createRet(value)
    fun retVoid() = builder.createRetVoid()
    fun br(target: BasicBlock) = builder.createBr(target)
    fun condBr(cond: Value, trueTarget: BasicBlock, falseTarget: BasicBlock) =
        builder.createCondBr(cond, trueTarget, falseTarget)
    
    fun phi(type: Type, vararg incoming: Pair<Value, BasicBlock>) =
        builder.createPhi(type, incoming.toList())
    
    fun select(cond: Value, trueValue: Value, falseValue: Value) =
        builder.createSelect(cond, trueValue, falseValue)
    
    fun call(callee: Value, vararg args: Value) =
        builder.createCall(callee, args.toList())
    
    fun callThis(callee: Value, thisValue: Value, vararg args: Value) =
        builder.createCallThis(callee, thisValue, args.toList())
    
    fun new(constructor: Value, vararg args: Value) =
        builder.createNew(constructor, args.toList())
    
    fun emptyObject() = builder.createEmptyObject()
    fun emptyArray(capacity: Int = 0) = builder.createEmptyArray(capacity)
    fun getProp(obj: Value, key: Value) = builder.createGetProperty(obj, key)
    fun setProp(obj: Value, key: Value, value: Value) =
        builder.createSetProperty(obj, key, value)
    fun getElem(arr: Value, index: Value) = builder.createGetElement(arr, index)
    fun setElem(arr: Value, index: Value, value: Value) =
        builder.createSetElement(arr, index, value)
    
    fun i32(value: Int) = builder.getConstantI32(value)
    fun i64(value: Long) = builder.getConstantI64(value)
    fun f64(value: Double) = builder.getConstantF64(value)
    fun bool(value: Boolean) = builder.getConstantBool(value)
    fun str(value: String) = builder.getConstantString(value)
    fun nullVal() = builder.getNull()
    fun undefined() = builder.getUndefined()
    
    fun block(name: String = ""): BasicBlock = builder.createBlock(name)
    fun insertPoint(block: BasicBlock) = builder.setInsertPoint(block)
    fun currentBlock(): BasicBlock? = builder.currentBlock
    fun currentFunc(): SSAFunction? = builder.currentFunction
}

/**
 * 使用DSL构建IR
 */
inline fun buildIR(block: IRBuilderDSL.() -> Unit): IRBuilder {
    val builder = IRBuilder()
    val dsl = IRBuilderDSL(builder)
    dsl.block()
    return builder
}
