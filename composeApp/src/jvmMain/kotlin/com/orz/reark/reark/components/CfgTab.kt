package com.orz.reark.reark.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset

@Composable
fun CfgTab(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Text(
            text = "控制流图 (CFG)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 流程图节点数据 - 简化为只显示起始地址和汇编代码
        val nodes = remember {
            listOf(
                FlowchartNode(
                    id = "0x1000",
                    address = "0x1000",
                    instructions = listOf(
                        "mov eax, 0x1",
                        "cmp eax, 0x1",
                        "test eax, eax"
                    ),
                    nextBlock = "0x1008",
                    jumpTarget = null
                ),
                FlowchartNode(
                    id = "0x1008",
                    address = "0x1008",
                    instructions = listOf(
                        "je 0x1020",
                        "mov ebx, 0x2",
                        "add ebx, 0x10"
                    ),
                    nextBlock = "0x1010",
                    jumpTarget = "0x1020"
                ),
                FlowchartNode(
                    id = "0x1010",
                    address = "0x1010",
                    instructions = listOf(
                        "add ebx, eax",
                        "mov ecx, ebx",
                        "shl ecx, 2",
                        "mov [mem], ecx"
                    ),
                    nextBlock = "0x1018",
                    jumpTarget = null
                ),
                FlowchartNode(
                    id = "0x1018",
                    address = "0x1018",
                    instructions = listOf(
                        "jmp 0x1030",
                        "nop"
                    ),
                    nextBlock = null,
                    jumpTarget = "0x1030"
                ),
                FlowchartNode(
                    id = "0x1020",
                    address = "0x1020",
                    instructions = listOf(
                        "mov eax, 0x3",
                        "sub eax, ecx",
                        "mul edx",
                        "mov result, eax"
                    ),
                    nextBlock = "0x1028",
                    jumpTarget = null
                ),
                FlowchartNode(
                    id = "0x1028",
                    address = "0x1028",
                    instructions = listOf(
                        "test eax, eax",
                        "jle 0x1040",
                        "cmp eax, 0x100"
                    ),
                    nextBlock = "0x1030",
                    jumpTarget = "0x1040"
                ),
                FlowchartNode(
                    id = "0x1030",
                    address = "0x1030",
                    instructions = listOf(
                        "push eax",
                        "push ebx",
                        "push ecx",
                        "call 0x1050",
                        "add esp, 0xC"
                    ),
                    nextBlock = "0x1038",
                    jumpTarget = "0x1050"
                ),
                FlowchartNode(
                    id = "0x1038",
                    address = "0x1038",
                    instructions = listOf(
                        "pop ebx",
                        "pop ecx",
                        "ret",
                        "nop"
                    ),
                    nextBlock = null,
                    jumpTarget = null
                ),
                FlowchartNode(
                    id = "0x1040",
                    address = "0x1040",
                    instructions = listOf(
                        "xor eax, eax",
                        "inc eax",
                        "mov [counter], eax"
                    ),
                    nextBlock = "0x1048",
                    jumpTarget = null
                ),
                FlowchartNode(
                    id = "0x1048",
                    address = "0x1048",
                    instructions = listOf(
                        "jmp 0x1030",
                        "nop",
                        "nop"
                    ),
                    nextBlock = null,
                    jumpTarget = "0x1030"
                ),
                FlowchartNode(
                    id = "0x1050",
                    address = "0x1050",
                    instructions = listOf(
                        "mov eax, [esp+4]",
                        "add eax, eax",
                        "mov edx, eax",
                        "shl edx, 3",
                        "or eax, edx",
                        "ret"
                    ),
                    nextBlock = "0x1058",
                    jumpTarget = null
                ),
                FlowchartNode(
                    id = "0x1058",
                    address = "0x1058",
                    instructions = listOf(
                        "ret 0x4",
                        "nop",
                        "nop",
                        "nop"
                    ),
                    nextBlock = null,
                    jumpTarget = null
                )
            )
        }

        // 流程图连接数据
        val connections = remember(nodes) {
            nodes.flatMap { node ->
                buildList {
                    node.nextBlock?.let {
                        add(FlowchartConnection(node.id, it, false))
                    }
                    node.jumpTarget?.let {
                        add(FlowchartConnection(node.id, it, true))
                    }
                }
            }
        }

        // 显示流程图
        Flowchart(
            modifier = Modifier
                .fillMaxSize(),
            nodes = nodes,
            connections = connections
        )
    }
}