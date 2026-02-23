/**
 * PandaASM 字节码后端
 * 
 * 提供将 PandaVM 字节码转换为 SSA IR 的功能。
 * 
 * 核心组件：
 * 
 * 1. **PandaAsmOpcodes** ([PandaAsmOpcodes])
 *    - 定义完整的 PandaASM 指令集操作码
 *    - 包括标准指令、Wide 前缀、Deprecated 前缀、Throw 前缀和 CallRuntime 前缀
 *    - 提供指令格式信息
 * 
 * 2. **InstructionMapping** ([InstructionMapping])
 *    - PandaASM 指令到 IR Opcode 的映射表
 *    - 提供指令分类、副作用分析和分支条件类型
 * 
 * 3. **RegisterToSSAMapper** ([RegisterToSSAMapper], [SSAConstructionContext])
 *    - 将 PandaASM 虚拟寄存器映射到 SSA 值
 *    - 自动生成 PHI 节点处理控制流合并
 *    - 处理累加器模型的显式化
 * 
 * 4. **PandaAsmParser** ([PandaAsmParser])
 *    - 解析 PandaASM 字节码为结构化指令
 *    - 支持多种操作数格式（立即数、寄存器、ID等）
 * 
 * 5. **BytecodeToIRConverter** ([BytecodeToIRConverter])
 *    - 主转换器，协调各组件完成字节码到 IR 的转换
 *    - 位于 converter 子包中
 * 
 * 子包：
 * 
 * - **converter**: 转换器实现
 *   - [ControlFlowAnalyzer]: 控制流分析
 *   - [StandardInstructionConverter]: 标准指令转换
 *   - [PrefixInstructionConverters]: 前缀指令转换
 * 
 * 使用示例：
 * ```kotlin
 * val module = Module("test")
 * val converter = BytecodeToIRConverter(module)
 * val result = converter.convert("func", bytecode, paramCount = 2)
 * ```
 */
package com.orz.reark.core.backend
