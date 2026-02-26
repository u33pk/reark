package com.orz.reark.core.pass.transform

import com.orz.reark.core.ir.*
import com.orz.reark.core.pass.FunctionPass
import com.orz.reark.core.pass.PassResult
import com.orz.reark.core.ir.Function as SSAFunction

/**
 * 冗余 TO_NUMERIC 消除
 * 
 * 如果 TO_NUMERIC 的输入已经是数字类型，可以消除该指令
 */
class ToNumericElimination : FunctionPass {
    
    override val name: String = "tonumericopt"
    override val description: String = "TO_NUMERIC Elimination"
    
    override fun run(function: SSAFunction): PassResult {
        var modified = false
        
        function.instructions().toList().forEach { inst ->
            if (inst is ToNumericInst) {
                val operand = inst.operand
                // 如果输入已经是数字类型，可以消除 TO_NUMERIC
                if (operand.type.isFloatingPoint() || operand.type.isInteger()) {
                    inst.replaceAllUsesWith(operand)
                    inst.eraseFromBlock()
                    modified = true
                }
            }
        }
        
        return PassResult.Success(modified)
    }
}