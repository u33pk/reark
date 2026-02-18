package com.orz.reark.reark.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier


data class CodeTab(
    val id: String,
    val title: String,
    val language: String,
    val content: String
)

@Composable
fun CodeDisplayPanel(modifier: Modifier = Modifier) {
    // 外层Tab状态
    var outerSelectedTabIndex by remember { mutableStateOf(0) }
    
    // 内层Tab状态
    var innerSelectedTabIndex by remember { mutableStateOf(0) }
    
    Column(modifier = modifier) {
        // 外层Tab栏 - 显示不同文件
        ImprovedTabRow(
            selectedTabIndex = outerSelectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            tabs = listOf(
                {
                    ImprovedTab(
                        selected = outerSelectedTabIndex == 0,
                        onClick = { outerSelectedTabIndex = 0 },
                        title = "test.pa",
                        icon = Icons.Default.InsertDriveFile
                    )
                }
            ),
            onAddTab = {
                // 这里可以添加新文件Tab
            }
        )
        
        // 内层Tab栏 - 显示PandaASM, JavaScript, CFG
        val innerTabs = remember {
            listOf("PandaASM", "JavaScript", "CFG")
        }
        
        ImprovedTabRow(
            selectedTabIndex = innerSelectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            tabs = innerTabs.mapIndexed { index, title ->
                {
                    ImprovedTab(
                        selected = innerSelectedTabIndex == index,
                        onClick = { innerSelectedTabIndex = index },
                        title = title,
                        icon = when (title) {
                            // TODO: 这里的图标有问题
                            "PandaASM" -> Icons.Default.Code
                            "JavaScript" -> Icons.Default.Code
                            "CFG" -> Icons.Default.Code
                            else -> Icons.Default.InsertDriveFile
                        },
                        showCloseButton = false
                    )
                }
            },
            onAddTab = null  // 禁用添加按钮
        )
        
        // 内容显示区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (innerSelectedTabIndex) {
                0 -> PandaAsmTab(modifier = Modifier.fillMaxSize())
                1 -> JavaScriptTab(modifier = Modifier.fillMaxSize())
                2 -> CfgTab(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier.verticalScroll(scrollState),
        content = content
    )
}