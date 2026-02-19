/**
 * SSA IR (Static Single Assignment Intermediate Representation)
 * 
 * 本项目提供基于LLVM风格的SSA中间表示，用于pandaASM到JavaScript的反编译。
 * 
 * 核心概念：
 * - [Value]: SSA值，是所有可被使用的值的基类
 * - [Instruction]: 指令，继承自Value（因为指令产生值）
 * - [BasicBlock]: 基本块，单入口单出口的指令序列
 * - [Function]: 函数，包含基本块和参数
 * - [Module]: 模块，包含多个函数
 * 
 * SSA形式的特点：
 * 1. 每个变量只被赋值一次
 * 2. 使用[PhiInst]节点合并来自不同路径的值
 * 3. def-use链自动维护
 * 
 * 使用示例：
 * ```kotlin
 * val module = Module("test")
 * val func = module.createFunction("add", i32Type)
 * val a = func.addArgument(i32Type, "a")
 * val b = func.addArgument(i32Type, "b")
 * 
 * val entry = func.createBlock("entry")
 * val builder = IRBuilder(entry)
 * val result = builder.createAdd(a, b, "sum")
 * builder.createRet(result)
 * 
 * // 运行优化
 * val pm = PassManager()
 * pm.addPass(DeadCodeElimination())
 * pm.addPass(ConstantFolding())
 * pm.run(module)
 * ```
 */
package com.orz.reark.core.ssa.ir
