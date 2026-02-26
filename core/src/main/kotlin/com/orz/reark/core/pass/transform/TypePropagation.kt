package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 类型传播与冗余类型转换消除
 *
 * 通过静态分析传播类型信息，消除冗余的类型转换指令
 *
 * 优化场景：
 * 1. TO_NUMERIC 后接算术运算：如果值已确定是数字，删除 TO_NUMERIC
 * 2. 循环变量的类型：参与数字比较和算术运算的变量是数字
 * 3. 常量传播后的类型：常量是数字，复制常量的变量也是数字
 *
 * 示例：
 * ```
 * // 优化前
 * %x = TO_NUMERIC reg_7_phi
 * %y = INC %x
 *
 * // 优化后（reg_7_phi 已知是数字）
 * %y = INC reg_7_phi
 * ```
 */
class TypePropagation : FunctionPass {

    override val name: String = "typeprop"
    override val description: String = "Type Propagation"

    override fun run(function: SSAFunction): PassResult {
        var modified = false

        // 第一遍：收集类型信息
        val knownNumericTypes = mutableSetOf<Value>()

        // 迭代收集类型信息直到不动点（限制最大迭代次数防止死循环）
        var changed = true
        var iterations = 0
        val maxIterations = function.blocks().flatMap { it.instructions() }.count() + 10

        while (changed && iterations < maxIterations) {
            changed = false
            iterations++

            function.instructions().forEach { inst ->
                when (inst) {
                    // 常量是数字
                    is ConstantInt, is ConstantFP -> {
                        if (knownNumericTypes.add(inst)) {
                            changed = true
                        }
                    }

                    // 参与数值比较的操作数是数字
                    is CmpInstruction -> {
                        if (isNumericComparison(inst.opcode)) {
                            if (knownNumericTypes.add(inst.left)) {
                                changed = true
                            }
                            if (knownNumericTypes.add(inst.right)) {
                                changed = true
                            }
                        }
                    }

                    // 算术运算的操作数和结果是数字
                    is AddInst, is SubInst, is MulInst, is DivInst, is ModInst,
                    is IncInst, is DecInst -> {
                        val operands = getArithmeticOperands(inst)
                        operands.forEach { op ->
                            if (knownNumericTypes.add(op)) {
                                changed = true
                            }
                        }
                    }

                    // Copy 指令：如果源是数字，目标也是数字
                    is CopyInst -> {
                        if (inst.source in knownNumericTypes) {
                            if (knownNumericTypes.add(inst)) {
                                changed = true
                            }
                        }
                    }

                    // Phi 节点：如果所有输入都是数字，结果也是数字
                    is PhiInst -> {
                        // 检查是否有至少一个输入是数字（简化处理，避免循环依赖）
                        val hasNumericInput = (0 until inst.incomingCount())
                            .any { i -> inst.getOperand(i) in knownNumericTypes }
                        // 并且没有非数字的输入（常量或已确定类型的值）
                        val allInputsKnownOrNumeric = (0 until inst.incomingCount())
                            .all { i ->
                                val op = inst.getOperand(i)
                                op is Constant || op in knownNumericTypes || op is Argument
                            }
                        if (hasNumericInput && allInputsKnownOrNumeric && knownNumericTypes.add(inst)) {
                            changed = true
                        }
                    }

                    // TO_NUMERIC 的输入如果是数字，TO_NUMERIC 是冗余的
                    is ToNumericInst -> {
                        if (inst.operand in knownNumericTypes) {
                            // 标记为待删除
                            knownNumericTypes.add(inst)
                            changed = true
                        }
                    }
                }
            }
        }

        // 第二遍：消除冗余的 TO_NUMERIC
        function.instructions().toList().forEach { inst ->
            if (inst is ToNumericInst) {
                if (inst.operand in knownNumericTypes) {
                    // TO_NUMERIC 是冗余的，替换为源值
                    inst.replaceAllUsesWith(inst.operand)
                    inst.eraseFromBlock()
                    modified = true
                }
            }
        }

        return PassResult.Success(modified)
    }

    /**
     * 判断比较指令是否是数值比较
     */
    private fun isNumericComparison(opcode: Opcode): Boolean {
        return when (opcode) {
            Opcode.LT, Opcode.LE, Opcode.GT, Opcode.GE -> true
            else -> false
        }
    }

    /**
     * 获取算术指令的操作数
     */
    private fun getArithmeticOperands(inst: Instruction): List<Value> {
        return when (inst) {
            is BinaryInstruction -> listOf(inst.left, inst.right)
            is UnaryInstruction -> listOf(inst.operand)
            else -> emptyList()
        }
    }
}
