package com.orz.reark.reark

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orz.reark.reark.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // 面板宽度状态
    var leftPanelWidth by remember { mutableStateOf(300.dp) }
    var rightPanelWidth by remember { mutableStateOf(400.dp) }
    var consoleHeight by remember { mutableStateOf(200.dp) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部菜单栏
        MenuBar()
        
        // 标题栏
        TopAppBar(
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "应用图标",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("ReArk - 反编译分析工具")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        // 主内容区域
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 主内容区域
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // 左侧：树状代码结构（带边框）
                    BorderedPanel(
                        modifier = Modifier
                            .width(leftPanelWidth)
                            .fillMaxHeight(),
                        showBorder = true
                    ) {
                        CodeTreePanel(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // 左侧分隔线
                    Splitter(
                        isVertical = true,
                        onDrag = { delta ->
                            val newWidth = leftPanelWidth + delta.dp
                            if (newWidth > 150.dp && newWidth < 500.dp) {
                                leftPanelWidth = newWidth
                            }
                        }
                    )
                    
                    // 中间：代码块显示（带边框）
                    BorderedPanel(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        showBorder = true
                    ) {
                        CodeDisplayPanel(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // 右侧分隔线
                    Splitter(
                        isVertical = true,
                        onDrag = { delta ->
                            val newWidth = rightPanelWidth - delta.dp
                            if (newWidth > 250.dp && newWidth < 600.dp) {
                                rightPanelWidth = newWidth
                            }
                        }
                    )
                    
                    // 右侧：Agent聊天界面（带边框）
                    BorderedPanel(
                        modifier = Modifier
                            .width(rightPanelWidth)
                            .fillMaxHeight(),
                        showBorder = true
                    ) {
                        AgentChatPanel(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // 控制台分隔线
                Splitter(
                    isVertical = false,
                    onDrag = { delta ->
                        val newHeight = consoleHeight - delta.dp
                        if (newHeight > 100.dp && newHeight < 400.dp) {
                            consoleHeight = newHeight
                        }
                    }
                )
                
                // 底部：控制台（带边框）
                BorderedPanel(
                    modifier = Modifier
                        .height(consoleHeight)
                        .fillMaxWidth(),
                    showBorder = true
                ) {
                    ConsoleTabs(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
